package org.telegram.android.core.model.service;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 20.10.13
 * Time: 3:36
 */
public class TLLocalActionEncryptedRequested extends TLAbsLocalAction {

    public static final int CLASS_ID = 0xc7365218;

    @Override
    public int getClassId() {
        return CLASS_ID;
    }
}
