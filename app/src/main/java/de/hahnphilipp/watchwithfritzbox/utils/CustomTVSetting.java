package de.hahnphilipp.watchwithfritzbox.utils;

import android.view.View;

import androidx.annotation.LayoutRes;

public class CustomTVSetting extends TVSetting {

    public @LayoutRes int customLayoutRes;
    public CustomTVSettingLayoutCallback layoutCallback;

    public CustomTVSetting(@LayoutRes int customLayoutRes, CustomTVSettingLayoutCallback layoutCallback) {
        super(null, null, null,null, null);
        this.customLayoutRes = customLayoutRes;
        this.layoutCallback = layoutCallback;
    }

    public interface CustomTVSettingLayoutCallback {
        void onBindView(View view);
    }
}
