package org.telegram.android.views;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.*;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import org.telegram.android.R;
import org.telegram.android.core.model.*;
import org.telegram.android.ui.EmojiProcessor;
import org.telegram.android.ui.FontController;
import org.telegram.android.ui.Placeholders;
import org.telegram.android.ui.TextUtil;

/**
 * Author: Korshakov Stepan
 * Created: 14.08.13 20:26
 */
public class MessageView extends BaseMsgView {

    private TextPaint bodyPaint;
    private TextPaint clockOutPaint;
    private TextPaint senderPaint;
    private TextPaint forwardingPaint;
    private Paint clockIconPaint;

    private Drawable statePending;
    private Drawable stateSent;
    private Drawable stateHalfCheck;
    private Drawable stateFailure;
    private static final int COLOR_NORMAL = 0xff70B15C;
    private static final int COLOR_ERROR = 0xffDB4942;
    private static final int COLOR_IN = 0xffA1AAB3;

    private String message;
    private Spannable spannable;
    private String date;
    private int state;
    private int prevState;
    private long stateChangeTime;
    private boolean isOut;
    private boolean isGroup;
    private boolean isForwarded;
    private boolean showState;
    private String forwarderName;
    private String forwarderNameMeasured;
    private String senderName;
    private String senderNameMeasured;

    private StaticLayout layout;
    private int layoutRealWidth;
    private int layoutHeight;
    private int timeWidth;
    private int forwardOffset;
    private boolean isGroupChat;

    public MessageView(Context context, boolean isGroupChat) {
        super(context);
        this.isGroupChat = isGroupChat;
        init();
    }

    private TextPaint initTextPaint() {
        return new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        // return new TextPaint();
        // return new TextPaint(Paint.ANTI_ALIAS_FLAG);
    }

    protected void init() {
        super.init();

        bodyPaint = initTextPaint();
        bodyPaint.setTypeface(FontController.loadTypeface(getContext(), "normal"));
        bodyPaint.setTextSize(getSp(16));
        bodyPaint.setColor(0xff000000);

        senderPaint = initTextPaint();
        senderPaint.setTypeface(FontController.loadTypeface(getContext(), "normal"));
        senderPaint.setTextSize(getSp(16));
        senderPaint.setColor(0xff000000);

        forwardingPaint = initTextPaint();
        forwardingPaint.setTypeface(FontController.loadTypeface(getContext(), "light"));
        forwardingPaint.setTextSize(getSp(16));
        forwardingPaint.setColor(0xff000000);

        clockOutPaint = initTextPaint();
        clockOutPaint.setTypeface(FontController.loadTypeface(getContext(), "italic"));
        clockOutPaint.setTextSize(getSp(12f));
        clockOutPaint.setColor(0xff70B15C);

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

    private void fixLinks(Spannable spannable) {
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

    @Override
    protected void bindNewView(ChatMessage msg) {
        this.message = msg.getMessage();
        if (msg.isBodyHasSmileys()) {
            this.spannable = application.getEmojiProcessor().processEmojiCompatMutable(msg.getMessage(), EmojiProcessor.CONFIGURATION_BUBBLES);
            Linkify.addLinks(this.spannable, Linkify.ALL);
            fixLinks(spannable);
        } else {
            Spannable tmpSpannable = new SpannableString(msg.getMessage());
            if (Linkify.addLinks(tmpSpannable, Linkify.ALL)) {
                this.spannable = tmpSpannable;
                fixLinks(spannable);
            } else {
                this.spannable = null;
            }
        }
        this.date = TextUtil.formatTime(msg.getDate(), getContext());
        this.state = msg.getState();
        this.prevState = -1;
        this.isOut = msg.isOut();
        this.showState = isOut;
        this.isGroup = msg.getPeerType() == PeerType.PEER_CHAT && !isOut;
        if (isGroup) {
            User user = application.getEngine().getUser(msg.getSenderId());
            this.senderName = user.getDisplayName();
            if (!msg.isForwarded()) {
                this.senderPaint.setColor(Placeholders.USER_PLACEHOLDERS_COLOR[msg.getSenderId() % Placeholders.USER_PLACEHOLDERS_COLOR.length]);
                this.forwardingPaint.setColor(Placeholders.USER_PLACEHOLDERS_COLOR[msg.getSenderId() % Placeholders.USER_PLACEHOLDERS_COLOR.length]);
            }
        }

        if (msg.isForwarded()) {
            isForwarded = true;
            User forwardedUser = application.getEngine().getUser(msg.getForwardSenderId());
            this.forwarderName = forwardedUser.getDisplayName();
            if (isOut) {
                this.forwardingPaint.setColor(0xff739f53);
                this.senderPaint.setColor(0xff739f53);
            } else {
                this.forwardingPaint.setColor(0xff4884cf);
                this.senderPaint.setColor(0xff4884cf);
            }
        } else {
            isForwarded = false;
        }

        requestLayout();
    }

    @Override
    protected void bindUpdate(ChatMessage msg) {
        this.date = TextUtil.formatTime(msg.getDate(), getContext());
        if (this.state != msg.getState()) {
            this.prevState = this.state;
            this.state = msg.getState();
            this.stateChangeTime = SystemClock.uptimeMillis();
        }
        if (isGroup) {
            User user = application.getEngine().getUser(msg.getSenderId());
            this.senderName = user.getDisplayName();
        }
        if (msg.isForwarded()) {
            isForwarded = true;
            User forwardedUser = application.getEngine().getUser(msg.getForwardSenderId());
            this.forwarderName = forwardedUser.getDisplayName();
        } else {
            isForwarded = false;
        }

        invalidate();
    }

    @Override
    protected void measureBubbleContent(int desiredWidth) {
        if (spannable != null) {
            layout = new StaticLayout(spannable, bodyPaint, desiredWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
        } else {
            layout = new StaticLayout(message, bodyPaint, desiredWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
        }

        if (layout.getLineCount() < 20) {
            int layoutTextWidth = 0;

            for (int i = 0; i < layout.getLineCount(); i++) {
                layoutTextWidth = (int) Math.max(layout.getLineWidth(i), layoutTextWidth);
            }

            if (layoutTextWidth < layout.getWidth() - getPx(10)) {
                if (spannable != null) {
                    layout = new StaticLayout(spannable, bodyPaint, layoutTextWidth + getPx(2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
                } else {
                    layout = new StaticLayout(message, bodyPaint, layoutTextWidth + getPx(2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
                }
            }
        }

        layoutRealWidth = layout.getWidth();

        timeWidth = (int) clockOutPaint.measureText(date) + getPx((showState ? 23 : 0) + 6);

        if (layout.getLineCount() == 1) {
            boolean isLastRtl = layout.getParagraphDirection(0) == Layout.DIR_RIGHT_TO_LEFT;
            if (!isLastRtl && desiredWidth - layoutRealWidth > timeWidth) {
                layoutRealWidth += timeWidth;
                layoutHeight = layout.getHeight() + getPx(3);
            } else if (isLastRtl && desiredWidth - layout.getWidth() > timeWidth) {
                layoutRealWidth = layout.getWidth() + timeWidth;
                layoutHeight = layout.getHeight() + getPx(3);
            } else {
                if (isLastRtl) {
                    layoutRealWidth = layout.getWidth();
                }

                layoutHeight = layout.getHeight() + getPx(17);
            }
        } else {
            boolean isLastRtl = layout.getParagraphDirection(layout.getLineCount() - 1) == Layout.DIR_RIGHT_TO_LEFT;
            if (!isLastRtl && (desiredWidth - layout.getLineWidth(layout.getLineCount() - 1) > timeWidth)) {
                layoutRealWidth = (int) Math.max(layoutRealWidth, layout.getLineWidth(layout.getLineCount() - 1) + timeWidth);
                layoutHeight = layout.getHeight() + getPx(3);
            } else if (isLastRtl && (desiredWidth - layout.getWidth() > timeWidth)) {
                layoutRealWidth = (int) Math.max(layoutRealWidth, layout.getWidth() + timeWidth);
                layoutHeight = layout.getHeight() + getPx(3);
            } else {
                layoutHeight = layout.getHeight() + getPx(17);
            }
        }

        if (layoutRealWidth < timeWidth) {
            layoutRealWidth = timeWidth;
        }

        if (isForwarded) {
            layoutHeight += getPx(19) * 2;
            forwardOffset = (int) forwardingPaint.measureText("From ");
            layoutRealWidth = (int) Math.max(layoutRealWidth, forwardingPaint.measureText("Forwarded message"));
            forwarderNameMeasured = TextUtils.ellipsize(forwarderName, senderPaint, desiredWidth - forwardOffset, TextUtils.TruncateAt.END).toString();
            layoutRealWidth = (int) Math.max(layoutRealWidth, forwardOffset + senderPaint.measureText(forwarderNameMeasured));
        }

        if (isGroup && !isOut && !isForwarded) {
            layoutHeight += getPx(19);
            senderNameMeasured = TextUtils.ellipsize(senderName, senderPaint, desiredWidth, TextUtils.TruncateAt.END).toString();
            int width = (int) senderPaint.measureText(senderNameMeasured);
            layoutRealWidth = Math.max(layoutRealWidth, width);
        }

        setBubbleMeasuredContent(layoutRealWidth, layoutHeight);
    }

    @Override
    protected boolean drawBubble(Canvas canvas) {

        boolean isAnimated = false;

        if (isForwarded) {
            canvas.drawText("Forwarded message", 0, getPx(16), forwardingPaint);
            canvas.drawText("From", 0, getPx(35), forwardingPaint);
            canvas.drawText(forwarderNameMeasured, forwardOffset, getPx(35), senderPaint);
            canvas.save();
            canvas.translate(0, getPx(19) * 2);
            layout.draw(canvas);
            canvas.restore();
        } else {
            if (!isOut & isGroup) {
                canvas.drawText(senderNameMeasured, 0, getPx(16), senderPaint);
                canvas.save();
                canvas.translate(0, getPx(19));
                layout.draw(canvas);
                canvas.restore();
            } else {
                layout.draw(canvas);
            }
        }


        if (showState) {
            if (state == MessageState.PENDING) {
                canvas.save();
                canvas.translate(layoutRealWidth - getPx(12), layoutHeight - getPx(12) - getPx(3));
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

                bounds(stateSent, layoutRealWidth - stateSent.getIntrinsicWidth() - offset,
                        layoutHeight - stateSent.getIntrinsicHeight() - getPx(3));
                stateSent.setAlpha(255);
                stateSent.draw(canvas);

                bounds(stateHalfCheck, layoutRealWidth - stateHalfCheck.getIntrinsicWidth() + getPx(5) - offset,
                        layoutHeight - stateHalfCheck.getIntrinsicHeight() - getPx(3));
                stateHalfCheck.setAlpha(alphaNew);
                stateHalfCheck.draw(canvas);

                clockOutPaint.setColor(COLOR_NORMAL);

                isAnimated = true;
            } else {
                Drawable stateDrawable = getStateDrawable(state);

                bounds(stateDrawable, layoutRealWidth - stateDrawable.getIntrinsicWidth(), layoutHeight - stateDrawable.getIntrinsicHeight() - getPx(3));
                stateDrawable.setAlpha(255);
                stateDrawable.draw(canvas);

                if (state == MessageState.READED) {
                    bounds(stateSent, layoutRealWidth - stateSent.getIntrinsicWidth() - getPx(5),
                            layoutHeight - stateDrawable.getIntrinsicHeight() - getPx(3));
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
        canvas.drawText(date, layoutRealWidth - timeWidth + getPx(6), layoutHeight - getPx(4), clockOutPaint);
        return isAnimated;
    }
}
