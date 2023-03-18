package pl.net.xtech.pushwebview.handler;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.UUID;

import pl.net.xtech.pushwebview.ByteDataHandler;
import pl.net.xtech.pushwebview.MainActivity;

public class HtmlDataHandler implements ByteDataHandler {

    MainActivity activity;

    public HtmlDataHandler(MainActivity activity) {
        this.activity = activity;
    }
    @Override
    public byte[] onData(final String requestPath, final Map<String, String> params, final byte[] data) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
                if (data!=null) {
                    if (params!=null && "PUT".equals(params.get("REQUEST_METHOD"))) {

                    } else {
                        String key = UUID.randomUUID().toString();
                        activity.addInputStream(key, new ByteArrayInputStream(data));
                        activity.loadUrl("file://stream/" + key + "/" + requestPath + "?content-type=" + params.get("content-type"));
//                                    mWebView.loadUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/6/69/How_to_use_icon.svg/1107px-How_to_use_icon.svg.png");
//                                    Log.d("HTTP", "after request streams map: "+inputStreams.size());
                    }
                } else {
//                                    Log.d("HTTP", "get request: "+requestPath);
                    if (params!=null) {
                        if (params.containsKey("append")) {
                            activity.loadUrl("javascript:document.body.insertAdjacentHTML('beforeend', '" + params.get("append") + "')");
                        } else if (params.containsKey("prepend")) {
                            activity.loadUrl("javascript:document.body.insertAdjacentHTML('afterbegin', '"+params.get("prepend")+"')");
                        } else if (params.containsKey("set")) {
//                                            mWebView.loadUrl("javascript:document.body.innerHTML='"+params.get("set")+"'");
                            activity.loadUrl("javascript:content_set('"+params.get("set")+"');");
                        }
                    }
                }
//            }
//        });
        return null;
    }
}
