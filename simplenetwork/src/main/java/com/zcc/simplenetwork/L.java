package com.zcc.simplenetwork;

import android.util.Log;



public class L {
    private static final String TAG = "neteaseASR";
    private static boolean isEnabled = BuildConfig.DEBUG;

    public static void i(String param) {
        if (isEnabled)
            Log.i(TAG, param);
    }

    public static void d(String params) {
        if (isEnabled)
            Log.d(TAG, params);
    }

    public static void e(String msg) {
        if (isEnabled)
            Log.e(TAG, msg);
    }
}
