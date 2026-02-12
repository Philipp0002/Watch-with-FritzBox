package de.hahnphilipp.watchwithfritzbox.utils;

import androidx.annotation.DrawableRes;

public class TVSetting {

    public String name;
    public String subtitle;
    public NavigationIcon navigationIcon;
    public @DrawableRes Integer drawableId;
    public Runnable onClick;

    public TVSetting(String name, String subtitle, NavigationIcon navigationIcon, @DrawableRes Integer drawableId, Runnable onClick) {
        this.name = name;
        this.subtitle = subtitle;
        this.drawableId = drawableId;
        this.onClick = onClick;
        this.navigationIcon = navigationIcon;
    }

    public enum NavigationIcon {
        NONE, CHEVRON, RADIO_SELECTED, RADIO_UNSELECTED;

        public static NavigationIcon selected(boolean selected) {
            return selected ? RADIO_SELECTED : RADIO_UNSELECTED;
        }
    }
}
