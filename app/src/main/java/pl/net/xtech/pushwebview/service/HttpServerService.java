package pl.net.xtech.pushwebview.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;

import java.io.IOException;

public class HttpServerService extends Service {

    private final static int ListenPort = 3478;

    private HttpServer httpServer = null;
    private HttpServiceBinder mBinder = null;

    public HttpServerService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            httpServer = new HttpServer(ListenPort);
            mBinder = new HttpServiceBinder(httpServer);
            httpServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        httpServer.stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @NonNull
    public static Intent getIntent(Context context) {
        return new Intent(context, HttpServerService.class);
    }

}