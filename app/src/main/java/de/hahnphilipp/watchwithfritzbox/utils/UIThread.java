package de.hahnphilipp.watchwithfritzbox.utils;

import android.os.Handler;
import android.os.Looper;

public class UIThread {
    public static void run(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
}
