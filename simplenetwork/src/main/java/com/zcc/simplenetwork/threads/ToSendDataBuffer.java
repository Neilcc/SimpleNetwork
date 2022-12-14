package com.zcc.simplenetwork.threads;


import com.zcc.simplenetwork.buffer.EncodedDataWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ToSendDataBuffer {
    // cached data
    private BlockingQueue<EncodedDataWrapper> mEncodedDataCache;
    private OnBufferStateListener mOnBufferStateListener;

    public ToSendDataBuffer() {
        mEncodedDataCache = new LinkedBlockingQueue<>();
    }

    public boolean add(EncodedDataWrapper bean) {
        try {
            mEncodedDataCache.add(bean);
            if (null != mOnBufferStateListener) {
                mOnBufferStateListener.onBufferChanged();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public EncodedDataWrapper poll() {
        EncodedDataWrapper encodedDataWrapper = mEncodedDataCache.poll();
        if (null != encodedDataWrapper) {
            byte[] bytes = encodedDataWrapper.getData();
        }
        return encodedDataWrapper;
    }

    public EncodedDataWrapper pollAllNotNull() {
        if (mEncodedDataCache.size() > 0) {
            List<EncodedDataWrapper> cachedDataList = new ArrayList<>();
            // Queue 中的数据全部挪到一个list中
            mEncodedDataCache.drainTo(cachedDataList);

            // 计算输出总长度
            int totalLength = 0;
            for (EncodedDataWrapper bean : cachedDataList) {
                totalLength += bean.getData().length;
            }

            if (totalLength <= 0) {
                // 如果没有数据就返回
                return null;
            }

            // 创建生成的字节数组
            byte[] output = new byte[totalLength];
            int index = 0;
            for (EncodedDataWrapper bean : cachedDataList) {
                byte[] frame = bean.getData();
                System.arraycopy(frame, 0, output, index, frame.length);
                index += frame.length;
            }

            return new EncodedDataWrapper(output);
        } else {
            return null;
        }
    }

    public EncodedDataWrapper pollAll() {
        List<EncodedDataWrapper> cachedDataList = new ArrayList<>();
        // Queue 中的数据全部挪到一个list中
        mEncodedDataCache.drainTo(cachedDataList);

        // 计算输出总长度
        int totalLength = 0;
        for (EncodedDataWrapper bean : cachedDataList) {
            totalLength += bean.getData().length;
        }

        // 创建生成的字节数组
        byte[] output = new byte[totalLength];
        int srcLen = 0;
        int index = 0;
        for (EncodedDataWrapper bean : cachedDataList) {
            byte[] frame = bean.getData();
            System.arraycopy(frame, 0, output, index, frame.length);
            index += frame.length;
        }

        return new EncodedDataWrapper(output);
    }

    public int size() {
        return mEncodedDataCache.size();
    }

    public void clear() {
        mEncodedDataCache.clear();
    }

    public void setOnBufferStateListener(OnBufferStateListener listener) {
        mOnBufferStateListener = listener;
    }

    public interface OnBufferStateListener {
        void onBufferChanged();
    }
}
