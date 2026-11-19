package com.hvr.volumencamara;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 101;
    private static final int SAVE_CSV = 102;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingCsv;
    private WebView webView;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        setContentView(webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setDomStorageEnabled(true);
        webView.addJavascriptInterface(new CsvBridge(), "HVRAndroid");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "Seleccionar foto de cámara"), PICK_IMAGE);
                return true;
            }
        });
        webView.loadUrl("file:///android_asset/index.html");
    }

    public class CsvBridge {
        @JavascriptInterface public void saveCsv(String csv) {
            runOnUiThread(() -> {
                pendingCsv = "\uFEFF" + csv;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/csv");
                intent.putExtra(Intent.EXTRA_TITLE, "HVR_volumen_camara.csv");
                startActivityForResult(intent, SAVE_CSV);
            });
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE) {
            if (fileCallback != null) {
                Uri[] result = resultCode == RESULT_OK && data != null && data.getData() != null ? new Uri[]{data.getData()} : null;
                fileCallback.onReceiveValue(result);
                fileCallback = null;
            }
        } else if (requestCode == SAVE_CSV && resultCode == RESULT_OK && data != null && data.getData() != null && pendingCsv != null) {
            try (OutputStream stream = getContentResolver().openOutputStream(data.getData())) {
                if (stream != null) stream.write(pendingCsv.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) { android.widget.Toast.makeText(this, "Error al guardar CSV", android.widget.Toast.LENGTH_LONG).show(); }
            pendingCsv = null;
        }
    }
    @Override public void onBackPressed() { if (webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}
