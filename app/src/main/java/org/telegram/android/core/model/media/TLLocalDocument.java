package org.telegram.android.core.model.media;

import org.telegram.tl.TLObject;

/**
 * Created by ex3ndr on 14.12.13.
 */
public class TLLocalDocument extends TLObject {

    private static final int CLASS_ID = 0;

    private boolean isAvailable = false;

    private TLAbsLocalFileLocation fileLocation = new TLLocalFileEmpty();

    private int uid = 0;
    private int date = 0;
    private String fileName = "";
    private String mimeType = "";

    private byte[] fastPreview = new byte[0];
    private int previewW = 0;
    private int previewH = 0;
    private TLAbsLocalFileLocation preview = new TLLocalFileEmpty();

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
        preview = new TLLocalFileEmpty();
    }

    public void setPreview(TLLocalFileLocation location, int w, int h) {
        previewW = w;
        previewH = h;
        preview = location;
        fastPreview = new byte[0];
    }


    public TLLocalDocument() {

    }

    @Override
    public int getClassId() {
        return 0;
    }
}
