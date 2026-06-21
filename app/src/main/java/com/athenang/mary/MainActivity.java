package com.athenang.mary;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.webkit.*;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private WebView w;
    private SpeechRecognizer sr;
    private boolean keepListening = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final int MIC_PERM = 100;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        w = new WebView(this);
        WebSettings ws = w.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        ws.setAllowFileAccessFromFileURLs(true);
        ws.setAllowUniversalAccessFromFileURLs(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        w.setWebViewClient(new WebViewClient());
        w.setWebChromeClient(new WebChromeClient() {
            public void onPermissionRequest(PermissionRequest r) { r.grant(r.getResources()); }
        });
        w.addJavascriptInterface(new MicBridge(), "MicBridge");
        w.loadUrl("file:///android_asset/mary.html");
        setContentView(w);

        // Richiedi permesso microfono subito all'avvio
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERM);
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        if (req == MIC_PERM) {
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) {
                js("onMicPermissionGranted()");
            } else {
                js("onMicError(-2)"); // permesso negato
            }
        }
    }

    class MicBridge {
        @JavascriptInterface
        public void startListening() {
            mainHandler.post(() -> {
                // Controlla permesso prima di avviare
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERM);
                    return;
                }
                keepListening = true;
                startSR();
            });
        }

        @JavascriptInterface
        public void stopListening() {
            mainHandler.post(() -> {
                keepListening = false;
                if (sr != null) { try { sr.stopListening(); sr.destroy(); } catch (Exception e) {} sr = null; }
                js("onMicStopped()");
            });
        }

        @JavascriptInterface
        public boolean hasPermission() {
            return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void startSR() {
        if (!keepListening) return;
        if (sr != null) { try { sr.destroy(); } catch (Exception e) {} }
        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle p) { js("onMicReady()"); }
            public void onBeginningOfSpeech() {}
            public void onRmsChanged(float v) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() {}
            public void onError(int error) {
                if (keepListening && (error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                    mainHandler.postDelayed(() -> startSR(), 300);
                } else if (error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    keepListening = false;
                    js("onMicError(" + error + ")");
                }
            }
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0).replace("'", "\\'").replace("\n"," ");
                    js("onSpeechResult('" + text + "')");
                }
                if (keepListening) mainHandler.postDelayed(() -> startSR(), 300);
            }
            public void onPartialResults(Bundle p) {}
            public void onEvent(int t, Bundle b) {}
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        try { sr.startListening(intent); } catch (Exception e) { js("onMicError(-1)"); }
    }

    private void js(String code) { mainHandler.post(() -> w.evaluateJavascript(code, null)); }

    @Override
    public void onBackPressed() { if (w.canGoBack()) w.goBack(); else super.onBackPressed(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sr != null) { try { sr.destroy(); } catch (Exception e) {} }
    }
}
