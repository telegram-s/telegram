package org.telegram.android.core.background;

/**
 * Author: Korshakov Stepan
 * Created: 15.08.13 13:47
 */
public interface SenderListener {
    public void onUploadStateChanged(int localId, MessageSender.SendState state);
}
