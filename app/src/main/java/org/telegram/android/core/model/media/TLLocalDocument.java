package org.telegram.android.core.model.media;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created by ex3ndr on 14.12.13.
 */
public class TLLocalDocument extends TLObject {

    public static final int CLASS_ID = 0x16f85dee;

    private boolean isAvailable = false;

    private TLAbsLocalFileLocation fileLocation = new TLLocalFileEmpty();

    private int uid = 0;
    private int date = 0;
    private String fileName = "";
    private String mimeType = "";

    private byte[] fastPreview = new byte[0];
    private int previewW = 0;
    private int previewH = 0;
    private TLAbsLocalFileLocation previewLocation = new TLLocalFileEmpty();

    public TLLocalDocument(TLAbsLocalFileLocation fileLocation, int uid, int date, String fileName, String mimeType) {
        this.fileLocation = fileLocation;
        this.uid = uid;
        this.date = date;
        this.fileName = fileName;
        this.mimeType = mimeType;
    }

    public void setFastPreview(byte[] data, int w, int h) {
        fastPreview = data;
        previewW = w;
        previewH = h;
        previewLocation = new TLLocalFileEmpty();
    }

    public void setPreview(TLLocalFileLocation location, int w, int h) {
        previewW = w;
        previewH = h;
        previewLocation = location;
        fastPreview = new byte[0];
    }

    public TLAbsLocalFileLocation getFileLocation() {
        return fileLocation;
    }

//    public int getUid() {
//        return uid;
//    }

//    public int getDate() {
//        return date;
//    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public byte[] getFastPreview() {
        return fastPreview;
    }

    public int getPreviewW() {
        return previewW;
    }

    public int getPreviewH() {
        return previewH;
    }

    public TLAbsLocalFileLocation getPreview() {
        return previewLocation;
    }

    public void setFileLocation(TLAbsLocalFileLocation fileLocation) {
        this.fileLocation = fileLocation;
    }

//    public void setUid(int uid) {
//        this.uid = uid;
//    }

//    public void setDate(int date) {
//        this.date = date;
//    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setFastPreview(byte[] fastPreview) {
        this.fastPreview = fastPreview;
    }

    public void setPreviewW(int previewW) {
        this.previewW = previewW;
    }

    public void setPreviewH(int previewH) {
        this.previewH = previewH;
    }

    public TLAbsLocalFileLocation getPreviewLocation() {
        return previewLocation;
    }

    public void setPreviewLocation(TLAbsLocalFileLocation previewLocation) {
        this.previewLocation = previewLocation;
    }

    public TLLocalDocument() {

    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLBool(isAvailable, stream);
        writeTLObject(fileLocation, stream);
        writeInt(uid, stream);
        writeInt(date, stream);
        writeTLString(fileName, stream);
        writeTLString(mimeType, stream);
        writeTLBytes(fastPreview, stream);
        writeInt(previewW, stream);
        writeInt(previewH, stream);
        writeTLObject(previewLocation, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        isAvailable = readTLBool(stream);
        fileLocation = (TLAbsLocalFileLocation) readTLObject(stream, context);
        uid = readInt(stream);
        date = readInt(stream);
        fileName = readTLString(stream);
        mimeType = readTLString(stream);
        fastPreview = readTLBytes(stream);
        previewW = readInt(stream);
        previewH = readInt(stream);
        previewLocation = (TLAbsLocalFileLocation) readTLObject(stream, context);
    }
}
