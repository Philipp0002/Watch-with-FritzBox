package de.hahnphilipp.watchwithfritzbox.player;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.async.GetHbbTVContent;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HbbTVOverlay extends Fragment {

    TVSettingsOverlayRecyclerAdapter tvOverlayRecyclerAdapter;
    WebView webView;

    public String hbbTvUrl = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.hbbtvoverlay, container, false);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        webView = view.findViewById(R.id.hbbtvwebview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setSupportZoom(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
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
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                GetHbbTVContent getHbbTVContent = new GetHbbTVContent();
                getHbbTVContent.hbbTvUrl = url;
                getHbbTVContent.futureRunFinished = new Runnable() {
                    @Override
                    public void run() {
                        Response response = getHbbTVContent.response;
                        ResponseBody responseBody = getHbbTVContent.responseBody;
                        String responseBodyString = getHbbTVContent.responseBodyString;
                        String responseMimeType = getHbbTVContent.responseMimeType;

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                webView.loadDataWithBaseURL(
                                        response.request().url().toString(),
                                        responseBodyString,
                                        (responseMimeType.toLowerCase().contains("text/html")) ? "text/html" : "application/xhtml+xml",
                                        null,
                                        hbbTvUrl);


                            }
                        });

                    }
                };
                getHbbTVContent.execute();
                return true;
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                injectJs1(webView);
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
            }

            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                super.onScaleChanged(view, oldScale, newScale);
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("HBBTVWEB", "LOADEDFINISHED");
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();
                webView.zoomOut();

                injectJs2(webView);


            }
        });

        // add progress bar
        webView.setWebChromeClient(new WebChromeClient() {
                                       @Override
                                       public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                                           Log.d("HBBTVWebView", consoleMessage.message() + " " + consoleMessage.lineNumber());
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

        //webView.loadUrl(hbbTvUrl);


        GetHbbTVContent getHbbTVContent = new GetHbbTVContent();
        getHbbTVContent.hbbTvUrl = hbbTvUrl;
        getHbbTVContent.futureRunFinished = () -> {
            Response response = getHbbTVContent.response;
            ResponseBody responseBody = getHbbTVContent.responseBody;
            String responseBodyString = getHbbTVContent.responseBodyString;

            getActivity().runOnUiThread(() -> {
                if (webView != null) {
                    webView.loadDataWithBaseURL(
                            response.request().url().toString(),
                            responseBodyString,
                            "application/xhtml+xml",
                            null,
                            hbbTvUrl);
                }
            });

        };
        getHbbTVContent.execute();

    }


    public void triggerColor(int keyCode) {
        String jsColor = "";
        if (keyCode == KeyEvent.KEYCODE_PROG_RED) {
            jsColor = "window.KeyEvent.VK_RED";
        } else if (keyCode == KeyEvent.KEYCODE_PROG_GREEN) {
            jsColor = "window.KeyEvent.VK_GREEN";
        } else if (keyCode == KeyEvent.KEYCODE_PROG_YELLOW) {
            jsColor = "window.KeyEvent.VK_YELLOW";
        } else if (keyCode == KeyEvent.KEYCODE_PROG_BLUE) {
            jsColor = "window.KeyEvent.VK_BLUE";
        }

        webView.evaluateJavascript("doKeyPress(" + jsColor + ");", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d("HBBTV", "onReceiveValue8:");
                Log.d("HBBTV", value + "");
            }
        });

    }

    public void injectJs1(WebView webView) {

        webView.evaluateJavascript("(function(d){ var e=d.createElement(\"script\");" +
                "e.setAttribute(\"type\",\"text/javascript\");e.setAttribute(\"src\",\"https://hahnphilipp.de/watchwithfritzbox/hbbtv1.js\");" +
                "d.head.appendChild(e)" +
                "}(document));", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d("HBBTV", "onReceiveValue3:");
                Log.d("HBBTV", value + "");
            }
        });
        webView.evaluateJavascript("(function(d){ var e=d.createElement(\"script\");" +
                "e.setAttribute(\"async\", \"async\");" +
                "e.setAttribute(\"type\",\"text/javascript\");e.setAttribute(\"src\",\"https://hahnphilipp.de/watchwithfritzbox/hbbdom1.js\");" +
                "d.head.appendChild(e)" +
                "}(document));", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d("HBBTV", "onReceiveValue4:");
                Log.d("HBBTV", value + "");
            }
        });
        webView.evaluateJavascript("(function(d){ var e=d.createElement(\"script\");" +
                "e.setAttribute(\"async\", \"async\");" +
                "e.setAttribute(\"type\",\"text/javascript\");e.setAttribute(\"src\",\"https://hahnphilipp.de/watchwithfritzbox/first1.js\");" +
                "d.head.appendChild(e)" +
                "}(document));", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d("HBBTV", "onReceiveValue7:");
                Log.d("HBBTV", value + "");
            }
        });
    }


    public void injectJs2(WebView webView) {
        //injectJs(tabId, 'https://cdn.dashjs.org/v2.9.3/dash.all.min.js', 'DASH.js injection done.', true, false, 'async');
        //injectJs(tabId, fileName,                                         succeededMessage,   addedToHead, addedAsFirstChild, withOption)
        webView.evaluateJavascript("(function(d){ var e=d.createElement(\"script\");" +
                "e.setAttribute(\"async\", \"async\");" +
                "e.setAttribute(\"type\",\"text/javascript\");e.setAttribute(\"src\",\"https://cdn.dashjs.org/v2.9.3/dash.all.min.js\");" +
                "d.head.appendChild(e)" +
                "}(document));", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d("HBBTV", "onReceiveValue1:");
                Log.d("HBBTV", value + "");
            }
        });

        webView.evaluateJavascript("(function(d){ var e=d.createElement(\"script\");" +
                "e.setAttribute(\"async\", \"async\");" +
                "e.setAttribute(\"type\",\"text/javascript\");e.setAttribute(\"src\",\"https://hahnphilipp.de/watchwithfritzbox/hbbobj1.js\");" +
                "d.head.appendChild(e)" +
                "}(document));", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d("HBBTV", "onReceiveValue2:");
                Log.d("HBBTV", value + "");
            }
        });

        webView.evaluateJavascript("try{GLOBALS.htmlfive = true;}catch(ReferenceError){}", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.d("HBBTV", "onReceiveValue6:");
                Log.d("HBBTV", value + "");
            }
        });
    }


}
