package com.zcc.wsmanager.data;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.zcc.simplenetwork.IProguardKeep;

public class EndCodeBean  implements IProguardKeep {
    private String end = "true";

    public String getEnd() {
        return TextUtils.isEmpty(end) ? "" : end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
