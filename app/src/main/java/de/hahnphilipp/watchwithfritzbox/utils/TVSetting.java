package de.hahnphilipp.watchwithfritzbox.utils;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;

public class TVSetting {

    public String name;
    public @DrawableRes int drawableId;
    public Runnable onClick;
    public boolean bigLayout = true;

    public TVSetting(String name, @DrawableRes int drawableId, Runnable onClick, boolean bigLayout) {
        this.name = name;
        this.drawableId = drawableId;
        this.onClick = onClick;
        this.bigLayout = bigLayout;
    }
}
