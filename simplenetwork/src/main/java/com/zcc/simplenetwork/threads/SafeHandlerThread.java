package com.zcc.simplenetwork.threads;

import android.os.Build;
import android.os.Looper;

public class SafeHandlerThread extends Thread {

    private final Object mStartLock = new Object();
    private Looper mLooper;
    private boolean isReady;

    public SafeHandlerThread(Runnable target) {
        super(target);
    }

    public SafeHandlerThread(String name) {
        super(name);
    }

    public SafeHandlerThread(Runnable target, String name) {
        super(target, name);
    }

    public Looper getLooper() {
        return mLooper;
    }

    public void quit(){
        mLooper.quit();
    }

    public void quitSafely(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mLooper.quitSafely();
        }else {
            mLooper.quit();
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        mLooper = Looper.myLooper();
        synchronized (mStartLock) {
            isReady = true;
            mStartLock.notify();
        }
        Looper.loop();
        synchronized (mStartLock) {
            isReady = false;
        }
    }

    public void waitUntilReady() {
        synchronized (mStartLock) {
            while (!isReady) {
                try {
                    mStartLock.wait();
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        }
    }
}
