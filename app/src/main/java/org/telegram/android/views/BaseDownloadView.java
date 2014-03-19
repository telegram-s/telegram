package org.telegram.android.views;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import org.telegram.android.core.background.MediaSender;
import org.telegram.android.core.background.SenderListener;
import org.telegram.android.log.Logger;
import org.telegram.android.media.DownloadListener;
import org.telegram.android.media.DownloadState;

/**
 * Created by ex3ndr on 22.02.14.
 */
public abstract class BaseDownloadView extends BaseMsgStateView {
    protected static final int STATE_NONE = -1;
    protected static final int STATE_DOWNLOADED = 0;
    protected static final int STATE_IN_PROGRESS = 1;
    protected static final int STATE_ERROR = 2;
    protected static final int STATE_PENDING = 3;

    protected static final int MODE_NONE = 0;
    protected static final int MODE_DOWNLOAD = 1;
    protected static final int MODE_UPLOAD = 2;

    protected static final int PROGRESS_INTERMEDIATE = 0;
    protected static final int PROGRESS_TRANSITION = 1;
    protected static final int PROGRESS_FULL = 2;

    private int prevState = STATE_NONE;
    private int currentState = STATE_NONE;

    protected int downloadProgress;
    protected int oldDownloadProgress;
    protected long downloadStateTime;
    protected long stateTime;

    protected boolean isInStateSwitch;
    protected float oldStateAlpha;
    protected float newStateAlpha;

    protected int progressState = PROGRESS_INTERMEDIATE;
    private long progressTransitionStart;
    protected float progressWaitAlpha;
    protected float progressAlpha;
    protected float downloadAnimatedProgress;

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

    protected int getPrevState() {
        return prevState;
    }

    protected String getDownloadKey() {
        return downloadKey;
    }

    protected void clearBinding() {
        this.prevState = STATE_NONE;
        this.currentState = STATE_NONE;
        this.mode = MODE_NONE;
        this.downloadProgress = 0;
        this.downloadStateTime = 0;
        this.stateTime = 0;
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
                if (currentState != STATE_IN_PROGRESS) {
                    if (prevState != currentState) {
                        prevState = currentState;
                        stateTime = SystemClock.uptimeMillis();
                    }
                    currentState = STATE_IN_PROGRESS;
                }
                break;
            case FAILURE:
                if (currentState != STATE_ERROR) {
                    if (prevState != currentState) {
                        prevState = currentState;
                        stateTime = SystemClock.uptimeMillis();
                    }
                    currentState = STATE_ERROR;
                }
                break;
            case COMPLETED:
                if (currentState == STATE_IN_PROGRESS) {
                    rebind();
                } else {
                    if (currentState != STATE_DOWNLOADED) {
                        if (prevState != currentState) {
                            prevState = currentState;
                            stateTime = SystemClock.uptimeMillis();
                        }
                        currentState = STATE_DOWNLOADED;
                    }
                }
                break;
            default:
            case CANCELLED:
            case NONE:
                if (currentState != STATE_PENDING) {
                    if (prevState != currentState) {
                        prevState = currentState;
                        stateTime = SystemClock.uptimeMillis();
                    }
                    currentState = STATE_PENDING;
                }
                break;
        }
        if (currentState == STATE_IN_PROGRESS) {
            if (downloadProgress != progress) {
                oldDownloadProgress = downloadProgress;
                downloadProgress = progress;
                downloadStateTime = SystemClock.uptimeMillis();
            }
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

    protected void calculateAnimations() {
        if (mode != MODE_NONE) {
            // State animations
            if (currentState != prevState) {
                long stateAnimationTime = SystemClock.uptimeMillis() - stateTime;
                if (stateAnimationTime < GLOBAL_STATE_ANIMATION_TIME) {
                    float progress = stateAnimationTime / (float) GLOBAL_STATE_ANIMATION_TIME;
                    isInStateSwitch = true;
                    newStateAlpha = progress;
                    oldStateAlpha = 1 - progress;
                } else {
                    isInStateSwitch = false;
                    oldStateAlpha = 0;
                    newStateAlpha = 1;
                }
            }

            if (currentState == STATE_IN_PROGRESS) {
                long downloadProgressAnimationTime = SystemClock.uptimeMillis() - downloadStateTime;
                float newDownloadProgress;
                if (downloadProgressAnimationTime < FADE_ANIMATION_TIME) {
                    // float alpha = fadeEasing(downloadProgressAnimationTime / (float) FADE_ANIMATION_TIME);
                    float alpha = downloadProgressAnimationTime / (float) FADE_ANIMATION_TIME;
                    newDownloadProgress = (downloadAnimatedProgress + (downloadProgress - downloadAnimatedProgress) * alpha);
                } else {
                    newDownloadProgress = downloadProgress;

                }

                if (downloadProgress == 0) {
                    progressState = PROGRESS_INTERMEDIATE;
                } else {
                    if (progressState == PROGRESS_TRANSITION) {
                        long transitionTime = SystemClock.uptimeMillis() - progressTransitionStart;
                        if (transitionTime < FADE_ANIMATION_TIME) {
                            float alpha = transitionTime / (float) FADE_ANIMATION_TIME;
                            progressWaitAlpha = 1 - alpha;
                            progressAlpha = alpha;
                        } else {
                            progressState = PROGRESS_FULL;
                        }
                    } else if (progressState == PROGRESS_INTERMEDIATE) {
                        progressState = PROGRESS_TRANSITION;
                        progressWaitAlpha = 1;
                        progressAlpha = 0;
                        progressTransitionStart = SystemClock.uptimeMillis();
                    }
                }

                downloadAnimatedProgress = newDownloadProgress;
                // Logger.d("BaseDownloadView", "downloadAnimatedProgress: " + downloadProgress + ", " + oldDownloadProgress + ", " + downloadAnimatedProgress);
            }
        } else {
            isInStateSwitch = false;
            oldStateAlpha = 0;
            newStateAlpha = 1;
        }
    }
}
