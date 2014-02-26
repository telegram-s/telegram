package org.telegram.android.preview.media;

import org.telegram.android.core.model.media.TLLocalDocument;

/**
* Created by ex3ndr on 22.02.14.
*/
public class MediaDocFastTask extends BaseTask {

    private TLLocalDocument doc;

    public MediaDocFastTask(TLLocalDocument doc) {
        this.doc = doc;
    }

    public TLLocalDocument getDoc() {
        return doc;
    }

    @Override
    public String getKey() {
        // Work-around: documents with fast cache doesn't contain preview location
        return "preview:" + doc.getFileLocation().getUniqKey();
    }
}
