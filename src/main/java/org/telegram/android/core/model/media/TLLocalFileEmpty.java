package org.telegram.android.core.model.media;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 15.10.13
 * Time: 2:43
 */
public class TLLocalFileEmpty extends TLAbsLocalFileLocation {

    public static final int CLASS_ID = 0x427ae7ee;

    @Override
    public int getClassId() {
        return CLASS_ID;
    }
}
