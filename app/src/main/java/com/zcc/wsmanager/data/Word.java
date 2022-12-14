package com.zcc.wsmanager.data;

import android.text.TextUtils;

import com.zcc.simplenetwork.IProguardKeep;


public class Word implements IProguardKeep {
    private String w;
    private String wp;
    private int wb;
    private int we;

    public String getW() {
        return TextUtils.isEmpty(w) ? "" : w;
    }

    public void setW(String w) {
        this.w = w;
    }

    public String getWp() {
        return TextUtils.isEmpty(wp) ? "" : wp;
    }

    public void setWp(String wp) {
        this.wp = wp;
    }

    public int getWb() {
        return wb;
    }

    public void setWb(int wb) {
        this.wb = wb;
    }

    public int getWe() {
        return we;
    }

    public void setWe(int we) {
        this.we = we;
    }
}
