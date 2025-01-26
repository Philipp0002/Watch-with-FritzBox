package de.hahnphilipp.watchwithfritzbox.utils;

import android.view.KeyEvent;

public interface KeyDownReceiver {
    boolean onKeyDown(int keyCode, KeyEvent event);
}
