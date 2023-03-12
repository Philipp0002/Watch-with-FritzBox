package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

public class AssetUtils {

    public static String getStringFromAsset(Context context, String fileName) throws IOException {
        InputStream is = context.getAssets().open(fileName);
        int size = is.available();

        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();

        String str = new String(buffer);
        return str;
    }

    public static InputStream getFileFromAsset(Context context, String fileName) throws IOException {
        InputStream is = context.getAssets().open(fileName);
        return is;
    }
}
