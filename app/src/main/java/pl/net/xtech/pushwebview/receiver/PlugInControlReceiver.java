package pl.net.xtech.pushwebview.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.util.Log;

public class PlugInControlReceiver extends BroadcastReceiver {

    public static IntentFilter intentFilter() {
        IntentFilter plugInControlFilter = new IntentFilter();
        plugInControlFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        plugInControlFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        plugInControlFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        return plugInControlFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals(Intent.ACTION_POWER_CONNECTED)) {
            Log.i("[Power state]", action+" enabling WiFi regardless of screen state");
            WifiManager wManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wManager.setWifiEnabled(true);
        }
        else if(action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (!pm.isScreenOn()) {
                Log.i("[Power state]", action+" screen is off - disabling WiFi");
                WifiManager wManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                wManager.setWifiEnabled(false);
            } else {
                Log.i("[Power state]", action+" screen is on - not changing WiFi state");
            }
        }
    }
}