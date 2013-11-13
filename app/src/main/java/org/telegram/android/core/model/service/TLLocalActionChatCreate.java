package org.telegram.android.core.model.service;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 17.10.13
 * Time: 19:33
 */
public class TLLocalActionChatCreate extends TLAbsLocalAction {

    public static final int CLASS_ID = 0x9884be37;

    @Override
    public int getClassId() {
        return CLASS_ID;
    }
}
