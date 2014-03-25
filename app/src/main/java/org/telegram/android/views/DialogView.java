package org.telegram.android.views;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.support.v4.text.BidiFormatter;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import org.telegram.android.R;
import org.telegram.android.TelegramApplication;
import org.telegram.android.config.UserSettings;
import org.telegram.android.core.TypingStates;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.media.TLAbsLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.wireframes.DialogWireframe;
import org.telegram.android.preview.AvatarLoader;
import org.telegram.android.preview.ImageHolder;
import org.telegram.android.preview.ImageReceiver;
import org.telegram.android.ui.*;
import org.telegram.i18n.I18nUtil;

/**
 * Author: Korshakov Stepan
 * Created: 06.08.13 18:40
 */
public class DialogView extends BaseView implements TypingStates.TypingListener, ImageReceiver {

    private static boolean IS_LARGE = false;

    public static void resetSettings() {
        isLoaded = false;
    }

    // private static final String TAG = "DialogView";

    private static final long AVATAR_FADE_TIME = 150;
    private static final boolean AVATAR_FADE = false;
    private static final boolean HIGHLIGHT_UNDEAD = false;

    // Resources
    private static int HIGHLIGHT_COLOR = 0xff3076a4;
    private static int UNREAD_COLOR = 0xff222222;
    private static int READ_COLOR = 0xff969696;

    private static int READ_TIME_COLOR = 0xff969696;
    //private static int UNREAD_TIME_COLOR = 0xff6597c4;
    private static int UNREAD_TIME_COLOR = 0xff222222;

    private static boolean isLoaded = false;
    private static Paint avatarPaint;
    private static Paint counterPaint;
    private static Paint placeholderPaint;
    private static TextPaint placeholderTextPaint;
    private static TextPaint titlePaint;
    private static TextPaint titleHighlightPaint;
    private static TextPaint titleEncryptedPaint;

    private static TextPaint bodyPaint;
    private static TextPaint bodyHighlightPaint;
    private static TextPaint bodyUnreadPaint;
    private static TextPaint typingPaint;

    private static TextPaint readClockPaint;
    private static TextPaint unreadClockPaint;

    private static TextPaint counterTitlePaint;

    private static Bitmap userPlaceHolder;
    private static Bitmap groupPlaceHolder;

    private Drawable statePending;
    private Drawable stateSent;
    private Drawable stateHalfCheck;
    private Drawable stateFailure;
    private Drawable secureIcon;

    // Help data
    private int currentUserUid;

    private TelegramApplication application;
    private AvatarLoader loader;

    // private ImageReceiver avatarReceiver;

    // Data

    private DialogWireframe description;
    private DialogLayout[] preparedLayouts;

    // PreparedData
    private TLAbsLocalAvatarPhoto photo;

    private int state;

    // private Drawable emptyDrawable;
    // private Bitmap empty;

    private Bitmap placeholder;
    private ImageHolder avatarHolder;

    private long avatarAppearTime;
    private Paint avatarBgPaint;

    // Typing

    private boolean needNewUpdateTyping;

    private Layout typingLayout;

    private int[] typingUids;
    private boolean userTypes;

    private Rect rect = new Rect();
    private Rect rect2 = new Rect();

    // Layouting
    private DialogLayout layout;

    protected static int px(float dp) {
        return (int) (dp * UiMeasure.DENSITY + 0.5f);
    }

    protected static int sp(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, UiMeasure.METRICS);
    }

    private static void checkResources(Context context) {
        if (!isLoaded) {
            TelegramApplication application = (TelegramApplication) context.getApplicationContext();
            IS_LARGE = application.getUserSettings().getDialogItemSize() == UserSettings.DIALOG_SIZE_LARGE;

            avatarPaint = new Paint();

            if (FontController.USE_SUBPIXEL) {
                titlePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                titlePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }

            titlePaint.setColor(0xff222222);
            if (IS_LARGE) {
                titlePaint.setTextSize(sp(20f));
                titlePaint.setTypeface(FontController.loadTypeface(context, "medium"));
            } else {
                titlePaint.setTextSize(sp(18f));
                titlePaint.setTypeface(FontController.loadTypeface(context, "medium"));
            }

            if (FontController.USE_SUBPIXEL) {
                titleHighlightPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                titleHighlightPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            titleHighlightPaint.setColor(0xff006FC8);
            if (IS_LARGE) {
                titleHighlightPaint.setTextSize(sp(20f));
                titleHighlightPaint.setTypeface(FontController.loadTypeface(context, "medium"));
            } else {
                titleHighlightPaint.setTextSize(sp(18f));
                titleHighlightPaint.setTypeface(FontController.loadTypeface(context, "medium"));
            }

            if (FontController.USE_SUBPIXEL) {
                titleEncryptedPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                titleEncryptedPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            titleEncryptedPaint.setColor(0xff368c3e);
            if (IS_LARGE) {
                titleEncryptedPaint.setTextSize(sp(20f));
                titleEncryptedPaint.setTypeface(FontController.loadTypeface(context, "medium"));
            } else {
                titleEncryptedPaint.setTextSize(sp(18f));
                titleEncryptedPaint.setTypeface(FontController.loadTypeface(context, "medium"));
            }

            if (FontController.USE_SUBPIXEL) {
                unreadClockPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                unreadClockPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            unreadClockPaint.setColor(UNREAD_TIME_COLOR);
            unreadClockPaint.setTextSize(sp(14));
            unreadClockPaint.setTypeface(FontController.loadTypeface(context, "regular"));

            if (FontController.USE_SUBPIXEL) {
                readClockPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                readClockPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            readClockPaint.setColor(READ_TIME_COLOR);
            readClockPaint.setTextSize(sp(14));
            readClockPaint.setTypeface(FontController.loadTypeface(context, "regular"));

            if (FontController.USE_SUBPIXEL) {
                bodyPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                bodyPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            bodyPaint.setColor(READ_COLOR);
            bodyPaint.setTextSize(sp(17f));
            bodyPaint.setTypeface(FontController.loadTypeface(context, "regular"));

            if (FontController.USE_SUBPIXEL) {
                bodyUnreadPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                bodyUnreadPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            bodyUnreadPaint.setColor(UNREAD_COLOR);
            bodyUnreadPaint.setTextSize(sp(17f));
            bodyUnreadPaint.setTypeface(FontController.loadTypeface(context, "regular"));

            if (FontController.USE_SUBPIXEL) {
                bodyHighlightPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                bodyHighlightPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            bodyHighlightPaint.setColor(HIGHLIGHT_COLOR);
            bodyHighlightPaint.setTextSize(sp(17f));
            bodyHighlightPaint.setTypeface(FontController.loadTypeface(context, "regular"));

            if (FontController.USE_SUBPIXEL) {
                typingPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                typingPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            typingPaint.setColor(0xff006FC8);
            typingPaint.setTextSize(sp(16f));
            typingPaint.setTypeface(FontController.loadTypeface(context, "regular"));

            if (FontController.USE_SUBPIXEL) {
                counterTitlePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                counterTitlePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            counterTitlePaint.setColor(0xffffffff);
            if (IS_LARGE) {
                counterTitlePaint.setTextSize(sp(15.5f));
            } else {
                counterTitlePaint.setTextSize(sp(14f));
            }
            counterTitlePaint.setTypeface(FontController.loadTypeface(context, "regular"));

            counterPaint = new Paint();
            counterPaint.setColor(0xff6ebb4d);
            counterPaint.setAntiAlias(true);

            placeholderPaint = new Paint();
            placeholderPaint.setAntiAlias(true);

            if (FontController.USE_SUBPIXEL) {
                placeholderTextPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                placeholderTextPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            placeholderTextPaint.setColor(0xffffffff);
            placeholderTextPaint.setTextSize(px(28f));
            placeholderTextPaint.setTypeface(FontController.loadTypeface(context, "regular"));
            placeholderTextPaint.setTextAlign(Paint.Align.CENTER);

            isLoaded = true;
        }
    }

    public DialogView(Context context) {
        super(context);
        checkResources(context);

        this.application = (TelegramApplication) context.getApplicationContext();

        this.loader = application.getUiKernel().getAvatarLoader();

        this.currentUserUid = application.getCurrentUid();

        application.getTypingStates().registerListener(this);

        avatarBgPaint = new Paint();
        avatarBgPaint.setStyle(Paint.Style.FILL);

        statePending = getResources().getDrawable(R.drawable.st_dialogs_clock);
        stateSent = getResources().getDrawable(R.drawable.st_dialogs_check);
        stateHalfCheck = getResources().getDrawable(R.drawable.st_dialogs_halfcheck);
        stateFailure = getResources().getDrawable(R.drawable.st_dialogs_warning);
        secureIcon = getResources().getDrawable(R.drawable.st_dialogs_lock);

        userPlaceHolder = ((BitmapDrawable) getResources().getDrawable(R.drawable.st_user_placeholder_dialog)).getBitmap();
        groupPlaceHolder = ((BitmapDrawable) getResources().getDrawable(R.drawable.st_group_placeholder)).getBitmap();
    }

    public void setDescription(DialogWireframe description, Object preparedLayouts) {
        this.preparedLayouts = (DialogLayout[]) preparedLayouts;
        this.description = description;
        this.state = description.getMessageState();

        if (description.getPeerType() == PeerType.PEER_CHAT) {
            this.typingUids = application.getTypingStates().getChatTypes(description.getPeerId());
        } else {
            this.userTypes = application.getTypingStates().isUserTyping(description.getPeerId());
        }

        photo = description.getDialogAvatar();

        if (description.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
            EncryptedChat encryptedChat = application.getEngine().getEncryptedChat(description.getPeerId());
            avatarBgPaint.setColor(Placeholders.getBgColor(encryptedChat.getUserId()));
            placeholder = userPlaceHolder;
        } else if (description.getPeerType() == PeerType.PEER_USER) {
            avatarBgPaint.setColor(Placeholders.getBgColor(description.getPeerId()));
            placeholder = userPlaceHolder;
        } else if (description.getPeerType() == PeerType.PEER_CHAT) {
            avatarBgPaint.setColor(Placeholders.getBgColor(description.getPeerId()));
            placeholder = groupPlaceHolder;
        } else {
            throw new UnsupportedOperationException("Unknown peer type #" + description.getPeerType());
        }

        if (photo instanceof TLLocalAvatarPhoto) {
            if (!loader.requestAvatar(
                    ((TLLocalAvatarPhoto) photo).getPreviewLocation(),
                    IS_LARGE ? AvatarLoader.TYPE_MEDIUM2 : AvatarLoader.TYPE_MEDIUM,
                    this)) {
                releaseAvatar();
            }
        } else {
            releaseAvatar();
            loader.cancelRequest(this);
        }

        if (getMeasuredHeight() != 0 || getMeasuredWidth() != 0) {
            buildLayout();
            invalidate();
        } else {
            requestLayout();
        }

        needNewUpdateTyping = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        releaseAvatar();
        loader.cancelRequest(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        postInvalidate();
    }

    public void unbind() {
        releaseAvatar();
        loader.cancelRequest(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        long start = System.currentTimeMillis();
        if (IS_LARGE) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), getPx(80));
        } else {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), getPx(68));
        }
        // Logger.d(TAG, "onMeasure in " + (System.currentTimeMillis() - start) + " ms");
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        long start = System.currentTimeMillis();
        if (changed) {
            buildLayout();
        }
        // Logger.d(TAG, "onLayout in " + (System.currentTimeMillis() - start) + " ms");
    }

    private void buildLayout() {
        layout = null;
        if (preparedLayouts != null) {
            int w = getMeasuredWidth();
            for (int i = 0; i < preparedLayouts.length; i++) {
                if (preparedLayouts[i].layoutW == w) {
                    layout = preparedLayouts[i];
                    break;
                }
            }
        }

        if (layout == null) {
            layout = new DialogLayout();
            layout.build(description, getMeasuredWidth(), getMeasuredHeight(), application);
        }
    }

    private void bound(Drawable src, int x, int y) {
        src.setBounds(x, y, x + src.getIntrinsicWidth(), y + src.getIntrinsicHeight());
    }

    private void updateTyping() {
        needNewUpdateTyping = false;
        if (description.getPeerType() == PeerType.PEER_USER || description.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
            if (userTypes) {
                typingLayout = new StaticLayout(getResources().getString(R.string.lang_common_typing), bodyHighlightPaint, layout.layoutMainWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            } else {
                typingLayout = null;
            }
        } else {
            if (typingUids != null && typingUids.length != 0) {
                String[] names = new String[typingUids.length];
                for (int i = 0; i < names.length; i++) {
                    names[i] = application.getEngine().getUserRuntime(typingUids[i]).getFirstName();
                }
                String typing = TextUtil.formatTyping(names);
                Spannable spannable = application.getEmojiProcessor().processEmojiCutMutable(typing, EmojiProcessor.CONFIGURATION_DIALOGS);
                CharSequence sequence = TextUtils.ellipsize(spannable, bodyHighlightPaint, layout.layoutMainWidth, TextUtils.TruncateAt.END);
                typingLayout = new StaticLayout(sequence, bodyHighlightPaint, layout.layoutMainWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            } else {
                typingLayout = null;
            }
        }
    }

    private void drawPlaceHolder(Canvas canvas) {
        placeholderPaint.setColor(layout.placeHolderColor);

        placeholderTextPaint.setAlpha(255);
        placeholderPaint.setAlpha(255);
        avatarPaint.setAlpha(255);

        rect.set(layout.layoutAvatarLeft, layout.layoutAvatarTop,
                layout.layoutAvatarLeft + layout.layoutAvatarWidth,
                layout.layoutAvatarTop + layout.layoutAvatarWidth);

        canvas.drawRect(rect, placeholderPaint);
        if (layout.usePlaceholder) {
            canvas.drawText(layout.placeHolderName, layout.placeholderLeft, layout.placeholderTop, placeholderTextPaint);
        } else {
            rect2.set(0, 0, placeholder.getWidth(), placeholder.getHeight());
            canvas.drawBitmap(placeholder, rect2, rect, avatarPaint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (layout == null) {
            requestLayout();
            return;
        }

        if (description == null) {
            return;
        }

        if (needNewUpdateTyping) {
            updateTyping();
        }

        if (layout.isEncrypted) {
            bounds(secureIcon, layout.layoutEncryptedLeft, layout.layoutEncryptedTop);
            secureIcon.draw(canvas);
        }

        if (layout.titleLayout != null) {
            canvas.save();
            canvas.translate(layout.layoutTitleLeft, layout.layoutTitleLayoutTop);
            layout.titleLayout.draw(canvas);
            canvas.restore();
        } else if (layout.titleString != null) {
            TextPaint paint = layout.isEncrypted ? titleEncryptedPaint : (layout.isHighlighted ? titleHighlightPaint : titlePaint);
            canvas.drawText(layout.titleString, layout.layoutTitleLeft, layout.layoutTitleTop, paint);
        }

        if (state != MessageState.FAILURE && description.getSenderId() == currentUserUid) {
            switch (state) {
                default:
                case MessageState.PENDING:
                    bound(statePending, layout.layoutClockLeft, layout.layoutClockTop);
                    statePending.draw(canvas);
                    break;
                case MessageState.SENT:
                    bound(stateSent, layout.layoutStateLeft, layout.layoutStateTop);
                    stateSent.draw(canvas);
                    break;
                case MessageState.READED:
                    bound(stateSent, layout.layoutStateLeftDouble, layout.layoutStateTop);
                    stateSent.draw(canvas);
                    bound(stateHalfCheck, layout.layoutStateLeft, layout.layoutStateTop);
                    stateHalfCheck.draw(canvas);
                    break;
            }
        }

        TextPaint timePaint = HIGHLIGHT_UNDEAD ? (layout.isUnreadIn ? unreadClockPaint : readClockPaint) : readClockPaint;
        canvas.drawText(layout.time, layout.layoutTimeLeft, layout.layoutTimeTop, timePaint);

        if (typingLayout != null) {
            canvas.save();
            canvas.translate(layout.layoutMainLeft, layout.layoutMainContentTop);
            typingLayout.draw(canvas);
            canvas.restore();
        } else if (layout.bodyLayout != null) {
            canvas.save();
            canvas.translate(layout.layoutMainLeft, layout.layoutMainContentTop);
            layout.bodyLayout.draw(canvas);
            canvas.restore();
        } else if (layout.bodyString != null) {
//            canvas.save();
//            canvas.translate(layout.layoutMainLeft, layout.layoutMainTop);
//            layout.bodyLayout.draw(canvas);
//            canvas.restore();
            canvas.drawText(layout.bodyString, layout.layoutMainLeft, layout.layoutMainTop, bodyPaint);
        }

        if (avatarHolder != null) {
            long time = SystemClock.uptimeMillis() - avatarAppearTime;
            if (time > AVATAR_FADE_TIME || !AVATAR_FADE) {
                avatarPaint.setAlpha(255);
                canvas.drawBitmap(avatarHolder.getBitmap(), layout.layoutAvatarLeft, layout.layoutAvatarTop, avatarPaint);
            } else {
                drawPlaceHolder(canvas);

                float alpha = time / (float) AVATAR_FADE_TIME;
                avatarPaint.setAlpha((int) (255 * alpha));
                canvas.drawBitmap(avatarHolder.getBitmap(), layout.layoutAvatarLeft, layout.layoutAvatarTop, avatarPaint);

                inavalidateForAnimation();
            }
        } else {
            drawPlaceHolder(canvas);
        }

        if (description.isErrorState() ||
                (state == MessageState.FAILURE && description.getSenderId() == currentUserUid)) {
            bound(stateFailure, layout.layoutMarkLeft, layout.layoutMarkTop);
            stateFailure.draw(canvas);
        } else if (description.getUnreadCount() > 0) {
            canvas.drawRoundRect(layout.layoutMarkRect, layout.layoutMarkRadius, layout.layoutMarkRadius, counterPaint);
            canvas.drawText(layout.unreadCountText, layout.layoutMarkLeft + layout.layoutMarkTextLeft, layout.layoutMarkTextTop, counterTitlePaint);
        }
    }

    @Override
    public void onChatTypingChanged(int chatId, int[] uids) {
        if (description != null) {
            if (description.getPeerType() == PeerType.PEER_CHAT && description.getPeerId() == chatId) {
                this.typingUids = uids;
                this.needNewUpdateTyping = true;
                this.invalidate();
            }
        }
    }

    @Override
    public void onUserTypingChanged(int uid, boolean types) {
        if (description != null) {
            if (description.getPeerType() == PeerType.PEER_USER & description.getPeerId() == uid) {
                this.userTypes = types;
                this.needNewUpdateTyping = true;
                this.invalidate();
            }
        }
    }

    @Override
    public void onEncryptedTypingChanged(int chatId, boolean types) {
        if (description != null) {
            if (description.getPeerType() == PeerType.PEER_USER_ENCRYPTED & description.getPeerId() == chatId) {
                this.userTypes = types;
                this.needNewUpdateTyping = true;
                this.invalidate();
            }
        }
    }

    public static Object prepareLayoutCache(DialogWireframe wireframe, TelegramApplication context) {
        checkResources(context);
        DialogLayout hl = new DialogLayout();
        hl.build(wireframe, UiMeasure.METRICS.widthPixels, px(80), context);
        DialogLayout wl = new DialogLayout();
        wl.build(wireframe, UiMeasure.METRICS.heightPixels, px(80), context);
        return new DialogLayout[]{hl, wl};
    }

    private void releaseAvatar() {
        if (avatarHolder != null) {
            avatarHolder.release();
            avatarHolder = null;
        }
    }

    @Override
    public void onImageReceived(ImageHolder holder, boolean intermediate) {
        releaseAvatar();
        avatarHolder = holder;
        if (intermediate) {
            this.avatarAppearTime = 0;
        } else {
            this.avatarAppearTime = SystemClock.uptimeMillis();
        }
        invalidate();
    }

    private static class DialogLayout {

        public int layoutW;
        public int layoutH;

        public boolean isRtl;

        public int layoutPadding;
        public int layoutBodyPadding;

        public int layoutAvatarWidth;
        public int layoutAvatarTop;
        public int layoutAvatarLeft;
        public int layoutTimeTop;
        public int layoutTimeLeft;

        public int layoutTitleTop;
        public int layoutTitleLayoutTop;
        public int layoutTitleLeft;
        public int layoutTitleWidth;

        public int layoutEncryptedTop;
        public int layoutEncryptedLeft;

        public int layoutStateTop;
        public int layoutStateLeft;
        public int layoutStateLeftDouble;

        public int layoutClockLeft;
        public int layoutClockTop;

        public int layoutMainWidth;
        public int layoutMainLeft;
        public int layoutMainTop;
        public int layoutMainContentTop;

        public int layoutMarkWidth;
        public int layoutMarkLeft;
        public int layoutMarkTop;
        public int layoutMarkBottom;
        public int layoutMarkTextLeft;
        public int layoutMarkTextTop;
        public int layoutMarkRadius;

        public RectF layoutMarkRect = new RectF();

        public RectF avatarRect = new RectF();

        public String unreadCountText;

        public String time;

        public boolean isGroup;
        public boolean isEncrypted;
        public boolean isHighlighted;
        public boolean isBodyHighlighted;
        public boolean isUnreadIn;

        public Layout bodyLayout;
        public String bodyString;
        public Layout titleLayout;
        public String titleString;

        public String placeHolderName;
        public int placeHolderColor;
        public float placeholderLeft;
        public float placeholderTop;
        public boolean usePlaceholder;

        public void build(DialogWireframe description, int w, int h, TelegramApplication application) {
            layoutH = h;
            layoutW = w;

            if (description.getPeerType() == PeerType.PEER_USER) {
                if (description.getPeerId() == 333000) {
                    isHighlighted = false;
                } else {
                    User user = description.getDialogUser();
                    isHighlighted = user.getLinkType() == LinkType.FOREIGN;
                }
                isGroup = false;
                isEncrypted = false;
            } else if (description.getPeerType() == PeerType.PEER_CHAT) {
                isHighlighted = false;
                isGroup = true;
                isEncrypted = false;
            } else if (description.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                isHighlighted = false;
                isGroup = false;
                isEncrypted = true;
            }

            isBodyHighlighted = description.getContentType() != ContentType.MESSAGE_TEXT;

            if (description.getUnreadCount() != 0 && !description.isMine()) {
                isUnreadIn = true;
            } else {
                isUnreadIn = false;
            }

            time = org.telegram.android.ui.TextUtil.formatDate(description.getDate(), application);
            isRtl = application.isRTL();

            if (IS_LARGE) {
                layoutAvatarWidth = px(64);
                layoutPadding = application.getResources().getDimensionPixelSize(R.dimen.dialogs_padding);
                layoutBodyPadding = layoutAvatarWidth + layoutPadding + px(12);
                layoutAvatarTop = px(8);
                layoutTitleTop = px(34);
                layoutMainTop = px(60);
                layoutTimeTop = px(34);

                layoutMarkTop = px(44);
                layoutMarkBottom = layoutMarkTop + px(22);
                layoutMarkTextTop = layoutMarkTop + px(18);
            } else {
                layoutAvatarWidth = px(54);
                layoutPadding = application.getResources().getDimensionPixelSize(R.dimen.dialogs_padding);
                layoutBodyPadding = layoutAvatarWidth + layoutPadding + px(12);
                layoutAvatarTop = px(8);
                layoutTitleTop = px(30);
                layoutMainTop = px(54);
                layoutTimeTop = px(30);

                layoutMarkTop = px(38);
                layoutMarkBottom = layoutMarkTop + px(22);
                layoutMarkTextTop = layoutMarkTop + px(18);
            }

            layoutMainContentTop = (int) (layoutMainTop + bodyPaint.getFontMetrics().ascent);
            layoutTitleLayoutTop = (int) (layoutTitleTop + titlePaint.getFontMetrics().ascent);
            layoutStateTop = layoutTimeTop - px(10);
            layoutClockTop = layoutTimeTop - px(12);
            layoutEncryptedTop = layoutTimeTop - px(14);

            if (isRtl) {
                layoutAvatarLeft = w - layoutPadding - layoutAvatarWidth;
            } else {
                layoutAvatarLeft = layoutPadding;
            }

            int timeWidth = (int) unreadClockPaint.measureText(time);
            if (isRtl) {
                layoutTimeLeft = layoutPadding;
                layoutStateLeftDouble = layoutPadding + timeWidth + px(2);
                layoutStateLeft = layoutStateLeftDouble + px(6);
                layoutClockLeft = layoutPadding + timeWidth + px(2);
            } else {
                layoutTimeLeft = w - layoutPadding - timeWidth;
                layoutClockLeft = w - layoutPadding - timeWidth - px(14);
                layoutStateLeft = w - layoutPadding - timeWidth - px(16);
                layoutStateLeftDouble = w - layoutPadding - timeWidth - px(6 + 16);
            }

            layoutMarkRadius = px(2);
            if (description.isErrorState() ||
                    (description.getMessageState() == MessageState.FAILURE && description.isMine())) {
                layoutMarkWidth = px(22);
                if (isRtl) {
                    layoutMarkLeft = layoutPadding; // getMeasuredWidth() - layoutMarkWidth - getPx(80);
                } else {
                    layoutMarkLeft = w - layoutMarkWidth - layoutPadding;
                }
            } else {
                if (description.getUnreadCount() > 0) {
                    if (description.getUnreadCount() >= 1000) {
                        unreadCountText = I18nUtil.getInstance().correctFormatNumber(description.getUnreadCount() / 1000) + "K";
                    } else {
                        unreadCountText = I18nUtil.getInstance().correctFormatNumber(description.getUnreadCount());
                    }
                    int width = (int) counterTitlePaint.measureText(unreadCountText);
                    Rect r = new Rect();
                    counterTitlePaint.getTextBounds(unreadCountText, 0, unreadCountText.length(), r);
                    layoutMarkTextTop = layoutMarkTop + (layoutMarkBottom - layoutMarkTop + r.top) / 2 - r.top;
                    if (width < px(22 - 14)) {
                        layoutMarkWidth = px(22);
                    } else {
                        layoutMarkWidth = px(14) + width;
                    }
                    layoutMarkTextLeft = (layoutMarkWidth - width) / 2;

                    if (isRtl) {
                        layoutMarkLeft = layoutPadding; //getMeasuredWidth() - layoutMarkWidth - getPx(80);
                    } else {
                        layoutMarkLeft = w - layoutMarkWidth - layoutPadding;
                    }
                } else {
                    layoutMarkLeft = 0;
                    layoutMarkWidth = 0;
                }
            }
            layoutMarkRect.set(layoutMarkLeft, layoutMarkTop, layoutMarkLeft + layoutMarkWidth, layoutMarkBottom);

            if (description.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                if (isRtl) {
                    if (description.isMine()) {
                        layoutTitleLeft = timeWidth + px(16) + px(16);
                    } else {
                        layoutTitleLeft = timeWidth + px(12);
                    }
                    layoutTitleWidth = w - layoutTitleLeft - layoutBodyPadding - px(14) - px(6);
                    layoutEncryptedLeft = w - layoutBodyPadding - px(12);
                } else {
                    layoutTitleLeft = layoutBodyPadding + px(16);
                    if (description.isMine()) {
                        layoutTitleWidth = w - layoutTitleLeft - timeWidth - px(24);
                    } else {
                        layoutTitleWidth = w - layoutTitleLeft - timeWidth - px(12);
                    }

                    layoutEncryptedLeft = layoutBodyPadding + px(2);
                }
            } else {
                if (isRtl) {
                    if (description.isMine()) {
                        layoutTitleLeft = timeWidth + px(16) + px(16);
                    } else {
                        layoutTitleLeft = timeWidth + px(12);
                    }
                    layoutTitleWidth = w - layoutTitleLeft - layoutBodyPadding;
                } else {
                    layoutTitleLeft = layoutBodyPadding;
                    if (description.isMine()) {
                        layoutTitleWidth = w - layoutTitleLeft - timeWidth - px(24) - px(12);
                    } else {
                        layoutTitleWidth = w - layoutTitleLeft - timeWidth - px(12);
                    }
                }
            }

            layoutMainWidth = w - layoutBodyPadding - layoutPadding;
            if (isRtl) {
                layoutMainLeft = w - layoutMainWidth - layoutBodyPadding;
                if (layoutMarkWidth != 0) {
                    layoutMainLeft += layoutMarkWidth + px(8);
                    layoutMainWidth -= layoutMarkWidth + px(8);
                }
            } else {
                layoutMainLeft = layoutBodyPadding;
                if (layoutMarkWidth != 0) {
                    layoutMainWidth -= layoutMarkWidth + px(8);
                }
            }

            avatarRect.set(layoutAvatarLeft, layoutAvatarTop, layoutAvatarLeft + layoutAvatarWidth, layoutAvatarTop + layoutAvatarWidth);

            // Building text layouts
            {
                String message = description.getMessage();
                if (message.length() > 150) {
                    message = message.substring(0, 150) + "...";
                }
                message = message.replace("\n", " ");

                TextPaint bodyTextPaint;
                if (isBodyHighlighted) {
                    bodyTextPaint = bodyHighlightPaint;
                } else {
                    if (HIGHLIGHT_UNDEAD) {
                        if (isUnreadIn) {
                            bodyTextPaint = bodyUnreadPaint;
                        } else {
                            bodyTextPaint = bodyPaint;
                        }
                    } else {
                        bodyTextPaint = bodyPaint;
                    }
                }

                int nameLength = 0;

                if (description.getContentType() != ContentType.MESSAGE_SYSTEM) {
                    if (description.isMine()) {
                        String name = application.getResources().getString(R.string.st_dialog_you);
                        nameLength = BidiFormatter.getInstance().unicodeWrap(name).length();
                        message = BidiFormatter.getInstance().unicodeWrap(name) + ": " + BidiFormatter.getInstance().unicodeWrap(message);
                    } else {
                        if (isGroup) {
                            User user = description.getSender();
                            nameLength = BidiFormatter.getInstance().unicodeWrap(user.getFirstName()).length();
                            message = BidiFormatter.getInstance().unicodeWrap(user.getFirstName().replace("\n", " ")) + ": " + BidiFormatter.getInstance().unicodeWrap(message);
                        }
                    }
                }

                String preSequence = TextUtils.ellipsize(message, bodyTextPaint, layoutMainWidth, TextUtils.TruncateAt.END).toString();

//                Spannable sequence = application.getEmojiProcessor().processEmojiCutMutable(preSequence, EmojiProcessor.CONFIGURATION_DIALOGS);
//                if (nameLength != 0) {
//                    sequence.setSpan(new ForegroundColorSpan(HIGHLIGHT_COLOR), 0, Math.min(nameLength, sequence.length()), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
//                }
//                CharSequence resSequence = TextUtils.ellipsize(sequence, bodyTextPaint, layoutMainWidth, TextUtils.TruncateAt.END);
//                bodyLayout = new StaticLayout(resSequence, bodyTextPaint, layoutMainWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
//                bodyString = null;

                Spannable sequence = application.getEmojiProcessor().processEmojiCutMutable(preSequence, EmojiProcessor.CONFIGURATION_DIALOGS);
                if (nameLength != 0) {
                    sequence.setSpan(new ForegroundColorSpan(HIGHLIGHT_COLOR), 0, Math.min(nameLength, sequence.length()), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                }

                CharSequence resSequence = TextUtils.ellipsize(sequence, bodyTextPaint, layoutMainWidth, TextUtils.TruncateAt.END);
                bodyLayout = new StaticLayout(resSequence, bodyTextPaint, layoutMainWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                bodyString = null;
//                if (EmojiProcessor.containsEmoji(message)) {
//                    Spannable sequence = application.getEmojiProcessor().processEmojiCutMutable(preSequence, EmojiProcessor.CONFIGURATION_DIALOGS);
//                    if (nameLength != 0) {
//                        sequence.setSpan(new ForegroundColorSpan(HIGHLIGHT_COLOR), 0, Math.min(nameLength, sequence.length()), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
//                    }
//
//                    CharSequence resSequence = TextUtils.ellipsize(sequence, bodyTextPaint, layoutMainWidth, TextUtils.TruncateAt.END);
//                    bodyLayout = new StaticLayout(resSequence, bodyTextPaint, layoutMainWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
//                    bodyString = null;
//                } else {
//                    bodyString = preSequence;
//                    bodyLayout = null;
//                }
            }

            // Title
            {
                String title = description.getDialogTitle();
                if (title.length() > 150) {
                    title = title.substring(150) + "...";
                }
                title = title.replace("\n", " ");

                TextPaint paint = isEncrypted ? titleEncryptedPaint : (isHighlighted ? titleHighlightPaint : titlePaint);

//                Spannable preSequence = application.getEmojiProcessor().processEmojiCutMutable(title, EmojiProcessor.CONFIGURATION_DIALOGS);
//                CharSequence sequence = TextUtils.ellipsize(preSequence, paint, layoutTitleWidth, TextUtils.TruncateAt.END);
//                titleLayout = new StaticLayout(sequence, paint, layoutTitleWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
//                titleString = null;

                if (EmojiProcessor.containsEmoji(title)) {
                    Spannable preSequence = application.getEmojiProcessor().processEmojiCutMutable(title, EmojiProcessor.CONFIGURATION_DIALOGS);
                    CharSequence sequence = TextUtils.ellipsize(preSequence, paint, layoutTitleWidth, TextUtils.TruncateAt.END);
                    titleLayout = new StaticLayout(sequence, paint, layoutTitleWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    titleString = null;
                } else {
                    titleString = TextUtils.ellipsize(title, paint, layoutTitleWidth, TextUtils.TruncateAt.END).toString();
                    titleLayout = null;
                }
            }

            // Placeholder
            placeHolderName = description.getDialogName();
            placeHolderColor = Placeholders.getBgColor(description.getPeerId());

            if (placeHolderName.length() > 0) {
                usePlaceholder = true;
                placeholderLeft = layoutAvatarLeft + layoutAvatarWidth / 2;
                Rect rect = new Rect();
                placeholderTextPaint.getTextBounds(placeHolderName, 0, placeHolderName.length(), rect);
                placeholderTop = layoutAvatarTop + (layoutAvatarWidth / 2 + ((rect.bottom - rect.top) / 2));
            } else {
                usePlaceholder = false;
            }
        }
    }
}
