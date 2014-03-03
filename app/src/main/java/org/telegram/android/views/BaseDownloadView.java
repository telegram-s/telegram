package org.telegram.android.views;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import org.telegram.android.core.background.MediaSender;
import org.telegram.android.core.background.SenderListener;
import org.telegram.android.media.DownloadListener;
import org.telegram.android.media.DownloadState;

/**
 * Created by ex3ndr on 22.02.14.
 */
public abstract class BaseDownloadView extends BaseMsgView {
    protected static final int STATE_NONE = -1;
    protected static final int STATE_DOWNLOADED = 0;
    protected static final int STATE_IN_PROGRESS = 1;
    protected static final int STATE_ERROR = 2;
    protected static final int STATE_PENDING = 3;

    protected static final int MODE_NONE = 0;
    protected static final int MODE_DOWNLOAD = 1;
    protected static final int MODE_UPLOAD = 2;

    private int currentState = STATE_NONE;

    protected int downloadProgress;
    protected int oldDownloadProgress;
    protected long downloadStateTime;

    protected int mode = MODE_NONE;
    private String downloadKey;
    private int uploadId;

    private DownloadListener downloadListener;
    private SenderListener senderListener;

    public BaseDownloadView(Context context) {
        super(context);
    }

    public BaseDownloadView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseDownloadView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void init() {
        super.init();

        downloadListener = new DownloadListener() {
            long lastNotify = 0;

            @Override
            public void onStateChanged(String _key, DownloadState state, int percent) {
                if (mode != MODE_DOWNLOAD || !_key.equals(downloadKey)) {
                    return;
                }

                updateDownloadState(state, percent);
                postInvalidate();
            }
        };
        application.getDownloadManager().registerListener(downloadListener);

        senderListener = new SenderListener() {
            @Override
            public void onUploadStateChanged(int localId, MediaSender.SendState state) {
                if (mode != MODE_UPLOAD || uploadId != localId) {
                    return;
                }

                updateUploadState(state);
                postInvalidate();
            }
        };
        application.getMediaSender().registerListener(senderListener);
    }

    protected int getState() {
        return currentState;
    }

    protected String getDownloadKey() {
        return downloadKey;
    }

    protected void clearBinding() {
        this.currentState = STATE_NONE;
        this.mode = MODE_NONE;
        this.downloadProgress = 0;
        this.downloadStateTime = 0;
    }

    protected void bindDownload(String key) {
        this.downloadKey = key;
        this.mode = MODE_DOWNLOAD;

        DownloadState state = application.getDownloadManager().getState(key);
        int progress = application.getDownloadManager().getDownloadProgress(key);
        updateDownloadState(state, progress);
    }

    protected void bindUpload(int id) {
        this.uploadId = id;
        this.mode = MODE_UPLOAD;
        MediaSender.SendState state = application.getMediaSender().getSendState(uploadId);
        updateUploadState(state);
    }

    private void updateDownloadState(DownloadState state, int progress) {
        switch (state) {
            case PENDING:
            case IN_PROGRESS:
                currentState = STATE_IN_PROGRESS;
                break;
            case FAILURE:
                currentState = STATE_ERROR;
                break;
            case COMPLETED:
                if (currentState == STATE_IN_PROGRESS) {
                    rebind();
                } else {
                    currentState = STATE_DOWNLOADED;
                }
                break;
            default:
            case CANCELLED:
            case NONE:
                currentState = STATE_PENDING;
                break;
        }
        if (state == DownloadState.IN_PROGRESS || state == DownloadState.PENDING) {
            currentState = STATE_IN_PROGRESS;
        }
        if (downloadProgress != progress) {
            oldDownloadProgress = downloadProgress;
            downloadProgress = progress;
            downloadStateTime = SystemClock.uptimeMillis();
        }
    }

    private void updateUploadState(MediaSender.SendState state) {
        if (state != null) {
            if (state.isCanceled()) {
                currentState = STATE_NONE;
                oldDownloadProgress = 0;
                downloadProgress = 0;
                downloadStateTime = 0;
            } else {
                currentState = STATE_IN_PROGRESS;
                if (downloadProgress != state.getUploadProgress()) {
                    oldDownloadProgress = downloadProgress;
                    downloadProgress = state.getUploadProgress();
                    downloadStateTime = SystemClock.uptimeMillis();
                }
            }
        } else {
            currentState = STATE_NONE;
            oldDownloadProgress = 0;
            downloadProgress = 0;
            downloadStateTime = 0;
        }
    }
}
