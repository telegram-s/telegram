package org.telegram.android.views;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import org.telegram.android.R;
import org.telegram.android.core.background.MediaSender;
import org.telegram.android.core.background.SenderListener;
import org.telegram.android.core.model.MessageState;
import org.telegram.android.core.model.media.TLLocalDocument;
import org.telegram.android.core.model.media.TLLocalEncryptedFileLocation;
import org.telegram.android.core.model.media.TLLocalFileDocument;
import org.telegram.android.core.model.media.TLUploadingDocument;
import org.telegram.android.core.wireframes.MessageWireframe;
import org.telegram.android.media.DownloadListener;
import org.telegram.android.media.DownloadManager;
import org.telegram.android.media.DownloadState;
import org.telegram.android.ui.FontController;
import org.telegram.android.ui.TextUtil;

/**
 * Created by ex3ndr on 15.12.13.
 */
public class MessageDocumentView extends MessageBaseDocView {
    private TextPaint fileNamePaint;
    private TextPaint fileDeskPaint;
    private Drawable documentIconOut;
    private Drawable documentIconOutDownloaded;
    private Drawable documentIconIn;
    private Drawable documentIconInDownloaded;
    private Drawable documentIcon;

    private String fileName;
    private String fileNameMeasured;
    private String fileSize;

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

        documentIconOut = getResources().getDrawable(R.drawable.st_bubble_in_doc);
        documentIconInDownloaded = getResources().getDrawable(R.drawable.st_bubble_in_doc_downloaded);
        documentIconIn = getResources().getDrawable(R.drawable.st_bubble_in_doc);
        documentIconInDownloaded = getResources().getDrawable(R.drawable.st_bubble_in_doc_downloaded);

        if (FontController.USE_SUBPIXEL) {
            fileNamePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        } else {
            fileNamePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        }
        fileNamePaint.setTypeface(FontController.loadTypeface(getContext(), "regular"));
        fileNamePaint.setTextSize(getSp(16f));
        fileNamePaint.setColor(0xff000000);

        if (FontController.USE_SUBPIXEL) {
            fileDeskPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        } else {
            fileDeskPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        }
        fileDeskPaint.setTypeface(FontController.loadTypeface(getContext(), "regular"));
        fileDeskPaint.setTextSize(getSp(14f));
        fileDeskPaint.setColor(0xff000000);
    }

    @Override
    protected void bindNewView(MessageWireframe message) {
        super.bindNewView(message);
        bindStateNew(message);
        if (message.message.isOut()) {
            documentIcon = documentIconOut;
            fileDeskPaint.setColor(0xff97bb7c);
        } else {
            documentIcon = documentIconIn;
            fileDeskPaint.setColor(0xffa1aab3);
        }
    }

    @Override
    protected void bindUpdate(MessageWireframe message) {
        super.bindUpdate(message);
        bindStateUpdate(message);
    }

    @Override
    protected void bindCommon(MessageWireframe message) {
        super.bindCommon(message);

        if (message.message.getExtras() instanceof TLUploadingDocument) {
            TLUploadingDocument doc = (TLUploadingDocument) message.message.getExtras();
            fileName = doc.getFileName();
            int index = fileName.lastIndexOf('.');
            if (index > 0) {
                String ext = fileName.substring(index + 1).trim();
                if (ext.length() > 4) {
                    ext = ext.substring(0, 3) + "\u2026";
                }
                fileSize = TextUtil.formatFileSize(doc.getFileSize()) + " " + ext.toUpperCase();
            } else {
                fileSize = TextUtil.formatFileSize(doc.getFileSize());
            }
        } else if (message.message.getExtras() instanceof TLLocalDocument) {
            TLLocalDocument doc = (TLLocalDocument) message.message.getExtras();
            fileName = doc.getFileName();
            int index = fileName.lastIndexOf('.');
            if (index > 0) {
                String ext = fileName.substring(index + 1).trim();
                if (ext.length() > 4) {
                    ext = ext.substring(0, 3) + "\u2026";
                }
                if (doc.getFileLocation() instanceof TLLocalFileDocument) {
                    fileSize = TextUtil.formatFileSize(((TLLocalFileDocument) doc.getFileLocation()).getSize()) + " " + ext.toUpperCase();
                } else if (doc.getFileLocation() instanceof TLLocalEncryptedFileLocation) {
                    fileSize = TextUtil.formatFileSize(((TLLocalEncryptedFileLocation) doc.getFileLocation()).getSize()) + " " + ext.toUpperCase();
                } else {
                    fileSize = ext.toUpperCase();
                }

            } else {
                if (doc.getFileLocation() instanceof TLLocalFileDocument) {
                    fileSize = TextUtil.formatFileSize(((TLLocalFileDocument) doc.getFileLocation()).getSize());
                } else if (doc.getFileLocation() instanceof TLLocalEncryptedFileLocation) {
                    fileSize = TextUtil.formatFileSize(((TLLocalEncryptedFileLocation) doc.getFileLocation()).getSize());
                } else {
                    fileSize = "";
                }
            }
        } else {
            fileSize = "";
            fileName = "unknown";
        }
    }

    @Override
    protected int measureHeight() {
        return px(54);
    }

    @Override
    protected void measureBubbleContent(int width) {
        super.measureBubbleContent(width);
        fileNameMeasured = TextUtils.ellipsize(fileName, fileNamePaint, getPx(160), TextUtils.TruncateAt.END).toString();
    }

    @Override
    protected void drawContent(Canvas canvas) {
        canvas.drawText(fileNameMeasured, getPx(54), getPx(24), fileNamePaint);
        canvas.drawText(fileSize, getPx(54), getPx(42), fileDeskPaint);

        // canvas.drawRect(new Rect(getPx(4), getPx(4), getPx(4 + 48), getPx(4 + 48)), iconBgPaint);

        if (getState() == STATE_DOWNLOADED) {
            documentIconInDownloaded.setBounds(new Rect(getPx(12), getPx(12), getPx(12 + 30), getPx(12 + 30)));
            documentIconInDownloaded.draw(canvas);
        } else {
            documentIcon.setBounds(new Rect(getPx(12), getPx(12), getPx(12 + 30), getPx(12 + 30)));
            documentIcon.draw(canvas);
        }
    }
}
