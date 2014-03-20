package org.telegram.android.views;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.AttributeSet;
import org.telegram.android.R;
import org.telegram.android.core.background.MediaSender;
import org.telegram.android.core.background.SenderListener;
import org.telegram.android.core.model.MessageState;
import org.telegram.android.core.model.media.TLLocalAudio;
import org.telegram.android.core.model.media.TLLocalDocument;
import org.telegram.android.core.model.media.TLUploadingAudio;
import org.telegram.android.core.model.media.TLUploadingDocument;
import org.telegram.android.core.wireframes.MessageWireframe;
import org.telegram.android.log.Logger;
import org.telegram.android.media.DownloadListener;
import org.telegram.android.media.DownloadManager;
import org.telegram.android.media.DownloadState;
import org.telegram.android.ui.FontController;

import java.util.Arrays;

/**
 * Created by ex3ndr on 12.01.14.
 */
public class MessageBaseDocView extends BaseDownloadView {
    private Paint progressPaint;
    private Paint progressBgPaint;
    private Paint progressBgLightPaint;
    private TextPaint clockOutPaint;
    private Paint clockIconPaint;

    private Drawable stateSent;
    private Drawable stateHalfCheck;
    private Drawable stateFailure;

    private boolean showState;

    private static final int COLOR_NORMAL = 0xff70B15C;
    private static final int COLOR_ERROR = 0xffDB4942;
    private static final int COLOR_IN = 0xffA1AAB3;

    protected int databaseId;

    protected int contentW;
    protected int contentH;

    public MessageBaseDocView(Context context) {
        super(context);
    }

    public MessageBaseDocView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageBaseDocView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void init() {
        super.init();

        progressPaint = new Paint();
        progressPaint.setStyle(Paint.Style.FILL);
        progressPaint.setColor(0xFF669dd8);

        progressBgPaint = new Paint();
        progressBgPaint.setStyle(Paint.Style.FILL);
        progressBgPaint.setColor(0x0F669dd8);

        progressBgLightPaint = new Paint();
        progressBgLightPaint.setStyle(Paint.Style.FILL);
        progressBgLightPaint.setColor(0x0F669dd8);

        if (FontController.USE_SUBPIXEL) {
            clockOutPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        } else {
            clockOutPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        }
        clockOutPaint.setTypeface(FontController.loadTypeface(getContext(), "regular"));
        clockOutPaint.setTextSize(getSp(12f));
        clockOutPaint.setColor(0xff70B15C);

        clockIconPaint = new Paint();
        clockIconPaint.setStyle(Paint.Style.STROKE);
        clockIconPaint.setColor(0xff12C000);
        clockIconPaint.setStrokeWidth(getPx(1));
        clockIconPaint.setAntiAlias(true);
        clockIconPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        stateSent = getResources().getDrawable(R.drawable.st_bubble_ic_check);
        stateHalfCheck = getResources().getDrawable(R.drawable.st_bubble_ic_halfcheck);
        stateFailure = getResources().getDrawable(R.drawable.st_bubble_ic_warning);
    }

    @Override
    protected void bindNewView(MessageWireframe message) {
        databaseId = message.databaseId;
        downloadProgress = 0;
        requestLayout();
    }

    @Override
    protected void bindCommon(MessageWireframe message) {
        clearBinding();
        if (message.message.getExtras() instanceof TLUploadingDocument || message.message.getExtras() instanceof TLUploadingAudio) {
            bindUpload(databaseId);
        } else if (message.message.getExtras() instanceof TLLocalDocument || message.message.getExtras() instanceof TLLocalAudio) {
            if (message.message.getExtras() instanceof TLLocalDocument) {
                TLLocalDocument doc = (TLLocalDocument) message.message.getExtras();
                bindDownload(DownloadManager.getDocumentKey(doc));
            } else if (message.message.getExtras() instanceof TLLocalAudio) {
                TLLocalAudio doc = (TLLocalAudio) message.message.getExtras();
                bindDownload(DownloadManager.getAudioKey(doc));
            }
        }

        this.showState = message.message.isOut();
    }

    protected int measureHeight() {
        return getPx(52);
    }

    protected int measureWidth() {
        return getPx(220);
    }

    @Override
    protected void measureBubbleContent(int width) {
        contentW = measureWidth();
        contentH = measureHeight();
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

    private Drawable getStateDrawable(int state) {
        switch (state) {
            default:
            case MessageState.SENT:
                return stateSent;
            case MessageState.READED:
                return stateHalfCheck;
            case MessageState.FAILURE:
                return stateFailure;
        }
    }

    protected void drawContent(Canvas canvas) {
    }

    private boolean drawProgress(Canvas canvas, int stateId, float stateAlpha) {
        boolean isAnimated = false;
        if (stateId == STATE_IN_PROGRESS) {

            progressBgLightPaint.setAlpha((int) (0x30 * stateAlpha));
            canvas.drawRect(new RectF(0, contentH - getPx(2), contentW, contentH), progressBgLightPaint);

            if (progressState == PROGRESS_FULL ||
                    progressState == PROGRESS_TRANSITION) {
                if (progressState == PROGRESS_TRANSITION) {
                    progressBgPaint.setAlpha((int) (0x0F * stateAlpha * progressAlpha));
                    progressPaint.setAlpha((int) (0xFF * stateAlpha * progressAlpha));
                } else {
                    progressBgPaint.setAlpha((int) (0x0F * stateAlpha));
                    progressPaint.setAlpha((int) (0xFF * stateAlpha));
                }
                int visibleProgress = (int) ((downloadAnimatedProgress * (contentW)) / 100);
                canvas.drawRect(new RectF(0, 0, visibleProgress, contentH), progressBgPaint);
                canvas.drawRect(new RectF(0, contentH - getPx(2), visibleProgress, contentH), progressPaint);
            }

            if (progressState == PROGRESS_INTERMEDIATE ||
                    progressState == PROGRESS_TRANSITION) {
                if (progressState == PROGRESS_TRANSITION) {
                    progressBgPaint.setAlpha((int) (0x0F * stateAlpha * progressWaitAlpha));
                    progressPaint.setAlpha((int) (0xFF * stateAlpha * progressWaitAlpha));
                } else {
                    progressBgPaint.setAlpha((int) (0x0F * stateAlpha));
                    progressPaint.setAlpha((int) (0xFF * stateAlpha));
                }
                long time = SystemClock.uptimeMillis();
                float p1 = (time % 999) / 999.0f;
                float p2 = ((time + 333) % 999) / 999.0f;
                float p3 = ((time + 666) % 999) / 999.0f;
                float[] ps = new float[]{p1, p2, p3};
                Arrays.sort(ps);

                float[] vs = new float[ps.length];
                int[] ws = new int[ps.length];
                for (int i = 0; i < vs.length; i++) {
                    vs[i] = progressEasing(ps[i]);
                    ws[i] = (int) ((1 - ps[i]) * getPx(4) + getPx(2));
                }

                int start1 = 0;
                int offset1 = (int) (vs[0] * contentW) - ws[0];

                int start2 = offset1 + ws[0] * 2;
                int offset2 = (int) (vs[1] * contentW) - ws[1];

                int start3 = offset2 + ws[1] * 2;
                int offset3 = (int) (vs[2] * contentW) - ws[2];

                int start4 = offset3 + ws[2] * 2;
                int offset4 = contentW;

                canvas.drawRect(new RectF(start1, contentH - getPx(2), offset1, contentH), progressPaint);
                canvas.drawRect(new RectF(start2, contentH - getPx(2), offset2, contentH), progressPaint);
                canvas.drawRect(new RectF(start3, contentH - getPx(2), offset3, contentH), progressPaint);
                canvas.drawRect(new RectF(start4, contentH - getPx(2), offset4, contentH), progressPaint);
            }
            isAnimated = true;
        }
        return isAnimated;
    }

    @Override
    protected boolean drawBubble(Canvas canvas) {

        boolean isAnimated = false;

        drawContent(canvas);

        isAnimated |= drawState(canvas, px(8), px(4));

        if (mode != MODE_NONE) {
            calculateAnimations();
            if (isInStateSwitch) {
                drawProgress(canvas, getPrevState(), oldStateAlpha);
                drawProgress(canvas, getState(), newStateAlpha);
                isAnimated = true;
            } else {
                isAnimated |= drawProgress(canvas, getState(), 1.0f);
            }
        }


        return isAnimated;
    }
}
