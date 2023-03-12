package de.hahnphilipp.watchwithfritzbox.utils;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

public class AnimationUtils {

    public static void scaleView(View v, float startScaleX, float endScaleX, float startScaleY, float endScaleY, long durationMillis) {
        Animation anim = new ScaleAnimation(
                startScaleX, endScaleX, // Start and end values for the X axis scaling
                startScaleY, endScaleY, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
        anim.setFillAfter(true); // Needed to keep the result of the animation
        anim.setFillEnabled(true);
        anim.setStartOffset(1);
        anim.setDuration(durationMillis);
        v.startAnimation(anim);
    }

}
