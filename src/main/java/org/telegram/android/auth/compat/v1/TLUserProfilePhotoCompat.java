package org.telegram.android.auth.compat.v1;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 18.09.13
 * Time: 2:33
 * To change this template use File | Settings | File Templates.
 */
public class TLUserProfilePhotoCompat extends TLUserProfilePhotoAbsCompat {
    protected TLFileLocationAbsCompat photoSmall;
    protected TLFileLocationAbsCompat photoBig;

    public TLFileLocationAbsCompat getPhotoSmall() {
        return photoSmall;
    }

    public TLFileLocationAbsCompat getPhotoBig() {
        return photoBig;
    }
}
