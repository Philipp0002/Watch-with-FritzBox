package de.hahnphilipp.watchwithfritzbox.utils;

import androidx.annotation.DrawableRes;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TVSetting tvSetting = (TVSetting) o;
        return Objects.equals(name, tvSetting.name) && Objects.equals(subtitle, tvSetting.subtitle) && navigationIcon == tvSetting.navigationIcon && Objects.equals(drawableId, tvSetting.drawableId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, subtitle, navigationIcon, drawableId);
    }

    public enum NavigationIcon {
        NONE, CHEVRON, RADIO_SELECTED, RADIO_UNSELECTED;

        public static NavigationIcon selected(boolean selected) {
            return selected ? RADIO_SELECTED : RADIO_UNSELECTED;
        }
    }
}
