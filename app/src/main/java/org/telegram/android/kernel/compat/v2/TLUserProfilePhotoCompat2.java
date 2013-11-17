package org.telegram.android.kernel.compat.v2;

import org.telegram.android.kernel.compat.v1.TLFileLocationAbsCompat;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 18.09.13
 * Time: 2:33
 * To change this template use File | Settings | File Templates.
 */
public class TLUserProfilePhotoCompat2 extends TLUserProfilePhotoAbsCompat2 {
    protected long photoId;
    protected TLFileLocationAbsCompat photoSmall;
    protected TLFileLocationAbsCompat photoBig;

    public long getPhotoId() {
        return photoId;
    }

    public TLFileLocationAbsCompat getPhotoSmall() {
        return photoSmall;
    }

    public TLFileLocationAbsCompat getPhotoBig() {
        return photoBig;
    }
}
