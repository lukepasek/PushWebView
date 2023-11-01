package pl.net.xtech.pushwebview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;
import static android.view.View.STATUS_BAR_HIDDEN;

import java.util.Iterator;
import java.util.List;

import pl.net.xtech.pushwebview.MainActivity;
import pl.net.xtech.pushwebview.android.DeviceAdmin;

public class EmptyActivity extends Activity {

    private boolean mainStarting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("empty", "activity created");
        final Window window = getWindow();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        int windowFlags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;

        window.addFlags(windowFlags);
        WindowManager.LayoutParams layout = window.getAttributes();
        layout.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        layout.screenBrightness = 0;
        window.setAttributes(layout);

        View decorView = getWindow().getDecorView();
// Hide both the navigation bar and the status bar.
// SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
// a general rule, you should design your app to hide the status bar whenever you
// hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        decorView.setBackgroundColor(Color.BLACK);
        setFullScreen();
        new WebView(this).destroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("empty", "activity started: "+getIntent());

        DevicePolicyManager deviceManger = (DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName compName = new ComponentName(getApplicationContext(), DeviceAdmin.class);
        boolean active = deviceManger.isAdminActive(compName);
        if (!active) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable this application as device administrator in order to allow it to turn off the screen.");
            try {
                startActivityForResult(intent, 1);
            } catch (Throwable e) {
                Log.e("START", "Error enabling device admin: " + e.getMessage());
            }
//        } else {
//            Log.d("device admin", "enabled");
        } else {
//            Log.d("empty", "starting main activity");
            startMainActivity();
        }
    }

    private void startMainActivity() {
        if (!mainStarting) {
            mainStarting = true;
            Handler mHandler = new Handler();
            mHandler.postDelayed(() -> {
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                i.setFlags(FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(i);
                overridePendingTransition(0, 0);
                mainStarting = false;
            }, 10);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==1) {
            startMainActivity();
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
                if (action==KeyEvent.ACTION_UP) {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK+FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    @Override
    public void onBackPressed() {
        startMainActivity();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
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
}