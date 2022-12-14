package com.zcc.wsmanager.data;

import com.alibaba.fastjson.JSON;
import com.zcc.simplenetwork.IProguardKeep;

import java.util.ArrayList;
import java.util.List;

public class SpeechDataBean implements IProguardKeep {
    private int bg;
    private int ed;
    private int type = -1;
    private int seg_id;
    private List<String> nbest;
    private List<List<Word>> ws;

    public static SpeechDataBean parse(String jsonStr) {
        return JSON.parseObject(jsonStr, SpeechDataBean.class);
    }

    public int getBg() {
        return bg;
    }

    public void setBg(int bg) {
        this.bg = bg;
    }

    public int getEd() {
        return ed;
    }

    public void setEd(int ed) {
        this.ed = ed;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getSeg_id() {
        return seg_id;
    }

    public void setSeg_id(int seg_id) {
        this.seg_id = seg_id;
    }

    public List<String> getNbest() {
        if (nbest == null) {
            nbest = new ArrayList<>();
        }
        return nbest;
    }

    public void setNbest(List<String> nbest) {
        this.nbest = nbest;
    }

    public List<List<Word>> getWs() {
        if (ws == null) {
            ws = new ArrayList<>();
        }
        return ws;
    }

    public void setWs(List<List<Word>> ws) {
        this.ws = ws;
    }
}
