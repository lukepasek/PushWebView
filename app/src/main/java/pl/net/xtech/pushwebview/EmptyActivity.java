package pl.net.xtech.pushwebview;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import static android.view.View.STATUS_BAR_HIDDEN;

public class EmptyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Window window = getWindow();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        int windowFlags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_FULLSCREEN;
        window.addFlags(windowFlags);
        WindowManager.LayoutParams layout = window.getAttributes();
        layout.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        layout.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE;
        layout.screenBrightness = -1f;
        window.setAttributes(layout);

        View decorView = getWindow().getDecorView();
// Hide both the navigation bar and the status bar.
// SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
// a general rule, you should design your app to hide the status bar whenever you
// hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        View v = new View(this);
        v.setBackgroundColor(Color.BLACK);
        getWindow().setContentView(v);
    }

    @Override
    protected void onStart() {
        super.onStart();
        processIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void processIntent(Intent intent) {
        if (intent.getBooleanExtra("finish", false) == true) {
            Intent newIntent = new Intent(this, MainActivity.class);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(newIntent);
        }
    }
}