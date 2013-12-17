package org.telegram.android.core.model.media;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 17.10.13
 * Time: 22:30
 */
public class TLLocalAvatarEmpty extends TLAbsLocalAvatarPhoto {

    public static final int CLASS_ID = 0xe1732cff;

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TLLocalAvatarEmpty;
    }
}
