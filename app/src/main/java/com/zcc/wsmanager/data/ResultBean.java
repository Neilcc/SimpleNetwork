package com.zcc.wsmanager.data;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.zcc.simplenetwork.IProguardKeep;

public class ResultBean implements IProguardKeep {
    private String code;
    private String action;
    private String msg;
    private SpeechDataBean data;
    private String sid;


    public static ResultBean parse(String jsonStr) {
        return JSON.parseObject(jsonStr, ResultBean.class);
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    public String getCode() {
        return TextUtils.isEmpty(code) ? "" : code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAction() {
        return TextUtils.isEmpty(action) ? "" : action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMsg() {
        return TextUtils.isEmpty(msg) ? "" : msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public SpeechDataBean getData() {
        if (data == null) {
            data = new SpeechDataBean();
        }
        return data;
    }

    public void setData(SpeechDataBean data) {
        this.data = data;
    }

    public String getSid() {
        return TextUtils.isEmpty(sid) ? "" : sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }
}
