package org.telegram.android.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import com.extradea.framework.images.ImageReceiver;
import org.telegram.android.R;
import org.telegram.android.core.model.ChatMessage;
import org.telegram.android.core.model.LinkType;
import org.telegram.android.core.model.MessageState;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.TLLocalContact;
import org.telegram.android.core.wireframes.MessageWireframe;
import org.telegram.android.ui.FontController;
import org.telegram.android.ui.Placeholders;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 24.09.13
 * Time: 20:53
 */
public class MessageContactView extends BaseMsgView {

    private TextPaint clockOutPaint;
    private TextPaint bodyPaint;
    private TextPaint senderPaint;

    private Paint clockIconPaint;

    private OnClickListener onAddContactClick;

    private Drawable addContactResource;
    private Drawable statePending;
    private Drawable stateSent;
    private Drawable stateHalfCheck;
    private Drawable stateFailure;

    private static final int COLOR_NORMAL = 0xff70B15C;
    private static final int COLOR_ERROR = 0xffDB4942;
    private static final int COLOR_IN = 0xffA1AAB3;

    private Drawable placeholder;
    private Bitmap avatar;
    private ImageReceiver receiver;

    private String title;
    private String phone;
    private String date;
    private int state;
    private int prevState;
    private long stateChangeTime;

    private int uid;

    private int layoutWidth;
    private int timeWidth;
    private boolean showState;

    private boolean showAddButton;

    public MessageContactView(Context context) {
        super(context);
        init();
    }

    public MessageContactView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MessageContactView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {
        super.init();

        if (FontController.USE_SUBPIXEL) {
            senderPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        } else {
            senderPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        }
        senderPaint.setTypeface(FontController.loadTypeface(getContext(), "regular"));
        senderPaint.setTextSize(getSp(16));
        senderPaint.setColor(0xff000000);

        if (FontController.USE_SUBPIXEL) {
            bodyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        } else {
            bodyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        }
        bodyPaint.setTypeface(FontController.loadTypeface(getContext(), "regular"));
        bodyPaint.setTextSize(getSp(16));
        bodyPaint.setColor(0xff273e57);

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

        statePending = getResources().getDrawable(R.drawable.st_bubble_ic_clock);
        stateSent = getResources().getDrawable(R.drawable.st_bubble_ic_check);
        stateHalfCheck = getResources().getDrawable(R.drawable.st_bubble_ic_halfcheck);
        stateFailure = getResources().getDrawable(R.drawable.st_bubble_ic_warning);
        addContactResource = getResources().getDrawable(R.drawable.st_bubble_ic_contact);

        receiver = new ImageReceiver() {
            @Override
            public void onImageLoaded(Bitmap result) {
                avatar = result;
                postInvalidate();
            }

            @Override
            public void onImageLoadFailure() {
                avatar = null;
                postInvalidate();
            }

            @Override
            public void onNoImage() {
                avatar = null;
                postInvalidate();
            }
        };
        receiver.register(application.getImageController());
    }

    public OnClickListener getOnAddContactClick() {
        return onAddContactClick;
    }

    public void setOnAddContactClick(OnClickListener onAddContactClick) {
        this.onAddContactClick = onAddContactClick;
    }

    @Override
    protected void bindNewView(MessageWireframe message) {
        this.state = message.message.getState();
        this.prevState = -1;
    }

    @Override
    protected void bindUpdate(MessageWireframe message) {
        if (this.state != message.message.getState()) {
            this.prevState = this.state;
            this.state = message.message.getState();
            this.stateChangeTime = SystemClock.uptimeMillis();
        }
    }

    @Override
    protected void bindCommon(MessageWireframe message) {
        TLLocalContact contact = (TLLocalContact) message.message.getExtras();
        title = (contact.getFirstName() + " " + contact.getLastName()).trim();
        phone = org.telegram.android.ui.TextUtil.formatPhone(contact.getPhoneNumber());
        if (message.message.isOut()) {
            senderPaint.setColor(0xff739f53);
        } else {
            senderPaint.setColor(0xff4884cf);
        }
        this.date = org.telegram.android.ui.TextUtil.formatTime(message.message.getDate(), getContext());
        this.showState = message.message.isOut();

        this.placeholder = getResources().getDrawable(Placeholders.getUserPlaceholder(contact.getUserId()));
        boolean isNotContact = (message.relatedUser != null) && (message.relatedUser.getLinkType() != LinkType.CONTACT);
        this.showAddButton = isNotContact && (contact.getUserId() != application.getCurrentUid());
        requestLayout();
    }

    public boolean isShowAddButton() {
        return showAddButton;
    }

    @Override
    protected void measureBubbleContent(int width) {
        int realWidth = width - getPx(50);// Avatar
        if (showAddButton) {
            realWidth -= getPx(54);// Button
        }

        realWidth -= getPx(4);

        timeWidth = (int) clockOutPaint.measureText(date) + getPx((showState ? 23 : 0) + 6);

        title = TextUtils.ellipsize(title, senderPaint, realWidth, TextUtils.TruncateAt.END).toString();
        layoutWidth = Math.max((int) senderPaint.measureText(title, 0, title.length()), (int) bodyPaint.measureText(phone, 0, phone.length()));
        layoutWidth += getPx(44);
        layoutWidth += getPx(4);
        if (showAddButton) {
            setBubbleMeasuredContent(layoutWidth + getPx(54 + 4), getPx(56));
        } else {
            setBubbleMeasuredContent(layoutWidth + getPx(4), getPx(56));
        }
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
    protected boolean drawBubble(Canvas canvas) {

        boolean isAnimated = false;

        placeholder.setBounds(-getPx(4), -getPx(1), getPx(42 - 4), getPx(42 - 1));
        placeholder.draw(canvas);

        canvas.drawText(title, getPx(44), getPx(16), senderPaint);
        canvas.drawText(phone, getPx(44), getPx(35), bodyPaint);

        if (showState) {
            int layoutHeight = getPx(56);
            if (state == MessageState.PENDING) {
                canvas.save();
                canvas.translate(layoutWidth - getPx(12), layoutHeight - getPx(12) - getPx(3));
                canvas.drawCircle(getPx(6), getPx(6), getPx(6), clockIconPaint);
                double time = (System.currentTimeMillis() / 10.0) % (12 * 60);
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

                bounds(stateSent, layoutWidth - stateSent.getIntrinsicWidth() - offset,
                        layoutHeight - stateSent.getIntrinsicHeight() - getPx(3));
                stateSent.setAlpha(255);
                stateSent.draw(canvas);

                bounds(stateHalfCheck, layoutWidth - stateHalfCheck.getIntrinsicWidth() + getPx(5) - offset,
                        layoutHeight - stateHalfCheck.getIntrinsicHeight() - getPx(3));
                stateHalfCheck.setAlpha(alphaNew);
                stateHalfCheck.draw(canvas);

                clockOutPaint.setColor(COLOR_NORMAL);

                isAnimated = true;
            } else {
                Drawable stateDrawable = getStateDrawable(state);

                bounds(stateDrawable, layoutWidth - stateDrawable.getIntrinsicWidth(), layoutHeight - stateDrawable.getIntrinsicHeight() - getPx(3));
                stateDrawable.setAlpha(255);
                stateDrawable.draw(canvas);

                if (state == MessageState.READED) {
                    bounds(stateSent, layoutWidth - stateSent.getIntrinsicWidth() - getPx(5),
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

        canvas.drawText(date, layoutWidth - timeWidth + getPx(6), getPx(52), clockOutPaint);

        if (showAddButton) {
            Paint p = new Paint();
            p.setStrokeWidth(getPx(1));
            p.setColor(0xffececec);
            canvas.drawLine(layoutWidth + getPx(10), getPx(2), layoutWidth + getPx(10), getPx(52), p);
            addContactResource.setBounds(layoutWidth + getPx(20), getPx(12),
                    layoutWidth + getPx(20) + addContactResource.getIntrinsicWidth(),
                    getPx(12) + addContactResource.getIntrinsicHeight());
            addContactResource.draw(canvas);
        }

        return isAnimated;
    }

    public void onAddClicked() {
        if (onAddContactClick != null) {
            onAddContactClick.onClick(this);
        }
    }
}
