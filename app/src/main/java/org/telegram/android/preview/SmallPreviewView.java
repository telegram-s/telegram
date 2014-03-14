package org.telegram.android.preview;

import android.content.Context;
import android.util.AttributeSet;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.WebSearchResult;
import org.telegram.android.core.model.media.TLLocalDocument;
import org.telegram.android.core.model.media.TLLocalPhoto;
import org.telegram.android.core.model.media.TLLocalVideo;

/**
 * Created by ex3ndr on 26.02.14.
 */
public class SmallPreviewView extends BaseView<MediaLoader> {

    private TLLocalPhoto fastPhotoTask;
    private TLLocalVideo fastVideoTask;
    private TLLocalDocument fastDocTask;
    private String photoFileName;
    private WebSearchResult searchResult;

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
        fastDocTask = null;
        photoFileName = null;
        searchResult = null;
        requestBind();
    }

    public void requestFast(TLLocalVideo video) {
        fastPhotoTask = null;
        fastVideoTask = video;
        fastDocTask = null;
        photoFileName = null;
        requestBind();
    }

    public void requestFast(TLLocalDocument document) {
        fastPhotoTask = null;
        fastVideoTask = null;
        fastDocTask = document;
        photoFileName = null;
        searchResult = null;
        requestBind();
    }

    public void requestFile(String fileName) {
        fastPhotoTask = null;
        fastVideoTask = null;
        fastDocTask = null;
        photoFileName = fileName;
        searchResult = null;
        requestBind();
    }

    public void requestSearchThumb(WebSearchResult searchResult) {
        fastPhotoTask = null;
        fastVideoTask = null;
        fastDocTask = null;
        photoFileName = null;
        this.searchResult = searchResult;
        requestBind();
    }

    public void clearImage() {
        fastPhotoTask = null;
        fastVideoTask = null;
        fastDocTask = null;
        photoFileName = null;
        searchResult = null;
        requestBind();
    }

    @Override
    protected void bind() {
        if (fastPhotoTask != null) {
            getLoader().requestFastSmallLoading(fastPhotoTask, this);
        } else if (fastVideoTask != null) {
            getLoader().requestFastSmallLoading(fastVideoTask, this);
        } else if (fastDocTask != null) {
            getLoader().requestFastSmallLoading(fastDocTask, this);
        } else if (photoFileName != null) {
            getLoader().requestRawSmall(photoFileName, this);
        } else if (searchResult != null) {
            getLoader().requestSearchThumb(searchResult, this);
        } else {
            getLoader().cancelRequest(this);
        }
    }
}
