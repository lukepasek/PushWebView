package pl.net.xtech.pushwebview.handler;

import static pl.net.xtech.pushwebview.MainActivity.RESOURCE_ROOT;

import android.content.Context;
import android.os.Build;
import android.telecom.StatusHints;
import android.util.Log;
import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;

import pl.net.xtech.pushwebview.MainActivity;

public class HtmlDataHandler implements ByteDataHandler {

    MainActivity activity;
    String context = "";

    public HtmlDataHandler(String context, MainActivity activity) {
        if (context!=null) {
            if (!context.startsWith("/")) {
                context = "/" + context;
            }
            if (!context.endsWith("/")) {
                context = context + "/";
            }
            this.context = context;
        }
        this.activity = activity;
    }
    @Override
    public byte[] onData(final String method, final String requestPath, final Map<String, String> params, Map<String, String> headers, final byte[] data) {
        Log.d("HTTP", method+" request: "+requestPath+", params:  "+params+", data: "+(data!=null?data.length:0));
        final String resourceName;
        if (requestPath.startsWith(context)) {
            resourceName = requestPath.substring(context.length());
        } else if (context.equals(requestPath+"/")) {
            resourceName = "";
        } else {
            resourceName = requestPath;
        }
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
                if (data!=null) {
                    String fileName = resourceName;//"/data".equals(requestPath)?"":requestPath.replaceFirst("^/data/", "");
                    if ("PUT".equals(method)) {
                        try {
                            activity.saveResource(fileName, data, params!=null?"true".equalsIgnoreCase(params.get("reload")):false);
                        } catch (IOException e) {
                            return getBytes("Error saving resource: "+e.getMessage()+"\r\n");
                        }
                    } else if ("DELETE".equals(method)) {
                        File dir = new File(activity.getFilesDir(), RESOURCE_ROOT);
                        File file = new File(dir, fileName);
                        if (file.exists() && file.isFile()) {
                            file.delete();
                        }
                    } else if ("POST".equals(method)) {
//                        String key = UUID.randomUUID().toString();
//                        activity.addInputStream(key, new ByteArrayInputStream(data));
//                        StringBuffer url = new StringBuffer("file://stream/").append(key).append(requestPath);
                        StringBuffer url = new StringBuffer("file://stream").append(requestPath);
                        activity.loadUrl(url.toString(), new ByteArrayInputStream(data));
                    }
                } else {
//                    Log.d("HTTP", "get request: "+requestPath);
                    if (params!=null) {
                        if (params.containsKey("append")) {
                            activity.loadUrl("javascript:document.body.insertAdjacentHTML('beforeend', '" + params.get("append") + "')");
                        } else if (params.containsKey("prepend")) {
                            activity.loadUrl("javascript:document.body.insertAdjacentHTML('afterbegin', '"+params.get("prepend")+"')");
                        } else if (params.containsKey("set")) {
//                                            mWebView.loadUrl("javascript:document.body.innerHTML='"+params.get("set")+"'");
                            activity.loadUrl("javascript:content_set('"+params.get("pset")+"');");
                        } else if (params.containsKey("body")) {
                            activity.loadUrl("javascript:document.body.innerHTML='"+params.get("body")+"'");
                        }
                    }
                }
//            }
//        });
        return getBytes("OK\r\n");
    }

    private byte[] getBytes(String s) {
        if (s==null || s.isEmpty()) {
            return new byte[0];
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return s.getBytes(StandardCharsets.UTF_8);
        } else {
            return s.getBytes();
        }
    }

}
