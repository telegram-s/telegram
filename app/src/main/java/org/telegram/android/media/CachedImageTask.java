package org.telegram.android.media;

import com.extradea.framework.images.tasks.ImageTask;
import org.telegram.android.core.model.media.TLLocalPhoto;
import org.telegram.android.core.model.media.TLLocalVideo;
import org.telegram.api.TLFileLocation;
import org.telegram.api.TLPhotoCachedSize;

/**
 * Author: Korshakov Stepan
 * Created: 20.08.13 1:01
 */
public class CachedImageTask extends ImageTask {
    private TLPhotoCachedSize size;
    private TLFileLocation location;
    private TLLocalPhoto photo;
    private TLLocalVideo video;
    private boolean blur;

    private boolean local;

    public boolean isBlur() {
        return blur;
    }

    public byte[] getData() {
        if (!local) {
            return size.getBytes();
        } else {
            if (photo != null) {
                return photo.getFastPreview();
            } else {
                return video.getFastPreview();
            }
        }
    }

    public CachedImageTask(TLLocalPhoto photo) {
        this.photo = photo;
        this.local = true;
        setPutInMemoryCache(true);
        setPutInDiskCache(false);
    }

    public CachedImageTask(TLLocalPhoto photo, int w, int h, boolean blur) {
        this.photo = photo;
        this.blur = blur;
        this.local = true;
        setMaxHeight(h);
        setMaxWidth(w);
        setPutInMemoryCache(true);
        setPutInDiskCache(false);
    }

    public CachedImageTask(TLLocalVideo video) {
        this.video = video;
        this.local = true;
        setPutInMemoryCache(true);
        setPutInDiskCache(false);
    }

    public CachedImageTask(TLLocalVideo video, int w, int h, boolean blur) {
        this.video = video;
        this.blur = blur;
        this.local = true;
        setMaxHeight(h);
        setMaxWidth(w);
        setPutInMemoryCache(true);
        setPutInDiskCache(false);
    }

    public CachedImageTask(TLPhotoCachedSize size) {
        this.size = size;
        this.location = (TLFileLocation) size.getLocation();
        this.local = false;
        setPutInMemoryCache(true);
        setPutInDiskCache(false);
    }

    public CachedImageTask(TLPhotoCachedSize size, int w, int h, boolean blur) {
        this.size = size;
        this.location = (TLFileLocation) size.getLocation();
        this.blur = blur;
        this.local = false;
        setMaxHeight(h);
        setMaxWidth(w);
        setPutInMemoryCache(true);
        setPutInDiskCache(false);
    }

    @Override
    public boolean skipDiskCacheCheck() {
        return true;
    }

    @Override
    protected String getKeyImpl() {
        if (local) {
            if (photo != null) {
                if (blur) {
                    return "blur:" + photo.getFastPreviewKey();
                } else {
                    return photo.getFastPreviewKey();
                }
            } else {
                if (blur) {
                    return "blur:" + video.getPreviewKey();
                } else {
                    return video.getPreviewKey();
                }
            }
        } else {
            if (blur) {
                return "blur:" + location.getVolumeId() + "+" + location.getLocalId();
            } else {
                return location.getVolumeId() + "+" + location.getLocalId();
            }
        }
    }
}
