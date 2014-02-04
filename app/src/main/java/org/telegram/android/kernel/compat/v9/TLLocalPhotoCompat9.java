package org.telegram.android.kernel.compat.v9;

import org.telegram.android.core.model.media.TLAbsLocalFileLocation;
import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;
import static org.telegram.tl.StreamingUtils.readTLBool;

/**
 * Created by ex3ndr on 04.02.14.
 */
public class TLLocalPhotoCompat9 extends TLObject {
    public static final int CLASS_ID = 0x4d7adc15;

    private int fastPreviewW;
    private int fastPreviewH;
    private byte[] fastPreview;
    private String fastPreviewKey;
    private boolean isOptimized;

    private int fullW;
    private int fullH;
    private TLAbsLocalFileLocation fullLocation;

    public int getFastPreviewW() {
        return fastPreviewW;
    }

    public int getFastPreviewH() {
        return fastPreviewH;
    }

    public byte[] getFastPreview() {
        return fastPreview;
    }

    public String getFastPreviewKey() {
        return fastPreviewKey;
    }

    public boolean isOptimized() {
        return isOptimized;
    }

    public int getFullW() {
        return fullW;
    }

    public int getFullH() {
        return fullH;
    }

    public TLAbsLocalFileLocation getFullLocation() {
        return fullLocation;
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
        writeTLBool(isOptimized, stream);
        writeInt(fullW, stream);
        writeInt(fullH, stream);
        writeTLObject(fullLocation, stream);
        writeTLBool(false, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        fastPreviewW = readInt(stream);
        fastPreviewH = readInt(stream);
        fastPreview = readTLBytes(stream);
        fastPreviewKey = readTLString(stream);
        isOptimized = readTLBool(stream);
        fullW = readInt(stream);
        fullH = readInt(stream);
        fullLocation = (TLAbsLocalFileLocation) readTLObject(stream, context);
        readTLBool(stream); // isAnimated
    }
}
