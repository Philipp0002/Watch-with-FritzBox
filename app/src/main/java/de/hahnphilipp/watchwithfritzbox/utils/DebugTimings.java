package de.hahnphilipp.watchwithfritzbox.utils;

import android.util.Log;

public class DebugTimings {

    private static long lastTime = -1;

    public static void logTiming(String label) {
        long currentTime = System.currentTimeMillis();
        if (lastTime != -1) {
            long delta = currentTime - lastTime;
            Log.d("Timing", "Timing " + label + ": " + delta + " ms");
        } else {
            Log.d("Timing","Timing " + label + ": start");
        }
        lastTime = currentTime;
    }

    public static void resetTiming() {
        lastTime = -1;
    }

}
