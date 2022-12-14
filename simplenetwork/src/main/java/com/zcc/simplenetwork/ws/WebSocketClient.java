package com.zcc.simplenetwork.ws;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;


import com.zcc.simplenetwork.L;
import com.zcc.simplenetwork.threads.SafeHandlerThread;

import com.zcc.simplenetwork.framework.Header;
import com.zcc.simplenetwork.framework.HttpException;
import com.zcc.simplenetwork.framework.NameValuePair;
import com.zcc.simplenetwork.framework.StatusLine;
import com.zcc.simplenetwork.framework.HttpResponseException;

import org.apache.http.conn.ConnectTimeoutException;

import com.zcc.simplenetwork.framework.BasicLineParser;
import com.zcc.simplenetwork.framework.BasicNameValuePair;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;


public class WebSocketClient {

    private static TrustManager[] sTrustManagers;
    private final Object mSendLock = new Object();
    private final Object mConFlagLock = new Object();
    /**
     * The default port of WebSockets, as defined in the spec. If the nullary
     * constructor is used, DEFAULT_PORT will be the port the WebSocketServer
     * is binded to. Note that ports under 1024 usually require root permissions.
     */
    int DEFAULT_PORT = 80;
    /**
     * The default wss port of WebSockets, as defined in the spec. If the nullary
     * constructor is used, DEFAULT_WSS_PORT will be the port the WebSocketServer
     * is binded to. Note that ports under 1024 usually require root permissions.
     */
    int DEFAULT_WSS_PORT = 443;
    private URI mURI;
    private IWebSocketStateListener mIWebSocketStateListener;
    private Socket mSocket;
    private Thread mThread;
    private SafeHandlerThread mHandlerThread;
    private Handler mHandler;
    private List<BasicNameValuePair> mExtraHeaders;
    private WSParser mParser;
    private boolean mConnected;
    private long mTimeout = 0;

    public WebSocketClient(URI uri, IWebSocketStateListener IWebSocketStateListener, List<BasicNameValuePair> extraHeaders) {
        this(uri, IWebSocketStateListener, extraHeaders, 0);
    }

    public WebSocketClient(URI uri, IWebSocketStateListener IWebSocketStateListener, List<BasicNameValuePair> extraHeaders, long mTimeoutMs) {
        this.mTimeout = mTimeout;
        this.mURI = uri;
        this.mIWebSocketStateListener = IWebSocketStateListener;
        this.mExtraHeaders = extraHeaders;
        synchronized (this.mConFlagLock) {
            this.mConnected = false;
        }
        this.mParser = new WSParser(this);
        this.mHandlerThread = new SafeHandlerThread("websocket-thread");
        this.mHandlerThread.start();
        this.mHandlerThread.waitUntilReady();
        this.mHandler = new Handler(this.mHandlerThread.getLooper());
    }

    public static void setTrustManagers(TrustManager[] tm) {
        sTrustManagers = tm;
    }

    private static SocketFactory getTrustAllHostsSocketFactory() {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, sTrustManagers, new SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception var1) {
            var1.printStackTrace();
            return null;
        }
    }

    public void release() {
        mIWebSocketStateListener = null;
        synchronized (mSendLock) {
            if (mSocket != null) {
                if (mSocket.isConnected()) {
                    mParser.close(100, "release connection");
                    try {
                        mSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            mHandlerThread.quit();
            mHandlerThread = null;
            mSocket = null;
        }
    }

    public IWebSocketStateListener getListener() {
        return this.mIWebSocketStateListener;
    }

    public void connect() {
        this.mThread = new Thread(new Runnable() {
            public void run() {
                int port = mURI.getPort() != -1 ? mURI.getPort() :
                        (!mURI.getScheme().equals("wss") && !mURI.getScheme().equals("https") ? 80 : 443);
                String path = TextUtils.isEmpty(mURI.getPath()) ? "/" : mURI.getPath();
                if (!TextUtils.isEmpty(mURI.getRawQuery())) {
                    path = path + "?" + mURI.getRawQuery();
                }
                String originScheme = mURI.getScheme().equals("wss") ? "https" : "http";
                try {
                    new URI(originScheme, "//" + mURI.getHost(), null);
                } catch (URISyntaxException var16) {
                    var16.printStackTrace();
                    return;
                }

                SocketFactory factory = !mURI.getScheme().equals("wss") &&
                        !mURI.getScheme().equals("https")
                        ? SocketFactory.getDefault()
                        : WebSocketClient.getTrustAllHostsSocketFactory();

                try {
                    mSocket = createSocket(factory, mURI.getHost(), port, 0);
                } catch (ConnectTimeoutException var13) {
                    if (mIWebSocketStateListener != null)
                        mIWebSocketStateListener.onDisconnect(-3, "Socket Connect Timeout!");
                    var13.printStackTrace();
                    return;
                } catch (UnknownHostException var14) {
                    if (mIWebSocketStateListener != null)
                        mIWebSocketStateListener.onDisconnect(-3, "Socket Connect UnknownHostException!");
                    var14.printStackTrace();
                    return;
                } catch (IOException var15) {
                    if (mIWebSocketStateListener != null)
                        mIWebSocketStateListener.onDisconnect(-3, "Socket Connect failed!");
                    var15.printStackTrace();
                    return;
                }

                try {
                    if (mSocket.getSoTimeout() == 0 || mSocket.getSoTimeout() > 60000) {
                        mSocket.setSoTimeout(0);
                    }

                    PrintWriter out = new PrintWriter(mSocket.getOutputStream());
                    out.print("GET " + path + " HTTP/1.1\r\n");
                    out.print("Upgrade: websocket\r\n");
                    out.print("Connection: Upgrade\r\n");
                    out.print("Host: " + mURI.getHost() + "\r\n");
                    out.print("Sec-WebSocket-Key: " + createSecret() + "\r\n");
                    out.print("Sec-WebSocket-Version: 13\r\n");
                    if (WebSocketClient.this.mExtraHeaders != null) {
                        for (NameValuePair mExtraHeader : WebSocketClient.this.mExtraHeaders) {
                            out.print(String.format("%s: %s\r\n", mExtraHeader.getName(), mExtraHeader.getValue()));
                        }
                    }
                    out.print("\r\n");
                    out.flush();
                    WSParser.HappyDataInputStream stream = new WSParser.HappyDataInputStream(mSocket.getInputStream());
                    StatusLine statusLine = parseStatusLine(readLine(stream));
                    if (statusLine == null) {
                        throw new HttpException("Received no reply from server.");
                    }

                    if (statusLine.getStatusCode() != 101) {
                        L.e(statusLine.getReasonPhrase());
                        throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
                    }
                    String line;
                    boolean isAccepted = false;
                    while (!TextUtils.isEmpty(line = readLine(stream))) {
                        Header header = parseHeader(line);
                        if (header.getName().equals("Sec-WebSocket-Accept")) {
                            isAccepted = true;
                        }
                    }
                    if (!isAccepted) {
                        L.e("shake hands error");
                        if (mIWebSocketStateListener != null)
                            mIWebSocketStateListener.onDisconnect(-3, "hand shake error");
                    }
                    synchronized (mConFlagLock) {
                        mConnected = true;
                    }
                    if (mIWebSocketStateListener != null)
                        mIWebSocketStateListener.onConnect();
                    mParser.start(stream);
                } catch (EOFException var17) {
                    var17.printStackTrace();
                    //ignore
                    if (mIWebSocketStateListener != null)
                        mIWebSocketStateListener.onError(var17);
                } catch (SSLException var18) {
                    var18.printStackTrace();
                    if (mIWebSocketStateListener != null)
                        mIWebSocketStateListener.onError(var18);
                } catch (Exception var19) {
                    var19.printStackTrace();
                    if (mIWebSocketStateListener != null)
                        mIWebSocketStateListener.onError(var19);
                }

            }
        });
        this.mThread.start();
    }

    Socket getSocket() {
        return mSocket;
    }

    public void disconnect() {
        if (this.mSocket != null) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    synchronized (WebSocketClient.this.mSendLock) {
                        if (WebSocketClient.this.mSocket != null) {
                            try {
                                WebSocketClient.this.mSocket.close();
                                L.i("Socket closed!");
                                synchronized (WebSocketClient.this.mConFlagLock) {
                                    WebSocketClient.this.mConnected = false;
                                }
                            } catch (IOException | NullPointerException exception) {
                                Log.i("alisr", "[websokect client]  disConnect() error");
//                                WebSocketClient.this.mIWebSocketStateListener.onError(var4);
                                //ignore
                            }
                        }
                    }
                }
            });
        }

    }

    public void send(String data) {
        this.sendFrame(this.mParser.frame(data));
    }

    public void send(byte[] data) {
        this.sendFrame(this.mParser.frame(data));
    }

    public boolean isConnected() {
        return this.mConnected;
    }

    public Socket createSocket(SocketFactory socketFactory, String host, int port, int timeout) throws IOException, UnknownHostException, ConnectTimeoutException {
        Socket socket = socketFactory.createSocket();
        SocketAddress remoteaddr = new InetSocketAddress(host, port);
        socket.connect(remoteaddr, timeout);
        return socket;
    }

    private StatusLine parseStatusLine(String line) {
        return TextUtils.isEmpty(line) ? null : BasicLineParser.parseStatusLine(line, new BasicLineParser());
    }

    private Header parseHeader(String line) {
        return BasicLineParser.parseHeader(line, new BasicLineParser());
    }

    private String readLine(WSParser.HappyDataInputStream reader) throws IOException {
        int readChar = reader.read();
        if (readChar == -1) {
            return null;
        } else {
            StringBuilder string = new StringBuilder("");

            do {
                if (readChar == 10) {
                    return string.toString();
                }

                if (readChar != 13) {
                    string.append((char) readChar);
                }

                readChar = reader.read();
            } while (readChar != -1);

            return null;
        }
    }

    private String readLine2(WSParser.HappyDataInputStream reader) throws IOException {
        int readChar = reader.read();
        if (readChar == -1) {
            return null;
        } else {
            InputStreamReader isr = new InputStreamReader(reader, Charset.forName("utf-8"));
            CharBuffer cb = CharBuffer.allocate(100);
            StringBuilder sb = new StringBuilder();

            while (isr.read(cb) > 0) {
                cb.flip();
                sb.append(cb.toString());
                cb.clear();
            }

            return sb.toString();
        }
    }

    private String createSecret() {
        byte[] nonce = new byte[16];

        for (int i = 0; i < 16; ++i) {
            nonce[i] = (byte) ((int) (Math.random() * 256.0D));
        }

        return Base64.encodeToString(nonce, 0).trim();
    }

    void sendFrame(final byte[] frame) {
        this.mHandler.post(new Runnable() {
            public void run() {
                try {
                    synchronized (WebSocketClient.this.mSendLock) {
                        if (mSocket == null) return;
                        OutputStream outputStream = WebSocketClient.this.mSocket.getOutputStream();
                        outputStream.write(frame);
                        outputStream.flush();
                    }
                } catch (Exception var5) {
                    if (mIWebSocketStateListener != null)
                        WebSocketClient.this.mIWebSocketStateListener.onError(var5);
                }

            }
        });
    }

    public void sendFrameSync(byte[] frame) {
        try {
            synchronized (WebSocketClient.this.mSendLock) {
                if (mSocket == null) return;
                OutputStream outputStream = WebSocketClient.this.mSocket.getOutputStream();
                outputStream.write(frame);
                outputStream.flush();
            }
        } catch (Exception e) {
            if (mIWebSocketStateListener != null)
                WebSocketClient.this.mIWebSocketStateListener.onError(e);
        }
    }

    private SSLSocketFactory getSSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init((KeyManager[]) null, sTrustManagers, (SecureRandom) null);
        return context.getSocketFactory();
    }

    public interface IWebSocketStateListener {
        void onConnect();

        void onMessage(String var1);

        void onMessage(byte[] var1);

        void onDisconnect(int var1, String var2);

        void onError(Exception var1);
    }
}
