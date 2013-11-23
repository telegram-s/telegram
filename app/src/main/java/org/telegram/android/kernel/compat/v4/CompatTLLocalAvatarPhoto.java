package org.telegram.android.kernel.compat.v4;

import java.io.Serializable;

/**
 * Created by ex3ndr on 23.11.13.
 */
public class CompatTLLocalAvatarPhoto extends CompatTLAbsLocalAvatarPhoto implements Serializable {

    private CompatTLAbsLocalFileLocation previewLocation;
    private CompatTLAbsLocalFileLocation fullLocation;

    public CompatTLAbsLocalFileLocation getPreviewLocation() {
        return previewLocation;
    }

    public CompatTLAbsLocalFileLocation getFullLocation() {
        return fullLocation;
    }
}
