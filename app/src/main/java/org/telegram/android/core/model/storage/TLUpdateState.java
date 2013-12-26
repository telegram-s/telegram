package org.telegram.android.core.model.storage;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created by ex3ndr on 27.12.13.
 */
public class TLUpdateState extends TLObject {

    public static final int CLASS_ID = 0xfa52fa81;

    private int pts;
    private int seq;
    private int date;
    private int qts;

    public TLUpdateState(int pts, int seq, int date, int qts) {
        this.pts = pts;
        this.seq = seq;
        this.date = date;
        this.qts = qts;
    }

    public TLUpdateState() {

    }

    public int getPts() {
        return pts;
    }

    public void setPts(int pts) {
        this.pts = pts;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public int getQts() {
        return qts;
    }

    public void setQts(int qts) {
        this.qts = qts;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeInt(pts, stream);
        writeInt(seq, stream);
        writeInt(date, stream);
        writeInt(qts, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        pts = readInt(stream);
        seq = readInt(stream);
        date = readInt(stream);
        qts = readInt(stream);
    }
}