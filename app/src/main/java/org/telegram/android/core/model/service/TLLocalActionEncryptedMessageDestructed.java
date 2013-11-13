package org.telegram.android.core.model.service;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 21.10.13
 * Time: 19:50
 */
public class TLLocalActionEncryptedMessageDestructed extends TLAbsLocalAction {

    public static final int CLASS_ID = 0x049255d2;

    @Override
    public int getClassId() {
        return CLASS_ID;
    }
}
