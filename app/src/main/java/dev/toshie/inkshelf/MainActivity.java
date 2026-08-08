package dev.toshie.inkshelf;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int FILE_REQ = 71;
    private WebView wv;
    private ValueCallback<Uri[]> fileCb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wv = new WebView(this);
        setContentView(wv);

        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        // Serve the bundled HTML from a proper https origin so IndexedDB persists reliably.
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest req) {
                return loader.shouldInterceptRequest(req.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("appassets.androidx.dev".equals(u.getHost())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                return true;
            }
        });

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                try {
                    startActivityForResult(p.createIntent(), FILE_REQ);
                } catch (Exception e) {
                    fileCb = null;
                    return false;
                }
                return true;
            }
        });

        wv.addJavascriptInterface(new Bridge(), "AndroidBridge");

        // Load the bundled HTML directly (bulletproof), keeping the same secure
        // https origin so IndexedDB is persistent. Asset-loader interception above
        // remains as a fallback and for any /assets/ subresources.
        String html = null;
        try {
            java.io.InputStream is = getAssets().open("inkshelf.html");
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
            is.close();
            html = bo.toString("UTF-8");
        } catch (Exception e) {
            Toast.makeText(this, "Asset read failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        if (html != null) {
            wv.loadDataWithBaseURL("https://appassets.androidx.dev/assets/inkshelf.html",
                    html, "text/html", "utf-8", null);
        } else {
            wv.loadUrl("https://appassets.androidx.dev/assets/inkshelf.html");
        }
    }

    @Override
    protected void onActivityResult(int rc, int res, Intent data) {
        if (rc == FILE_REQ && fileCb != null) {
            fileCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            fileCb = null;
        } else {
            super.onActivityResult(rc, res, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (wv.canGoBack()) wv.goBack();
        else super.onBackPressed();
    }

    private class Bridge {
        @JavascriptInterface
        public void openUrl(String u) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public void saveFile(final String name, final String b64) {
            runOnUiThread(() -> {
                try {
                    byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                    if (Build.VERSION.SDK_INT >= 29) {
                        ContentValues cv = new ContentValues();
                        cv.put(MediaStore.Downloads.DISPLAY_NAME, name);
                        cv.put(MediaStore.Downloads.MIME_TYPE, "text/html");
                        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                        OutputStream os = getContentResolver().openOutputStream(uri);
                        os.write(bytes);
                        os.close();
                        Toast.makeText(MainActivity.this, "Saved " + name + " to Downloads", Toast.LENGTH_LONG).show();
                    } else {
                        File f = new File(getExternalFilesDir(null), name);
                        FileOutputStream fo = new FileOutputStream(f);
                        fo.write(bytes);
                        fo.close();
                        Toast.makeText(MainActivity.this, "Saved to " + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
