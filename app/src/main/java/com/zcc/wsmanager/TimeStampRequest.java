package com.zcc.wsmanager;


import androidx.annotation.NonNull;

import com.zcc.simplenetwork.http.BaseHttpRequestData;
import com.zcc.simplenetwork.http.SimpleHttpUrlConnection;

import java.util.HashMap;
import java.util.Map;

public class TimeStampRequest extends SimpleHttpUrlConnection<BaseHttpRequestData, TSRep> {

    public TimeStampRequest() {
        super(APIs.GET_TIME);
        setIHeaderInfoBuilder(new TSHeader());
    }

    @Override
    public void send(BaseHttpRequestData data, @NonNull CallBack<TSRep> callBack) {
        super.send(data, callBack);
    }

    public void send(@NonNull SimpleHttpUrlConnection.CallBack<TSRep> callBack) {
        send(null, callBack);
    }

    @Override
    public Class<TSRep> getRepClass() {
        return TSRep.class;
    }

    private static class TSHeader implements IHeaderInfoBuilder {

        @Override
        public Map<String, String> getHeaderMap() {
            return new HashMap<>();
        }
    }


}
