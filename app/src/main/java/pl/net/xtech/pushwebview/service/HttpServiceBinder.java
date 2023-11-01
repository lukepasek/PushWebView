package pl.net.xtech.pushwebview.service;


import android.os.Binder;

import pl.net.xtech.pushwebview.handler.ByteDataHandler;

public class HttpServiceBinder extends Binder {

    private final HttpServer httpServer;

    public HttpServiceBinder(HttpServer httpServer) {
        this.httpServer = httpServer;
    }


    public void addContextHandler(String path, ByteDataHandler dataHandler) {
        httpServer.addContextHandler(path, dataHandler);
    }
}