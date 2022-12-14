package com.zcc.wsmanager;


import android.util.Log;

import com.zcc.simplenetwork.threads.ToSendDataBuffer;
import com.zcc.simplenetwork.ws.WebSocketClient;
import com.zcc.wsmanager.data.EndCodeBean;

import java.net.URI;


/**
 */
public class ASRWebSocketClient implements ToSendDataBuffer.OnBufferStateListener, WebSocketClient.IWebSocketStateListener {

    private static final int FRAME_ITEVAL_MS = 40;
    private final Object MUTEX = new Object();
    private final String endCodeBean = new EndCodeBean().toString();
    private WebSocketClient webSocketClient;
    private ToSendDataBuffer toSendDataBuffer;
    private volatile boolean isConnected = false;
    private volatile long lastSendTime = 0L;
    private long mNetWorkTimeOutMS;
    private volatile long lassSessionCounter = 0;
//    private boolean isFirstTIme = true;

    public ASRWebSocketClient(ToSendDataBuffer toSendDataBuffer) {
        this.toSendDataBuffer = toSendDataBuffer;
        this.toSendDataBuffer.setOnBufferStateListener(this);
        this.mNetWorkTimeOutMS = 1000;

    }

    public synchronized void connect() {
        webSocketClient = new WebSocketClient(
                URI.create(
                        "wss://zcc.test"
                ),
                this,
                null,
                mNetWorkTimeOutMS
        );
        webSocketClient.connect();
//        this.isFirstTIme = true;
    }

    private long getServerTime() {
        return System.currentTimeMillis() / 1000L;
    }


    public synchronized void sendStopMsg() {
        if (webSocketClient != null) {
            webSocketClient.send(endCodeBean.getBytes());
        }
    }

    private synchronized void reset() {
        synchronized (MUTEX) {
            isConnected = false;
        }
        if (webSocketClient != null) {
            if (webSocketClient.isConnected())
                webSocketClient.disconnect();
            webSocketClient.release();
            webSocketClient = null;
        }
    }

//    private final String SAVE_SENDING_BYTES ="/sdcard/zccsendingBYtes";

    @Override
    public void onBufferChanged() {
        if (webSocketClient != null) {
            if (lastSendTime == 0 || System.currentTimeMillis() - lastSendTime > FRAME_ITEVAL_MS) {
                lastSendTime = System.currentTimeMillis();
                synchronized (MUTEX) {
                    if (isConnected) {
                        byte[] temp = toSendDataBuffer.pollAll().getData();
//                        if (isFirstTIme) {
//                            NELogger.d(this, "==== first value : send first frame length: "+ temp.length);
//                            for (int i = 0; i < 20 && i < temp.length; i++) {
//                                NELogger.d(this, "==== first value : send first frame : " + temp[i] + "index" + i);
//                            }
//                            isFirstTIme = false;
//                        }
//                        FileSaving.getInstance().save(SAVE_SENDING_BYTES, temp);
                        webSocketClient.send(temp);
                    }
                }
            }
        } else {
            //ignore
            // let outside control this
        }
    }

    @Override
    public void onConnect() {
        synchronized (MUTEX) {
            isConnected = true;
        }
    }

    @Override
    public void onMessage(String var1) {
        try {
            Log.d("zcc", "onMessage");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(byte[] var1) {
// ignore
    }

    @Override
    public void onDisconnect(int var1, String var2) {
        if (var1 != 10 && var2 != null && !var2.isEmpty()) {
            reset();
        } else {
            reset();
        }
    }

    @Override
    public void onError(Exception var1) {
        String msg = (var1 != null ? var1.getMessage() : "null");
        reset();
    }


    private static class TimeOutMessageRunnable implements Runnable {
        long mLastSendTime;

        public TimeOutMessageRunnable(long mLastSendTime) {
            this.mLastSendTime = mLastSendTime;
        }

        @Override
        public void run() {

        }
    }

}
