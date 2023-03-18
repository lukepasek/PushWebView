package pl.net.xtech.pushwebview;

import java.io.IOException;
import java.util.Map;

public interface ByteDataHandler {
    public byte[] onData(String requestPath, Map<String, String> params, byte[] data);// throws IOException;
}
