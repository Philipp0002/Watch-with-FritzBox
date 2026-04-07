package de.hahnphilipp.watchwithfritzbox.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.caverock.androidsvg.SVG;

import java.io.File;

public class SvgEncoder implements ResourceEncoder<SVG> {

    private BitmapEncoder bitmapEncoder;
    private BitmapPool bitmapPool;

    public SvgEncoder(ArrayPool arrayPool, BitmapPool bitmapPool) {
        bitmapEncoder = new BitmapEncoder(arrayPool);
        this.bitmapPool = bitmapPool;
    }

    @NonNull
    @Override
    public EncodeStrategy getEncodeStrategy(@NonNull Options options) {
        return bitmapEncoder.getEncodeStrategy(options);
    }

    @Override
    public boolean encode(@NonNull Resource<SVG> data, @NonNull File file, @NonNull Options options) {
        SVG svg = data.get();

        // Natürliche Größe als Fallback
        Picture picture = svg.renderToPicture();
        int width = picture.getWidth();
        int height = picture.getHeight();

        // Falls SVG keine intrinsische Größe hat (width/height = 0), Fallback setzen
        if (width <= 0) width = 512;
        if (height <= 0) height = 512;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        picture.draw(canvas);

        BitmapResource bitmapResource = BitmapResource.obtain(bitmap, bitmapPool);
        if (bitmapResource == null) return false;

        return bitmapEncoder.encode(bitmapResource, file, options);
    }
}
