package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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
