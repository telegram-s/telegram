package org.telegram.android.media.avatar;

import org.telegram.api.TLFileLocation;

/**
 * Created by ex3ndr on 10.12.13.
 */
public class AvatarTask {
    private TLFileLocation location;

    private int size;

    public AvatarTask(TLFileLocation location) {
        this.location = location;
        this.size = 0;
    }

    public AvatarTask(TLFileLocation location, int size) {
        this.location = location;
        this.size = size;
    }

    public TLFileLocation getLocation() {
        return location;
    }

    public int getSize() {
        return size;
    }
}
