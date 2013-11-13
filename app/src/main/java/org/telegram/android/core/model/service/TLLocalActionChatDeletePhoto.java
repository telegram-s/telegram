package org.telegram.android.core.model.service;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 17.10.13
 * Time: 19:49
 */
public class TLLocalActionChatDeletePhoto extends TLAbsLocalAction {

    public static final int CLASS_ID = 0x4bbce3f5;

    @Override
    public int getClassId() {
        return CLASS_ID;
    }
}
