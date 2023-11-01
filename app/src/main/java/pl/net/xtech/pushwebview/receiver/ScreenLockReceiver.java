package pl.net.xtech.pushwebview.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.util.Log;

import pl.net.xtech.pushwebview.android.Utils;

public class ScreenLockReceiver extends BroadcastReceiver {
    public static IntentFilter intentFilter() {
        IntentFilter screenLockIntentFilter = new IntentFilter();
        screenLockIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenLockIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
//        screenLockIntentFilter.addAction(Intent.ACTION_USER_PRESENT);
        return screenLockIntentFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("[Screen state]", "Screen state "+intent);
//        if(intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
//            Log.i("[user present]", "starting main activity as lock screen");
//            Intent newIntent = new Intent(context, MainActivity.class);
//            newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            context.startActivity(newIntent);
//        }

        if(intent.getAction().equals(Intent.ACTION_SCREEN_ON) || intent.getAction().equals(Intent.ACTION_USER_PRESENT)){
            Log.i("[Screen on]", "Enabling WiFi regardless of power state");
            WifiManager wManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wManager.setWifiEnabled(true);
        } else if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
            if (!Utils.isChargerConnected(context)) {
                Log.i("[Screen off]", "Power state is unplugged - disabling WiFi");
                WifiManager wManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                wManager.setWifiEnabled(false);
            } else {
                Log.i("[Screen off]", "Power state is plugged in - not changing WiFi state");
            }
        }
    }
}
