package com.zcc.simplenetwork.buffer;

public class EncodedDataWrapper {
    byte[] encodedData;

    public EncodedDataWrapper(byte[] encodedData) {
        this.encodedData = encodedData;
    }

    public byte[] getData() {
        return encodedData;
    }


}
