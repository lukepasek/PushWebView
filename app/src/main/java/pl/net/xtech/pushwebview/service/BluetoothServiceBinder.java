package pl.net.xtech.pushwebview.service;

import android.os.Binder;

import pl.net.xtech.pushwebview.ByteDataHandler;

public class BluetoothServiceBinder extends Binder {

    private final BluetoothService btService;

    public BluetoothServiceBinder(BluetoothService btService) {
            this.btService = btService;
        }

        public void addMessageListener(ByteDataHandler listener) {
            btService.addDataListener(listener);
        }
    }