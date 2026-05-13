package de.hahnphilipp.watchwithfritzbox.utils;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;

import androidx.annotation.RequiresApi;

/**
 * Corner-Logo Advertisement Detector (Rolling Window Variance)
 */
public class LogoDetector {

    private static final String TAG = "LogoDetector";

    // ── Tunables ──────────────────────────────────────────────────────────────

    /**
     * Ms between two PixelCopy-Captures.
     */
    private static final long ANALYZE_INTERVAL_MS = 600;

    /**
     * Thumbnail resolution. Higher = more accurate, but more CPU usage in getPixels().
     */
    private static final int THUMB_W = 854;
    private static final int THUMB_H = 480;

    /**
     * ROI size as portion of thumbnail.
     */
    private static final float ROI_FRAC_W = 0.12f;
    private static final float ROI_FRAC_H = 0.15f;

    /**
     * Distance of ROI from corner (of thumbnail).
     */
    private static final int ROI_MARGIN = 4;

    /**
     * Amount of frames in rolling buffer.
     * e.g. ANALYZE_INTERVAL_MS=600 and WINDOW_SIZE=20 means approx. 12 secs.
     */
    private static final int WINDOW_SIZE = 20;

    /**
     * Variance threshold (Luma 0-255, quadratic).
     * Pixels below set variance are considered stable (hotspot).
     * strict:      30  (only same pixel)
     * normal:       80
     * tolerant:    150  (similar pixels)
     */
    private static final float VARIANCE_HOTSPOT_THR = 80f;

    /**
     * Minimal amount of stable pixels in ROI to consider logo present.
     * 0.0–1.0.
     * Tiny logo: 0.15–0.20; huge logo: 0.25–0.35.
     */
    private static final float HOTSPOT_DENSITY_THR = 0.18f;

    /**
     * Minimum amount of frames in rolling buffer before making decision.
     */
    private static final int MIN_FRAMES_BEFORE_DECIDE = 6;

    /**
     * Consecutive frames with logo until state PROGRAMME.
     */
    private static final int CONFIRM_PRESENT = 4;

    /**
     * Consecutive frames without logo until state ADVERTISEMENT.
     */
    private static final int CONFIRM_ABSENT = 8;


    public interface Listener {
        void onAdvertisementStateChanged(boolean isAdvertisement);
    }

    private SurfaceView mSurfaceView;
    private final Listener mListener;
    private final Handler mMainHandler;

    private HandlerThread mAnalysisThread;
    private Handler mAnalysisHandler;

    private boolean mRunning;
    private Bitmap mBitmap;

    // ROI definition [left, top, right, bottom] in coordinate system of thumbnail
    private int[][] mRois;

    /**
     * Rolling buffer: mWindow[roiIndex][frameSlot][pixelIndex] = Luma value.
     */
    private float[][][] mWindow;
    private int mWindowHead; // next slot to write
    private int mWindowFill; // used slots (0..WINDOW_SIZE)

    private enum State {UNDECIDED, PROGRAMME, ADVERTISEMENT}

    private State mState = State.UNDECIDED;
    private int mConfirmPresentStreak;
    private int mConfirmAbsentStreak;

    public LogoDetector(SurfaceView surfaceView,
                        Handler mainHandler,
                        Listener listener) {
        mSurfaceView = surfaceView;
        mMainHandler = mainHandler;
        mListener = listener;
        buildRois();
    }

    public synchronized void start() {
        if (mRunning) return;
        mRunning = true;
        mAnalysisThread = new HandlerThread("logo-detector");
        mAnalysisThread.start();
        mAnalysisHandler = new Handler(mAnalysisThread.getLooper());
        mAnalysisHandler.post(mAnalysisLoop);
        Log.i(TAG, "LogoDetector started  window=" + WINDOW_SIZE
                + " × " + ANALYZE_INTERVAL_MS + "ms = "
                + (WINDOW_SIZE * ANALYZE_INTERVAL_MS / 1000) + "s");
    }

    public synchronized void stop() {
        if (!mRunning) return;
        mRunning = false;
        if (mAnalysisHandler != null)
            mAnalysisHandler.removeCallbacksAndMessages(null);
        if (mAnalysisThread != null) {
            mAnalysisThread.quitSafely();
            mAnalysisThread = null;
        }
        Log.i(TAG, "LogoDetector stopped");
    }

    /**
     * Reset rolling buffer + state.
     */
    public synchronized void resetWindow() {
        mWindow = null;
        mWindowHead = 0;
        mWindowFill = 0;
        mState = State.UNDECIDED;
        mConfirmPresentStreak = 0;
        mConfirmAbsentStreak = 0;
        Log.i(TAG, "LogoDetector: window reset");
    }

    /**
     * Limit active corners when channels logo position is known.
     * Also resets rolling buffer.
     */
    public synchronized void setActiveCorners(boolean topLeft, boolean topRight,
                                              boolean botLeft, boolean botRight) {
        boolean[] mask = {topLeft, topRight, botLeft, botRight};
        int rw = Math.max(2, (int) (THUMB_W * ROI_FRAC_W));
        int rh = Math.max(2, (int) (THUMB_H * ROI_FRAC_H));
        int mg = ROI_MARGIN;
        int[][] all = {
                {mg, mg, mg + rw, mg + rh},
                {THUMB_W - mg - rw, mg, THUMB_W - mg, mg + rh},
                {mg, THUMB_H - mg - rh, mg + rw, THUMB_H - mg},
                {THUMB_W - mg - rw, THUMB_H - mg - rh, THUMB_W - mg, THUMB_H - mg},
        };
        int count = 0;
        for (boolean b : mask) if (b) count++;
        if (count == 0) {
            buildRois();
            resetWindow();
            return;
        }
        mRois = new int[count][];
        int j = 0;
        for (int i = 0; i < 4; i++) if (mask[i]) mRois[j++] = all[i];
        resetWindow();
    }

    private void buildRois() {
        int rw = Math.max(2, (int) (THUMB_W * ROI_FRAC_W));
        int rh = Math.max(2, (int) (THUMB_H * ROI_FRAC_H));
        int mg = ROI_MARGIN;
        mRois = new int[][]{
                {mg, mg, mg + rw, mg + rh},
                {THUMB_W - mg - rw, mg, THUMB_W - mg, mg + rh},
                {mg, THUMB_H - mg - rh, mg + rw, THUMB_H - mg},
                {THUMB_W - mg - rw, THUMB_H - mg - rh, THUMB_W - mg, THUMB_H - mg},
        };
    }

    private final Runnable mAnalysisLoop = new Runnable() {
        @Override
        public void run() {
            if (!mRunning) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mMainHandler.post(mCaptureOnMainThread);
            }
            if (mRunning && mAnalysisHandler != null) {
                mAnalysisHandler.postDelayed(this, ANALYZE_INTERVAL_MS);
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    private final Runnable mCaptureOnMainThread = () -> {
        if (!mRunning) return;
        if (mSurfaceView.getWidth() <= 0 || mSurfaceView.getHeight() <= 0) return;

        final Bitmap target;
        if (mBitmap != null && !mBitmap.isRecycled()
                && mBitmap.getWidth() == THUMB_W
                && mBitmap.getHeight() == THUMB_H) {
            target = mBitmap;
        } else {
            target = Bitmap.createBitmap(THUMB_W, THUMB_H, Bitmap.Config.ARGB_8888);
            mBitmap = target;
        }

        try {
            PixelCopy.request(
                    mSurfaceView, target,
                    result -> {
                        if (result == PixelCopy.SUCCESS) analyseFrame(target);
                        else Log.w(TAG, "PixelCopy result=" + result);
                    },
                    mAnalysisHandler  // Callback auf Background-Thread
            );
        } catch (Exception e) {
            Log.w(TAG, "PixelCopy.request() threw: " + e.getMessage());
        }
    };

    private float[] extractRoiLuma(Bitmap bmp, int[] roi) {
        int l = roi[0], t = roi[1], r = roi[2], b = roi[3];
        int w = r - l, h = b - t;
        int[] pixels = new int[w * h];
        bmp.getPixels(pixels, 0, w, l, t, w, h);
        float[] luma = new float[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            luma[i] = 0.299f * ((c >> 16) & 0xFF)
                    + 0.587f * ((c >> 8) & 0xFF)
                    + 0.114f * (c & 0xFF);
        }
        return luma;
    }

    /**
     * Calculates hotspot density of a ROI across all frames in rolling buffer.
     * <p>
     * Variance via Computational formula for variance: Var(X) = E[X^2] − E[X]^2
     * Stable for Luma values 0-255 and WINDOW_SIZE <= 60.
     *
     * @param slices slices[frameIndex][pixelIndex] – Luma value per frame
     * @param fill   Amount of valid entries in slices
     * @return Portion of stable pixels (0.0–1.0)
     */
    private float computeHotspotDensity(float[][] slices, int fill) {
        if (fill < 2) return 0f;

        int pixelCount = slices[0].length;
        int hotspots = 0;

        for (int p = 0; p < pixelCount; p++) {
            float sumX = 0f;
            float sumX2 = 0f;
            for (int f = 0; f < fill; f++) {
                float v = slices[f][p];
                sumX += v;
                sumX2 += v * v;
            }
            float mean = sumX / fill;
            float variance = sumX2 / fill - mean * mean;
            if (variance < VARIANCE_HOTSPOT_THR) hotspots++;
        }

        return (float) hotspots / pixelCount;
    }

    /**
     * Assembles rolling buffer slices for a ROI chronologically.
     * (oldest frame first).
     */
    private float[][] assembleSlices(int roiIndex) {
        float[][] slices = new float[mWindowFill][];
        // Buffer full? -> point to oldest slot
        int oldest = (mWindowFill < WINDOW_SIZE) ? 0 : mWindowHead;
        for (int f = 0; f < mWindowFill; f++) {
            slices[f] = mWindow[roiIndex][(oldest + f) % WINDOW_SIZE];
        }
        return slices;
    }

    private synchronized void analyseFrame(Bitmap bmp) {
        if (!mRunning) return;

        int nRois = mRois.length;

        // Create rolling puffer lazy
        if (mWindow == null) {
            mWindow = new float[nRois][WINDOW_SIZE][];
        }

        // Put current frame into rolling buffer
        for (int i = 0; i < nRois; i++) {
            mWindow[i][mWindowHead] = extractRoiLuma(bmp, mRois[i]);
        }
        mWindowHead = (mWindowHead + 1) % WINDOW_SIZE;
        if (mWindowFill < WINDOW_SIZE) mWindowFill++;

        // Too few data to make decision
        if (mWindowFill < MIN_FRAMES_BEFORE_DECIDE) return;

        // Calc hotspot density for all ROIs; best corner wins
        float bestDensity = 0f;
        int bestRoi = 0;
        for (int i = 0; i < nRois; i++) {
            float density = computeHotspotDensity(assembleSlices(i), mWindowFill);
            if (density > bestDensity) {
                bestDensity = density;
                bestRoi = i;
            }
        }

        boolean logoDetected = (bestDensity >= HOTSPOT_DENSITY_THR);
        State oldState = mState;

        if (logoDetected) {
            mConfirmPresentStreak++;
            mConfirmAbsentStreak = 0;
        } else {
            mConfirmAbsentStreak++;
            mConfirmPresentStreak = 0;
        }

        if (mState != State.PROGRAMME && mConfirmPresentStreak >= CONFIRM_PRESENT) {
            mState = State.PROGRAMME;
        } else if (mState != State.ADVERTISEMENT && mConfirmAbsentStreak >= CONFIRM_ABSENT) {
            mState = State.ADVERTISEMENT;
        }

        Log.d(TAG, String.format(
                "roi=%d  density=%.2f  thr=%.2f  logo=%b  +%d/-%d  fill=%d/%d  → %s",
                bestRoi, bestDensity, HOTSPOT_DENSITY_THR, logoDetected,
                mConfirmPresentStreak, mConfirmAbsentStreak,
                mWindowFill, WINDOW_SIZE, mState));

        if (mState != oldState) {
            boolean isAd = (mState == State.ADVERTISEMENT);
            Log.i(TAG, "LogoDetector → " + mState
                    + "  (bestDensity=" + String.format("%.2f", bestDensity) + ")");
            fireCallback(isAd);
        }
    }

    private void fireCallback(boolean isAdvertisement) {
        mMainHandler.post(() -> {
            if (mListener != null)
                mListener.onAdvertisementStateChanged(isAdvertisement);
        });
    }
}