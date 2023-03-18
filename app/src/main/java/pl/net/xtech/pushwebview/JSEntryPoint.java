package pl.net.xtech.pushwebview;

import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class JSEntryPoint {

    private MainActivity mainActivity;
    public JSEntryPoint  (MainActivity mainActivity) {
        this.mainActivity = mainActivity;
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
        Log.d("Message", cm.message() + " -- From line "
                + cm.lineNumber() + " of "
                + cm.sourceId() );
        return true;
    }

    @JavascriptInterface
    public String getNetworkInfo()
    {
        return mainActivity.updateWifiStateInfo();
    }

    @JavascriptInterface
    public String getData()
    {
        return mainActivity.getData();
    }
}