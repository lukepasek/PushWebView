package pl.net.xtech.pushwebview.handler;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;

import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import pl.net.xtech.pushwebview.android.DeviceAdmin;
import pl.net.xtech.pushwebview.EmptyActivity;
import pl.net.xtech.pushwebview.MainActivity;
import pl.net.xtech.pushwebview.android.Utils;

public class ApiHandler implements ByteDataHandler {

    MainActivity activity;

    public ApiHandler(MainActivity activity) {
        this.activity = activity;
    }

    public byte[] onData(final String method, final String requestPath, final Map<String, String> params, Map<String, String> headers, final InputStream in) {
        return toBytes("Not supported!");
    }

    @Override
    public byte[] onData(final String method, final String requestPath, final Map<String, String> params, Map<String, String> headers, final byte[] data) { //} throws IOException {
        if (params.size()==0 || requestPath.endsWith("/help") || params.containsKey("help")) {
            StringBuilder sb = new StringBuilder();
            sb.append("# Available commands").append("\r\n");
            sb.append("screen_on <0|1>").append("\r\n");
            sb.append("brightness <0-255>").append("\r\n");
            sb.append("no_wallpaper").append("\r\n");
            sb.append("reload").append("\r\n");
            sb.append("reset").append("\r\n");
            return toBytes(sb.toString());
        }
        if (params.containsKey("screen_on")) {
            boolean on = Boolean.parseBoolean(params.get("screen_on")) || "1".equals(params.get("screen_on"));

            if (on) {
                showEmptyActivity();
                return toBytes("OK\r\n");
            }

            PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
            if (on != pm.isScreenOn()) {
                if (on) {
                    Log.d("API", "Activating empty activity");
                    showEmptyActivity();
                } else {
                    DevicePolicyManager deviceManger = (DevicePolicyManager)activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
                    ComponentName compName = new ComponentName(activity.getApplicationContext(), DeviceAdmin.class);
                    boolean active = deviceManger.isAdminActive(compName);
                    if (active) {
                        deviceManger.lockNow();
                    } else {
                        Log.d("screen off", "Device admin not active");
                        return toBytes("Screen off: Device admin not active\r\n");
                    }
                }
            }
        }
        if (params.containsKey("brightness")) {
            try {
                final int bright = Integer.parseInt(params.get("brightness"));
                activity.runOnUiThread( () -> activity.setBrightness(bright) );
            } catch (NumberFormatException nfe) {
                return toBytes("Error: "+nfe.getMessage());
            }
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
        if (params.containsKey("reload")) {
            activity.reload();
        }
        if (params.containsKey("reset")) {
            activity.reset();
        }
        if (params.containsKey("settings")) {
            activity.startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
        }
        if (params.containsKey("list_usb")) {
            return toBytes(Utils.enumerateUsbDevices(activity.getApplicationContext()));
        }
        if (params.containsKey("state")) {
            StringBuilder sb = new StringBuilder();
            Context ctx = activity.getApplicationContext();
            sb.append("Power state: "+Utils.getBatteryChargingStatus(ctx)).append("\r\n");
            sb.append("Screen state: "+Utils.getScreenState(ctx)).append("\r\n");
            sb.append("WiFi state: "+Utils.getWiFiState(ctx)).append("\r\n");
            sb.append("Bluetooth state: "+Utils.getBluetoothState(ctx)).append("\r\n");
            sb.append("Window active: "+activity.getWindow().isActive()).append("\r\n");
        }
        return toBytes("OK\r\n");
    }

    private void showEmptyActivity() {
        Log.d("API", "Activating empty activity");
        Context ctx = activity.getApplicationContext();
        Intent intent = new Intent(ctx, EmptyActivity.class);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK + FLAG_ACTIVITY_NO_ANIMATION);
        ctx.startActivity(intent);
    }

    private byte[] toBytes(String s) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return s.getBytes(StandardCharsets.UTF_8);
        } else {
            return s.getBytes();
        }
    }
}
