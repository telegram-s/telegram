package org.telegram.android.core.background;

import android.content.Context;
import com.extradea.framework.persistence.ContextPersistence;

/**
 * Author: Korshakov Stepan
 * Created: 08.08.13 20:16
 */
public class UpdateState extends ContextPersistence {
    private int pts;
    private int seq;
    private int date;
    private int qts;

    public UpdateState(Context context) {
        super(context, true);
        tryLoad();
    }

    public synchronized void resetState() {
        this.pts = 0;
        this.seq = 0;
        this.date = 0;
        this.qts = 0;
        trySave();
    }

    public synchronized void setFullState(int pts, int seq, int date, int qts) {
        this.pts = pts;
        this.seq = seq;
        this.date = date;
        this.qts = qts;
        trySave();
    }

    public synchronized int getQts() {
        return qts;
    }

    public synchronized void setQts(int qts) {
        this.qts = qts;
        trySave();
    }

    public synchronized int getPts() {
        return pts;
    }

    public synchronized void setPts(int pts) {
        this.pts = pts;
        trySave();
    }

    public synchronized int getSeq() {
        return seq;
    }

    public synchronized void setSeq(int seq) {
        this.seq = seq;
        trySave();
    }

    public synchronized int getDate() {
        return date;
    }

    public synchronized void setDate(int date) {
        this.date = date;
        trySave();
    }
}
