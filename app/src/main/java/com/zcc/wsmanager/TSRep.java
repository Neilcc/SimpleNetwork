package com.zcc.wsmanager;


import com.alibaba.fastjson.JSON;
import com.zcc.simplenetwork.IProguardKeep;

public class TSRep implements IProguardKeep {
    private long curtime;

    public static TSRep parse(String s) {
        return JSON.parseObject(s, TSRep.class);
    }

    public long getCurtime() {
        return curtime;
    }

    public void setCurtime(long curtime) {
        this.curtime = curtime;
    }

}
