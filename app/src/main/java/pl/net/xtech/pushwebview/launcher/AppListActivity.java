package pl.net.xtech.pushwebview.launcher;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pl.net.xtech.pushwebview.R;
import pl.net.xtech.pushwebview.android.Utils;

public class AppListActivity extends Activity {

    private static final String TAG = "AppListActivity";

    private BroadcastReceiver timeTickReceiver;
    private ScrollView mScrollView;
    
    private boolean showAppActions = false;
    private LinearLayout mAppLayout;

    private TextView clock;
    private TextView date;

    private Typeface webfont;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webfont = Typeface.createFromAsset( getAssets(), "fontawesome-webfont.ttf" );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.app_list);

        mScrollView = (ScrollView) findViewById(R.id.scroll);

        LinearLayout innerLayout = mScrollView.findViewById(R.id.scroll_list_layout);


        mScrollView.setVerticalScrollBarEnabled(false);
        mScrollView.setHorizontalScrollBarEnabled(false);

        timeTickReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                runOnUiThread(() -> {
                    if (clock!=null) {
                        clock.setText(getTimeStr());
                    }
                    }
                );
            }
        };

        int decorFlags = getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LOW_PROFILE;
        getWindow().getDecorView().setSystemUiVisibility(decorFlags);

        mAppLayout = innerLayout;
        updateAppList();
    }

    private final static int NORMAL = 0;
    private final static int FAVORITE = 1;
    private final static int HIDDEN = 2;
    private void updateAppList() {
        LinearLayout layout = AppListActivity.this.mAppLayout;
        boolean showActions = AppListActivity.this.showAppActions;

        Log.d(TAG, "update app list: "+ showActions);

        runOnUiThread(() -> {
            layout.removeAllViewsInLayout();
            clock = (TextView) LayoutInflater.from(this).inflate(R.layout.text_big, null);
            clock.setText(getTimeStr());
            clock.setOnLongClickListener(v -> {
                AppListActivity.this.showAppActions = !AppListActivity.this.showAppActions;
                updateAppList();
                return true;
            });
            layout.addView(clock);
            date = (TextView) LayoutInflater.from(this).inflate(R.layout.text_left, null);
            date.setText(" " + getDateStr());
            layout.addView(date);
        });


        new Thread() {
            @Override
            public void run() {
                Map<String, Intent> apps = getInstalledApps();
                LinkedList<String> favoritesApps = new LinkedList<>();
                LinkedList<String> normalApps = new LinkedList<>();
                LinkedList<String> hiddenApps = new LinkedList<>();

                SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
                for (String key: apps.keySet()) {
                    int appState = sharedPreferences.getInt(key, -1);
                    Log.d(TAG, "app: "+ key+" mode: "+appState);

                    if (appState == FAVORITE) {
                        favoritesApps.add(key);
                    } else if (appState == HIDDEN) {
                        hiddenApps.add(key);
                    } else {
                        normalApps.add(key);
                    }
                }

                String[] favoriteAppNames = (String[]) favoritesApps.toArray(new String[favoritesApps.size()]);
                String[] defaultAppNames = (String[]) normalApps.toArray(new String[normalApps.size()]);

                Arrays.sort(favoriteAppNames);
                Arrays.sort(defaultAppNames);
                runOnUiThread(() -> {
                    for (String appName : favoriteAppNames) {
                        addAppToView(appName, apps.get(appName), showActions, FAVORITE);
                    }
                    LinearLayout ll1 = new LinearLayout(AppListActivity.this);
                    TextView sep1 = (TextView) LayoutInflater.from(AppListActivity.this).inflate(R.layout.text, null);
                    sep1.setText("");
                    ll1.addView(sep1);
                    layout.addView(ll1);
                    for (String appName : defaultAppNames) {
                        addAppToView(appName, apps.get(appName), showActions, NORMAL);
                    }
                    LinearLayout ll2 = new LinearLayout(AppListActivity.this);
                    TextView sep2 = (TextView) LayoutInflater.from(AppListActivity.this).inflate(R.layout.text, null);
                    sep1.setText("");
                    ll2.addView(sep2);
                    layout.addView(ll2);
                    if (showActions) {
                        for (String appName : hiddenApps) {
                            addAppToView(appName, apps.get(appName), showActions, HIDDEN);
                        }
                    }
                });
            }

            private void addAppToView(String appName, Intent intent, boolean showActions, int mode) {

                TextView tv = (TextView) LayoutInflater.from(AppListActivity.this).inflate(R.layout.text, null);

                String text;
                if (appName.length()<20) {
                    text = "  " + appName + "  ";
                } else {
                    text = "  " + appName.substring(0, 20) +"..."+"  ";
                }
//                if (showActions) {
//                    if (mode==FAVORITE) {
//                        text = "   \u2605   " + text;
//                    } else if (mode==HIDDEN) {
//                        text = "   \u25cc   " + text;
//                    }
//                }
                tv.setText(text);
                tv.setOnClickListener(v -> {
                    startActivity(intent);
                });
                LinearLayout ll = new LinearLayout(AppListActivity.this);

                ll.setOrientation(LinearLayout.HORIZONTAL);
                ll.setGravity(Gravity.END);
                ll.addView(tv);

                if (showActions) {

                    TextView favButton = (TextView) LayoutInflater.from(AppListActivity.this).inflate(R.layout.text, null);
//                    Button favButton = new Button(AppListActivity.this);
                    favButton.setBackgroundColor(Color.TRANSPARENT);
                    favButton.setTypeface(webfont);
                    if (mode==FAVORITE) {
                        favButton.setText("  \uf006  ");
                    } else {
                        favButton.setText("  \uf005  ");
                    }
                    favButton.setOnClickListener(v -> {
                        if (mode!=FAVORITE) {
                            getPreferences(MODE_PRIVATE).edit().putInt(appName, FAVORITE).commit();
                        } else {
                            getPreferences(MODE_PRIVATE).edit().putInt(appName, NORMAL).commit();
                        }
                        updateAppList();
                    });
                    TextView hideButton = (TextView) LayoutInflater.from(AppListActivity.this).inflate(R.layout.text, null);
                    hideButton.setBackgroundColor(Color.TRANSPARENT);
                    if (mode==HIDDEN) {
                        hideButton.setText("  \uf06e  ");
                    } else {
                        hideButton.setText("  \uf070  ");
                    }
                    hideButton.setTypeface(webfont);
                    hideButton.setOnClickListener(v -> {
                        if (mode!=HIDDEN) {
                            getPreferences(MODE_PRIVATE).edit().putInt(appName, HIDDEN).commit();
                        } else {
                            getPreferences(MODE_PRIVATE).edit().putInt(appName, NORMAL).commit();
                        }
                        updateAppList();
                    });
                    ll.addView(favButton);
                    ll.addView(hideButton);
                }
                layout.addView(ll);
            }
        }.run();
    }

    private String getDateStr() {
        Time now = new Time(Time.getCurrentTimezone());
        now.setToNow();
        return "17/04/2023";
    }

    private String getTimeStr() {
        Time now = new Time(Time.getCurrentTimezone());
        now.setToNow();
        return Utils.formatTime(now);
    }

    private Map<String, Intent> getInstalledApps() {
        PackageManager pm = getPackageManager();
        Map<String, Intent> apps = new HashMap<>();
        List<PackageInfo> packs = pm.getInstalledPackages(0);
        //List<PackageInfo> packs = getPackageManager().getInstalledPackages(PackageManager.GET_PERMISSIONS);
        for (int i = 0; i < packs.size(); i++) {
            PackageInfo p = packs.get(i);
//            if ((!isSystemPackage(p))) {
                String appName = p.applicationInfo.loadLabel(pm).toString();
//                Drawable icon = p.applicationInfo.loadIcon(getPackageManager());
//                String packages = p.applicationInfo.packageName;
//                        Log.d(TAG, appName+": " + p.applicationInfo);
            Intent launchIntent = pm.getLaunchIntentForPackage(p.packageName);
//            startActivity( launchIntent );

            if (launchIntent!=null) {
                apps.put(appName, launchIntent);
            }
//            }
        }
        return apps;
    }

    private boolean isSystemPackage(PackageInfo pkgInfo) {
        return (pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(timeTickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        mScrollView.scrollTo(0, 0);
        timeTickReceiver.onReceive(this, new Intent(Intent.ACTION_TIME_TICK));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(timeTickReceiver);
    }

    @Override
    public void onBackPressed() {
//        mScrollView.scrollTo(0, 0);
        ObjectAnimator objectAnimator = ObjectAnimator.ofInt(mScrollView, "scrollY", 0).setDuration(50);
        objectAnimator.start();
        if (showAppActions) {
            showAppActions = false;
            updateAppList();
        }
    }
}