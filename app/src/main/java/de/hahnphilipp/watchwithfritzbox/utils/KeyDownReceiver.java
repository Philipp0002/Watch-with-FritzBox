package de.hahnphilipp.watchwithfritzbox.utils;

import android.view.KeyEvent;

public interface KeyDownReceiver {
    boolean onKeyUp(int keyCode, KeyEvent event);
    boolean onKeyDownLong(int keyCode, KeyEvent event);
}
