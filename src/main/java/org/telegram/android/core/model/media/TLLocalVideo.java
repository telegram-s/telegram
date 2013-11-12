package org.telegram.android.core.model.media;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 15.10.13
 * Time: 18:40
 */
public class TLLocalVideo extends TLObject {

    public static final int CLASS_ID = 0x168ae7c9;

    private int duration;
    private TLAbsLocalFileLocation videoLocation;

    private int previewW;
    private int previewH;
    private String previewKey;
    private TLAbsLocalFileLocation previewLocation;
    private byte[] fastPreview;

    public TLLocalVideo() {

    }

    public TLLocalVideo(int duration, TLAbsLocalFileLocation videoLocation, int previewW, int previewH,
                        String previewKey, TLAbsLocalFileLocation previewLocation, byte[] fastPreview) {
        this.duration = duration;
        this.videoLocation = videoLocation;
        this.previewW = previewW;
        this.previewH = previewH;
        this.previewKey = previewKey;
        this.previewLocation = previewLocation;
        this.fastPreview = fastPreview;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public TLAbsLocalFileLocation getVideoLocation() {
        return videoLocation;
    }

    public void setVideoLocation(TLAbsLocalFileLocation videoLocation) {
        this.videoLocation = videoLocation;
    }

    public int getPreviewW() {
        return previewW;
    }

    public void setPreviewW(int previewW) {
        this.previewW = previewW;
    }

    public int getPreviewH() {
        return previewH;
    }

    public void setPreviewH(int previewH) {
        this.previewH = previewH;
    }

    public String getPreviewKey() {
        return previewKey;
    }

    public void setPreviewKey(String previewKey) {
        this.previewKey = previewKey;
    }

    public TLAbsLocalFileLocation getPreviewLocation() {
        return previewLocation;
    }

    public void setPreviewLocation(TLAbsLocalFileLocation previewLocation) {
        this.previewLocation = previewLocation;
    }

    public byte[] getFastPreview() {
        return fastPreview;
    }

    public void setFastPreview(byte[] fastPreview) {
        this.fastPreview = fastPreview;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeInt(duration, stream);
        writeTLObject(videoLocation, stream);
        writeInt(previewW, stream);
        writeInt(previewH, stream);
        writeTLString(previewKey, stream);
        writeTLObject(previewLocation, stream);
        writeTLBytes(fastPreview, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        duration = readInt(stream);
        videoLocation = (TLAbsLocalFileLocation) readTLObject(stream, context);
        previewW = readInt(stream);
        previewH = readInt(stream);
        previewKey = readTLString(stream);
        previewLocation = (TLAbsLocalFileLocation) readTLObject(stream, context);
        fastPreview = readTLBytes(stream);
    }
}
