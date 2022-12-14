package com.zcc.simplenetwork.buffer;

public class ByteBufferWrapper {
    public byte[] bytes;
    public boolean isFirst;
    public boolean isLast;

    public ByteBufferWrapper(byte[] bytes) {
        this.bytes = bytes;
    }
}
