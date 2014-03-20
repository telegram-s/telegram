package org.telegram.android.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import org.telegram.android.R;
import org.telegram.android.core.model.LinkType;
import org.telegram.android.core.model.MessageState;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalContact;
import org.telegram.android.core.wireframes.MessageWireframe;
import org.telegram.android.preview.AvatarLoader;
import org.telegram.android.preview.ImageHolder;
import org.telegram.android.preview.ImageReceiver;
import org.telegram.android.ui.FontController;
import org.telegram.android.ui.Placeholders;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 24.09.13
 * Time: 20:53
 */
public class MessageContactView extends BaseMsgStateView {

    private TextPaint clockOutPaint;
    private TextPaint bodyPaint;
    private TextPaint senderPaint;

    private Paint clockIconPaint;

    private Paint placeHolderBgPaint;

    private Paint avatarPaint;

    private OnClickListener onAddContactClick;

    private Drawable addContactResource;

    private Rect rect = new Rect();

    private Drawable basePlaceholder;

    private String title;
    private String phone;
    private String date;

    private int uid;

    private long avatarAppearTime;
    private ImageHolder contactAvatar;
    private ImageReceiver receiver = new ImageReceiver() {
        @Override
        public void onImageReceived(ImageHolder mediaHolder, boolean intermediate) {
            unbindAvatar();
            contactAvatar = mediaHolder;
            if (intermediate) {
                avatarAppearTime = 0;
            } else {
                avatarAppearTime = SystemClock.uptimeMillis();
            }
            invalidate();
        }
    };

    private int layoutWidth;
    private int timeWidth;
    private boolean showState;

    private boolean showAddButton;
    private AvatarLoader loader;

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

        addContactResource = getResources().getDrawable(R.drawable.st_bubble_ic_contact);

        placeHolderBgPaint = new Paint();

        avatarPaint = new Paint();

        loader = application.getUiKernel().getAvatarLoader();
    }

    public OnClickListener getOnAddContactClick() {
        return onAddContactClick;
    }

    public void setOnAddContactClick(OnClickListener onAddContactClick) {
        this.onAddContactClick = onAddContactClick;
    }

    private void unbindAvatar() {
        if (contactAvatar != null) {
            contactAvatar.release();
            contactAvatar = null;
        }
    }

    @Override
    protected void bindNewView(MessageWireframe message) {
        bindStateNew(message);

        unbindAvatar();
        if (message.relatedUser != null) {
            if (message.relatedUser.getPhoto() instanceof TLLocalAvatarPhoto) {
                TLLocalAvatarPhoto avatarPhoto = (TLLocalAvatarPhoto) message.relatedUser.getPhoto();
                loader.requestAvatar(avatarPhoto.getPreviewLocation(), AvatarLoader.TYPE_SMALL, receiver);
            } else {
                loader.cancelRequest(receiver);
            }
        } else {
            loader.cancelRequest(receiver);
        }
    }

    @Override
    protected void bindUpdate(MessageWireframe message) {
        bindStateUpdate(message);
    }

    @Override
    protected void bindCommon(MessageWireframe message) {
        TLLocalContact contact = (TLLocalContact) message.message.getExtras();
        if (contact.getLastName().trim().length() == 0) {
            title = contact.getFirstName().trim();
        } else {
            title = contact.getFirstName().trim() + " " + contact.getLastName().trim();
        }
        phone = contact.getPhoneNumber();
        if (message.message.isOut()) {
            senderPaint.setColor(0xff739f53);
        } else {
            senderPaint.setColor(0xff4884cf);
        }
        this.date = org.telegram.android.ui.TextUtil.formatTime(message.message.getDate(), getContext());
        this.showState = message.message.isOut();

        this.basePlaceholder = getResources().getDrawable(R.drawable.st_user_placeholder_chat);
        if (contact.getUserId() > 0) {
            placeHolderBgPaint.setColor(Placeholders.getBgColor(contact.getUserId()));
        } else {
            placeHolderBgPaint.setColor(Placeholders.GREY);
        }
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
        layoutWidth += getPx(42);
        layoutWidth += getPx(4);
        if (showAddButton) {
            setBubbleMeasuredContent(layoutWidth + getPx(56 + 16), getPx(60));
        } else {
            setBubbleMeasuredContent(layoutWidth + getPx(16), getPx(60));
        }
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
    protected int getOutPressedBubbleResource() {
        return R.drawable.st_bubble_out_media_overlay;
    }

    @Override
    protected int getInPressedBubbleResource() {
        return R.drawable.st_bubble_in_media_overlay;
    }

    @Override
    protected boolean drawBubble(Canvas canvas) {
        boolean isAnimated = false;

        rect.set(getPx(6), getPx(6), getPx(42 + 6), getPx(42 + 6));
        if (contactAvatar != null) {
            long animationTime = SystemClock.uptimeMillis() - avatarAppearTime;
            if (animationTime < FADE_ANIMATION_TIME) {
                float alpha = fadeEasing(animationTime / (float) FADE_ANIMATION_TIME);

                canvas.drawRect(rect, placeHolderBgPaint);
                basePlaceholder.setBounds(rect);
                basePlaceholder.draw(canvas);

                avatarPaint.setAlpha((int) (alpha * 255));
                canvas.drawBitmap(contactAvatar.getBitmap(), new Rect(0, 0, getPx(42), getPx(42)), rect, avatarPaint);
                isAnimated = true;
            } else {
                avatarPaint.setAlpha(255);
                canvas.drawBitmap(contactAvatar.getBitmap(), new Rect(0, 0, getPx(42), getPx(42)), rect, avatarPaint);
            }
        } else {
            canvas.drawRect(rect, placeHolderBgPaint);
            basePlaceholder.setBounds(rect);
            basePlaceholder.draw(canvas);
        }

        canvas.drawText(title, getPx(56), getPx(20), senderPaint);
        canvas.drawText(phone, getPx(56), getPx(38), bodyPaint);

        isAnimated |= drawState(canvas, px(8), px(4));

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
