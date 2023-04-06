package pl.net.xtech.pushwebview;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothClient extends Thread {
    private final String deviceAddress;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private BluetoothAdapter bluetoothAdapter;
    private DataHandler<String> dataHandler;
    private UUID uuid;
    private Context applicationContext;

    private boolean run = true;

    private String TAG = "BT client";

    public BluetoothClient(BluetoothAdapter bluetoothAdapter, String deviceAddress, String serviceUuid, DataHandler<String> dataHandler, Context applicationContext) {
        this.bluetoothAdapter = bluetoothAdapter!=null?bluetoothAdapter:BluetoothAdapter.getDefaultAdapter();
        this.deviceAddress = deviceAddress;
        this.mmDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
        this.uuid = UUID.fromString(serviceUuid);
        this.dataHandler = dataHandler;
        this.applicationContext = applicationContext;
//    }
//
//    public BluetoothClient(BluetoothDevice device) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
//        BluetoothSocket tmp = null;
//
//        try {
//            // Get a BluetoothSocket to connect with the given BluetoothDevice.
//            // MY_UUID is the app's UUID string, also used in the server code.
//            tmp = mmDevice.createRfcommSocketToServiceRecord();
//        } catch (IOException e) {
////            Log.e(TAG, "Socket's create() method failed", e);
//            e.printStackTrace();
//        }

        mmSocket = null;

        start();
    }

    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mmDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
        this.bluetoothAdapter.cancelDiscovery();

        int connectError = 0;

        while(run) {
            try {
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                mmSocket.connect();
                Log.i(TAG, "Connected to "+mmSocket.getRemoteDevice());
                InputStream ins = mmSocket.getInputStream();
                OutputStream outs = mmSocket.getOutputStream();
                connectError = 0;
                outs.write("# connected\r\n".getBytes());
                outs.flush();
                BufferedReader r = new BufferedReader(new InputStreamReader(ins));

                while (true) {
                    String l = r.readLine();
                    if (l.length()==0) {
                        outs.write(Utils.getDeviceInfo(applicationContext));
                        outs.flush();
                    } else {
                        if (!l.startsWith("AT+")) {
                            dataHandler.onData(deviceAddress, null, l);
                        }
                    }
                }
            } catch (IOException connectException) {
                Log.d("BT", this+" Socket connect error #"+connectError+": "+connectException.getMessage());
                int btState = bluetoothAdapter.getState();
                if (btState == BluetoothAdapter.STATE_OFF || btState == BluetoothAdapter.STATE_TURNING_OFF) {
                    Log.i("BT", "Bluetooth is off - shutting down");
                    run = false;
                    return;
                }
                connectError++;
                if (mmSocket!=null && mmSocket.isConnected()) {
                    try {
                        mmSocket.close();
                    } catch (IOException closeException) {
                    }
                    mmSocket = null;
                }
                if (run && connectError>5) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {}
                }
//            return;
            }
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
//        manageMyConnectedSocket(mmSocket);
    }

    // Closes the client socket and causes the thread to finish.
    public void close() {
        try {
            run = false;
            if (mmSocket!=null && mmSocket.isConnected()) {
                mmSocket.close();
            }
            this.interrupt();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

    public boolean isConnected() {
        return isAlive();
    }
}