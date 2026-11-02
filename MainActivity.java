package com.tarun.hisabbook;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Shows the Hisab Book screens (the www folder) in a full-screen WebView.
 * All data stays on the phone. The page talks to this class for two things:
 * sharing a file (picture, CSV, backup) and the Android back button.
 */
public class MainActivity extends Activity {

    private static final int REQ_PICK_FILE = 1001;

    private WebView web;
    private ValueCallback<Uri[]> pendingPick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);
        setContentView(web);

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // The app never leaves its own screens.
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             WebChromeClient.FileChooserParams params) {
                if (pendingPick != null) {
                    pendingPick.onReceiveValue(null);
                }
                pendingPick = callback;
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(pick, "Choose backup file"), REQ_PICK_FILE);
                } catch (Exception e) {
                    pendingPick = null;
                    return false;
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_PICK_FILE) {
            if (pendingPick != null) {
                Uri[] result = null;
                if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                    result = new Uri[] { data.getData() };
                }
                pendingPick.onReceiveValue(result);
                pendingPick = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        String js = "(function(){try{return window.onAndroidBack?window.onAndroidBack():false;}catch(e){return false;}})()";
        web.evaluateJavascript(js, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (!"true".equals(value)) {
                    finish();
                }
            }
        });
    }

    /** Called from the page as window.Android.shareFile(name, mime, base64). */
    private class Bridge {
        @JavascriptInterface
        public void shareFile(String name, String mime, String base64) {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                File dir = new File(getCacheDir(), "shared");
                dir.mkdirs();
                File file = new File(dir, name.replaceAll("[^A-Za-z0-9._-]", "_"));
                FileOutputStream out = new FileOutputStream(file);
                out.write(bytes);
                out.close();

                final Uri uri = FileProvider.getUriForFile(
                        MainActivity.this, getPackageName() + ".fileprovider", file);
                final String type = mime;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent send = new Intent(Intent.ACTION_SEND);
                        send.setType(type);
                        send.putExtra(Intent.EXTRA_STREAM, uri);
                        send.setClipData(ClipData.newRawUri("", uri));
                        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(send, "Share"));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Could not share the file", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}
