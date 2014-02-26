package org.telegram.android.preview;

import android.content.Context;
import android.util.AttributeSet;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.media.TLLocalPhoto;
import org.telegram.android.core.model.media.TLLocalVideo;

/**
 * Created by ex3ndr on 26.02.14.
 */
public class SmallPreviewView extends BaseView<MediaLoader> {

    private TLLocalPhoto fastPhotoTask;
    private TLLocalVideo fastVideoTask;
    private String photoFileName;

    public SmallPreviewView(Context context) {
        super(context);
    }

    public SmallPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SmallPreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected MediaLoader bindLoader() {
        return application.getUiKernel().getMediaLoader();
    }

    public void requestFast(TLLocalPhoto photo) {
        fastPhotoTask = photo;
        fastVideoTask = null;
        photoFileName = null;
        requestBind();
    }

    public void requestFast(TLLocalVideo video) {
        fastPhotoTask = null;
        fastVideoTask = video;
        photoFileName = null;
        requestBind();
    }

    public void requestFile(String fileName) {
        fastPhotoTask = null;
        fastVideoTask = null;
        photoFileName = fileName;
        requestBind();
    }

    public void clearImage() {
        fastPhotoTask = null;
        fastVideoTask = null;
        photoFileName = null;
        requestBind();
    }

    @Override
    protected void bind() {
        if (fastPhotoTask != null) {
            getLoader().requestFastSmallLoading(fastPhotoTask, this);
        } else if (fastVideoTask != null) {
            getLoader().requestFastSmallLoading(fastVideoTask, this);
        } else if (photoFileName != null) {
            getLoader().requestRawSmall(photoFileName, this);
        } else {
            getLoader().cancelRequest(this);
        }
    }
}
