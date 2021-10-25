package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class AssetUtils {

    public static String getStringFromAsset(Context context, String fileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream is = context.getResources().getAssets().open(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8 ));
        String str;
        while ((str = br.readLine()) != null) {
            sb.append(str);
        }
        br.close();
        return str;
    }
}
