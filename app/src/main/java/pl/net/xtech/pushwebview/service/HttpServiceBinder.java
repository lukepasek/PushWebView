package pl.net.xtech.pushwebview.service;


import android.os.Binder;

import pl.net.xtech.pushwebview.ByteDataHandler;
import pl.net.xtech.pushwebview.HttpServer;

public class HttpServiceBinder extends Binder {

    private final HttpServer httpServer;

    public HttpServiceBinder(HttpServer httpServer) {
        this.httpServer = httpServer;
    }


    public void addContextHandler(String data, ByteDataHandler dataHandler) {
        httpServer.addContextHandler(data, dataHandler);
    }
}