package org.telegram.android.views;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import com.extradea.framework.images.ImageReceiver;
import com.extradea.framework.images.tasks.RoundedImageTask;
import org.telegram.android.R;
import org.telegram.android.StelsApplication;
import org.telegram.android.config.UserSettings;
import org.telegram.android.core.TypingStates;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.media.TLAbsLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.core.wireframes.DialogWireframe;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.ui.*;
import org.telegram.i18n.I18nUtil;

/**
 * Author: Korshakov Stepan
 * Created: 06.08.13 18:40
 */
public class DialogView extends BaseView implements TypingStates.TypingListener {

    private static boolean IS_LARGE = false;

    public static void resetSettings() {
        isLoaded = false;
    }

    // Resources
    private static int HIGHLIGHT_COLOR = 0xff3076a4;
    private static int UNREAD_COLOR = 0xff010101;
    private static int READ_COLOR = 0xffb1b1b1;

    private static int READ_TIME_COLOR = 0xff898989;
    private static int UNREAD_TIME_COLOR = 0xff6597c4;

    private static boolean isLoaded = false;
    private static Paint avatarPaint;
    private static Paint counterPaint;
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

    private static Bitmap userPlaceholder;
    private static Bitmap groupPlaceholder;

    private Drawable statePending;
    private Drawable stateSent;
    private Drawable stateHalfCheck;
    private Drawable stateFailure;
    private Drawable secureIcon;

    // Help data
    private int currentUserUid;

    private StelsApplication application;

    private ImageReceiver avatarReceiver;

    // Data

    private DialogWireframe description;
    private DialogLayout[] preparedLayouts;

    // PreparedData
    private String title;
    private String body;

    private TLAbsLocalAvatarPhoto photo;

    private int state;

    //    private Drawable emptyDrawable;
    private Bitmap empty;
    private Bitmap avatar;
    private Paint avatarBgPaint;

    // Typing

    private boolean needNewUpdateTyping;

    private Layout typingLayout;

    private int[] typingUids;
    private boolean userTypes;

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
            StelsApplication application = (StelsApplication) context.getApplicationContext();
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
                titlePaint.setTypeface(FontController.loadTypeface(context, "regular"));
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
                titleHighlightPaint.setTypeface(FontController.loadTypeface(context, "regular"));
            } else {
                titleHighlightPaint.setTextSize(sp(18f));
                titleHighlightPaint.setTypeface(FontController.loadTypeface(context, "medium"));
            }

            if (FontController.USE_SUBPIXEL) {
                titleEncryptedPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                titleEncryptedPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            titleEncryptedPaint.setColor(0xff3a9d43);
            if (IS_LARGE) {
                titleEncryptedPaint.setTextSize(sp(20f));
                titleEncryptedPaint.setTypeface(FontController.loadTypeface(context, "regular"));
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
            counterPaint.setColor(0xff8ec85f);

            if (IS_LARGE) {
                userPlaceholder = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.st_user_placeholder_common)).getBitmap();
                groupPlaceholder = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.st_group_placeholder_common)).getBitmap();
            } else {
                userPlaceholder = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.st_user_placeholder_common)).getBitmap();
                groupPlaceholder = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.st_group_placeholder_common)).getBitmap();
            }

            isLoaded = true;
        }
    }

    public DialogView(Context context) {
        super(context);
        checkResources(context);

        this.application = (StelsApplication) context.getApplicationContext();

        this.currentUserUid = application.getCurrentUid();
        this.avatarReceiver = new ImageReceiver() {
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
        this.avatarReceiver.register(application.getImageController());

        application.getTypingStates().registerListener(this);

        avatarBgPaint = new Paint();
        avatarBgPaint.setStyle(Paint.Style.FILL);

        statePending = getResources().getDrawable(R.drawable.st_bubble_ic_clock);
        stateSent = getResources().getDrawable(R.drawable.st_dialogs_check);
        stateHalfCheck = getResources().getDrawable(R.drawable.st_dialogs_halfcheck);
        stateFailure = getResources().getDrawable(R.drawable.st_dialogs_warning);
        secureIcon = getResources().getDrawable(R.drawable.st_ic_lock_green);
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
            avatarBgPaint.setColor(Placeholders.getUserBgColor(encryptedChat.getUserId()));
            empty = userPlaceholder;
        } else if (description.getPeerType() == PeerType.PEER_USER) {
            avatarBgPaint.setColor(Placeholders.getUserBgColor(description.getPeerId()));
            empty = userPlaceholder;
        } else {
            avatarBgPaint.setColor(Placeholders.getGroupBgColor(description.getPeerId()));
            empty = groupPlaceholder;
        }

        if (photo instanceof TLLocalAvatarPhoto) {
            TLLocalAvatarPhoto avatarPhoto = (TLLocalAvatarPhoto) photo;
            if (avatarPhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                StelsImageTask task = new StelsImageTask((TLLocalFileLocation) avatarPhoto.getPreviewLocation());
                if (IS_LARGE) {
                    task.setMaxHeight(getPx(64));
                    task.setMaxWidth(getPx(64));
                } else {
                    task.setMaxHeight(getPx(56));
                    task.setMaxWidth(getPx(56));
                }
                task.setFillRect(true);
                RoundedImageTask roundedImageTask = new RoundedImageTask(task);
                if (IS_LARGE) {
                    roundedImageTask.setRadius(getPx(2));
                } else {
                    roundedImageTask.setRadius(0);
                }
                avatarReceiver.receiveImage(roundedImageTask);
                avatar = avatarReceiver.getResult();
            } else {
                avatarReceiver.receiveImage(null);
            }
        } else {
            avatarReceiver.receiveImage(null);
        }

        if (getMeasuredHeight() != 0 || getMeasuredWidth() != 0) {
            buildLayout();
        } else {
            requestLayout();
        }

        needNewUpdateTyping = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        avatarReceiver.onRemovedFromParent();
        avatar = null;
        postInvalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        avatarReceiver.onAddedToParent();
        avatar = avatarReceiver.getResult();
        postInvalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (IS_LARGE) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), getPx(80));
        } else {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), getPx(68));
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            buildLayout();
        }
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
                typingLayout = new StaticLayout(TextUtil.formatTyping(names), bodyHighlightPaint, layout.layoutMainWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            } else {
                typingLayout = null;
            }
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

        canvas.save();
        canvas.translate(layout.layoutTitleLeft, layout.layoutTitleTop);
        layout.titleLayout.draw(canvas);
        canvas.restore();

        if (state != MessageState.FAILURE && description.getSenderId() == currentUserUid) {
            switch (state) {
                default:
                case MessageState.PENDING:
                    bound(statePending, layout.layoutStateLeft, layout.layoutStateTop);
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

        canvas.drawText(layout.time, layout.layoutTimeLeft, layout.layoutTimeTop, layout.isUnreadOut ? unreadClockPaint : readClockPaint);

        if (typingLayout != null) {
            canvas.save();
            canvas.translate(layout.layoutMainLeft, layout.layoutMainTop);
            typingLayout.draw(canvas);
            canvas.restore();
        } else {
            canvas.save();
            canvas.translate(layout.layoutMainLeft, layout.layoutMainTop);
            layout.bodyLayout.draw(canvas);
            canvas.restore();
        }

        if (avatar != null) {
            canvas.drawBitmap(avatar, layout.layoutAvatarLeft, layout.layoutAvatarTop, avatarPaint);
        } else {
            canvas.drawRoundRect(layout.avatarRect, getPx(2), getPx(2), avatarBgPaint);
            canvas.drawBitmap(empty, layout.layoutAvatarLeft, layout.layoutAvatarTop, avatarPaint);
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

    public static Object prepareLayoutCache(DialogWireframe wireframe, StelsApplication context) {
        checkResources(context);
        DialogLayout hl = new DialogLayout();
        hl.build(wireframe, UiMeasure.METRICS.widthPixels, px(80), context);
        DialogLayout wl = new DialogLayout();
        wl.build(wireframe, UiMeasure.METRICS.heightPixels, px(80), context);
        return new DialogLayout[]{hl, wl};
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
        public int layoutTitleLeft;
        public int layoutTitleWidth;

        public int layoutEncryptedTop;
        public int layoutEncryptedLeft;

        public int layoutStateTop;
        public int layoutStateLeft;
        public int layoutStateLeftDouble;

        public int layoutMainWidth;
        public int layoutMainLeft;
        public int layoutMainTop;

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
        public boolean isUnreadOut;

        public Layout bodyLayout;
        public Layout titleLayout;

        public void build(DialogWireframe description, int w, int h, StelsApplication application) {
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

            if (description.getUnreadCount() != 0 && description.isMine()) {
                isUnreadOut = true;
            } else {
                isUnreadOut = false;
            }

            time = org.telegram.android.ui.TextUtil.formatDate(description.getDate(), application);
            isRtl = application.isRTL();

            if (IS_LARGE) {
                layoutAvatarWidth = px(64);
                layoutPadding = px(8);
                layoutBodyPadding = layoutAvatarWidth + layoutPadding + px(10);
                layoutAvatarTop = px(8);
                layoutTitleTop = px(16);
                layoutMainTop = px(46);
                layoutTimeTop = px(34);
                layoutStateTop = px(22);
                layoutEncryptedTop = px(20);

                layoutMarkTop = px(44);
                layoutMarkBottom = layoutMarkTop + px(22);
                layoutMarkTextTop = layoutMarkTop + px(18);
            } else {
                layoutAvatarWidth = px(56);
                layoutPadding = px(8);
                layoutBodyPadding = layoutAvatarWidth + layoutPadding + px(10);
                layoutAvatarTop = px(6);
                layoutTitleTop = px(12);
                layoutMainTop = px(38);
                layoutTimeTop = px(28);
                layoutStateTop = px(16);
                layoutEncryptedTop = px(14);

                layoutMarkTop = px(38);
                layoutMarkBottom = layoutMarkTop + px(22);
                layoutMarkTextTop = layoutMarkTop + px(18);
            }

            if (isRtl) {
                layoutAvatarLeft = w - layoutPadding + layoutAvatarWidth;
            } else {
                layoutAvatarLeft = layoutPadding;
            }

            int timeWidth = (int) unreadClockPaint.measureText(time);
            if (isRtl) {
                layoutTimeLeft = px(8);
            } else {
                layoutTimeLeft = w - px(8) - timeWidth;
            }

            if (isRtl) {
                layoutStateLeft = px(8) + timeWidth + px(7);
                layoutStateLeftDouble = px(8) + timeWidth + px(2);
            } else {
                layoutStateLeft = w - px(8) - timeWidth - px(16);
                layoutStateLeftDouble = w - px(8) - timeWidth - px(21);
            }

            layoutMarkRadius = px(2);
            if (description.isErrorState() ||
                    (description.getMessageState() == MessageState.FAILURE && description.isMine())) {
                layoutMarkWidth = px(30);
                if (isRtl) {
                    layoutMarkLeft = px(8); // getMeasuredWidth() - layoutMarkWidth - getPx(80);
                } else {
                    layoutMarkLeft = w - layoutMarkWidth - px(8);
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
                        layoutMarkLeft = px(8); //getMeasuredWidth() - layoutMarkWidth - getPx(80);
                    } else {
                        layoutMarkLeft = w - layoutMarkWidth - px(8);
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

            layoutMainWidth = w - px(6) - layoutBodyPadding;
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
                    if (isUnreadOut) {
                        bodyTextPaint = bodyUnreadPaint;
                    } else {
                        bodyTextPaint = bodyPaint;
                    }
                }

                int nameLength = 0;
                if (description.isMine()) {
                    String name = application.getResources().getString(R.string.st_dialog_you);
                    nameLength = name.length();
                    message = name + ": " + message;
                } else {
                    if (isGroup) {
                        User user = description.getSender();
                        nameLength = user.getFirstName().length();
                        message = user.getFirstName() + ": " + message;
                    }
                }

                String preSequence = TextUtils.ellipsize(message, bodyTextPaint, layoutMainWidth, TextUtils.TruncateAt.END).toString();
                Spannable sequence = application.getEmojiProcessor().processEmojiCutMutable(preSequence, EmojiProcessor.CONFIGURATION_DIALOGS);

                if (nameLength != 0) {
                    sequence.setSpan(new ForegroundColorSpan(HIGHLIGHT_COLOR), 0, Math.min(nameLength, sequence.length()), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                }

                CharSequence resSequence = TextUtils.ellipsize(sequence, bodyTextPaint, layoutMainWidth, TextUtils.TruncateAt.END);
                bodyLayout = new StaticLayout(resSequence, bodyTextPaint, layoutMainWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            }

            // Title
            {
                String title = description.getDialogTitle();
                if (title.length() > 150) {
                    title = title.substring(150) + "...";
                }
                title = title.replace("\n", " ");

                TextPaint paint = isEncrypted ? titleEncryptedPaint : (isHighlighted ? titleHighlightPaint : titlePaint);
                Spannable preSequence = application.getEmojiProcessor().processEmojiCutMutable(title, EmojiProcessor.CONFIGURATION_DIALOGS);
                CharSequence sequence = TextUtils.ellipsize(preSequence, paint, layoutTitleWidth, TextUtils.TruncateAt.END);
                titleLayout = new StaticLayout(sequence, paint, layoutTitleWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            }
        }
    }
}
