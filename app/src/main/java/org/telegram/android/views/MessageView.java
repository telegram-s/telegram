package org.telegram.android.views;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.*;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.TypedValue;
import org.telegram.android.R;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.*;
import org.telegram.android.core.wireframes.MessageWireframe;
import org.telegram.android.log.Logger;
import org.telegram.android.ui.*;

/**
 * Author: Korshakov Stepan
 * Created: 14.08.13 20:26
 */
public class MessageView extends BaseMsgView {
    private static final String TAG = "MessageView";

    private static TextPaint bodyPaint;
    private static TextPaint clockOutPaint;
    private static TextPaint senderPaintBase;
    private static TextPaint forwardingPaintBase;
    private static Rect inBubblePadding;
    private static Rect outBubblePadding;
    private static boolean isLoaded;

    private Paint clockIconPaint;

    private Drawable statePending;
    private Drawable stateSent;
    private Drawable stateHalfCheck;
    private Drawable stateFailure;
    private static final int COLOR_NORMAL = 0xff70B15C;
    private static final int COLOR_ERROR = 0xffDB4942;
    private static final int COLOR_IN = 0xffA1AAB3;

    private int state;
    private int prevState;
    private long stateChangeTime;

    private MessageLayout messageLayout;
    private MessageLayout[] cachedLayout;

    private MessageWireframe wireframe;

    public MessageView(Context context) {
        super(context);
        init();
    }

    private static void checkResources(Context context) {
        if (isLoaded) {
            return;
        }

        bodyPaint = initTextPaint();
        bodyPaint.setTypeface(FontController.loadTypeface(context, "regular"));
        bodyPaint.setTextSize(sp(16));
        bodyPaint.setColor(0xff000000);

        clockOutPaint = initTextPaint();
        clockOutPaint.setTypeface(FontController.loadTypeface(context, "italic"));
        clockOutPaint.setTextSize(sp(12f));
        clockOutPaint.setColor(0xff70B15C);

        senderPaintBase = initTextPaint();
        senderPaintBase.setTypeface(FontController.loadTypeface(context, "regular"));
        senderPaintBase.setTextSize(sp(16));
        senderPaintBase.setColor(0xff000000);

        forwardingPaintBase = initTextPaint();
        forwardingPaintBase.setTypeface(FontController.loadTypeface(context, "light"));
        forwardingPaintBase.setTextSize(sp(16));
        forwardingPaintBase.setColor(0xff000000);

        Drawable inBubble = context.getResources().getDrawable(R.drawable.st_bubble_in);
        inBubblePadding = new Rect();
        inBubble.getPadding(inBubblePadding);

        Drawable outBubble = context.getResources().getDrawable(R.drawable.st_bubble_out);
        outBubblePadding = new Rect();
        inBubble.getPadding(outBubblePadding);

        isLoaded = true;
    }

    protected void init() {
        super.init();

        checkResources(getContext());

        clockIconPaint = new Paint();
        clockIconPaint.setStyle(Paint.Style.STROKE);
        clockIconPaint.setColor(0xff12C000);
        clockIconPaint.setStrokeWidth(getPx(1));
        clockIconPaint.setAntiAlias(true);
        clockIconPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        statePending = getResources().getDrawable(R.drawable.st_bubble_ic_clock);
        stateSent = getResources().getDrawable(R.drawable.st_bubble_ic_check);
        stateHalfCheck = getResources().getDrawable(R.drawable.st_bubble_ic_halfcheck);
        stateFailure = getResources().getDrawable(R.drawable.st_bubble_ic_warning);
    }

    @Override
    protected int getInBubbleResource() {
        return R.drawable.st_bubble_in;
    }

    @Override
    protected int getOutBubbleResource() {
        return R.drawable.st_bubble_out;
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
            case MessageState.PENDING:
                return statePending;
        }
    }

    @Override
    protected void bindNewView(MessageWireframe msg) {
        this.wireframe = msg;
        if (msg.cachedLayout instanceof MessageLayout[]) {
            cachedLayout = (MessageLayout[]) msg.cachedLayout;
        }
        this.state = msg.message.getState();
        this.prevState = -1;

        messageLayout = null;
        requestLayout();
    }

    @Override
    protected void bindUpdate(MessageWireframe msg) {
        this.wireframe = msg;
        if (msg.cachedLayout instanceof MessageLayout[]) {
            cachedLayout = (MessageLayout[]) msg.cachedLayout;
        }
        if (this.state != msg.message.getState()) {
            this.prevState = this.state;
            this.state = msg.message.getState();
            this.stateChangeTime = SystemClock.uptimeMillis();
        }

        if (messageLayout != null) {
            invalidate();
        } else {
            requestLayout();
        }
    }

    @Override
    protected void measureBubbleContent(int desiredWidth) {
        if (messageLayout == null || messageLayout.layoutDesiredWidth != desiredWidth) {
            messageLayout = null;
            if (cachedLayout != null) {
                for (MessageLayout l : cachedLayout) {
                    if (Math.abs(l.layoutDesiredWidth - desiredWidth) < 6) {
                        messageLayout = l;
                        break;
                    }
                }
            }

            if (messageLayout == null) {
                messageLayout = new MessageLayout();
                messageLayout.build(wireframe, desiredWidth, application);
            }
        }

        setBubbleMeasuredContent(messageLayout.layoutRealWidth, messageLayout.layoutHeight);
    }

    @Override
    protected boolean drawBubble(Canvas canvas) {
        if (messageLayout == null) {
            requestLayout();
            return false;
        }

        boolean isAnimated = false;

        if (messageLayout.isForwarded) {
            canvas.drawText("Forwarded message", 0, getPx(16), messageLayout.forwardingPaint);
            canvas.drawText("From", 0, getPx(35), messageLayout.forwardingPaint);
            canvas.drawText(messageLayout.forwarderNameMeasured, messageLayout.forwardOffset, getPx(35), messageLayout.senderPaint);
            canvas.save();
            canvas.translate(0, getPx(19) * 2);
            messageLayout.layout.draw(canvas);
            canvas.restore();
        } else {
            if (!messageLayout.isOut & messageLayout.isGroup) {
                canvas.drawText(messageLayout.senderNameMeasured, 0, getPx(16), messageLayout.senderPaint);
                canvas.save();
                canvas.translate(0, getPx(19));
                messageLayout.layout.draw(canvas);
                canvas.restore();
            } else {
                messageLayout.layout.draw(canvas);
            }
        }


        if (messageLayout.showState) {
            if (state == MessageState.PENDING) {
                canvas.save();
                canvas.translate(messageLayout.layoutRealWidth - getPx(12), messageLayout.layoutHeight - getPx(12) - getPx(3));
                canvas.drawCircle(getPx(6), getPx(6), getPx(6), clockIconPaint);
                double time = (System.currentTimeMillis() / 15.0) % (12 * 60);
                double angle = (time / (6 * 60)) * Math.PI;

                int x = (int) (Math.sin(-angle) * getPx(4));
                int y = (int) (Math.cos(-angle) * getPx(4));
                canvas.drawLine(getPx(6), getPx(6), getPx(6) + x, getPx(6) + y, clockIconPaint);

                x = (int) (Math.sin(-angle * 12) * getPx(5));
                y = (int) (Math.cos(-angle * 12) * getPx(5));
                canvas.drawLine(getPx(6), getPx(6), getPx(6) + x, getPx(6) + y, clockIconPaint);

                canvas.restore();

                clockOutPaint.setColor(COLOR_NORMAL);

                isAnimated = true;
            } else if (state == MessageState.READED && prevState == MessageState.SENT && (SystemClock.uptimeMillis() - stateChangeTime < STATE_ANIMATION_TIME)) {
                long animationTime = SystemClock.uptimeMillis() - stateChangeTime;
                float progress = easeStateFade(animationTime / (float) STATE_ANIMATION_TIME);
                int offset = (int) (getPx(5) * progress);
                int alphaNew = (int) (progress * 255);

                bounds(stateSent, messageLayout.layoutRealWidth - stateSent.getIntrinsicWidth() - offset,
                        messageLayout.layoutHeight - stateSent.getIntrinsicHeight() - getPx(3));
                stateSent.setAlpha(255);
                stateSent.draw(canvas);

                bounds(stateHalfCheck, messageLayout.layoutRealWidth - stateHalfCheck.getIntrinsicWidth() + getPx(5) - offset,
                        messageLayout.layoutHeight - stateHalfCheck.getIntrinsicHeight() - getPx(3));
                stateHalfCheck.setAlpha(alphaNew);
                stateHalfCheck.draw(canvas);

                clockOutPaint.setColor(COLOR_NORMAL);

                isAnimated = true;
            } else {
                Drawable stateDrawable = getStateDrawable(state);

                bounds(stateDrawable, messageLayout.layoutRealWidth - stateDrawable.getIntrinsicWidth(), messageLayout.layoutHeight - stateDrawable.getIntrinsicHeight() - getPx(3));
                stateDrawable.setAlpha(255);
                stateDrawable.draw(canvas);

                if (state == MessageState.READED) {
                    bounds(stateSent, messageLayout.layoutRealWidth - stateSent.getIntrinsicWidth() - getPx(5),
                            messageLayout.layoutHeight - stateDrawable.getIntrinsicHeight() - getPx(3));
                    stateSent.setAlpha(255);
                    stateSent.draw(canvas);
                }

                if (state == MessageState.FAILURE) {
                    clockOutPaint.setColor(COLOR_ERROR);
                } else {
                    clockOutPaint.setColor(COLOR_NORMAL);
                }
            }
        } else {
            clockOutPaint.setColor(COLOR_IN);
        }
        canvas.drawText(wireframe.date, messageLayout.layoutRealWidth - messageLayout.timeWidth + getPx(6), messageLayout.layoutHeight - getPx(4), clockOutPaint);
        return isAnimated;
    }

    public static Object prepareLayout(MessageWireframe wireframe, StelsApplication application1) {
        checkResources(application1);

        int maxWidthH;
        int maxWidthV;
        if (wireframe.message.isOut()) {
            maxWidthH = getBubblePadding(wireframe, outBubblePadding, UiMeasure.METRICS.widthPixels, application1);
            maxWidthV = getBubblePadding(wireframe, outBubblePadding, UiMeasure.METRICS.heightPixels, application1);
        } else {
            maxWidthH = getBubblePadding(wireframe, inBubblePadding, UiMeasure.METRICS.widthPixels, application1);
            maxWidthV = getBubblePadding(wireframe, inBubblePadding, UiMeasure.METRICS.heightPixels, application1);
        }

        // getBubblePadding(wireframe,)
        MessageLayout layoutV = new MessageLayout();
        layoutV.build(wireframe, maxWidthV, application1);
        MessageLayout layoutH = new MessageLayout();
        layoutH.build(wireframe, maxWidthH, application1);
        return new MessageLayout[]{layoutV, layoutH};
    }

    private static class MessageLayout {
        private Spannable spannable;

        private TextPaint senderPaint;
        private TextPaint forwardingPaint;

        private StaticLayout layout;
        private int layoutDesiredWidth;
        private int layoutRealWidth;
        private int layoutHeight;
        private int timeWidth;

        private String forwarderName;
        private String forwarderNameMeasured;
        private String senderName;
        private String senderNameMeasured;

        private boolean isOut;
        private boolean isGroup;
        private boolean isForwarded;
        private boolean showState;

        private int forwardOffset;

        public void build(MessageWireframe wireframe, int desiredWidth, StelsApplication application) {

            Logger.d(TAG, "Build layout start");

            checkResources(application);

            senderPaint = initTextPaint();
            senderPaint.setTypeface(FontController.loadTypeface(application, "regular"));
            senderPaint.setTextSize(sp(16));
            senderPaint.setColor(0xff000000);

            forwardingPaint = initTextPaint();
            forwardingPaint.setTypeface(FontController.loadTypeface(application, "light"));
            forwardingPaint.setTextSize(sp(16));
            forwardingPaint.setColor(0xff000000);

            this.layoutDesiredWidth = desiredWidth;
            this.isOut = wireframe.message.isOut();
            this.showState = isOut;
            this.isGroup = wireframe.message.getPeerType() == PeerType.PEER_CHAT && !isOut;
            if (isGroup) {
                User user = wireframe.senderUser;
                this.senderName = user.getDisplayName();
                if (!wireframe.message.isForwarded()) {
                    senderPaint.setColor(Placeholders.USER_PLACEHOLDERS_COLOR[wireframe.message.getSenderId() % Placeholders.USER_PLACEHOLDERS_COLOR.length]);
                    forwardingPaint.setColor(Placeholders.USER_PLACEHOLDERS_COLOR[wireframe.message.getSenderId() % Placeholders.USER_PLACEHOLDERS_COLOR.length]);
                }
            }

            if (wireframe.message.isForwarded()) {
                isForwarded = true;
                this.forwarderName = wireframe.forwardUser.getDisplayName();
                if (isOut) {
                    forwardingPaint.setColor(0xff739f53);
                    senderPaint.setColor(0xff739f53);
                } else {
                    forwardingPaint.setColor(0xff4884cf);
                    senderPaint.setColor(0xff4884cf);
                }
            } else {
                isForwarded = false;
            }

            if (isGroup) {
                User user = application.getEngine().getUser(wireframe.message.getSenderId());
                this.senderName = user.getDisplayName();
            }
            if (wireframe.message.isForwarded()) {
                isForwarded = true;
                this.forwarderName = wireframe.forwardUser.getDisplayName();
            } else {
                isForwarded = false;
            }

            layoutDesiredWidth = desiredWidth;
            long start = SystemClock.uptimeMillis();

            this.spannable = application.getEmojiProcessor().processEmojiCompatMutable(wireframe.message.getMessage(), EmojiProcessor.CONFIGURATION_BUBBLES);

            // spannable = new SpannableString(wireframe.message.getMessage());
            Logger.d(TAG, "Emoji processed in " + (SystemClock.uptimeMillis() - start) + " ms");
            start = SystemClock.uptimeMillis();
            Linkify.addLinks(this.spannable, Linkify.ALL);
            fixLinks(spannable);
            Logger.d(TAG, "Added links in " + (SystemClock.uptimeMillis() - start) + " ms");
            start = SystemClock.uptimeMillis();
            layout = new StaticLayout(spannable, bodyPaint, desiredWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
            Logger.d(TAG, "Built base layout in " + (SystemClock.uptimeMillis() - start) + " ms");

            if (layout.getLineCount() < 20) {
                int layoutTextWidth = 0;

                for (int i = 0; i < layout.getLineCount(); i++) {
                    layoutTextWidth = (int) Math.max(layout.getLineWidth(i), layoutTextWidth);
                }

                if (layoutTextWidth < layout.getWidth() - px(10)) {
                    layout = new StaticLayout(spannable, bodyPaint, layoutTextWidth + px(2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
                }
            }

            layoutRealWidth = layout.getWidth();

            timeWidth = (int) clockOutPaint.measureText(wireframe.date) + px((showState ? 23 : 0) + 6);

            if (layout.getLineCount() == 1) {
                boolean isLastRtl = layout.getParagraphDirection(0) == Layout.DIR_RIGHT_TO_LEFT;
                if (!isLastRtl && desiredWidth - layoutRealWidth > timeWidth) {
                    layoutRealWidth += timeWidth;
                    layoutHeight = layout.getHeight() + px(3);
                } else if (isLastRtl && desiredWidth - layout.getWidth() > timeWidth) {
                    layoutRealWidth = layout.getWidth() + timeWidth;
                    layoutHeight = layout.getHeight() + px(3);
                } else {
                    if (isLastRtl) {
                        layoutRealWidth = layout.getWidth();
                    }

                    layoutHeight = layout.getHeight() + px(17);
                }
            } else {
                boolean isLastRtl = layout.getParagraphDirection(layout.getLineCount() - 1) == Layout.DIR_RIGHT_TO_LEFT;
                if (!isLastRtl && (desiredWidth - layout.getLineWidth(layout.getLineCount() - 1) > timeWidth)) {
                    layoutRealWidth = (int) Math.max(layoutRealWidth, layout.getLineWidth(layout.getLineCount() - 1) + timeWidth);
                    layoutHeight = layout.getHeight() + px(3);
                } else if (isLastRtl && (desiredWidth - layout.getWidth() > timeWidth)) {
                    layoutRealWidth = (int) Math.max(layoutRealWidth, layout.getWidth() + timeWidth);
                    layoutHeight = layout.getHeight() + px(3);
                } else {
                    layoutHeight = layout.getHeight() + px(17);
                }
            }

            if (layoutRealWidth < timeWidth) {
                layoutRealWidth = timeWidth;
            }

            if (isForwarded) {
                layoutHeight += px(19) * 2;
                forwardOffset = (int) forwardingPaintBase.measureText("From ");
                layoutRealWidth = (int) Math.max(layoutRealWidth, forwardingPaintBase.measureText("Forwarded message"));
                forwarderNameMeasured = TextUtils.ellipsize(forwarderName, senderPaintBase, desiredWidth - forwardOffset, TextUtils.TruncateAt.END).toString();
                layoutRealWidth = (int) Math.max(layoutRealWidth, forwardOffset + senderPaintBase.measureText(forwarderNameMeasured));
            }

            if (isGroup && !isOut && !isForwarded) {
                layoutHeight += px(19);
                senderNameMeasured = TextUtils.ellipsize(senderName, senderPaintBase, desiredWidth, TextUtils.TruncateAt.END).toString();
                int width = (int) senderPaintBase.measureText(senderNameMeasured);
                layoutRealWidth = Math.max(layoutRealWidth, width);
            }

            Logger.d(TAG, "Build layout end");
        }
    }

    private static void fixLinks(Spannable spannable) {
        for (URLSpan span : spannable.getSpans(0, spannable.length(), URLSpan.class)) {
            final String url = span.getURL();
            int start = spannable.getSpanStart(span);
            int end = spannable.getSpanEnd(span);
            int flags = spannable.getSpanFlags(span);
            URLSpan newSpan = new URLSpan(url) {
                @Override
                public void updateDrawState(TextPaint paramTextPaint) {
                    super.updateDrawState(paramTextPaint);
                    paramTextPaint.setUnderlineText(false);
                    paramTextPaint.setColor(0xff006FC8);
                }
            };
            spannable.removeSpan(span);
            spannable.setSpan(newSpan, start, end, flags);
        }
    }

    private static TextPaint initTextPaint() {
        if (FontController.USE_SUBPIXEL) {
            return new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        } else {
            return new TextPaint(Paint.ANTI_ALIAS_FLAG);
        }
    }
}
