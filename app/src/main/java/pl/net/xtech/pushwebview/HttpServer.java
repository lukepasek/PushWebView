package pl.net.xtech.pushwebview;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class HttpServer extends NanoHTTPD {

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
            ByteDataHandler l = listeners.get(ctx);
            long bodySize = 0;
            if (l!=null) {
                byte[] resultData = null;
                if (session.getMethod().name().equals("POST")) {
                    Map <String,String> headers  = session.getHeaders();
                    int size = 0;
                    if (headers.containsKey("content-length")) {
                        size = Integer.parseInt(headers.get("content-length"));
//                    } else if (splitbyte < rlen) {
//                        size = rlen - splitbyte;
                    } else {
                        size = 0;
                    }
                    System.out.println("expecting "+size+" bytes");

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    InputStream in = session.getInputStream();
                    Map<String, String> files = new HashMap<>();
//                    try {
//                        session.parseBody(files);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    } catch (ResponseException e) {
//                        e.printStackTrace();
//                    }
                    if (files.size()==0) {
                        if (size>0 && size<=1024*8) {
                            byte[] inBuf = new byte[size];
                            int inBytes = 0;
                            try {
                                while (inBytes<size) {
                                    int read = in.read(inBuf, 0, (size-inBytes));
                                    inBytes += read;
                                    out.write(inBuf, 0, read);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
//                    byte[] buf = new byte[1024];
                            int available = -1;
                            try {
                                do {
                                    available = in.available();
                                    for (int i = 0; i < available; i++) {
                                        out.write(in.read());
                                    }
                                } while (available > 0);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
//                        try {
//                            in.close();
//                        } catch (IOException e) {}
                            }
                        }
                        bodySize = out.size();
                    } else {
                        System.out.println("got files from body: "+files.size());
                        for (String f: files.keySet()) {
                            System.out.println("got files from body: "+files.size());
                        }
                    }
                    resultData = l.onData(session.getUri(), session.getParms(), out.toByteArray());
                } else if (session.getMethod().name().equals("GET")) {
                    resultData = l.onData(session.getUri(), session.getParms(), null);
                }
                if (resultData!=null)
                    return new Response(Response.Status.OK, null, new ByteArrayInputStream(resultData));
                else
                    return new Response("");
            } else {
                String msg = "<html><body><h1>Hello server</h1>\n";
                msg += "<p> request uri: " + session.getUri() + "</p>";
                msg += "<p> context: " + ctx + "</p>";
                return new Response(msg + "</body></html>\n");
            }
        }

        public void addContextHandler(String context, ByteDataHandler listener) {
            if (context.startsWith("/")) {
                listeners.put(context.replaceFirst("^/", ""), listener);
            } else {
                listeners.put(context, listener);
            }
        }

}