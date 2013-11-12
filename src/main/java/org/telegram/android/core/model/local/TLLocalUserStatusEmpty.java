package org.telegram.android.core.model.local;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 11.11.13
 * Time: 8:41
 */
public class TLLocalUserStatusEmpty extends TLAbsLocalUserStatus {

    public static final int CLASS_ID = 0x6d1ac6cc;

    @Override
    public int getClassId() {
        return CLASS_ID;
    }
}
