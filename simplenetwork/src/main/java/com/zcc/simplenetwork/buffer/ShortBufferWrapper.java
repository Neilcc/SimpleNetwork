package com.zcc.simplenetwork.buffer;

public class ShortBufferWrapper {

    public short[] data;
    public boolean isFirst;
    public boolean isLast;

    public ShortBufferWrapper(short[] data, boolean isFirst, boolean isLast) {
        this.data = data;
        this.isFirst = isFirst;
        this.isLast = isLast;
    }
}
