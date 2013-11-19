package org.telegram.android.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.*;
import android.widget.Checkable;
import com.extradea.framework.images.ImageReceiver;
import org.telegram.android.R;
import org.telegram.android.core.model.ChatMessage;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.ui.FontController;
import org.telegram.android.ui.Placeholders;
import org.telegram.i18n.I18nUtil;

/**
 * Author: Korshakov Stepan
 * Created: 18.08.13 19:36
 */
public abstract class BaseMsgView extends BaseView implements Checkable {
    protected static final long FADE_ANIMATION_TIME = 160;
    protected static final long STATE_ANIMATION_TIME = FADE_ANIMATION_TIME;

    private static final int TOUCHED_NONE = 0;
    private static final int TOUCHED_OUTSIDE = 4;
    private static final int TOUCHED_AVATAR = 1;
    private static final int TOUCHED_BUBBLE = 2;
    private static final int TOUCHED_ADD_CONTACT = 3;

    private int BUBBLE_PADDING;
    private int AVATAR_OFFSET;
    private int AVATAR_SIZE;
    private int AVATAR_LEFT;
    private int AVATAR_BOTTOM;
    private int UNREAD_HEIGHT;
    private int UNREAD_OFFSET;

    private TextPaint timeDivPaint;
    private TextPaint newDivPaint;
    private Paint avatarPaint;
    private Drawable placeholder;
    private Bitmap avatar;
    private ImageReceiver receiver;
    private long avatarImageTime;
    private boolean placeholderDrawn;

    private Paint selectorPaint;
    private Paint newMessagesPaint;

    private boolean showAvatar;
    private boolean isOut;
    private String timeDivText;
    private int timeDivMeasure;

    private String newDivText;
    private int newDivMeasure;

    private int bubbleContentWidth;
    private int bubbleContentHeight;

    private Drawable bubbleInDrawable;
    private Drawable bubbleInDrawableOverlay;
    private Rect bubbleInPadding;
    private Drawable bubbleOutDrawable;
    private Drawable bubbleOutDrawableOverlay;
    private Rect bubbleOutPadding;

    private Drawable currentBubbleDrawable;
    private Rect currentBubblePadding;

    private Drawable serviceDrawable;
    private Rect servicePadding;

    private Rect avatarRect;

    private int oldId = -1;

    private int touchedElement = TOUCHED_NONE;

    private boolean showTimeSeparator;

    private boolean showUnreadMessagesNotify;
    private int unreadMessagesCount;

    private ChatMessage message;

    private OnClickListener onBubbleClick;
    private OnClickListener onAvatarClick;

    private boolean isBubblePressed = false;
    private boolean isBubbleChecked = false;

    private Rect rect = new Rect();

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable longClickRunnable = new Runnable() {
        @Override
        public void run() {
            if (performLongClick()) {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                touchedElement = TOUCHED_NONE;
            }
        }
    };

    public BaseMsgView(Context context) {
        super(context);
        init();
    }

    public BaseMsgView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BaseMsgView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {
        AVATAR_OFFSET = getPx(39);
        AVATAR_SIZE = getPx(42);
        AVATAR_LEFT = getPx(6);
        AVATAR_BOTTOM = getPx(4);
        BUBBLE_PADDING = getPx(39);
        UNREAD_HEIGHT = getPx(24);
        UNREAD_OFFSET = getPx(18);

        timeDivPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        timeDivPaint.setTextSize(getSp(15));
        timeDivPaint.setColor(0xffFFFFFF);
        timeDivPaint.setTypeface(FontController.loadTypeface(getContext(), "regular"));

        newDivPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        newDivPaint.setTextSize(getSp(16));
        newDivPaint.setColor(0xffFFFFFF);
        newDivPaint.setTypeface(FontController.loadTypeface(getContext(), "regular"));

        avatarPaint = new Paint();
       /* avatarPaint.setAntiAlias(true);
        avatarPaint.setFilterBitmap(true);
        avatarPaint.setDither(true);*/
        receiver = new ImageReceiver() {
            @Override
            public void onImageLoaded(Bitmap result) {
                avatar = result;
                avatarImageTime = SystemClock.uptimeMillis();
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

        bubbleInPadding = new Rect();
        bubbleOutPadding = new Rect();

        bubbleInDrawable = getResources().getDrawable(getInBubbleResource());
        bubbleOutDrawable = getResources().getDrawable(getOutBubbleResource());
        int resource = getOutPressedBubbleResource();
        if (resource != 0) {
            bubbleOutDrawableOverlay = getResources().getDrawable(resource);
        } else {
            bubbleOutDrawableOverlay = null;
        }

        resource = getInPressedBubbleResource();
        if (resource != 0) {
            bubbleInDrawableOverlay = getResources().getDrawable(resource);
        } else {
            bubbleInDrawableOverlay = null;
        }

        bubbleInDrawable.getPadding(bubbleInPadding);
        bubbleOutDrawable.getPadding(bubbleOutPadding);

        serviceDrawable = getResources().getDrawable(org.telegram.android.R.drawable.st_bubble_service);
        servicePadding = new Rect();
        serviceDrawable.getPadding(servicePadding);

        avatarRect = new Rect();

        selectorPaint = new Paint();
        selectorPaint.setColor(0x6633b5e5);

        newMessagesPaint = new Paint();
        newMessagesPaint.setColor(0x66435266);
    }

    protected float fadeEasing(float src) {
        return 1 - (1 - src) * (1 - src);
    }

    protected float easeStateFade(float src) {
        return 1 - (1 - src) * (1 - src);
    }

    public final void rebind() {
        bind(message, showTimeSeparator, showUnreadMessagesNotify, unreadMessagesCount);
    }

    public final void bind(ChatMessage message, boolean showTime, boolean showUnread, int unreadMessagesCount) {
        this.message = message;
        if (showTimeSeparator != showTime) {
            requestLayout();
        }
        if (showUnreadMessagesNotify != showUnread) {
            requestLayout();
        }

        this.showUnreadMessagesNotify = showUnread;
        this.unreadMessagesCount = unreadMessagesCount;

        if (showUnreadMessagesNotify) {
            newDivText = I18nUtil.getInstance().getPluralFormatted(R.plurals.st_new_messages, unreadMessagesCount);
        }

        showTimeSeparator = showTime;
        if (showTimeSeparator) {
            timeDivText = org.telegram.android.ui.TextUtil.formatDateLong(message.getDate());
        }
        bindCommonInt(message);
        bindCommon(message);
        if (oldId != -1 && message.getDatabaseId() == oldId) {
            bindUpdateInt(message);
            bindUpdate(message);
        } else {
            bindNewInt(message);
            bindNewView(message);
        }
        oldId = message.getDatabaseId();
    }

    private void bindCommonInt(ChatMessage message) {
        showAvatar = !message.isOut() && message.getPeerType() == PeerType.PEER_CHAT;
        isOut = message.isOut();

        if (isOut) {
            currentBubbleDrawable = bubbleOutDrawable;
            currentBubblePadding = bubbleOutPadding;
        } else {
            currentBubbleDrawable = bubbleInDrawable;
            currentBubblePadding = bubbleInPadding;
        }

        if (showAvatar) {
            placeholder = getResources().getDrawable(Placeholders.getUserBubblePlaceholder(message.getSenderId()));
            User user = application.getEngine().getUser(message.getSenderId());
            if (user != null) {
                if (user.getPhoto() instanceof TLLocalAvatarPhoto) {
                    TLLocalAvatarPhoto profilePhoto = (TLLocalAvatarPhoto) user.getPhoto();
                    if (profilePhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                        StelsImageTask task = new StelsImageTask((TLLocalFileLocation) profilePhoto.getPreviewLocation());
                        task.setMaxWidth(getPx(42));
                        task.setMaxHeight(getPx(42));
                        task.setFillRect(true);
                        receiver.receiveImage(task);
                        avatar = receiver.getResult();
                        if (avatar != null) {
                            avatarImageTime = 0;
                        }
                    } else {
                        receiver.receiveImage(null);
                    }
                } else {
                    receiver.receiveImage(null);
                }
            } else {
                receiver.receiveImage(null);
            }
        } else {
            receiver.receiveImage(null);
        }
    }

    private void bindNewInt(ChatMessage message) {

    }

    private void bindUpdateInt(ChatMessage message) {

    }

    protected void bindNewView(ChatMessage message) {

    }

    protected void bindUpdate(ChatMessage message) {

    }

    protected void bindCommon(ChatMessage message) {

    }

    public OnClickListener getOnAvatarClickListener() {
        return onAvatarClick;
    }

    public void setOnAvatarClickListener(OnClickListener onAvatarClick) {
        this.onAvatarClick = onAvatarClick;
    }

    public OnClickListener getOnBubbleClickListener() {
        return onBubbleClick;
    }

    public void setOnBubbleClickListener(OnClickListener onBubbleClick) {
        this.onBubbleClick = onBubbleClick;
    }

    public int getContentX() {
        if (isOut) {
            return getWidth() - currentBubblePadding.right - bubbleContentWidth;
        } else {
            if (showAvatar) {
                return AVATAR_OFFSET + currentBubblePadding.left;
            } else {
                return currentBubblePadding.left;
            }
        }
    }

    public int getContentY() {
        int offset = 0;
        if (showTimeSeparator) {
            offset += getPx(44);
        }
        offset += currentBubblePadding.top;
        return offset;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int bubbleMaxSize = width - BUBBLE_PADDING - currentBubblePadding.left - currentBubblePadding.right;
        if (showAvatar) {
            bubbleMaxSize -= AVATAR_OFFSET;
        }

        measureBubbleContent(bubbleMaxSize);

        int height = bubbleContentHeight + currentBubblePadding.bottom + currentBubblePadding.top;
        if (height < AVATAR_BOTTOM + AVATAR_SIZE && showAvatar) {
            height = AVATAR_BOTTOM + AVATAR_SIZE;
        }

        int realHeight = height;
        if (showTimeSeparator) {
            timeDivMeasure = (int) timeDivPaint.measureText(timeDivText);
            realHeight += getPx(44);
        }

        if (showUnreadMessagesNotify) {
            newDivMeasure = (int) newDivPaint.measureText(newDivText);
            realHeight += UNREAD_HEIGHT;
        }

        setMeasuredDimension(width, realHeight);

        if (showAvatar) {
            avatarRect.set(AVATAR_LEFT, height - AVATAR_BOTTOM - AVATAR_SIZE, AVATAR_LEFT + AVATAR_SIZE, height - AVATAR_BOTTOM);
        }
    }

    protected void setBubbleMeasuredContent(int w, int h) {
        bubbleContentWidth = w;
        bubbleContentHeight = h;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int bubbleStart, bubbleEnd;
            if (isOut) {
                bubbleEnd = getWidth();
                bubbleStart = 0;
                // bubbleStart = getWidth() - (bubbleContentWidth + currentBubblePadding.left + currentBubblePadding.right);
            } else {
                if (showAvatar) {
                    bubbleStart = AVATAR_OFFSET;
                    // bubbleEnd = AVATAR_OFFSET + bubbleContentWidth + currentBubblePadding.left + currentBubblePadding.right;
                    bubbleEnd = getWidth();
                } else {
                    bubbleStart = 0;
                    // bubbleEnd = bubbleContentWidth + currentBubblePadding.left + currentBubblePadding.right;
                    bubbleEnd = getWidth();
                }
            }

            boolean processed = false;
            if (this instanceof MessageContactView) {
                MessageContactView contactView = (MessageContactView) this;
                if (contactView.isShowAddButton()) {
                    if (event.getX() >= bubbleEnd - getPx(56) && event.getX() <= bubbleEnd) {
                        touchedElement = TOUCHED_ADD_CONTACT;
                        processed = true;
                    }
                }
            }

            if (!processed) {
                if (event.getX() >= bubbleStart && event.getX() <= bubbleEnd) {
                    touchedElement = TOUCHED_BUBBLE;
                } else if (event.getY() > getHeight() - AVATAR_BOTTOM - AVATAR_SIZE && event.getX() < AVATAR_OFFSET && showAvatar) {
                    touchedElement = TOUCHED_AVATAR;
                } else {
                    touchedElement = TOUCHED_NONE;
                }
            }

            if (touchedElement != TOUCHED_NONE) {
                handler.removeCallbacks(longClickRunnable);
                handler.postDelayed(longClickRunnable, ViewConfiguration.getLongPressTimeout());
            }
            invalidate();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (touchedElement != TOUCHED_NONE) {
                handler.removeCallbacks(longClickRunnable);
            }
            if (touchedElement == TOUCHED_BUBBLE) {
                if (onBubbleClick != null) {
                    playSoundEffect(SoundEffectConstants.CLICK);
                    onBubbleClick.onClick(this);
                }
            } else if (touchedElement == TOUCHED_AVATAR) {
                if (onAvatarClick != null) {
                    playSoundEffect(SoundEffectConstants.CLICK);
                    onAvatarClick.onClick(this);
                }
            } else if (touchedElement == TOUCHED_ADD_CONTACT) {
                MessageContactView contactView = (MessageContactView) this;
                contactView.onAddClicked();
            }
            touchedElement = TOUCHED_NONE;
            invalidate();
        } else if (event.getAction() == MotionEvent.ACTION_OUTSIDE || event.getAction() == MotionEvent.ACTION_CANCEL) {
            handler.removeCallbacks(longClickRunnable);
            touchedElement = TOUCHED_NONE;
            invalidate();
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        receiver.onRemovedFromParent();
        avatar = null;
        postInvalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        receiver.onAddedToParent();
        avatar = receiver.getResult();
        postInvalidate();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        bubbleOutDrawable.setState(getDrawableState());
        bubbleInDrawable.setState(getDrawableState());
        invalidate();
    }

    private void applyDrawingState() {
        if (isChecked()) {
            int[] drawableState = new int[]{android.R.attr.state_checked};
            bubbleOutDrawable.setState(drawableState);
            bubbleInDrawable.setState(drawableState);
            if (touchedElement == TOUCHED_BUBBLE) {
                isBubblePressed = true;
            } else {
                isBubblePressed = false;
            }
        } else if (touchedElement == TOUCHED_BUBBLE) {
            int[] drawableState = new int[]{android.R.attr.state_pressed};
            bubbleOutDrawable.setState(drawableState);
            bubbleInDrawable.setState(drawableState);
            isBubblePressed = true;
        } else {
            int[] drawableState = new int[0];
            bubbleOutDrawable.setState(drawableState);
            bubbleInDrawable.setState(drawableState);
            isBubblePressed = false;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        applyDrawingState();

        boolean isAnimating = false;
        canvas.save();

        if (isBubbleChecked) {
            int topOffset = 1;

            if (showTimeSeparator) {
                topOffset += getPx(44);
            }
            if (showUnreadMessagesNotify) {
                topOffset += UNREAD_HEIGHT;
            }
            rect.set(0, topOffset, getWidth(), getHeight());
            canvas.drawRect(rect, selectorPaint);
        }

        if (showUnreadMessagesNotify) {
            rect.set(0, 0, getWidth(), UNREAD_HEIGHT);
            canvas.drawRect(rect, newMessagesPaint);
            canvas.drawText(newDivText, (getWidth() - newDivMeasure) / 2, UNREAD_OFFSET, newDivPaint);
            canvas.translate(0, UNREAD_HEIGHT);
        }

        if (showTimeSeparator) {
            serviceDrawable.setBounds(
                    getWidth() / 2 - timeDivMeasure / 2 - servicePadding.left,
                    getPx(44 - 8) - serviceDrawable.getIntrinsicHeight(),
                    getWidth() / 2 + timeDivMeasure / 2 + servicePadding.right,
                    getPx(44 - 8));
            serviceDrawable.draw(canvas);
            canvas.drawText(timeDivText, getWidth() / 2 - timeDivMeasure / 2, getPx(44 - 17), timeDivPaint);
            canvas.translate(0, getPx(44));
        }
        if (isOut) {
            canvas.translate(getWidth() - bubbleContentWidth - currentBubblePadding.left - currentBubblePadding.right, 0);
            currentBubbleDrawable.setBounds(0, 0,
                    bubbleContentWidth + currentBubblePadding.left + currentBubblePadding.right,
                    bubbleContentHeight + currentBubblePadding.top + currentBubblePadding.bottom);
            currentBubbleDrawable.draw(canvas);
            canvas.save();
            canvas.translate(currentBubblePadding.left, currentBubblePadding.top);
            isAnimating = isAnimating | drawBubble(canvas);
            canvas.restore();
            if (isBubblePressed) {
                if (bubbleOutDrawableOverlay != null) {
                    bubbleOutDrawableOverlay.setBounds(0, 0,
                            bubbleContentWidth + currentBubblePadding.left + currentBubblePadding.right,
                            bubbleContentHeight + currentBubblePadding.top + currentBubblePadding.bottom);
                    bubbleOutDrawableOverlay.draw(canvas);
                }
            }
        } else {
            if (showAvatar) {
                if (avatar != null) {
                    long animationTime = SystemClock.uptimeMillis() - avatarImageTime;
                    if (animationTime > FADE_ANIMATION_TIME) {
                        avatarPaint.setAlpha(255);
                        canvas.drawBitmap(avatar, avatarRect.left, avatarRect.top, avatarPaint);
                        // canvas.drawBitmap(avatar, new Rect(0, 0, avatar.getWidth(), avatar.getHeight()), avatarRect, avatarPaint);
                    } else {
                        float animationPercent = fadeEasing((float) animationTime / FADE_ANIMATION_TIME);
                        int placeholderAlpha = (int) ((1 - animationPercent) * 255);
                        int avatarAlpha = (int) (animationPercent * 255);
                        avatarPaint.setAlpha(avatarAlpha);
                        placeholder.setAlpha(placeholderAlpha);

                        placeholder.setBounds(avatarRect);
                        placeholder.draw(canvas);

                        canvas.drawBitmap(avatar, avatarRect.left, avatarRect.top, avatarPaint);
                        // canvas.drawBitmap(avatar, new Rect(0, 0, avatar.getWidth(), avatar.getHeight()), avatarRect, avatarPaint);

                        isAnimating = true;
                    }
                } else {
                    placeholder.setBounds(avatarRect);
                    placeholder.setAlpha(255);
                    placeholder.draw(canvas);
                }
                canvas.translate(AVATAR_OFFSET, 0);
            }
            currentBubbleDrawable.setBounds(0, 0,
                    bubbleContentWidth + currentBubblePadding.left + currentBubblePadding.right,
                    bubbleContentHeight + currentBubblePadding.top + currentBubblePadding.bottom);
            currentBubbleDrawable.draw(canvas);
            canvas.save();
            canvas.translate(currentBubblePadding.left, currentBubblePadding.top);
            isAnimating = isAnimating | drawBubble(canvas);
            canvas.restore();
            if (isBubblePressed) {
                if (bubbleInDrawableOverlay != null) {
                    bubbleInDrawableOverlay.setBounds(0, 0,
                            bubbleContentWidth + currentBubblePadding.left + currentBubblePadding.right,
                            bubbleContentHeight + currentBubblePadding.top + currentBubblePadding.bottom);
                    bubbleInDrawableOverlay.draw(canvas);
                }
            }
        }
        canvas.restore();

        if (isAnimating) {
            if (Build.VERSION.SDK_INT >= 16) {
                postInvalidateOnAnimation();
            } else {
                invalidate();
            }
        }
    }

    protected abstract void measureBubbleContent(int width);

    protected abstract int getInBubbleResource();

    protected abstract int getOutBubbleResource();

    protected int getOutPressedBubbleResource() {
        return 0;
    }

    protected int getInPressedBubbleResource() {
        return 0;
    }

    /**
     * Drawing buuble content
     *
     * @return isAnimating?
     */
    protected abstract boolean drawBubble(Canvas canvas);

    @Override
    public void setChecked(boolean checked) {
        isBubbleChecked = checked;
        invalidate();
    }

    @Override
    public boolean isChecked() {
        return isBubbleChecked;
    }

    @Override
    public void toggle() {
        isBubbleChecked = !isBubbleChecked;
        invalidate();
    }
}