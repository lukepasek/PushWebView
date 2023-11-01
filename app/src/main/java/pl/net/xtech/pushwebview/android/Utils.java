package pl.net.xtech.pushwebview.android;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.text.format.Time;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;

public class Utils {

    public static String formatTime(Time time) {
        int h = time.hour;
        int m = time.minute;
        StringBuilder b = new StringBuilder();
        if (h<10) {
            b.append("0");
        }
        b.append(h);
        b.append(":");
        if (m<10) {
            b.append("0");
        }
        b.append(m);
        return b.toString();
    }

    public static String enumerateUsbDevices(Context context) {
        UsbManager _usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = _usbManager.getDeviceList();
        StringBuffer sb = new StringBuffer();

        sb.append("USB devices: "+ deviceList.size());

        Log.i("USB", "USB devices: "+ deviceList.size());
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
//         _usbDevice = null;

        // Iterate all the available devices and find ours.
        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();
            sb.append("\r\n - ").append(device.getDeviceName()+" "+device.getVendorId()+":"+device.getProductId());
            Log.i("USB", device.getDeviceName()+" "+device.getDeviceClass()+"/"+device.getDeviceSubclass()+"/"+device.getDeviceProtocol()+" " +Integer.toHexString(device.getVendorId())+":"+Integer.toHexString(device.getProductId()));
//             if (device.getProductId() == _productId && device.getVendorId() == _vendorId) {
//                 _usbDevice = device;
//                 _deviceName = _usbDevice.getDeviceName();
//             }
        }

//         if (_usbDevice == null) {
//             Log("Cannot find the device. Did you forgot to plug it?");
//             Log(String.format("\t I search for VendorId: %s and ProductId: %s", _vendorId, _productId));
//             return false;
//         }
//
//         // Create and intent and request a permission.
//         PendingIntent mPermissionIntent = PendingIntent.getBroadcast(_context, 0, new Intent(ACTION_USB_PERMISSION), 0);
//         IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
//         _context.registerReceiver(mUsbReceiver, filter);
//
//         _usbManager.requestPermission(_usbDevice, mPermissionIntent);
//         Log("Found the device");
        return sb.toString();
    }


    public static boolean isChargerConnected(Context context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = context.registerReceiver(null, filter);
//        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        if (plugged>-1 && (plugged & (BatteryManager.BATTERY_PLUGGED_AC | BatteryManager.BATTERY_PLUGGED_USB | BatteryManager.BATTERY_PLUGGED_WIRELESS))!=0) {
            return true;
        }
        return false;
    }

    public static int getBatteryCharge(Context context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = context.registerReceiver(null, filter);
//        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        return level;
    }

    public static int getBatteryChargingStatus(Context context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
//            return batteryManager.isCharging();
//        } else {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent intent = context.registerReceiver(null, filter);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            int result = 0;
////            return status;
            if (plugged>-1) {
                result = plugged;
            }
        if (status>-1) {
            result += status*100;
        }
//
//
//            if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) {
//                return 10+status;
//            }
//        }
        return result;
    }

    public static int getScreenState(Context applicationContext) {
        PowerManager pm = (PowerManager) applicationContext.getSystemService(Context.POWER_SERVICE);
        return pm.isScreenOn()?1:0;
    }

    public static int getWiFiState(Context applicationContext) {
        WifiManager wm = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);
        return wm.getWifiState();
    }

    public static byte[] getDeviceInfo(Context applicationContext) {
        StringBuffer sb = new StringBuffer("-----------------------------------\r\n");
        String unique_id = android.provider.Settings.Secure.getString(applicationContext.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        sb.append("Local time: ").append(System.currentTimeMillis()).append("\r\n");
        sb.append("ID: ").append(unique_id).append("\r\n");
        sb.append("Power state: ").append(Utils.getBatteryChargingStatus(applicationContext)).append("\r\n");
        sb.append("Battery level: ").append(Utils.getBatteryCharge(applicationContext)).append("\r\n");
        sb.append("Charger connected: ").append(Utils.isChargerConnected(applicationContext)).append("\r\n");
        sb.append("Screen state: ").append(Utils.getScreenState(applicationContext)).append("\r\n");
        sb.append("WiFi state: ").append(Utils.getWiFiState(applicationContext)).append("\r\n");
        sb.append("Bluetooth state: ").append(Utils.getBluetoothState(applicationContext)).append("\r\n");
        sb.append("\r\n");
//                        sb.append("WiFi state: ").append(Utils.getWiFiState(applicationContext));
        return sb.toString().getBytes();
    }

    public static int getBluetoothState(Context applicationContext) {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter!=null) {
            return btAdapter.getState();
        }
        return -1;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
