package org.telegram.opus;

import java.nio.ByteBuffer;

/**
 * Created by ex3ndr on 18.03.14.
 */
public class OpusLib {
    public native int startRecord(String path);
    public native int writeFrame(ByteBuffer frame, int len);
    public native void stopRecord();
    public native int openOpusFile(String path);
    public native int seekOpusFile(float position);
    public native int isOpusFile(String path);
    public native void closeOpusFile();
    public native void readOpusFile(ByteBuffer buffer, int capacity);
    public native int getFinished();
    public native int getSize();
    public native long getPcmOffset();
    public native long getTotalPcmDuration();
}
