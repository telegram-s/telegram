package org.telegram.android.preview;

import android.graphics.Bitmap;

/**
 * Created by ex3ndr on 06.02.14.
 */
public interface AvatarReceiver {
    public void onAvatarReceived(AvatarHolder avatarHolder, boolean intermediate);
}
