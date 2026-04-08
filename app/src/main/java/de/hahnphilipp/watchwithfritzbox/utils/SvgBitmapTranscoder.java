package de.hahnphilipp.watchwithfritzbox.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.caverock.androidsvg.SVG;

public class SvgBitmapTranscoder implements ResourceTranscoder<SVG, Bitmap> {

    private final BitmapPool bitmapPool;

    public SvgBitmapTranscoder(BitmapPool bitmapPool) {
        this.bitmapPool = bitmapPool;
    }

    @Override
    public Resource<Bitmap> transcode(
            @NonNull Resource<SVG> toTranscode, @NonNull Options options) {
        SVG svg = toTranscode.get();

        int w = (int) svg.getDocumentWidth();
        int h = (int) svg.getDocumentHeight();

        if (w <= 0 || h <= 0) {
            RectF viewBox = svg.getDocumentViewBox();
            if (viewBox != null) {
                w = (int) viewBox.width();
                h = (int) viewBox.height();
            }
        }

        if (w <= 0) w = 512;
        if (h <= 0) h = 512;

        Picture picture = svg.renderToPicture(w, h);
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        new Canvas(bitmap).drawPicture(picture);
        return BitmapResource.obtain(bitmap, bitmapPool);
    }
}
