package org.telegram.android.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.StaticLayout;
import android.util.AttributeSet;
import org.telegram.android.R;
import org.telegram.android.core.background.MediaSender;
import org.telegram.android.core.background.SenderListener;
import org.telegram.android.core.model.media.TLLocalDocument;
import org.telegram.android.core.model.media.TLLocalFileDocument;
import org.telegram.android.core.model.media.TLUploadingDocument;
import org.telegram.android.core.wireframes.MessageWireframe;
import org.telegram.android.media.DownloadListener;
import org.telegram.android.media.DownloadManager;
import org.telegram.android.media.DownloadState;
import org.telegram.android.ui.TextUtil;

/**
 * Created by ex3ndr on 15.12.13.
 */
public class MessageDocumentView extends BaseMsgView {

    private DownloadListener downloadListener;
    private SenderListener senderListener;

    private Paint progressPaint;
    private Paint progressBgPaint;
    private Paint downloadBgPaint;
    private Drawable documentIcon;

    private int downloadProgress;
    private int oldDownloadProgress;
    private long downloadStateTime;
    private String downloadString;
    private String key;

    private int databaseId;

    private String fileName;
    private String fileSize;

    private boolean isDownloaded;

    private int contentW;
    private int contentH;

    public MessageDocumentView(Context context) {
        super(context);
    }

    public MessageDocumentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageDocumentView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void init() {
        super.init();

        documentIcon = getResources().getDrawable(R.drawable.app_icon);

        progressPaint = new Paint();
        progressPaint.setStyle(Paint.Style.FILL);
        progressPaint.setColor(0xFF669dd8);

        downloadBgPaint = new Paint();
        downloadBgPaint.setStyle(Paint.Style.FILL);
        downloadBgPaint.setColor(0xB6000000);

        progressBgPaint = new Paint();
        progressBgPaint.setStyle(Paint.Style.FILL);
        progressBgPaint.setColor(0x0F669dd8);

        downloadListener = new DownloadListener() {
            @Override
            public void onStateChanged(final String _key, final DownloadState state, final int percent) {
                if (!_key.equals(key))
                    return;

                if (downloadProgress != percent) {
                    oldDownloadProgress = downloadProgress;
                    downloadProgress = percent;
                    downloadStateTime = SystemClock.uptimeMillis();
                }
                switch (state) {
                    case CANCELLED:
                        downloadString = getResources().getString(R.string.st_bubble_media_cancelled);
                        break;
                    case FAILURE:
                        downloadString = getResources().getString(R.string.st_bubble_media_try_again);
                        break;
                    case NONE:
                        downloadString = getResources().getString(R.string.st_bubble_media_download);
                        break;
                    case IN_PROGRESS:
                    case PENDING:
                        downloadString = getResources().getString(R.string.st_bubble_media_in_progress);
                        break;
                    case COMPLETED:
                        downloadString = null;
                        isDownloaded = true;
                        rebind();
                        break;
                }
                postInvalidate();
            }
        };
        application.getDownloadManager().registerListener(downloadListener);

        senderListener = new SenderListener() {
            @Override
            public void onUploadStateChanged(int localId, MediaSender.SendState state) {
                if (databaseId != localId)
                    return;

                if (downloadProgress != state.getUploadProgress()) {
                    oldDownloadProgress = downloadProgress;
                    downloadProgress = state.getUploadProgress();
                    downloadStateTime = SystemClock.uptimeMillis();
                }

                if (state.isCanceled()) {
                    downloadString = getResources().getString(R.string.st_bubble_media_cancelled);
                } else if (state.isUploaded()) {
                    downloadString = null;
                } else if (state.isSent()) {
                    downloadString = null;
                    isDownloaded = true;
                    // rebind();
                } else {
                    downloadString = getResources().getString(R.string.st_bubble_media_in_progress);
                }
                postInvalidate();
            }
        };
        application.getMediaSender().registerListener(senderListener);
    }

    @Override
    protected void bindNewView(MessageWireframe message) {
        databaseId = message.databaseId;
        downloadProgress = 0;
    }

    @Override
    protected void bindCommon(MessageWireframe message) {
        if (message.message.getExtras() instanceof TLUploadingDocument) {
            TLUploadingDocument doc = (TLUploadingDocument) message.message.getExtras();
            fileName = doc.getFileName();
            fileSize = "???";
            isDownloaded = false;

            MediaSender.SendState state = application.getMediaSender().getSendState(databaseId);
            if (state != null) {
                if (downloadProgress != state.getUploadProgress()) {
                    oldDownloadProgress = downloadProgress;
                    downloadProgress = state.getUploadProgress();
                    downloadStateTime = SystemClock.uptimeMillis();
                }
                if (state.isCanceled()) {
                    downloadString = getResources().getString(R.string.st_bubble_media_cancelled);
                } else if (state.isUploaded()) {
                    downloadString = null;
                } else {
                    downloadString = getResources().getString(R.string.st_bubble_media_in_progress);
                }
            }
        } else if (message.message.getExtras() instanceof TLLocalDocument) {
            TLLocalDocument doc = (TLLocalDocument) message.message.getExtras();
            fileName = doc.getFileName();
            fileSize = TextUtil.formatFileSize(((TLLocalFileDocument) doc.getFileLocation()).getSize());
            key = DownloadManager.getDocumentKey(doc);

            if (application.getDownloadManager().getState(key) == DownloadState.COMPLETED) {
                isDownloaded = true;
            } else {
                if (downloadProgress != application.getDownloadManager().getDownloadProgress(key)) {
                    oldDownloadProgress = downloadProgress;
                    downloadProgress = application.getDownloadManager().getDownloadProgress(key);
                    downloadStateTime = SystemClock.uptimeMillis();
                }
            }
        }

        if (message.message.isOut()) {
            downloadBgPaint.setColor(0xffD9FFB9);
        } else {
            downloadBgPaint.setColor(Color.WHITE);
        }
    }

    @Override
    protected void measureBubbleContent(int width) {
        contentW = getPx(200);
        contentH = getPx(56);
        setBubbleMeasuredContent(contentW, contentH);
    }

    @Override
    protected int getOutPressedBubbleResource() {
        return R.drawable.st_bubble_out_media_overlay;
    }

    @Override
    protected int getInPressedBubbleResource() {
        return R.drawable.st_bubble_in_media_overlay;
    }

    @Override
    protected int getInBubbleResource() {
        return R.drawable.st_bubble_in_media_normal;
    }

    @Override
    protected int getOutBubbleResource() {
        return R.drawable.st_bubble_out_media_normal;
    }

    @Override
    protected boolean drawBubble(Canvas canvas) {
        canvas.drawRect(new RectF(0, 0, contentW, contentH), downloadBgPaint);

        if (!isDownloaded) {
            int visibleProgress = (downloadProgress * (contentW + getPx(6))) / 100;
            canvas.drawRect(new RectF(-getPx(3), -getPx(3), visibleProgress - getPx(3), contentH), progressBgPaint);
            canvas.drawRect(new RectF(-getPx(3), contentH, visibleProgress - getPx(3), contentH + getPx(3)), progressPaint);
        }

        return false;
    }
}
