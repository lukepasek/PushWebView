package pl.net.xtech.pushwebview.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.UUID;

import pl.net.xtech.pushwebview.ByteDataHandler;
import pl.net.xtech.pushwebview.Utils;

public class BluetoothService extends Service {

    private AcceptThread acceptThread;
    private BluetoothServiceBinder mBinder;
    private HashSet<ByteDataHandler> listeners = new HashSet<ByteDataHandler>(0);

    public BluetoothService() {
    }

    void addDataListener(ByteDataHandler listener) {
        listeners.add(listener);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("BT SVC", "Starting...");
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        btAdapter.setName("PUSHWEBVIEW");
        int btState = btAdapter.getState();
        if (btState != BluetoothAdapter.STATE_ON && btState != BluetoothAdapter.STATE_TURNING_ON) {
            btAdapter.enable();
        }
        Log.i("BT SVC","Local address: "+btAdapter.getAddress());
        if (acceptThread == null || !acceptThread.isAlive()) {
            acceptThread = new AcceptThread(btAdapter);
            acceptThread.start();
        }
        mBinder = new BluetoothServiceBinder(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (acceptThread!=null) {
            acceptThread.cancel();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @NonNull
    public static Intent getIntent(Context context) {
        return new Intent(context, BluetoothService.class);
    }

    private class AcceptThread extends Thread {

        private final static String TAG = "BT";

        private BluetoothAdapter bluetoothAdapter;

        private BluetoothServerSocket mmServerSocket;

        private boolean run = true;

        public AcceptThread(BluetoothAdapter bluetoothAdapter) {
            this.bluetoothAdapter = bluetoothAdapter;
        }

        public void run() {
            BluetoothSocket socket = null;
            while (run) {
                try {
                    if (mmServerSocket==null) {
                        mmServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("RFCOMM", UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
                        Log.i("RFCOMM", "New server socket created: "+mmServerSocket);
                    }
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    if (mmServerSocket!=null) {
                        try {
                            mmServerSocket.close();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                        mmServerSocket = null;
                    }
//                    break;
                    if (run) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                    } else {
                        break;
                    }
                }

                if (socket != null) {
                    Log.i(TAG, "New RFCOMM connection from "+ socket.getRemoteDevice().getName() +" ("+socket.getRemoteDevice().getAddress()+")");

                    try {
                        OutputStream outs = socket.getOutputStream();
                        InputStream ins = socket.getInputStream();
//                        outs.write("Hello!\r\n".getBytes());
                        StringBuffer sb = new StringBuffer();
                        String unique_id = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                        sb.append("ID: ").append(unique_id).append("\r\n");
                        sb.append("Power state: ").append(Utils.getBatteryChargingStatus(getApplicationContext())).append("\r\n");
                        sb.append("Battery level: ").append(Utils.getBatteryCharge(getApplicationContext())).append("\r\n");
                        sb.append("Charger connected: ").append(Utils.isChargerConnected(getApplicationContext())).append("\r\n");
                        sb.append("Screen state: ").append(Utils.getScreenState(getApplicationContext())).append("\r\n");
                        sb.append("WiFi state: ").append(Utils.getWiFiState(getApplicationContext())).append("\r\n");
                        sb.append(Utils.enumerateUsbDevices(getApplicationContext())).append("\r\n");
//                        sb.append("WiFi state: ").append(Utils.getWiFiState(getApplicationContext()));
//                        sb.append("Window active: ").append(getWindow().isActive());
                        outs.write(sb.toString().getBytes());
                        outs.flush();
//                        byte[] buf = new byte[1024];
//                        ins.available();
                        Thread.sleep(100);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            run = false;
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}