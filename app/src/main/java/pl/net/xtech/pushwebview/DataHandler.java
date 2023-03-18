package pl.net.xtech.pushwebview;

import java.util.Map;

public interface DataHandler<T> {
    public T onData(String requestPath, Map<String, String> params, T data);
}
