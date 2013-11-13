package org.telegram.android.core.model.service;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 17.10.13
 * Time: 22:40
 */
public class TLLocalActionUnknown extends TLAbsLocalAction {

    public static final int CLASS_ID = 0x81b8e672;

    @Override
    public int getClassId() {
        return CLASS_ID;
    }
}
