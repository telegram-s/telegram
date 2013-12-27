package org.telegram.android.core.background;

import android.content.Context;
import org.telegram.android.core.model.TLLocalContext;
import org.telegram.android.core.model.storage.TLUpdateState;
import org.telegram.android.critical.TLPersistence;
import org.telegram.android.kernel.compat.CompatObjectInputStream;
import org.telegram.android.kernel.compat.Compats;
import org.telegram.android.kernel.compat.v7.CompatUpdateState;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

/**
 * Author: Korshakov Stepan
 * Created: 08.08.13 20:16
 */
public class UpdateState {

    private static final String FILE_NAME = "update_state.bin";
    private TLPersistence<TLUpdateState> statePersistence;

    public UpdateState(Context context) {
        statePersistence = new TLPersistence<TLUpdateState>(context, FILE_NAME, TLUpdateState.class, TLLocalContext.getInstance());

        File obsoleteFile = new File(context.getFilesDir().getAbsolutePath(), "org.telegram.android.core.background.UpdateState.sav");
        if (obsoleteFile.exists()) {
            try {
                FileInputStream inputStream = new FileInputStream(obsoleteFile);
                ObjectInputStream dataInputStream = new CompatObjectInputStream(inputStream, Compats.UPDATE_PROCESSOR);
                CompatUpdateState updateState = (CompatUpdateState) dataInputStream.readObject();
                setFullState(updateState.getPts(), updateState.getSeq(), updateState.getDate(), updateState.getQts());
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            obsoleteFile.delete();
        }
    }

    public synchronized void resetState() {
        statePersistence.getObj().setPts(0);
        statePersistence.getObj().setSeq(0);
        statePersistence.getObj().setDate(0);
        statePersistence.getObj().setQts(0);
        statePersistence.write();
    }

    public synchronized void setFullState(int pts, int seq, int date, int qts) {
        statePersistence.getObj().setPts(pts);
        statePersistence.getObj().setSeq(seq);
        statePersistence.getObj().setDate(date);
        statePersistence.getObj().setQts(qts);
        statePersistence.write();
    }

    public synchronized int getQts() {
        return statePersistence.getObj().getQts();
    }

    public synchronized void setQts(int qts) {
        statePersistence.getObj().setQts(qts);
        statePersistence.write();
    }

    public synchronized int getPts() {
        return statePersistence.getObj().getPts();
    }

    public synchronized void setPts(int pts) {
        statePersistence.getObj().setPts(pts);
        statePersistence.write();
    }

    public synchronized int getSeq() {
        return statePersistence.getObj().getSeq();
    }

    public synchronized void setSeq(int seq) {
        statePersistence.getObj().setSeq(seq);
        statePersistence.write();
    }

    public synchronized int getDate() {
        return statePersistence.getObj().getDate();
    }

    public synchronized void setDate(int date) {
        statePersistence.getObj().setDate(date);
        statePersistence.write();
    }
}
