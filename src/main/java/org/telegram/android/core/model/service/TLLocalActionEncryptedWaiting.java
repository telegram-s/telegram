package org.telegram.android.core.model.service;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 20.10.13
 * Time: 3:37
 */
public class TLLocalActionEncryptedWaiting extends TLAbsLocalAction {

    public static final int CLASS_ID = 0xc29a6770;

    @Override
    public int getClassId() {
        return CLASS_ID;
    }
}
