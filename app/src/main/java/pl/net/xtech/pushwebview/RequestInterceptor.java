package pl.net.xtech.pushwebview;

import android.content.res.AssetManager;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static pl.net.xtech.pushwebview.MainActivity.BASE_DATA_URL;

public class RequestInterceptor extends WebViewClient {

    MainActivity activity;

    public RequestInterceptor(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        Log.d("HTTP", "intercepting resource request: '"+url+"'");
        try {
            if (url.startsWith(BASE_DATA_URL)) {
                String contentType = null;
                String fileName = url.substring(BASE_DATA_URL.length());
                if (fileName.length()==0) {
                    fileName = "index.html";
                    contentType = "text/html";
                } else if (fileName.toLowerCase().endsWith(".svg")) {
                    contentType = "image/svg+xml";
                } else if (fileName.toLowerCase().endsWith(".css")) {
                    contentType = "text/css";
                } else if (fileName.toLowerCase().endsWith(".js")) {
                    contentType = "text/javascript";
                }
                File dir = new File(activity.getFilesDir(), "data");
                File file = new File(dir, fileName);
                Log.d("HTTP", "intercepting resource request: "+url+" mapped resource: "+fileName);
                if (file.isFile()) {
                    Log.d("HTTP", "serving resource from: "+file.getAbsolutePath());
                    return new WebResourceResponse(contentType, null, new FileInputStream(file));
                } else {
                    Log.d("HTTP", "serving resource from asset: "+fileName);
                    return new WebResourceResponse(contentType, null, activity.getAssets().open(fileName, AssetManager.ACCESS_STREAMING));
                }
            } else if (url.startsWith("file://stream/")) {
                InputStream stream = activity.removeInputStream(url.substring("file://stream/".length()));
                if (stream!=null) {
                    Log.d("HTTP", "serving stream "+url);
                    return new WebResourceResponse(null, null, stream);
                }
            }
        } catch (IOException e) {
            Log.e("asset", e.getMessage(), e);
        }
        return super.shouldInterceptRequest(view, url);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        Log.d("HTTP", "Page finished: "+ url);
        activity.updateWifiStateInfo();
        activity.updateTime();
    }
}
