package org.telegram.android.core.model.media;

import org.telegram.tl.TLObject;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 15.10.13
 * Time: 2:38
 */
public abstract class TLAbsLocalFileLocation extends TLObject {
   public abstract String getUniqKey();
}
