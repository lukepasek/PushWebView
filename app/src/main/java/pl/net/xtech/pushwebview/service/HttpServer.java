package pl.net.xtech.pushwebview.service;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import fi.iki.elonen.NanoHTTPD;
import pl.net.xtech.pushwebview.handler.ByteDataHandler;

public class HttpServer extends NanoHTTPD {

//    private HashMap<String, WeakReference<ByteDataHandler>> listeners = new HashMap<>();
        private HashMap<String, ByteDataHandler> listeners = new HashMap<>();

        public HttpServer(int listenPort) throws IOException {
            super(listenPort);
        }

//        private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
//
//        // Instantiates the queue of Runnables as a LinkedBlockingQueue
//        private final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
//
//        // Sets the amount of time an idle thread waits before terminating
//        private static final int KEEP_ALIVE_TIME = 1;
//        // Sets the Time Unit to seconds
//        private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
//
//        // Creates a thread pool manager
//        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
//                NUMBER_OF_CORES,       // Initial pool size
//                NUMBER_OF_CORES,       // Max pool size
//                KEEP_ALIVE_TIME,
//                KEEP_ALIVE_TIME_UNIT,
//                workQueue
//        );

        @Override
        public Response serve(IHTTPSession session) {
            String ctx = session.getUri().replaceFirst("^/", "").replaceFirst("/.*$", "");
            ByteDataHandler l = getListener(ctx);

            long bodySize = 0;
            if (l!=null) {
                byte[] resultData = null;
                String requestMethod = session.getMethod().name();
                if ("POST".equals(requestMethod) || "PUT".equals(requestMethod)) {
                    Map <String,String> headers  = session.getHeaders();
                    int size = 0;
                    if (headers.containsKey("content-length")) {
                        size = Integer.parseInt(headers.get("content-length"));
//                    } else if (splitbyte < rlen) {
//                        size = rlen - splitbyte;
                    } else {
                        size = 0;
                    }
//                    Log.d("HTTP", "expecting "+size+" bytes");
                    int read = 0;

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    InputStream in = session.getInputStream();
//                    resultData = l.onData(session.getMethod().name(), session.getUri(), session.getParms(), session.getHeaders(), in);
                    byte[] buf = new byte[1967];

//                    Map<String, String> files = new HashMap<>();
//                    try {
//                        session.parseBody(files);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    } catch (ResponseException e) {
//                        e.printStackTrace();
//                    }
//                    if (files.size()==0) {
//                        if (size>0 && size<=1024*1024) {
//                            byte[] inBuf = new byte[size];
//                            int inBytes = 0;
//                            try {
//                                while (inBytes<size) {
//                                    int read = in.read(inBuf, 0, (size-inBytes));
//                                    inBytes += read;
//                                    out.write(inBuf, 0, read);
//                                }
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        } else {

                            try {
                                while (read<size) {
                                    int max = (size-read);
                                    int r = in.read(buf, 0, max<buf.length?max:buf.length);
                                    if (r>-1) {
                                        read = read + r;
                                        out.write(buf, 0, r);
//                                        Log.d("WEB", "read "+r+ " total "+read+" of "+size);
                                    } else {
                                        break;
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
//                        try {
//                            in.close();
//                        } catch (IOException e) {}
                            }
//                        }
                        bodySize = out.size();
//                    } else {
//                        Log.d("HTTP", "got files from body: "+files.size());
//                        for (String f: files.keySet()) {
//                            Log.d("HTTP", "got files from body: "+files.size());
//                        }
//                    }
                    byte[] data = out.toByteArray();
                    Log.d("HTTP", "received "+data.length+" bytes of "+size);
                    resultData = l.onData(requestMethod, session.getUri(), session.getParms(), session.getHeaders(), data);
                } else if (session.getMethod().name().equals("GET")) {
                    resultData = l.onData(requestMethod, session.getUri(), session.getParms(), session.getHeaders(), ((byte[])null));
                }
                if (resultData!=null)
                    return new Response(Response.Status.OK, null, new ByteArrayInputStream(resultData));
                else
                    return new Response(Response.Status.NO_CONTENT, null, "");
            } else {
                String msg = "<html><body><h1>Push WebView</h1>\n";
                msg += "<p> No handler for context: '" +ctx+ "'</p>";
                msg += "<p> request uri: " + session.getUri() + "</p>";
                msg += "<p> context: '" + ctx + "'</p>";
                msg += "<p> param count: '" + session.getParms().size() + "'</p>";
                msg += "<p> available contexts: </p>";
                msg += "<ul>";
                for (String k: listeners.keySet()) {
                    msg += "<li><a href=\"/"+k+"\">"+k+"</a></li>";
                }
                msg += "</ul>";
                return new Response(msg + "</body></html>\n");
            }
        }

    private ByteDataHandler getListener(String ctx) {
        if (listeners.containsKey(ctx)) {
            return  listeners.get(ctx);
//            WeakReference<ByteDataHandler> rl = listeners.get(ctx);
//            if (rl != null) {
//                ByteDataHandler l = rl.get();
//                if (l==null) {
//                    Log.w("HTTP","Invalid application state!");
//                }
//                return l;
//            }
        }
        return null;
    }

    private void addListener(String ctx, ByteDataHandler listener) {
        if (listeners.containsKey(ctx)) {
            listeners.remove(ctx);
        }
        listeners.put(ctx, listener);
//        new WeakReference<ByteDataHandler>(listener)
    }

    public void addContextHandler(String context, ByteDataHandler listener) {
        if (context.startsWith("/")) {
            addListener(context.replaceFirst("^/", ""), listener);
        } else {
            addListener(context, listener);
        }
    }

}