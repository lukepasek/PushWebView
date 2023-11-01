package pl.net.xtech.pushwebview.handler;

import java.io.InputStream;
import java.util.Map;

public interface ByteDataHandler {
    public byte[] onData(String method, String requestPath, Map<String, String> params, Map<String, String> headers, byte[] data);// throws IOException;
}
