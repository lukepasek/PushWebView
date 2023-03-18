package pl.net.xtech.pushwebview.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.util.Log;

import pl.net.xtech.pushwebview.MainActivity;
import pl.net.xtech.pushwebview.Utils;

public class ScreenLockReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("[Screen state]", "Screen state "+intent);

        if(intent.getAction().equals(Intent.ACTION_SCREEN_ON) || intent.getAction().equals(Intent.ACTION_USER_PRESENT)){
            Log.i("[Screen on]", "Enabling WiFi regardless of power state");
            WifiManager wManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wManager.setWifiEnabled(true);
        }
        else if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
//            Log.i("[Screen state]", "Screen OFF "+intent);
//            Intent pwrIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
//            int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
//            Log.i("[Screen state]", "Power state "+pwrIntent+" plugged: "+chargePlug);
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
