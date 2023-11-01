package pl.net.xtech.pushwebview.service;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.PowerManager.WakeLock;
import android.os.PowerManager;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Arrays;

import pl.net.xtech.pushwebview.EmptyActivity;
import pl.net.xtech.pushwebview.android.Utils;

public class TouchDetectService extends Service implements SensorEventListener {
    public static final String TAG = TouchDetectService.class.getName();
    public static final int SCREEN_OFF_RECEIVER_DELAY = 500;

    private SensorManager mSensorManager = null;
    private WakeLock mWakeLock = null;

    @NonNull
    public static Intent getIntent(Context context) {
        return new Intent(context, TouchDetectService.class);
    }

    /*
     * Register this as a sensor event listener.
     */
    private void registerListener() {
        mSensorManager.registerListener(this,
//                mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
    }

    /*
     * Un-register this as a sensor event listener.
     */
    private void unregisterListener() {
        mSensorManager.unregisterListener(this);
    }

    public BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            if (Utils.getScreenState(getApplicationContext())==0) {
            Log.i(TAG, "onReceive(" + intent + ")");

            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.i(TAG, "Screen off - registering sensor listener.");
                Runnable runnable = new Runnable() {
                        public void run() {
                            Log.i(TAG, "Runnable executing.");
                            unregisterListener();
                            registerListener();
                        }
                    };
                    new Handler().postDelayed(runnable, SCREEN_OFF_RECEIVER_DELAY);
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    Log.i(TAG, "Screen on - unregistering sensor listener.");
                    unregisterListener();
                }
            }
//        }
    };

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, sensor.getName()+" accuracy changed: "+accuracy);
    }

    private float[] sensorData = new float[0];
    private float[] sensorDataDelta = new float[0];
    private int sensorAccuracy = 0;

    public void onSensorChanged(SensorEvent event) {
//        if (Utils.getScreenState(getApplicationContext())==0) {
        float[] newSensorData = event.values;
//           Log.i(TAG, "onSensorChanged(): "+newSensorData);

           if (sensorData.length==newSensorData.length && sensorAccuracy==event.accuracy) {
               float deltaSum = 0;
               for (int i=0; i<sensorData.length; i++) {
                   sensorDataDelta[i] = Math.abs(sensorData[i]-newSensorData[i]);
                   sensorData[i] = newSensorData[i];
                   deltaSum += sensorDataDelta[i];
               }
               if (deltaSum<4 && sensorDataDelta[2]>0.40) {
                   Log.i(TAG, "sensor delta: " + deltaSum + ": " + Arrays.toString(sensorDataDelta));
                   Context ctx = getApplicationContext();
                   Intent intent = new Intent(ctx, EmptyActivity.class);
                   intent.setFlags(FLAG_ACTIVITY_NEW_TASK + FLAG_ACTIVITY_NO_ANIMATION);
                   ctx.startActivity(intent);
               }
           } else {
               sensorData = new float[newSensorData.length];
               sensorDataDelta = new float[sensorData.length];
               sensorAccuracy=event.accuracy;
           }
//        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        PowerManager manager =
                (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        registerReceiver(mReceiver, getIntentFilter());
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter screenLockIntentFilter = new IntentFilter();
        screenLockIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenLockIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
//        screenLockIntentFilter.addAction(Intent.ACTION_USER_PRESENT);
        return screenLockIntentFilter;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        unregisterListener();
        mWakeLock.release();
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

//        startForeground(Process.myPid(), new Notification());
//        registerListener();
        mWakeLock.acquire();
        return START_STICKY;
    }
}