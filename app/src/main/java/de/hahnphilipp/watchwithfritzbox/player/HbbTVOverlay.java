package de.hahnphilipp.watchwithfritzbox.player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.webkit.WebViewAssetLoader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.util.Hex;

import org.videolan.libvlc.MediaPlayer;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.hbbtv.AitApplication;
import de.hahnphilipp.watchwithfritzbox.hbbtv.HbbTVApplication;
import de.hahnphilipp.watchwithfritzbox.hbbtv.HbbTVChannel;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;
import de.hahnphilipp.watchwithfritzbox.utils.KeyDownReceiver;

public class HbbTVOverlay extends Fragment implements KeyDownReceiver {

    public TVPlayerActivity context;

    private WebView webView;
    private HbbTVChannel hbbTvChannelInfo = new HbbTVChannel();

    private View overlayExtraKeys;
    private View extraKeyRed;
    private View extraKeyBlue;
    private View extraKeyYellow;
    private View extraKeyGreen;

    private List<HbbTVApplication> hbbTvApplications;
    private HbbTVApplication currentHbbTvApplication;

    HashMap<HbbTVKeyTypes, Boolean> keyTypesFocusable = new HashMap<>(
            Map.ofEntries(
                    Map.entry(HbbTVKeyTypes.RED, false),
                    Map.entry(HbbTVKeyTypes.GREEN, false),
                    Map.entry(HbbTVKeyTypes.YELLOW, false),
                    Map.entry(HbbTVKeyTypes.BLUE, false),
                    Map.entry(HbbTVKeyTypes.NAVIGATION, false),
                    Map.entry(HbbTVKeyTypes.VCR, false),
                    Map.entry(HbbTVKeyTypes.SCROLL, false),
                    Map.entry(HbbTVKeyTypes.INFO, false),
                    Map.entry(HbbTVKeyTypes.NUMERIC, false),
                    Map.entry(HbbTVKeyTypes.ALPHA, false),
                    Map.entry(HbbTVKeyTypes.OTHER, false)
            )
    );


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.overlay_hbbtv, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        overlayExtraKeys = view.findViewById(R.id.overlay_extra_keys);
        webView = view.findViewById(R.id.webView);
        webView.setEnabled(false);

        extraKeyRed = view.findViewById(R.id.extra_key_red);
        extraKeyRed.setOnClickListener(view1 -> triggerColor(KeyEvent.KEYCODE_PROG_RED));
        extraKeyBlue = view.findViewById(R.id.extra_key_blue);
        extraKeyBlue.setOnClickListener(view1 -> triggerColor(KeyEvent.KEYCODE_PROG_BLUE));
        extraKeyYellow = view.findViewById(R.id.extra_key_yellow);
        extraKeyYellow.setOnClickListener(view1 -> triggerColor(KeyEvent.KEYCODE_PROG_YELLOW));
        extraKeyGreen = view.findViewById(R.id.extra_key_green);
        extraKeyGreen.setOnClickListener(view1 -> triggerColor(KeyEvent.KEYCODE_PROG_GREEN));

        initializeHbbTv();
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(currentHbbTvApplication == null) {
            return false;
        }
        if (event.getAction() == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_BACK && overlayExtraKeys.getVisibility() == View.VISIBLE) {
                overlayExtraKeys.setVisibility(View.GONE);
                return true;
            }

            if(overlayExtraKeys.getVisibility() == View.VISIBLE) {
                return true;
            }

            HbbTVKeyTypes type = HbbTVKeycodeMappings.getHbbTvKeyTypeByKeyCode(keyCode);
            if(type == null) {
                return false;
            }
            char c = (char) event.getUnicodeChar();
            if (Character.isAlphabetic(c) && keyTypesFocusable.get(HbbTVKeyTypes.ALPHA)) {
                String key = "" + c;
                key = key.replace("'", "\\'");
                webView.evaluateJavascript("document.dispatchEvent(new KeyboardEvent('keydown',{'key':'" + key + "', 'bubbles': true, 'cancelable': true}));", null);
                return true;
            }

            if (keyTypesFocusable.get(type)) {
                String jsKeyCode = "window[\"VK_" + HbbTVKeycodeMappings.getHbbTvKeyCodeByKeyCode(keyCode) + "\"]";
                webView.evaluateJavascript("document.dispatchEvent(new KeyboardEvent('keydown',{'keyCode':" + jsKeyCode + ", 'bubbles': true, 'cancelable': true}));", null);
                return true;
            }

        }

        return overlayExtraKeys.getVisibility() == View.VISIBLE;
    }

    @Override
    public boolean onKeyDownLong(int keyCode, KeyEvent event) {
        if(currentHbbTvApplication != null) {
            overlayExtraKeys.setVisibility(View.VISIBLE);
            extraKeyRed.requestFocus();
        }
        return true;
    }

    public void launchHbbTVApp(HbbTVApplication app, Integer serviceId, Integer networkId, Integer originalNetworkId, Integer tsId) {
        if(networkId == null || originalNetworkId == null || serviceId == null || tsId == null) {
            Log.w("HBBTV", "Cannot launch HbbTV app due to missing identifiers: serviceId=" + serviceId + ", networkId=" + networkId + ", originalNetworkId=" + originalNetworkId + ", tsId=" + tsId);
            return;
        }
        if (app.httpDescriptor != null && app.simpleApplicationLocationDescriptor != null) {
            String url = app.httpDescriptor.url + app.simpleApplicationLocationDescriptor.initialPath;
            if (url.equals(webView.getUrl())
                    && hbbTvChannelInfo.sid == serviceId
                    && hbbTvChannelInfo.tsid == tsId
                    && hbbTvChannelInfo.onid == originalNetworkId
                    && hbbTvChannelInfo.nid == networkId) {
                return;
            }
            hbbTvChannelInfo.sid = serviceId;
            hbbTvChannelInfo.nid = networkId;
            hbbTvChannelInfo.onid = originalNetworkId;
            hbbTvChannelInfo.tsid = tsId;
            currentHbbTvApplication = app;
            webView.loadUrl(url);
        }
    }

    public void processHbbTvInfo(MediaPlayer.CommonDescriptors commonDescriptors) {
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        boolean allowHbbTV = sp.getBoolean("setting_enable_hbbtv", false);
        try {
            byte[] bytes = Hex.stringToBytes(commonDescriptors.getCommonDescriptorsHex().replace(" ", ""));
            List<AitApplication> aitApplications = AitApplication.parseAitApplicationsFromHex(bytes);
            hbbTvApplications = aitApplications.stream().map(HbbTVApplication::fromAitApplication).toList();
            if (currentHbbTvApplication == null && allowHbbTV) {
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hbbTvApplications.stream().filter(app -> app.controlCode == 1).findFirst().ifPresent(app -> {
                            Log.d("HBBTV", "Launching HbbTV app: " + app + " for serviceId " + commonDescriptors.getServiceId() + ", networkId " + commonDescriptors.getNetworkId() + ", originalNetworkId " + commonDescriptors.getOriginalNetworkId() + ", tsId " + commonDescriptors.getTransportStreamId());
                            launchHbbTVApp(app, commonDescriptors.getServiceId(), commonDescriptors.getNetworkId(), commonDescriptors.getOriginalNetworkId(), commonDescriptors.getTransportStreamId());
                        });
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearHbbTv() {
        hbbTvApplications = null;
        currentHbbTvApplication = null;
        webView.loadUrl("about:blank");
        keyTypesFocusable.forEach((hbbTVKeyTypes, aBoolean) -> keyTypesFocusable.put(hbbTVKeyTypes, false));
        overlayExtraKeys.setVisibility(View.GONE);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void initializeHbbTv() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setSupportZoom(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setUserAgentString("HbbTV/1.2.1 (; WWF; WatchWithFritzbox; 1.0; 1.0;) CE-HTML/1.0 LOH; NetFront/4.1 SmartTvA/3.0.0");

        WebView.setWebContentsDebuggingEnabled(true);
        webView.setBackgroundColor(Color.TRANSPARENT);

        webView.setWebViewClient(new WebViewClient() {

            /*@Override
            public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    int keyCode = event.getKeyCode();
                    if (keyCode == KeyEvent.KEYCODE_PROG_RED ||
                            keyCode == KeyEvent.KEYCODE_PROG_GREEN ||
                            keyCode == KeyEvent.KEYCODE_PROG_YELLOW ||
                            keyCode == KeyEvent.KEYCODE_PROG_BLUE) {

                        triggerColor(keyCode);
                    }
                }
            }*/

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                injectJs(view);
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                Log.d("HBBTVWebView", "onPageCommitVisible: " + url);
                injectJs(view);
            }

            final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(requireContext()))
                    .build();

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String urlString = request.getUrl().toString();
                String urlStringWithoutQuery = request.getUrl().getPath();
                if (urlString.contains("androidplatform.net")) {
                    WebResourceResponse response = assetLoader.shouldInterceptRequest(request.getUrl());
                    if(response!= null)
                        response.setResponseHeaders(Map.of("Access-Control-Allow-Origin", "*"));
                    return response;
                }
                if (urlString.contains("data:") ||
                        urlStringWithoutQuery != null && urlStringWithoutQuery.endsWith(".mp4")) {
                    return super.shouldInterceptRequest(view, request);
                }

                try {
                    // URL der angeforderten Ressource
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestProperty("User-Agent", request.getRequestHeaders().getOrDefault("User-Agent", "MyWebView"));
                    connection.connect();

                    String originalMime = connection.getContentType();

                    if (originalMime.contains("video/")) {
                        return super.shouldInterceptRequest(view, request);
                    }

                    // Response-Daten lesen
                    InputStream inputStream = connection.getInputStream();

                    String encoding = connection.getContentEncoding();
                    if (encoding == null) encoding = "UTF-8";

                    // Beispiel: MIME-Type anpassen
                    String newMimeType = originalMime;
                    if (originalMime.contains("application/vnd.hbbtv.xhtml")) {
                        // z.B. wenn der Server keinen Typ liefert
                        newMimeType = "application/xhtml+xml";
                    }

                    // Neue Response zurückgeben
                    return new WebResourceResponse(newMimeType, encoding, inputStream);

                } catch (Exception e) {
                    e.printStackTrace();
                    return super.shouldInterceptRequest(view, request);
                }
            }
        });

        // add progress bar
        webView.setWebChromeClient(new WebChromeClient() {
                                       @Override
                                       public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                                           Log.d("HBBTVWebView", consoleMessage.messageLevel().toString() + " | " + consoleMessage.message() + " | " + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber());
                                           return true;
                                       }

                                       @Override
                                       public void onPermissionRequest(PermissionRequest request) {
                                           String[] resources = request.getResources();
                                           for (int i = 0; i < resources.length; i++) {
                                               if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(resources[i])) {
                                                   request.grant(resources);
                                                   return;
                                               }
                                           }

                                           super.onPermissionRequest(request);
                                       }
                                   }
        );

        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public String getEvents() throws JsonProcessingException {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.writeValueAsString(EpgUtils.getAllEvents(requireContext(), ChannelUtils.getLastSelectedChannel(requireContext())));
            }

            @JavascriptInterface
            public void notifyPlayback(boolean playing) {
                context.runOnUiThread(() -> {
                    if(!playing) {
                        context.launchPlayer(false, false, false);
                    } else {
                        context.pausePlayer();
                    }
                });
            }

            @JavascriptInterface
            public void notifyApplicationDestroy() {
                clearHbbTv();
            }

            @JavascriptInterface
            public void notifyKeysetChanged(int keyset) throws JsonProcessingException {
                keyTypesFocusable.put(HbbTVKeyTypes.RED, (keyset & 0x1) != 0);
                keyTypesFocusable.put(HbbTVKeyTypes.GREEN, (keyset & 0x2) != 0);
                keyTypesFocusable.put(HbbTVKeyTypes.YELLOW, (keyset & 0x4) != 0);
                keyTypesFocusable.put(HbbTVKeyTypes.BLUE, (keyset & 0x8) != 0);
                keyTypesFocusable.put(HbbTVKeyTypes.NAVIGATION, (keyset & 0x10) != 0);
                keyTypesFocusable.put(HbbTVKeyTypes.VCR, (keyset & 0x20) != 0);
                keyTypesFocusable.put(HbbTVKeyTypes.SCROLL, (keyset & 0x40) != 0);
                keyTypesFocusable.put(HbbTVKeyTypes.INFO, (keyset & 0x80) != 0);
                keyTypesFocusable.put(HbbTVKeyTypes.NUMERIC, (keyset & 0x100) != 0);
                keyTypesFocusable.put(HbbTVKeyTypes.ALPHA, (keyset & 0x200) != 0);
                keyTypesFocusable.put(HbbTVKeyTypes.OTHER, (keyset & 0x400) != 0);

                requireActivity().runOnUiThread(() -> {
                    extraKeyRed.setVisibility(keyTypesFocusable.get(HbbTVKeyTypes.RED) ? View.VISIBLE : View.GONE);
                    extraKeyYellow.setVisibility(keyTypesFocusable.get(HbbTVKeyTypes.YELLOW) ? View.VISIBLE : View.GONE);
                    extraKeyGreen.setVisibility(keyTypesFocusable.get(HbbTVKeyTypes.GREEN) ? View.VISIBLE : View.GONE);
                    extraKeyBlue.setVisibility(keyTypesFocusable.get(HbbTVKeyTypes.BLUE) ? View.VISIBLE : View.GONE);
                });
            }
        }, "WatchWithFritzbox");

    }

    public void triggerColor(int keyCode) {
        overlayExtraKeys.setVisibility(View.GONE);
        String jsColor = "window[\"VK_" + HbbTVKeycodeMappings.getHbbTvKeyCodeByKeyCode(keyCode) + "\"]";
        /*if (keyCode == KeyEvent.KEYCODE_PROG_RED) {
            jsColor = "window.KeyEvent.VK_RED";
        } else if (keyCode == KeyEvent.KEYCODE_PROG_GREEN) {
            jsColor = "window.KeyEvent.VK_GREEN";
        } else if (keyCode == KeyEvent.KEYCODE_PROG_YELLOW) {
            jsColor = "window.KeyEvent.VK_YELLOW";
        } else if (keyCode == KeyEvent.KEYCODE_PROG_BLUE) {
            jsColor = "window.KeyEvent.VK_BLUE";
        }*/

        webView.evaluateJavascript("document.dispatchEvent(new KeyboardEvent('keydown',{'keyCode':" + jsColor + ", 'bubbles': true, 'cancelable': true}));", null);


    }

    public void injectJs(WebView webView) {
        // https://appassets.androidplatform.net is a reserved domain (do not change!)
        String js = "var s=document.createElement('script');"
                + "s.src='https://appassets.androidplatform.net/assets/hbbtv-polyfills/dash.all.min.js';"
                + "document.head.appendChild(s);";
        webView.evaluateJavascript(js, null);

        try {
            js = "window.HBBTV_POLYFILL_NS = window.HBBTV_POLYFILL_NS || {};" +
                    "window.HBBTV_POLYFILL_NS.currentChannel = " +
                    new ObjectMapper().writeValueAsString(hbbTvChannelInfo) +
                    ";";
            webView.evaluateJavascript(js, null);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        js = "var s=document.createElement('script');"
                + "s.type = 'module';"
                + "s.src='https://appassets.androidplatform.net/assets/hbbtv-polyfills/index.js';"
                + "document.head.appendChild(s);";
        webView.evaluateJavascript(js, null);
    }

    public enum HbbTVKeyTypes {
        RED,
        GREEN,
        YELLOW,
        BLUE,
        NAVIGATION,
        VCR,
        SCROLL,
        INFO,
        NUMERIC,
        ALPHA,
        OTHER;
    }

    public enum HbbTVKeycodeMappings {
        ENTER(HbbTVKeyTypes.NAVIGATION, KeyEvent.KEYCODE_ENTER, "ENTER"),
        DPAD_ENTER(HbbTVKeyTypes.NAVIGATION, KeyEvent.KEYCODE_DPAD_CENTER, "ENTER"),
        DPAD_LEFT(HbbTVKeyTypes.NAVIGATION, KeyEvent.KEYCODE_DPAD_LEFT, "LEFT"),
        DPAD_UP(HbbTVKeyTypes.NAVIGATION, KeyEvent.KEYCODE_DPAD_UP, "UP"),
        DPAD_RIGHT(HbbTVKeyTypes.NAVIGATION, KeyEvent.KEYCODE_DPAD_RIGHT, "RIGHT"),
        DPAD_DOWN(HbbTVKeyTypes.NAVIGATION, KeyEvent.KEYCODE_DPAD_DOWN, "DOWN"),
        KEYCODE_BACK(HbbTVKeyTypes.NAVIGATION, KeyEvent.KEYCODE_BACK, "BACK"),

        RED(HbbTVKeyTypes.RED, KeyEvent.KEYCODE_PROG_RED, "RED"),
        YELLOW(HbbTVKeyTypes.YELLOW, KeyEvent.KEYCODE_PROG_YELLOW, "YELLOW"),
        GREEN(HbbTVKeyTypes.GREEN, KeyEvent.KEYCODE_PROG_GREEN, "GREEN"),
        BLUE(HbbTVKeyTypes.BLUE, KeyEvent.KEYCODE_PROG_BLUE, "BLUE"),

        NUM0(HbbTVKeyTypes.NUMERIC, KeyEvent.KEYCODE_0, "0"),
        NUM1(HbbTVKeyTypes.NUMERIC, KeyEvent.KEYCODE_1, "1"),
        NUM2(HbbTVKeyTypes.NUMERIC, KeyEvent.KEYCODE_2, "2"),
        NUM3(HbbTVKeyTypes.NUMERIC, KeyEvent.KEYCODE_3, "3"),
        NUM4(HbbTVKeyTypes.NUMERIC, KeyEvent.KEYCODE_4, "4"),
        NUM5(HbbTVKeyTypes.NUMERIC, KeyEvent.KEYCODE_5, "5"),
        NUM6(HbbTVKeyTypes.NUMERIC, KeyEvent.KEYCODE_6, "6"),
        NUM7(HbbTVKeyTypes.NUMERIC, KeyEvent.KEYCODE_7, "7"),
        NUM8(HbbTVKeyTypes.NUMERIC, KeyEvent.KEYCODE_8, "8"),
        NUM9(HbbTVKeyTypes.NUMERIC, KeyEvent.KEYCODE_9, "9"),

        PG_UP(HbbTVKeyTypes.SCROLL, KeyEvent.KEYCODE_PAGE_UP, "PAGE_UP"),
        PG_DOWN(HbbTVKeyTypes.SCROLL, KeyEvent.KEYCODE_PAGE_DOWN, "PAGE_DOWN"),

        MEDIA_PAUSE(HbbTVKeyTypes.VCR, KeyEvent.KEYCODE_MEDIA_PAUSE, "PAUSE"),
        MEDIA_REWIND(HbbTVKeyTypes.VCR, KeyEvent.KEYCODE_MEDIA_REWIND, "REWIND"),
        MEDIA_STOP(HbbTVKeyTypes.VCR, KeyEvent.KEYCODE_MEDIA_STOP, "STOP"),
        MEDIA_PLAY(HbbTVKeyTypes.VCR, KeyEvent.KEYCODE_MEDIA_PLAY, "PLAY"),
        MEDIA_FAST_FWD(HbbTVKeyTypes.VCR, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, "FAST_FWD");

        private HbbTVKeyTypes type;
        private int keyCode;
        private String keyCodeVk;

        HbbTVKeycodeMappings(HbbTVKeyTypes type, int keyCode, String keyCodeVk) {
            this.type = type;
            this.keyCode = keyCode;
            this.keyCodeVk = keyCodeVk;
        }

        static boolean isOfType(HbbTVKeyTypes type, int keyCode) {
            return Arrays.stream(values())
                    .anyMatch(m -> m.keyCode == keyCode && type.equals(m.type));
        }

        static String getHbbTvKeyCodeByKeyCode(int keyCode) {
            return Arrays.stream(values()).filter(m -> m.keyCode == keyCode)
                    .map(m -> m.keyCodeVk).findFirst().orElse(null);
        }

        static HbbTVKeyTypes getHbbTvKeyTypeByKeyCode(int keyCode) {
            return Arrays.stream(values()).filter(m -> m.keyCode == keyCode)
                    .map(m -> m.type).findFirst().orElse(null);
        }

    }

}