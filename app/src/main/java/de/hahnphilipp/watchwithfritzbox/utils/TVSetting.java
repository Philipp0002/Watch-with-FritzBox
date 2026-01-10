package de.hahnphilipp.watchwithfritzbox.utils;

import androidx.annotation.DrawableRes;

public class TVSetting {

    public String name;
    public @DrawableRes Integer drawableId;
    public Runnable onClick;
    public boolean bigLayout = true;

    public TVSetting(String name, @DrawableRes Integer drawableId, Runnable onClick, boolean bigLayout) {
        this.name = name;
        this.drawableId = drawableId;
        this.onClick = onClick;
        this.bigLayout = bigLayout;
    }
}
