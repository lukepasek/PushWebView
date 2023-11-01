package pl.net.xtech.pushwebview.client;

import java.util.Map;

public interface DataHandler<T> {
    public T onData(String requestPath, Map<String, String> params, T data);
}
