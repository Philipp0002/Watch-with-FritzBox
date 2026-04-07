package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

public class GlideUtils {

    public static RequestBuilder<Drawable> multiRequestBuilder(Context context, List<String> urls, RequestBuilderCustomizer customizer) {
        RequestBuilder<Drawable> drawableRequestBuilder = null;
        for(int i = urls.size() - 1; i >= 0; i--) {
            String url = urls.get(i);
            if(drawableRequestBuilder == null) {
                drawableRequestBuilder = Glide.with(context)
                        .load(Uri.parse(url));
            } else {
                drawableRequestBuilder = Glide.with(context)
                        .load(Uri.parse(url))
                        .error(drawableRequestBuilder);
            }

            if(customizer != null) {
                drawableRequestBuilder = customizer.customize(drawableRequestBuilder);
            }
        }
        return drawableRequestBuilder;
    }

    public interface RequestBuilderCustomizer {
        RequestBuilder<Drawable> customize(RequestBuilder<Drawable> requestBuilder);
    }
}
