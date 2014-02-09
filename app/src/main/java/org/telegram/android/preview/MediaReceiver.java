package org.telegram.android.preview;

import android.graphics.Bitmap;

/**
 * Created by ex3ndr on 09.02.14.
 */
public interface MediaReceiver {
    public void onMediaReceived(Bitmap preview, int regionW, int regionH, String key, boolean intermediate);
}
