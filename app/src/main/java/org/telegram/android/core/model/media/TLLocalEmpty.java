package org.telegram.android.core.model.media;

import org.telegram.tl.TLObject;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 17.10.13
 * Time: 22:52
 */
public class TLLocalEmpty extends TLObject {

    public static final int CLASS_ID = 0xc83c0808;

    @Override
    public int getClassId() {
        return CLASS_ID;
    }
}
