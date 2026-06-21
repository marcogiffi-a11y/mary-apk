package com.athenang.mary;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;

public class MainActivity extends Activity {
    private WebView w;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        w = new WebView(this);
        WebSettings ws = w.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setAllowFileAccess(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        w.setWebViewClient(new WebViewClient());
        w.setWebChromeClient(new WebChromeClient() {
            public void onPermissionRequest(PermissionRequest r) {
                r.grant(r.getResources());
            }
        });
        w.loadUrl("file:///android_asset/mary.html");
        setContentView(w);
    }

    @Override
    public void onBackPressed() {
        if (w.canGoBack()) w.goBack();
        else super.onBackPressed();
    }
}
