package org.telegram.android.media.avatar;

import android.graphics.Bitmap;
import org.telegram.android.core.model.media.TLLocalFileLocation;

/**
 * Created by ex3ndr on 10.12.13.
 */
public class AvatarHolder {

    private AvatarLoader loader;

    private Bitmap avatar;

    private int sourceId;

    public AvatarHolder(AvatarLoader loader) {
        this.loader = loader;
    }

    public Bitmap getAvatar() {
        return avatar;
    }

    public void clearAvatar() {
        avatar = null;
    }

    public int getSourceId() {
        return sourceId;
    }

    public void clearAvatarSource() {
        if (sourceId != 0) {
            loader.disconnectSource(this, sourceId);
            sourceId = 0;
        }
        onAvatarChanged();
    }

    public void setAvatarSource(TLLocalFileLocation location, int size) {
        if (sourceId != 0) {
            loader.disconnectSource(this, sourceId);
            sourceId = 0;
        }

        sourceId = loader.connectSource(this, location, size);
    }

    public void onAvatarLoaded(Bitmap src) {
        this.avatar = src;
        onAvatarChanged();
    }

    public void onAvatarChanged()
    {

    }
}
