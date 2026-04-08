package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.PictureDrawable;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.caverock.androidsvg.SVG;

import java.io.InputStream;

@GlideModule
public class SvgModule extends AppGlideModule {
    @Override
    public void registerComponents(
            @NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry
                .register(SVG.class, Bitmap.class, new SvgBitmapTranscoder(glide.getBitmapPool()))
                .append(SVG.class, new SvgEncoder(glide.getArrayPool(), glide.getBitmapPool()))
                .append(InputStream.class, SVG.class, new SvgDecoder());
    }

    // Disable manifest parsing to avoid adding similar modules twice.
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
