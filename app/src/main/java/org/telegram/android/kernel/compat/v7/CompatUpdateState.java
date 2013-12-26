package org.telegram.android.kernel.compat.v7;

import org.telegram.android.kernel.compat.CompatContextPersistence;

/**
 * Created by ex3ndr on 27.12.13.
 */
public class CompatUpdateState extends CompatContextPersistence {
    private int pts;
    private int seq;
    private int date;
    private int qts;

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
}
