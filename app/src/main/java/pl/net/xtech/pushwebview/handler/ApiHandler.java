package pl.net.xtech.pushwebview.handler;

import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.util.Map;

import pl.net.xtech.pushwebview.ByteDataHandler;
import pl.net.xtech.pushwebview.DeviceAdmin;
import pl.net.xtech.pushwebview.EmptyActivity;
import pl.net.xtech.pushwebview.MainActivity;

public class ApiHandler implements ByteDataHandler {

    MainActivity activity;

    public ApiHandler(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public byte[] onData(final String requestPath, final Map<String, String> params, final byte[] data) { //} throws IOException {
        if (params.containsKey("screen_on")) {
            boolean on = Boolean.parseBoolean(params.get("screen_on")) || "1".equals(params.get("screen_on"));
            PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
            if (on != pm.isScreenOn()) {
                if (on) {
                    Intent intent = new Intent(activity.getApplicationContext(), EmptyActivity.class);
                    intent.putExtra("finish", true);
                    activity.startActivity(intent);
                } else {
                    DevicePolicyManager deviceManger = (DevicePolicyManager)activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
                    ComponentName compName = new ComponentName(activity.getApplicationContext(), DeviceAdmin.class);
                    boolean active = deviceManger.isAdminActive(compName);
                    if (active) {
                        deviceManger.lockNow();
                    } else {
                        Log.d("screen off", "Device admin not active");
                    }
                }
            }
        }
        if (params.containsKey("brightness")) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int bright = Integer.parseInt(params.get("brightness"));
                        activity.setBrightness(bright);
                    } catch (NumberFormatException nfe) {}
                }
            });
        }
        if (params.containsKey("no_wallpaper")) {
            final WallpaperManager wallpaperManager = WallpaperManager.getInstance(activity.getApplicationContext());
//            wallpaperManager.clear();
            try {
                wallpaperManager.setStream(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (params.containsKey("reset")) {
            activity.reset();
        }
        return null;
    }
}
