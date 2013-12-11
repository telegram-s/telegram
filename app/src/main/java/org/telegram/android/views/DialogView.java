package org.telegram.android.views;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import com.extradea.framework.images.ImageReceiver;
import com.extradea.framework.images.tasks.RoundedImageTask;
import org.telegram.android.R;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.TypingStates;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.media.TLAbsLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.core.wireframes.DialogWireframe;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.ui.EmojiProcessor;
import org.telegram.android.ui.FontController;
import org.telegram.android.ui.Placeholders;
import org.telegram.android.ui.TextUtil;
import org.telegram.i18n.I18nUtil;

/**
 * Author: Korshakov Stepan
 * Created: 06.08.13 18:40
 */
public class DialogView extends BaseView implements TypingStates.TypingListener {

    // Resources
    private static int HIGHLIGHT_COLOR = 0xff5d90ca;
    private static int UNREAD_COLOR = 0xff010101;
    private static int READ_COLOR = 0xffa8a8a8;

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

    // PreparedData
    private String title;
    private String body;
    // private String senderTitle;
    private String time;
    private String unreadCountText;

    private TLAbsLocalAvatarPhoto photo;

    private int state;
    private int unreadCount;

    private Layout bodyLayout;
    // private Layout senderLayout;
    private Layout titleLayout;

    //    private Drawable emptyDrawable;
    private Bitmap empty;
    private RectF avatarRect;
    private Bitmap avatar;
    private Paint avatarBgPaint;

    private boolean isGroup;
    private boolean isEncrypted;
    private boolean isHighlighted;
    private boolean isBodyHighlighted;
    private boolean isUnreadOut;

    // Typing

    private boolean needNewUpdateTyping;

    private Layout typingLayout;

    private int[] typingUids;
    private boolean userTypes;

    // Layouting

    private boolean isRtl;

    private int layoutAvatarTop;
    private int layoutAvatarLeft;
    private int layoutTimeTop;
    private int layoutTimeLeft;

    private int layoutTitleTop;
    private int layoutTitleLeft;
    private int layoutTitleWidth;

    private int layoutEncryptedTop;
    private int layoutEncryptedLeft;

    private int layoutStateTop;
    private int layoutStateLeft;
    private int layoutStateLeftDouble;

    private int layoutMainWidth;
    private int layoutMainLeft;
    private int layoutMainTop;

    private int layoutMarkWidth;
    private int layoutMarkLeft;
    private int layoutMarkTop;
    private int layoutMarkBottom;
    private int layoutMarkTextLeft;
    private int layoutMarkTextTop;
    private int layoutMarkRadius;
    private RectF layoutMarkRect;

    public DialogView(Context context) {
        super(context);

        this.application = (StelsApplication) context.getApplicationContext();

        this.currentUserUid = application.getCurrentUid();
        this.isRtl = application.isRTL();
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

        if (!isLoaded) {
            avatarPaint = new Paint();

            if (FontController.USE_SUBPIXEL) {
                titlePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                titlePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            titlePaint.setColor(0xff010101);
            titlePaint.setTextSize(getSp(20f));
            titlePaint.setTypeface(FontController.loadTypeface(context, "regular"));

            if (FontController.USE_SUBPIXEL) {
                titleHighlightPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                titleHighlightPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            titleHighlightPaint.setColor(0xff006FC8);
            titleHighlightPaint.setTextSize(getSp(20f));
            titleHighlightPaint.setTypeface(FontController.loadTypeface(context, "regular"));

            if (FontController.USE_SUBPIXEL) {
                titleEncryptedPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                titleEncryptedPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            titleEncryptedPaint.setColor(0xff68b741);
            titleEncryptedPaint.setTextSize(getSp(20f));
            titleEncryptedPaint.setTypeface(FontController.loadTypeface(context, "regular"));

            if (FontController.USE_SUBPIXEL) {
                unreadClockPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                unreadClockPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            unreadClockPaint.setColor(UNREAD_TIME_COLOR);
            unreadClockPaint.setTextSize(getSp(14));
            unreadClockPaint.setTypeface(FontController.loadTypeface(context, "regular"));

            if (FontController.USE_SUBPIXEL) {
                readClockPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                readClockPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            readClockPaint.setColor(READ_TIME_COLOR);
            readClockPaint.setTextSize(getSp(14));
            readClockPaint.setTypeface(FontController.loadTypeface(context, "regular"));

            if (FontController.USE_SUBPIXEL) {
                bodyPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                bodyPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            bodyPaint.setColor(READ_COLOR);
            bodyPaint.setTextSize(getSp(17f));
            bodyPaint.setTypeface(FontController.loadTypeface(context, "regular"));

            if (FontController.USE_SUBPIXEL) {
                bodyUnreadPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                bodyUnreadPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            bodyUnreadPaint.setColor(UNREAD_COLOR);
            bodyUnreadPaint.setTextSize(getSp(17f));
            bodyUnreadPaint.setTypeface(FontController.loadTypeface(context, "regular"));

            if (FontController.USE_SUBPIXEL) {
                bodyHighlightPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                bodyHighlightPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            bodyHighlightPaint.setColor(HIGHLIGHT_COLOR);
            bodyHighlightPaint.setTextSize(getSp(17f));
            bodyHighlightPaint.setTypeface(FontController.loadTypeface(context, "regular"));

//            if (FontController.USE_SUBPIXEL) {
//                senderPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
//            } else {
//                senderPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
//            }
//            senderPaint.setColor(0xff808080);
//            senderPaint.setTextSize(getSp(16f));
//            senderPaint.setTypeface(FontController.loadTypeface(context, "regular"));
//
//            if (FontController.USE_SUBPIXEL) {
//                senderHighlightPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
//            } else {
//                senderHighlightPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
//            }
//            senderHighlightPaint.setColor(0xff006FC8);
//            senderHighlightPaint.setTextSize(getSp(16f));
//            senderHighlightPaint.setTypeface(FontController.loadTypeface(context, "regular"));

            if (FontController.USE_SUBPIXEL) {
                typingPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                typingPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            typingPaint.setColor(0xff006FC8);
            typingPaint.setTextSize(getSp(16f));
            typingPaint.setTypeface(FontController.loadTypeface(context, "regular"));

            if (FontController.USE_SUBPIXEL) {
                counterTitlePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            } else {
                counterTitlePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            }
            counterTitlePaint.setColor(0xffffffff);
            counterTitlePaint.setTextSize(getSp(15.5f));
            counterTitlePaint.setTypeface(FontController.loadTypeface(context, "regular"));

            counterPaint = new Paint();
            counterPaint.setColor(0xff4595D6);
            isLoaded = true;

            userPlaceholder = ((BitmapDrawable) getResources().getDrawable(R.drawable.st_user_placeholder_common)).getBitmap();
            groupPlaceholder = ((BitmapDrawable) getResources().getDrawable(R.drawable.st_group_placeholder_common)).getBitmap();
        }

        statePending = getResources().getDrawable(R.drawable.st_bubble_ic_clock);
        stateSent = getResources().getDrawable(R.drawable.st_dialogs_check);
        stateHalfCheck = getResources().getDrawable(R.drawable.st_dialogs_halfcheck);
        stateFailure = getResources().getDrawable(R.drawable.st_dialogs_warning);
        secureIcon = getResources().getDrawable(R.drawable.st_ic_lock_green);

        avatarRect = new RectF();
    }

    public void setDescription(DialogWireframe description) {
        this.description = description;
        this.time = org.telegram.android.ui.TextUtil.formatDate(description.getDate(), getContext());
        this.state = description.getMessageState();
        this.unreadCount = description.getUnreadCount();

        if (description.getPeerType() == PeerType.PEER_CHAT) {
            this.typingUids = application.getTypingStates().getChatTypes(description.getPeerId());
        } else {
            this.userTypes = application.getTypingStates().isUserTyping(description.getPeerId());
        }

        prepareData();

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
                task.setMaxHeight(getPx(64));
                task.setMaxWidth(getPx(64));
                task.setFillRect(true);
                RoundedImageTask roundedImageTask = new RoundedImageTask(task);
                roundedImageTask.setRadius(getPx(2));
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
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), getPx(80));
    }

    private void prepareData() {
        // Title and highlight
        this.title = description.getDialogTitle();
        this.photo = description.getDialogAvatar();

        if (description.getPeerType() == PeerType.PEER_USER) {
            if (description.getPeerId() == 333000) {
                this.isHighlighted = false;
            } else {
                User user = description.getDialogUser();
                this.photo = user.getPhoto();
                this.isHighlighted = user.getLinkType() == LinkType.FOREIGN;
            }
            this.isGroup = false;
            this.isEncrypted = false;
        } else if (description.getPeerType() == PeerType.PEER_CHAT) {
            this.isHighlighted = false;
            this.isGroup = true;
            this.isEncrypted = false;
        } else if (description.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
            this.isHighlighted = false;
            this.isGroup = false;
            this.isEncrypted = true;
        }

        body = description.getMessage();

        isBodyHighlighted = description.getContentType() != ContentType.MESSAGE_TEXT;

        if (description.getUnreadCount() != 0 && description.getSenderId() != currentUserUid) {
            isUnreadOut = true;
        } else {
            isUnreadOut = false;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            buildLayout();
        }
    }

    private void buildLayout() {
        layoutAvatarTop = getPx(8);
        if (isRtl) {
            layoutAvatarLeft = getMeasuredWidth() - getPx(8 + 64);
        } else {
            layoutAvatarLeft = getPx(8);
        }

        int timeWidth = (int) unreadClockPaint.measureText(time);

        layoutTimeTop = getPx(34);
        if (isRtl) {
            layoutTimeLeft = getPx(8);
        } else {
            layoutTimeLeft = getMeasuredWidth() - getPx(8) - timeWidth;
        }

        layoutStateTop = getPx(22);

        if (isRtl) {
            layoutStateLeft = getPx(8) + timeWidth + getPx(7);
            layoutStateLeftDouble = getPx(8) + timeWidth + getPx(2);
        } else {
            layoutStateLeft = getMeasuredWidth() - getPx(8) - timeWidth - getPx(16);
            layoutStateLeftDouble = getMeasuredWidth() - getPx(8) - timeWidth - getPx(21);
        }

        layoutMarkTop = getPx(40);
        layoutMarkTextTop = getPx(18);
        layoutMarkBottom = getPx(62);
        layoutMarkRadius = getPx(2);
        layoutMarkTextTop = getPx(58);
        if (description.isErrorState() ||
                (state == MessageState.FAILURE && description.getSenderId() == currentUserUid)) {
            layoutMarkWidth = getPx(30);
            if (isRtl) {
                layoutMarkLeft = getPx(8); // getMeasuredWidth() - layoutMarkWidth - getPx(80);
            } else {
                layoutMarkLeft = getMeasuredWidth() - layoutMarkWidth - getPx(8);
            }
        } else {
            if (unreadCount > 0) {
                if (unreadCount >= 1000) {
                    unreadCountText = I18nUtil.getInstance().correctFormatNumber(unreadCount / 1000) + "K";
                } else {
                    unreadCountText = I18nUtil.getInstance().correctFormatNumber(unreadCount);
                }
                int width = (int) counterTitlePaint.measureText(unreadCountText);
                Rect r = new Rect();
                counterTitlePaint.getTextBounds(unreadCountText, 0, unreadCountText.length(), r);
                layoutMarkTextTop = layoutMarkTop + (layoutMarkBottom - layoutMarkTop + r.top) / 2 - r.top;
                if (width < getPx(22 - 14)) {
                    layoutMarkWidth = getPx(22);
                } else {
                    layoutMarkWidth = getPx(14) + width;
                }
                layoutMarkTextLeft = (layoutMarkWidth - width) / 2;

                if (isRtl) {
                    layoutMarkLeft = getPx(8); //getMeasuredWidth() - layoutMarkWidth - getPx(80);
                } else {
                    layoutMarkLeft = getMeasuredWidth() - layoutMarkWidth - getPx(8);
                }
            } else {
                layoutMarkLeft = 0;
                layoutMarkWidth = 0;
            }
        }
        layoutMarkRect = new RectF(layoutMarkLeft, layoutMarkTop, layoutMarkLeft + layoutMarkWidth, layoutMarkBottom);

        layoutTitleTop = getPx(16);
        if (isEncrypted) {
            layoutEncryptedTop = getPx(20);
            if (isRtl) {
                if (description.isMine()) {
                    layoutTitleLeft = timeWidth + getPx(16) + getPx(16);
                } else {
                    layoutTitleLeft = timeWidth + getPx(12);
                }
                layoutTitleWidth = getMeasuredWidth() - layoutTitleLeft - getPx(80) - getPx(14) - getPx(6);
                layoutEncryptedLeft = getMeasuredWidth() - getPx(80) - getPx(12);
            } else {
                layoutTitleLeft = getPx(80) + getPx(20);
                if (description.isMine()) {
                    layoutTitleWidth = getMeasuredWidth() - layoutTitleLeft - timeWidth - getPx(24);
                } else {
                    layoutTitleWidth = getMeasuredWidth() - layoutTitleLeft - timeWidth - getPx(12);
                }

                layoutEncryptedLeft = getPx(86);
            }
        } else {
            if (isRtl) {
                if (description.isMine()) {
                    layoutTitleLeft = timeWidth + getPx(16) + getPx(16);
                } else {
                    layoutTitleLeft = timeWidth + getPx(12);
                }
                layoutTitleWidth = getMeasuredWidth() - layoutTitleLeft - getPx(80);
            } else {
                layoutTitleLeft = getPx(82);
                if (description.isMine()) {
                    layoutTitleWidth = getMeasuredWidth() - layoutTitleLeft - timeWidth - getPx(24) - getPx(12);
                } else {
                    layoutTitleWidth = getMeasuredWidth() - layoutTitleLeft - timeWidth - getPx(12);
                }
            }
        }

        layoutMainTop = getPx(44);
        layoutMainWidth = getMeasuredWidth() - getPx(78 + 8);
        if (isRtl) {
            layoutMainLeft = getMeasuredWidth() - layoutMainWidth - getPx(80);
            if (layoutMarkWidth != 0) {
                layoutMainLeft += layoutMarkWidth + getPx(8);
                layoutMainWidth -= layoutMarkWidth + getPx(8);
            }
        } else {
            layoutMainLeft = getPx(82);
            if (layoutMarkWidth != 0) {
                layoutMainWidth -= layoutMarkWidth + getPx(8);
            }
        }

        // Building text layouts
        {
            String str = body.replace("\n", " ");

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
            if (description.getSenderId() == currentUserUid) {
                String name = getResources().getString(R.string.st_dialog_you);
                nameLength = name.length();
                str = name + ": " + str;
            } else {
                if (isGroup) {
                    User user = description.getSender();
                    nameLength = user.getFirstName().length();
                    str = user.getFirstName() + ": " + str;
                }
            }

            String preSequence = TextUtils.ellipsize(str, bodyTextPaint, layoutMainWidth, TextUtils.TruncateAt.END).toString();
            Spannable sequence = application.getEmojiProcessor().processEmojiCutMutable(preSequence, EmojiProcessor.CONFIGURATION_DIALOGS);

            if (nameLength != 0) {
                sequence.setSpan(new ForegroundColorSpan(HIGHLIGHT_COLOR), 0, nameLength, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            }

            CharSequence resSequence = TextUtils.ellipsize(sequence, bodyTextPaint, layoutMainWidth, TextUtils.TruncateAt.END);
            bodyLayout = new StaticLayout(resSequence, bodyTextPaint, layoutMainWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        }

        // Title
        {
            String wTitle = title.replace("\n", " ");
            if (wTitle.length() > 150) {
                wTitle = wTitle.substring(150) + "...";
            }
            TextPaint paint = isEncrypted ? titleEncryptedPaint : (isHighlighted ? titleHighlightPaint : titlePaint);
            Spannable preSequence = application.getEmojiProcessor().processEmojiCutMutable(wTitle, EmojiProcessor.CONFIGURATION_DIALOGS);
            CharSequence sequence = TextUtils.ellipsize(preSequence, paint, layoutTitleWidth, TextUtils.TruncateAt.END);
            titleLayout = new StaticLayout(sequence, paint, layoutTitleWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        }

        avatarRect.set(layoutAvatarLeft, layoutAvatarTop, layoutAvatarLeft + getPx(64), layoutAvatarTop + getPx(64));
    }

    private void bound(Drawable src, int x, int y) {
        src.setBounds(x, y, x + src.getIntrinsicWidth(), y + src.getIntrinsicHeight());
    }

    private void updateTyping() {
        needNewUpdateTyping = false;
        if (description.getPeerType() == PeerType.PEER_USER || description.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
            if (userTypes) {
                typingLayout = new StaticLayout(getResources().getString(R.string.lang_common_typing), bodyHighlightPaint, layoutMainWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            } else {
                typingLayout = null;
            }
        } else {
            if (typingUids != null && typingUids.length != 0) {
                String[] names = new String[typingUids.length];
                for (int i = 0; i < names.length; i++) {
                    names[i] = application.getEngine().getUserRuntime(typingUids[i]).getFirstName();
                }
                typingLayout = new StaticLayout(TextUtil.formatTyping(names), bodyHighlightPaint, layoutMainWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            } else {
                typingLayout = null;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (description == null) {
            return;
        }

        if (needNewUpdateTyping) {
            updateTyping();
        }

        if (isEncrypted) {
            bounds(secureIcon, layoutEncryptedLeft, layoutEncryptedTop);
            secureIcon.draw(canvas);
        }

        canvas.save();
        canvas.translate(layoutTitleLeft, layoutTitleTop);
        titleLayout.draw(canvas);
        canvas.restore();

        if (state != MessageState.FAILURE && description.getSenderId() == currentUserUid) {
            switch (state) {
                default:
                case MessageState.PENDING:
                    bound(statePending, layoutStateLeft, layoutStateTop);
                    statePending.draw(canvas);
                    break;
                case MessageState.SENT:
                    bound(stateSent, layoutStateLeft, layoutStateTop);
                    stateSent.draw(canvas);
                    break;
                case MessageState.READED:
                    bound(stateSent, layoutStateLeftDouble, layoutStateTop);
                    stateSent.draw(canvas);
                    bound(stateHalfCheck, layoutStateLeft, layoutStateTop);
                    stateHalfCheck.draw(canvas);
                    break;
            }
        }

        canvas.drawText(time, layoutTimeLeft, layoutTimeTop, isUnreadOut ? unreadClockPaint : readClockPaint);

        if (typingLayout != null) {
            canvas.save();
            canvas.translate(layoutMainLeft, layoutMainTop);
            typingLayout.draw(canvas);
            canvas.restore();
        } else {
            canvas.save();
            canvas.translate(layoutMainLeft, layoutMainTop);
            bodyLayout.draw(canvas);
            canvas.restore();
        }

        if (avatar != null) {
            canvas.drawBitmap(avatar, layoutAvatarLeft, layoutAvatarTop, avatarPaint);
        } else {
            canvas.drawRoundRect(avatarRect, getPx(2), getPx(2), avatarBgPaint);
            canvas.drawBitmap(empty, layoutAvatarLeft, layoutAvatarTop, avatarPaint);
        }

        if (description.isErrorState() ||
                (state == MessageState.FAILURE && description.getSenderId() == currentUserUid)) {
            bound(stateFailure, layoutMarkLeft, layoutMarkTop);
            stateFailure.draw(canvas);
        } else if (unreadCount > 0) {
            canvas.drawRoundRect(layoutMarkRect, layoutMarkRadius, layoutMarkRadius, counterPaint);
            canvas.drawText(unreadCountText, layoutMarkLeft + layoutMarkTextLeft, layoutMarkTextTop, counterTitlePaint);
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
}
