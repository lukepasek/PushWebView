package pl.net.xtech.pushwebview.handler;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static pl.net.xtech.pushwebview.MainActivity.BASE_DATA_URL;
import static pl.net.xtech.pushwebview.MainActivity.RESOURCE_ROOT;

import pl.net.xtech.pushwebview.MainActivity;
//import static pl.net.xtech.pushwebview.MainActivity.bytesToHex;

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
                String fileName = url.substring(BASE_DATA_URL.length());
                String contentType = getContentType(fileName);
                InputStream inputStream = activity.getResource(fileName);
                return new WebResourceResponse(contentType, null, inputStream);
            } else if (url.startsWith("file://stream/")) {
                String contentType = getContentType(url);
                String[] keyAndParams = url.replace("file://stream/", "").split("/", 2);
                Map<String, String> params = parseParams(keyAndParams.length>1?keyAndParams[1]:null);
                InputStream stream = activity.removeInputStream(url);
                if (stream!=null) {
                    Log.d("HTTP", "serving stream "+url+" "+contentType+" stream: "+stream);
//                    return new WebResourceResponse(params.get("content-type"), null, stream);
                    return new WebResourceResponse(contentType, null, stream);
                } else {
                    return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(("Stream not found: "+url).getBytes()));
                }
            }
        } catch (IOException e) {
            Log.e("asset", e.getMessage(), e);
        }
//        catch (NoSuchAlgorithmException e) {
//            throw new RuntimeException(e);
//        }
        return super.shouldInterceptRequest(view, url);
    }

    private Map<String, String> parseParams(String params) {
        if (params==null) {
            return new HashMap<>(0);
        }
        int ps = params.indexOf('?');
        if (ps<0) {
            return new HashMap<>(0);
        }
        String[] pa = params.substring(ps+1).split("&");
        HashMap<String, String> paramMap = new HashMap<>(pa.length);
        for (String p: pa) {
            int s = p.indexOf('=');
            if (s>0) {
                paramMap.put(p.substring(0, s).replaceFirst("^_h_", ""), p.substring(s+1));
            } else {
                paramMap.put(p, "");
            }
        }
        Log.d("HTTP", "Parsed params/headers: "+paramMap);
        return paramMap;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        Log.d("HTTP", "Page started: "+ url);
        activity.setDocumentLoaded(false);
//        view.setVisibility(View.INVISIBLE);
        view.setAlpha(0.0f);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        Log.d("HTTP", "Page finished: "+ url);
        activity.setDocumentLoaded(true);
//        view.setVisibility(View.VISIBLE);
        view.setAlpha(1.0f);
    }

    private String getContentType(String fileName) {
        String contentType = null;
        if (fileName.length()==0) {
//            fileName = "index.html";
            contentType = "text/html";
        } else if (fileName.toLowerCase().endsWith(".html")) {
            contentType = "text/html";
        } else if (fileName.toLowerCase().endsWith(".svg")) {
            contentType = "image/svg+xml";
        } else if (fileName.toLowerCase().endsWith(".css")) {
            contentType = "text/css";
        } else if (fileName.toLowerCase().endsWith(".js")) {
            contentType = "text/javascript";
        } else if (fileName.toLowerCase().endsWith(".png")) {
            contentType = "image/png";
        } else if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
            contentType = "image/jpeg";
        }
        return contentType;
    }

}
