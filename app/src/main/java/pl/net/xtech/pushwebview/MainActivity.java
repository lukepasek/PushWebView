package pl.net.xtech.pushwebview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
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
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.LinearLayout;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import pl.net.xtech.pushwebview.handler.ApiHandler;
import pl.net.xtech.pushwebview.handler.HtmlDataHandler;
import pl.net.xtech.pushwebview.receiver.PlugInControlReceiver;
import pl.net.xtech.pushwebview.receiver.ScreenLockReceiver;
import pl.net.xtech.pushwebview.service.HttpServerService;
import pl.net.xtech.pushwebview.service.HttpServiceBinder;

public class MainActivity extends Activity {

    private LinearLayout mLinearLayout;
    protected WebView mWebView;
//    private TextView mTextView;
    private ServiceConnection httpServiceConnection;

    private BroadcastReceiver wifiBroadcastReceiver;

    public final static String BASE_DATA_URL = "file://data/";

    protected HashMap<String, InputStream> inputStreams = new HashMap<>();

    boolean documentLoaded = false;

    private String data;

//    private Timer timer;

    private BroadcastReceiver timeTickReciever;
    private BroadcastReceiver screenLockReceiver;
    private PlugInControlReceiver plugInControlReviever;
    private BluetoothClient bluetoothClient;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Window window = getWindow();
        window.requestFeature(Window.FEATURE_NO_TITLE);
//        window.requestFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        int windowFlags = WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
//                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_FULLSCREEN;// | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        window.addFlags(windowFlags);
        WindowManager.LayoutParams layout = window.getAttributes();
        layout.screenBrightness = -1.0f;
        layout.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        layout.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        window.setAttributes(layout);
        View decorView = window.getDecorView();
        decorView.setBackgroundColor(Color.BLACK);
        setFullScreen();

//        if (Build.VERSION.SDK_INT >= 21) {
//            getWindow().getDecorView().setSystemUiVisibility(
//                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav
//                            // bar
////                            | View.SYSTEM_UI_FLAG_IMMERSIVE
//            );
//        }

        setContentView(R.layout.main);
        mWebView = (WebView) findViewById(R.id.main_webview);
//        mWebView.clearView();
//        mLinearLayout = new LinearLayout(this);
//        mLinearLayout.setBackgroundColor(Color.BLACK);
//        mLinearLayout.setOrientation(LinearLayout.VERTICAL);
//        getWindow().setContentView(mLinearLayout);//, layout);

//        mWebView = new WebView(this);
//        Log.i("WebViewActivity", "UA: " + mWebView.getSettings().getUserAgentString());
//        mWebView.setLayoutParams(new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.MATCH_PARENT));
//
//        mLinearLayout.addView(mWebView);
        mWebView.setBackgroundColor(Color.BLACK);
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);

//        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {

            mWebView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
//                    setFullScreen();
                    int action = event.getAction();
                    if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
                        Log.i("touch", "event: " + event);
                        mWebView.loadUrl("javascript:clickAt(" + event.getRawX() + "," + event.getRawY() + "," + action + ")");
                    }
                    return true;
                }
            });

//        }
        mWebView.getSettings().setJavaScriptEnabled(true);
//        mWebView.getSettings().setUseWideViewPort(false);
//        mWebView.getSettings().setBuiltInZoomControls(false);
//        mWebView.getSettings().setAllowContentAccess(true);
//        mWebView.getSettings().setAllowFileAccess(true);
//        mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
//        mWebView.getSettings().setGeolocationEnabled(false);

        mWebView.addJavascriptInterface(new JSEntryPoint(this), "android");

        wifiBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                updateWifiStateInfo();
            }
        };

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                if (consoleMessage.messageLevel()==ConsoleMessage.MessageLevel.ERROR) {
                    Log.e("JS console", consoleMessage.messageLevel() + ": " + consoleMessage.message() + " at " + consoleMessage.lineNumber() + " in " + consoleMessage.sourceId());
                } else {
                    Log.d("JS console", consoleMessage.messageLevel() + ": " + consoleMessage.message() + " at " + consoleMessage.lineNumber() + " in " + consoleMessage.sourceId());
                }
//                throw new RuntimeException(consoleMessage.message()+" "+consoleMessage.sourceId()+" @"+consoleMessage.lineNumber());
                return true;
//                    return super.onConsoleMessage(consoleMessage);
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Log.d("JS alert", message);
                return super.onJsAlert(view, url, message, result);
            }
        });

        mWebView.setWebViewClient(new RequestInterceptor(MainActivity.this));

        httpServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                HttpServiceBinder httpServiceBinder = (HttpServiceBinder) service;
                httpServiceBinder.addContextHandler("data", new HtmlDataHandler(MainActivity.this));
                httpServiceBinder.addContextHandler("api", new ApiHandler(MainActivity.this));
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

//        setBrightness(127);

        timeTickReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                runOnUiThread(() -> updateTime());
            }
        };

        screenLockReceiver = new ScreenLockReceiver();
        IntentFilter screenLockIntentFilter = new IntentFilter();
        screenLockIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenLockIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenLockIntentFilter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenLockReceiver, screenLockIntentFilter);

        plugInControlReviever = new PlugInControlReceiver();
        IntentFilter plugInControlFilter = new IntentFilter();
        screenLockIntentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        screenLockIntentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        screenLockIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(plugInControlReviever, plugInControlFilter);

    }

    @SuppressLint("NewApi")
    private void setFullScreen() {
        try {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;


//            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

            if (Build.VERSION.SDK_INT<=17) {
                flags = flags | View.SYSTEM_UI_FLAG_LOW_PROFILE;
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
        Log.i("main", "touch even: "+ev);
        // Here we actually handle the touch event (e.g. if the action is ACTION_MOVE,
        // scroll this container).
        // This method will only be called if the touch event was intercepted in
        // onInterceptTouchEvent
        setFullScreen();
        return false;
    }


    @Override
    public void onBackPressed() {
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.d("focus", "window focus change: "+hasFocus);
        setFullScreen();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(HttpServerService.getIntent(this));
        unregisterReceiver(screenLockReceiver);
        unregisterReceiver(plugInControlReviever);
        screenLockReceiver = null;
        if (bluetoothClient!=null) {
            bluetoothClient.close();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("lifecycle", "start");
        registerReceiver(wifiBroadcastReceiver, new IntentFilter("android.net.wifi.STATE_CHANGE"));
        ComponentName httpServiceName = startService(HttpServerService.getIntent(this));
        Log.d("[start]", "http service started: "+httpServiceName.toString());
        bindService(HttpServerService.getIntent(this), httpServiceConnection, BIND_IMPORTANT);

//        ComponentName btServiceName = startService(BluetoothService.getIntent(this));
//        Log.d("[start]", "bt service started: "+btServiceName.toString());

        Display d = ((WindowManager)getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        Point dim = new Point();
        d.getSize(dim);
        Log.d("orientation on start", d.getName()+": rotation: "+d.getRotation()+", size: "+dim);
//        if (!documentLoaded && d.getRotation()==Surface.ROTATION_90  || d.getRotation()==Surface.ROTATION_180) {
//            if (!documentLoaded) {
//                documentLoaded = true;
//                mWebView.loadUrl(BASE_DATA_URL);
//            }
//        }

        DevicePolicyManager deviceManger = (DevicePolicyManager)
                getSystemService(Context. DEVICE_POLICY_SERVICE ) ;
        ComponentName compName = new ComponentName( MainActivity.this, DeviceAdmin.class ) ;
        boolean active = deviceManger.isAdminActive( compName ) ;
        if (!active) {
            Intent intent = new Intent(DevicePolicyManager. ACTION_ADD_DEVICE_ADMIN) ;
            intent.putExtra(DevicePolicyManager. EXTRA_DEVICE_ADMIN , compName ) ;
            intent.putExtra(DevicePolicyManager. EXTRA_ADD_EXPLANATION , "You should enable the app!" ) ;
            try {
                startActivityForResult(intent, 1);
            } catch (Throwable e) {
                Log.e("START", "Error enabling device admin: " + e.getMessage());
            }
        } else {
            Log.d("device admin", "enabled");
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Display d = ((WindowManager)getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        Point dim = new Point();
        d.getSize(dim);
//        if (d.getRotation()==Surface.ROTATION_90  || d.getRotation()==Surface.ROTATION_180) {
//            if (!documentLoaded) {
//                documentLoaded = true;
//                mWebView.loadUrl(BASE_DATA_URL);
//            }
//        }
        Log.d("orientation change", d.getName()+": rotation: "+d.getRotation()+", size: "+dim);
    }

    @Override
    protected void onStop() {
        super.onStop();
        String stage = "stop";
        Log.d("lifecycle", stage);
        unbindService(httpServiceConnection);
        unregisterReceiver(wifiBroadcastReceiver);

        Log.i("[stop]", "Power state on "+stage+": "+Utils.getBatteryChargingStatus(getApplicationContext()));
        Log.i("[stop]", "Screen state on "+stage+": "+Utils.getScreenState(getApplicationContext()));
        Log.i("[stop]", "WiFi state on "+stage+": "+Utils.getWiFiState(getApplicationContext()));
        Log.i("[stop]", "Window active on "+stage+": "+getWindow().isActive());

        if (bluetoothClient!=null) {
            bluetoothClient.close();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("lifecycle", "pause");
        unregisterReceiver(timeTickReciever);
//        if (timer!=null) {
//            timer.cancel();
//            timer = null;
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Context appCtx = getApplicationContext();
        String stage = "resume";
        Log.d("lifecycle", stage);
        Utils.enumerateUsbDevices(getApplicationContext());
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        int btState = -1;
        if (btAdapter!=null) {
            Log.i("BT", "Local address: " + btAdapter.getAddress());
            btState = btAdapter.getState();
        }
        updateTime();
        updateWifiStateInfo();
        registerReceiver(timeTickReciever, new IntentFilter(Intent.ACTION_TIME_TICK));

        Log.i("[resume]", "Power state on "+stage+": "+Utils.getBatteryChargingStatus(appCtx));
        Log.i("[resume]", "Screen state on "+stage+": "+Utils.getScreenState(appCtx));
        Log.i("[resume]", "WiFi state on "+stage+": "+Utils.getWiFiState(appCtx));
        Log.i("[resume]", "Bluetooth state on "+stage+": "+Utils.getBluetoothState(appCtx));
        Log.i("[resume]", "Window active on "+stage+": "+getWindow().isActive());

        if (btAdapter!=null) {
            if (btState == BluetoothAdapter.STATE_ON || btState == BluetoothAdapter.STATE_TURNING_ON) {
                if (bluetoothClient == null) {
                    bluetoothClient = new BluetoothClient(btAdapter, "98:D3:41:F6:13:23", "00001101-0000-1000-8000-00805f9b34fb", new DataHandler<String>() {
                        @Override
                        public String onData(String requestPath, Map<String, String> params, String data) {
                            MainActivity.this.data = data;
                            if (data != null) {
                                runOnUiThread(() -> mWebView.loadUrl("javascript:update_values(android.getData())"));
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

        if (!documentLoaded) {
            documentLoaded = true;
            mWebView.loadUrl(BASE_DATA_URL);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action==KeyEvent.ACTION_UP) {
                    startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    String updateWifiStateInfo() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String listenAddr;
        if (wm.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            listenAddr = "&#x25CF&nbsp;";//getNetworkInfo();
        } else {
            listenAddr = "&#x26A0&nbsp;";
        }
//        String listenAddr = "&#x25CF&nbsp;";//getNetworkInfo();
//        mWebView.loadUrl("javascript:var e=document.getElementById('wifi-info');if(e){e.innerHTML='"+listenAddr+"';}");
        mWebView.loadUrl("javascript:updateWifiInfo('"+listenAddr+"')");
        return listenAddr;
    }

    void updateTime() {
        Time now = new Time(Time.getCurrentTimezone());
        now.setToNow();
        String timeStr = Utils.formatTime(now);
//        mWebView.loadUrl("javascript:var e=document.getElementById('clock');if(e){e.innerHTML='"+timeStr+"';}");
        mWebView.loadUrl("javascript:updateClock('"+timeStr+"')");
    }

    public void reset() {
        mWebView.loadUrl(BASE_DATA_URL);
    }

    public void addInputStream(String key, ByteArrayInputStream byteArrayInputStream) {
        inputStreams.put(key, byteArrayInputStream);
    }

    public void loadUrl(String url) {
        runOnUiThread( () -> mWebView.loadUrl(url) );
    }

    public InputStream removeInputStream(String key) {
        return inputStreams.remove(key);
    }

    public void setBrightness(int bright) {
        if (bright<0 && bright >255) {
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
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        WindowManager.LayoutParams layout = getWindow().getAttributes();
//                            layout.flags = android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        layout.screenBrightness = bright/255f;
        Log.d("CFG", "Window brightness set to "+layout.screenBrightness);
        getWindow().setAttributes(layout);
    }

    public String getData() {
        return data;
    }

    public String getNetworkInfo() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        } else {
            return  "&#x26A0 No network (" + wm.getWifiState() +")";
        }
    }
}