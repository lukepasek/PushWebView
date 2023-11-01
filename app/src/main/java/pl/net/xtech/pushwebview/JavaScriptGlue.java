package pl.net.xtech.pushwebview;

import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.widget.Toast;

public class JavaScriptGlue {

    private ValueCallback<Integer> wifiStateCallback = null;
    private ValueCallback<String> dataCallback = null;

    private MainActivity mainActivity;
    public JavaScriptGlue(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @android.webkit.JavascriptInterface
    public void printObject(Object obj){
        Log.d("JS", "Javascript object: "+obj);
    }

    @android.webkit.JavascriptInterface
    public void showToast(String echo){
        Toast toast = Toast.makeText(mainActivity, echo, Toast.LENGTH_SHORT);
        toast.show();
    }

    @android.webkit.JavascriptInterface
    public void onError(String error){
        throw new Error(error);
    }

    @android.webkit.JavascriptInterface
    public boolean onConsoleMessage(ConsoleMessage cm)
    {
        Log.i("JS", cm.messageLevel().name()+" console message form "+cm.sourceId()+" at "+cm.lineNumber()+": "+cm.message());
        return true;
    }

    @android.webkit.JavascriptInterface
    public String getNetworkInfo()
    {
        return mainActivity.getNetworkInfo();
    }

    @android.webkit.JavascriptInterface
    public String getData()
    {
        return "test data from android";
    }

    @android.webkit.JavascriptInterface
    public void registerDataCallback(ValueCallback<String> callback) {
        dataCallback = callback;
        Log.i("JS", "Registered data callback: "+callback);
    }

    @android.webkit.JavascriptInterface
    public void registerWifiStateCallback(ValueCallback<Integer> callback) {
        wifiStateCallback = callback;
        Log.i("JS", "Registered wifi state callback: "+callback);
    }

    public void updateWifiState(int state) {
        if (wifiStateCallback != null) {
            wifiStateCallback.onReceiveValue(state);
        }
    }

    public void unregisterCallbacks() {
        if (wifiStateCallback != null) {
            wifiStateCallback = null;
        }
        if (dataCallback != null) {
            dataCallback = null;
        }
    }
}