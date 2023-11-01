package pl.net.xtech.pushwebview;

import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.format.Formatter;
import android.text.format.Time;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pl.net.xtech.pushwebview.android.Utils;
import pl.net.xtech.pushwebview.client.BluetoothClient;
import pl.net.xtech.pushwebview.client.DataHandler;
import pl.net.xtech.pushwebview.handler.ApiHandler;
import pl.net.xtech.pushwebview.handler.HtmlDataHandler;
import pl.net.xtech.pushwebview.handler.RequestInterceptor;
import pl.net.xtech.pushwebview.receiver.PlugInControlReceiver;
import pl.net.xtech.pushwebview.receiver.ScreenLockReceiver;
import pl.net.xtech.pushwebview.service.HttpServerService;
import pl.net.xtech.pushwebview.service.HttpServiceBinder;
import pl.net.xtech.pushwebview.service.TouchDetectService;

public class MainActivity extends Activity {

    protected WebView mWebView;
    private ServiceConnection httpServiceConnection;
    private BroadcastReceiver wifiBroadcastReceiver;
    public final static String BASE_DATA_URL = "file://data/";
    public final static String RESOURCE_ROOT = "data";
    protected HashMap<String, InputStream> inputStreams = new HashMap<>();
    boolean documentLoaded = false;
    boolean documentLoading = false;
    private String data;

    private BroadcastReceiver timeTickReceiver;
    private BroadcastReceiver screenLockReceiver;
    private PlugInControlReceiver plugInControlReceiver;
    private BroadcastReceiver userPresentReceiver;
    private BluetoothClient bluetoothClient;

    private ExecutorService executorService = Executors.newFixedThreadPool(4);

    private float brightness = -1f;

    private final JavaScriptGlue mJavaScriptGlue = new JavaScriptGlue(this);

    private final static String SCREEN_BRIGHTNESS_KEY = "SCREEN_BRIGHTNESS_KEY";

    private final static String TAG = MainActivity.class.getSimpleName();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Window window = getWindow();
//        window.requestFeature(Window.FEATURE_NO_TITLE);
//        window.requestFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        int windowFlags = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
//                0;//WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
//                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_FULLSCREEN;

                //WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
//                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
//                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
//                WindowManager.LayoutParams.FLAG_FULLSCREEN
//                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
//                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
//                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
//                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                ;


//        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);


        WindowManager.LayoutParams layout = window.getAttributes();
        if (savedInstanceState!=null) {
            brightness = savedInstanceState.getFloat(SCREEN_BRIGHTNESS_KEY, -1f);
        }
        layout.screenBrightness = brightness;
        layout.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        layout.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        window.setAttributes(layout);

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        View decorView = window.getDecorView();
        decorView.setBackgroundColor(Color.BLACK);
        setFullScreen();

        setContentView(R.layout.main);
        mWebView = (WebView) findViewById(R.id.main_webview);

//        ViewParent parent = mWebView.getParent();
//        while (parent!=null) {
//            LayoutTransition lt = ((ViewGroup) parent).getLayoutTransition();
//            if (lt != null) {
//                lt.disableTransitionType(LayoutTransition.APPEARING);
//                lt.disableTransitionType(LayoutTransition.DISAPPEARING);
//            }
//            parent = parent.getParent();
//        }

        mWebView = (WebView) findViewById(R.id.main_webview);
        mWebView.setAlpha(0);
//        mWebView.setBackgroundColor(Color.BLACK);
//        mWebView.setBackgroundColor(Color.WHITE);
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.addJavascriptInterface(mJavaScriptGlue, "android");
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAllowContentAccess(true);

        mWebView.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
                Log.i(TAG+":touch", "event: " + event);
                mWebView.loadUrl("javascript:clickAt(" + event.getRawX() + "," + event.getRawY() + "," + action + ")");
            }
            return true;
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                    Log.e("JS console", consoleMessage.messageLevel() + ": " + consoleMessage.message() + " at " + consoleMessage.lineNumber() + " in " + consoleMessage.sourceId());
                } else {
                    Log.d("JS console", consoleMessage.messageLevel() + ": " + consoleMessage.message() + " at " + consoleMessage.lineNumber() + " in " + consoleMessage.sourceId());
                }
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Log.d("JS alert", message);
                result.confirm();
                return true; //super.onJsAlert(view, url, message, result);
            }
        });

        mWebView.setWebViewClient(new RequestInterceptor(MainActivity.this));

        httpServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "Http server service connected");
                HttpServiceBinder httpServiceBinder = (HttpServiceBinder) service;
                httpServiceBinder.addContextHandler(RESOURCE_ROOT, new HtmlDataHandler(RESOURCE_ROOT, MainActivity.this));
                httpServiceBinder.addContextHandler("api", new ApiHandler(MainActivity.this));
//                httpServiceBinder.addContextHandler("/", new HtmlDataHandler(MainActivity.this));
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "Http server service disconnected");
            }
        };

        wifiBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                updateWifiStateInfo();
            }
        };

        timeTickReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                runOnUiThread(() -> updateTime());
            }
        };

        screenLockReceiver = new ScreenLockReceiver();
        registerReceiver(screenLockReceiver, ScreenLockReceiver.intentFilter());

        plugInControlReceiver = new PlugInControlReceiver();
        registerReceiver(plugInControlReceiver, PlugInControlReceiver.intentFilter());

//        userPresentReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(final Context context, final Intent intent) {
//                setFullScreen();
//            }
//        };
//        registerReceiver(userPresentReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
        bindService(HttpServerService.getIntent(getApplicationContext()), httpServiceConnection, BIND_AUTO_CREATE | BIND_IMPORTANT);


//        startService(TouchDetectService.getIntent(getApplicationContext()));

    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putFloat(SCREEN_BRIGHTNESS_KEY, brightness);
    }

    @SuppressLint("NewApi")
    private void setFullScreen() {
        try {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;

            if (Build.VERSION.SDK_INT <= 17) {
                flags = flags | View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_LOW_PROFILE;
            } else {
                flags = flags
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                ;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        } catch (Throwable t) {
        }
    }

//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        /*
//         * This method JUST determines whether we want to intercept the motion.
//         * If we return true, onTouchEvent will be called and we do the actual
//         * scrolling there.
//         */
//
//
////        final int action = ev.getAction();//MotionEventCompat.getActionMasked(ev);
//
//        return true;
//    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.i("main", "touch even: " + ev);
        return false;
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.d("focus", "window focus change: " + hasFocus);
        if (hasFocus) {
//            setFullScreen();
            loadUrl(BASE_DATA_URL);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(httpServiceConnection);
        stopService(HttpServerService.getIntent(this));
        unregisterReceiver(screenLockReceiver);
        unregisterReceiver(plugInControlReceiver);
        unregisterReceiver(userPresentReceiver);
        unregisterReceiver(timeTickReceiver);
        screenLockReceiver = null;
        plugInControlReceiver = null;
        if (bluetoothClient != null) {
            bluetoothClient.close();
        }
        mJavaScriptGlue.unregisterCallbacks();
    }

    @Override
    protected void onStart() {
        super.onStart();
        String stage = "start";
        Log.d("lifecycle", this+": "+stage+", intent: "+getIntent());
        bindService(HttpServerService.getIntent(this), httpServiceConnection, BIND_AUTO_CREATE | BIND_IMPORTANT);
        registerWifiBroadcastReceiver();
        registerTimeTickReceiver();

//        ComponentName btServiceName = startService(BluetoothService.getIntent(this));
//        Log.d("[start]", "bt service started: "+btServiceName.toString());

//        Display d = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
//                .getDefaultDisplay();
//        Point dim = new Point();
//        d.getSize(dim);
//        Log.d("orientation on start", d.getName() + ": rotation: " + d.getRotation() + ", size: " + dim);
//        if (d.getRotation() == Surface.ROTATION_90 || d.getRotation() == Surface.ROTATION_180) {
//            if (!documentLoaded) {
//                documentLoaded = true;
//                mWebView.loadUrl(BASE_DATA_URL);
//            }
//        }



//        int btState = -1;
//        if (btAdapter != null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                    Log.i("BT", "Local address: " + btAdapter.getAddress());
//                }
//            } else {
//                Log.i("BT", "Local address: " + btAdapter.getAddress());
//            }
//            btState = btAdapter.getState();
//        }
//        updateTime();
//        updateWifiStateInfo();


        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter!=null) {
            int btState = btAdapter.getState();
            if (btState == BluetoothAdapter.STATE_ON || btState == BluetoothAdapter.STATE_TURNING_ON) {
                if (bluetoothClient == null) {
                    bluetoothClient = new BluetoothClient(btAdapter, "98:D3:41:F6:13:23", "00001101-0000-1000-8000-00805f9b34fb", new DataHandler<String>() {
                        @Override
                        public String onData(String requestPath, Map<String, String> params, String data) {
                            if (data != null) {
                                loadUrl("javascript:updateData?.()");
                            }
                            return null;
                        }
                    }, getApplicationContext());
                }
            } else {
                if (bluetoothClient != null) {
                    bluetoothClient.close();
                    bluetoothClient = null;
                }
            }
        }
    }

    private void registerTimeTickReceiver() {
        registerReceiver(timeTickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    private void registerWifiBroadcastReceiver() {
        registerReceiver(wifiBroadcastReceiver, new IntentFilter("android.net.wifi.STATE_CHANGE"));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Display d = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        Point dim = new Point();
        d.getSize(dim);
//        if (d.getRotation()==Surface.ROTATION_90  || d.getRotation()==Surface.ROTATION_180) {
//            if (!documentLoaded) {
//                documentLoaded = true;
//                mWebView.loadUrl(BASE_DATA_URL);
//            }
//        }
        Log.d("configuration change", d.getName() + ": rotation: " + d.getRotation() + ", size: " + dim);
    }

    @Override
    protected void onStop() {
//        Thread.dumpStack();
        super.onStop();
        String stage = "stop";
        Log.d("lifecycle", this+": "+stage);
//        unbindService(httpServiceConnection);
        unregisterReceiver(wifiBroadcastReceiver);
        unregisterReceiver(timeTickReceiver);


//        mWebView.pauseTimers();

//        Log.i("[stop]", "Power state on " + stage + ": " + Utils.getBatteryChargingStatus(getApplicationContext()));
//        Log.i("[stop]", "Screen state on " + stage + ": " + Utils.getScreenState(getApplicationContext()));
//        Log.i("[stop]", "WiFi state on " + stage + ": " + Utils.getWiFiState(getApplicationContext()));
//        Log.i("[stop]", "Window active on " + stage + ": " + getWindow().isActive());

//        if (bluetoothClient != null) {
//            bluetoothClient.close();
//        }

//        Window window = getWindow();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
//            setTurnScreenOn(true);
//        } else {
//            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
//        }
    }

    private void setBrightness(float brightness) {
        Window window = getWindow();
        WindowManager.LayoutParams layout = window.getAttributes();
        layout.screenBrightness = brightness;
        window.setAttributes(layout);
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        String stage = "pause";
//        Log.d("lifecycle", this+": "+stage);
//    }
//
    @Override
    protected void onResume() {
        super.onResume();
        String stage = "resume";
        Log.d("lifecycle", this + ": " + stage+", focus: "+hasWindowFocus());
        setFullScreen();
    }

////        ComponentName httpServiceName = startService(HttpServerService.getIntent(this));
////        Log.d("[start]", "http service started: " + httpServiceName.toString());
//        bindService(HttpServerService.getIntent(this), httpServiceConnection, BIND_AUTO_CREATE);//BIND_IMPORTANT);
//
//        Utils.enumerateUsbDevices(getApplicationContext());
//        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
//        int btState = -1;
//        if (btAdapter != null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                    Log.i("BT", "Local address: " + btAdapter.getAddress());
//                }
//            } else {
//                Log.i("BT", "Local address: " + btAdapter.getAddress());
//            }
//            btState = btAdapter.getState();
//        }
//        updateTime();
//        updateWifiStateInfo();
//        registerReceiver(timeTickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
//
////        Log.i("[resume]", "Power state on "+stage+": "+Utils.getBatteryChargingStatus(appCtx));
////        Log.i("[resume]", "Screen state on "+stage+": "+Utils.getScreenState(appCtx));
////        Log.i("[resume]", "WiFi state on "+stage+": "+Utils.getWiFiState(appCtx));
////        Log.i("[resume]", "Bluetooth state on "+stage+": "+Utils.getBluetoothState(appCtx));
////        Log.i("[resume]", "Window active on "+stage+": "+getWindow().isActive());
//
//        if (btAdapter!=null) {
//            if (btState == BluetoothAdapter.STATE_ON || btState == BluetoothAdapter.STATE_TURNING_ON) {
//                if (bluetoothClient == null) {
//                    bluetoothClient = new BluetoothClient(btAdapter, "98:D3:41:F6:13:23", "00001101-0000-1000-8000-00805f9b34fb", new DataHandler<String>() {
//                        @Override
//                        public String onData(String requestPath, Map<String, String> params, String data) {
//                            MainActivity.this.data = data;
//                            if (data != null) {
//                                runOnUiThread(() -> mWebView.loadUrl("javascript:update_values(android.getData())"));
//                            }
//                            return null;
//                        }
//                    }, getApplicationContext());
//                }
//            } else {
//                if (bluetoothClient != null) {
//                    bluetoothClient.close();
//                    bluetoothClient = null;
//                }
//            }
//        }
//
//        if (!documentLoaded) {
//            mWebView.loadUrl(BASE_DATA_URL);
//        } else {
//            updateTime();
//            updateWifiStateInfo();
//        }
//    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action==KeyEvent.ACTION_UP) {
                    startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                    overridePendingTransition(0, 0);
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action==KeyEvent.ACTION_UP) {
//                    startActivity(new Intent(this, AppListActivity.class));
                    Intent intent = new Intent(this, EmptyActivity.class);
                    intent.setFlags(FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    String updateWifiStateInfo() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String listenAddr;
        int wifiState = wm.getWifiState();
        mJavaScriptGlue.updateWifiState(wifiState);
        if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
            listenAddr = "&#x25CF&nbsp;";//getNetworkInfo();
        } else {
            listenAddr = "&#x26A0&nbsp;";
        }
//        String listenAddr = "&#x25CF&nbsp;";//getNetworkInfo();
//        mWebView.loadUrl("javascript:var e=document.getElementById('wifi-info');if(e){e.innerHTML='"+listenAddr+"';}");
        if (documentLoaded) {
            loadUrl("javascript:updateWifiInfo?.('" + listenAddr + "')");
        }
        return listenAddr;
    }

    void updateTime() {
        Time now = new Time(Time.getCurrentTimezone());
        now.setToNow();
        String timeStr = Utils.formatTime(now);
//        mWebView.loadUrl("javascript:var e=document.getElementById('clock');if(e){e.innerHTML='"+timeStr+"';}");
        if (documentLoaded) {
            loadUrl("javascript:updateClock?.('" + timeStr + "')");
        }
    }

    public void reload() {
        loadUrl(BASE_DATA_URL, true);
    }

    public void loadUrl(String url) {
        loadUrl(url, false);
    }
    public void loadUrl(String url, boolean reload) {
        if (reload || url.startsWith("javascript:") || !url.equals(mWebView.getOriginalUrl())) {
            runOnUiThread(() -> mWebView.loadUrl(url));
        } else {
            Log.d("WV", "not reloading url: "+url);
        }
    }

    public void loadUrl(String url, InputStream inputStream)  {
        inputStreams.put(url, inputStream);
        loadUrl("javascript:setImage('"+url+"')");
    }

    public InputStream removeInputStream(String key) {
        return inputStreams.remove(key);
    }

    public void setBrightness(int bright) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (checkSelfPermission(Manifest.permission.WRITE_SETTINGS)!=PackageManager.PERMISSION_GRANTED) {
//                Log.d("CFG", "Can't set brightness - missing permission "+Manifest.permission.WRITE_SETTINGS);
//                return;
//            }
//        }
        if (bright<0 && bright >255) {
            Log.w("CFG", "Invalid brightness level: "+bright);
            return;
        }

        ContentResolver cResolver = getContentResolver();
        try {
            int brightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);
            Log.d("CFG", "Current system brightness: "+brightness);
            Settings.System.putInt(
                    cResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            );
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, 255);
        } catch (Settings.SettingNotFoundException | SecurityException e) {
            Log.d("CFG", "Can't set global system brightness: "+e.getMessage());
//            e.printStackTrace();
        }

        if (bright>=-1 && bright<=255) {
            brightness = bright / 255f;
            Window window = getWindow();
            WindowManager.LayoutParams layout = window.getAttributes();
            layout.screenBrightness = brightness;
            window.setAttributes(layout);
            Log.d("CFG", "Window brightness set to " + layout.screenBrightness);
        }
    }

    public String getNetworkInfo() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        } else {
            return  "&#x26A0 No network (" + wm.getWifiState() +")";
        }
    }

    public void setDocumentLoaded(boolean loaded) {
        documentLoaded = loaded;
        if (documentLoaded) {
            Log.d("WEB", "loaded: "+mWebView.getOriginalUrl());
            if (BASE_DATA_URL.equals(mWebView.getOriginalUrl())) {
                updateTime();
                updateWifiStateInfo();
            }
            Log.d("WEB", "resource input store: "+inputStreams);
        }
    }

    public void saveResource(String fileName, byte[] data, boolean reload) throws IOException {
        if (fileName.isEmpty()) {
            fileName = "index.html";
        }
        try (FileOutputStream fos = openFileOutput(RESOURCE_ROOT+"_"+fileName, Context.MODE_PRIVATE)) {
            fos.write(data);
            Log.d("PUT", "Resource '"+fileName+"' saved to private file");
        } catch (IOException e) {
            Log.w("PUT", "Error saving '"+fileName+"' to private file: " + e.getMessage());
            File dir = new File(getFilesDir(), RESOURCE_ROOT);
            if (!dir.isDirectory() && !dir.mkdirs()) {
                throw new IOException("Resource save error - cant create root directory: "+dir.getAbsolutePath());
            }
            File file = new File(dir, fileName);

            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(data);
                Log.d("PUT", "Resource '"+fileName+"' saved to file: "+file.getAbsolutePath());
            }
        }
        if (reload) {
            reload();
        }
    }

    public InputStream getResource(String fileName) throws IOException {
        if (fileName.length()==0) {
            fileName="index.html";
        }

        try {
            FileInputStream fis = openFileInput(RESOURCE_ROOT+"_"+fileName);
            if (fis!=null) {
                Log.d("HTTP", "serving resource "+fileName+" from private file");
                return fis;
            }
        } catch (FileNotFoundException e) {
            // ignore and try file and asset
        }
        File dir = new File(getFilesDir(), RESOURCE_ROOT);
        File file = new File(dir, fileName);
        if (file.isFile()) {
            Log.d("HTTP", "serving resource "+fileName+" from: "+file.getAbsolutePath());
            return new FileInputStream(file);
        } else {
            Log.d("HTTP", "serving resource "+fileName+" from asset");
            return getAssets().open(fileName, AssetManager.ACCESS_STREAMING);
        }
    }

    public void reset() {
        Log.d("HTTP", "Listing local files");
        for (String f: fileList()) {
            Log.d("HTTP", "--- "+f);
            if (f.startsWith(RESOURCE_ROOT+"_")) {
                if (deleteFile(f)) {
                    Log.d("HTTP", "file deleted: "+f);
                }
            }
        }
        reload();
    }

    public void screenOn() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            KeyguardManager km = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
//            km.requestDismissKeyguard(this, null);
//        } else {
//            Intent intent = new Intent(this, EmptyActivity.class);
//            intent.setFlags(FLAG_ACTIVITY_NEW_TASK+FLAG_ACTIVITY_NO_ANIMATION);
//            intent.putExtra("finish", true);
//            startActivity(intent);
//        }

//        PowerManager pm;
//        PowerManager.WakeLock wl;
//
//        pm = (PowerManager) getSystemService(POWER_SERVICE);
//        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "pushwebview2:wakelock_on_screen_on");
//        wl.acquire(); //Or wl.acquire(timeout)
    }
}