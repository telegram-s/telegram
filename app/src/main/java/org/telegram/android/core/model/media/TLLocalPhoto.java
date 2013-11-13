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
 * Time: 2:16
 */
public class TLLocalPhoto extends TLObject {

    public static final int CLASS_ID = 0x36740e72;

    private int fastPreviewW;
    private int fastPreviewH;
    private byte[] fastPreview;
    private String fastPreviewKey;

    private int fullW;
    private int fullH;
    private TLAbsLocalFileLocation fullLocation;

    public TLLocalPhoto() {

    }

    public int getFastPreviewW() {
        return fastPreviewW;
    }

    public void setFastPreviewW(int fastPreviewW) {
        this.fastPreviewW = fastPreviewW;
    }

    public int getFastPreviewH() {
        return fastPreviewH;
    }

    public void setFastPreviewH(int fastPreviewH) {
        this.fastPreviewH = fastPreviewH;
    }

    public byte[] getFastPreview() {
        return fastPreview;
    }

    public void setFastPreview(byte[] fastPreview) {
        this.fastPreview = fastPreview;
    }

    public int getFullW() {
        return fullW;
    }

    public void setFullW(int fullW) {
        this.fullW = fullW;
    }

    public int getFullH() {
        return fullH;
    }

    public void setFullH(int fullH) {
        this.fullH = fullH;
    }

    public TLAbsLocalFileLocation getFullLocation() {
        return fullLocation;
    }

    public void setFullLocation(TLAbsLocalFileLocation fullLocation) {
        this.fullLocation = fullLocation;
    }

    public String getFastPreviewKey() {
        return fastPreviewKey;
    }

    public void setFastPreviewKey(String fastPreviewKey) {
        this.fastPreviewKey = fastPreviewKey;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeInt(fastPreviewW, stream);
        writeInt(fastPreviewH, stream);
        writeTLBytes(fastPreview, stream);
        writeTLString(fastPreviewKey, stream);
        writeInt(fullW, stream);
        writeInt(fullH, stream);
        writeTLObject(fullLocation, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        fastPreviewW = readInt(stream);
        fastPreviewH = readInt(stream);
        fastPreview = readTLBytes(stream);
        fastPreviewKey = readTLString(stream);
        fullW = readInt(stream);
        fullH = readInt(stream);
        fullLocation = (TLAbsLocalFileLocation) readTLObject(stream, context);
    }
}
