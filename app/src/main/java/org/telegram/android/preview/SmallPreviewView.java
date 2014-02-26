package org.telegram.android.preview;

import android.content.Context;
import android.util.AttributeSet;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.media.TLLocalPhoto;

/**
 * Created by ex3ndr on 26.02.14.
 */
public class SmallPreviewView extends BaseView<MediaLoader> {

    private TLLocalPhoto fastTask;
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
        fastTask = photo;
        photoFileName = null;
        requestBind();
    }

    public void requestFile(String fileName) {
        fastTask = null;
        photoFileName = fileName;
        requestBind();
    }

    public void clearImage() {
        fastTask = null;
        photoFileName = null;
        requestBind();
    }

    @Override
    protected void bind() {
        if (fastTask != null) {
            getLoader().requestFastSmallLoading(fastTask, this);
        } else if (photoFileName != null) {
            getLoader().requestRawSmall(photoFileName, this);
        } else {
            getLoader().cancelRequest(this);
        }
    }
}
