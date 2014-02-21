package org.telegram.android.core;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;
import org.telegram.android.R;
import org.telegram.android.StartActivity;
import org.telegram.android.TelegramApplication;
import org.telegram.android.config.NotificationSettings;
import org.telegram.android.core.model.ChatMessage;
import org.telegram.android.core.model.MessageState;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.log.Logger;
import org.telegram.android.preview.AvatarView;
import org.telegram.android.screens.RootControllerHolder;
import org.telegram.android.ui.Placeholders;
import org.telegram.tl.TLObject;

import java.util.Random;

/**
 * Author: Korshakov Stepan
 * Created: 01.08.13 9:43
 */
public class Notifications {

    private class NotificationConfig {
        public boolean useSound;
        public boolean useNotification;
        public boolean useInAppNotification;
        public boolean useVibration;
        public boolean useCustomSound;
        public String customSoundUri;
    }

    private static final String TAG = "Notificagtions";

    private static final long QUITE_PERIOD = 300;
    private static final long IN_APP_TIMEOUT = 3000;

    private static final int MAX_SENDER_LENGTH = 100;
    private static final int MAX_MESSAGE_LENGTH = 200;

    private static final long[] VIBRATE_PATTERN = new long[]{0, 200};
    private static final int NOTIFICATION_MESSAGE = 0;
    private static final int NOTIFICATION_SYSTEM = 1;

    private TelegramApplication application;
    private NotificationManager manager;

    private int lastNotifiedMid = -1;
    private long lastNotifiedTime = -1;

    private int lastPeerId;
    private int lastPeerType;

    private Random rnd = new Random();

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Logger.d(TAG, "notify");
            hideInApp();
        }
    };

    private SoundPool pool;
    private int soundId;

    private View notificationView;
    private Activity notificationActivity;
    private WindowManager windowManager;

    public Notifications(TelegramApplication application) {
        this.application = application;
        this.windowManager = (WindowManager) application.getSystemService(Context.WINDOW_SERVICE);
        this.manager = (NotificationManager) application.getSystemService(Context.NOTIFICATION_SERVICE);
        this.pool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
        this.soundId = this.pool.load(application, R.raw.message, 0);
    }

    public void onActivityPaused() {
        hideInAppNow();
    }

    public void hideInApp() {
        if (notificationActivity != null) {
            AlphaAnimation alpha = new AlphaAnimation(1.0F, 0.0f);
            alpha.setDuration(250);
            alpha.setFillAfter(true);
            notificationView.findViewById(R.id.mainContainer).startAnimation(alpha);
            final View view = notificationView;
            notificationView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    windowManager.removeView(view);
                }
            }, 300);
            notificationView = null;
            notificationActivity = null;
            Logger.d(TAG, "Hide in-app");
        }
    }

    public void hideInAppNow() {
        if (notificationActivity != null) {
            windowManager.removeView(notificationView);
            notificationView = null;
            notificationActivity = null;
            Logger.d(TAG, "Hide in-app fast");
        }
    }

    public synchronized void onNewSystemMessage(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(application);
        builder.setSmallIcon(R.drawable.app_notify);
        builder.setTicker(message);
        builder.setContentTitle("Telegram");
        builder.setContentText(message);
        builder.setContentIntent(
                PendingIntent.getActivity(application, 0, new Intent().setClass(application, StartActivity.class), 0));
        manager.notify(NOTIFICATION_SYSTEM, builder.build());
    }

    public void onAuthUnrecognized(String deviceName) {
        onNewSystemMessage(application.getString(R.string.st_notification_new_login).replace("{device}", deviceName));
    }

    public void onAuthUnrecognized(String deviceName, String loc) {
        onNewSystemMessage(application.getString(R.string.st_notification_new_login_location)
                .replace("{device}", deviceName)
                .replace("{location}", loc));
    }

    // Private chats

    public void onNewMessageGeo(String senderTitle, int uid, int mid, TLObject photo) {
        notifyMessage(mid, senderTitle, uid,
                application.getString(R.string.st_notification_sent_map)
                        .replace("{name}", senderTitle),
                PeerType.PEER_USER, uid, photo);
    }

    public void onNewMessageDoc(String senderTitle, int uid, int mid, TLObject photo) {
        notifyMessage(mid, senderTitle, uid,
                application.getString(R.string.st_notification_sent_document)
                        .replace("{name}", senderTitle),
                PeerType.PEER_USER, uid, photo);
    }

    public void onNewMessageAudio(String senderTitle, int uid, int mid, TLObject photo) {
        notifyMessage(mid, senderTitle, uid,
                application.getString(R.string.st_notification_sent_audio)
                        .replace("{name}", senderTitle),
                PeerType.PEER_USER, uid, photo);
    }

    public void onNewMessageContact(String senderTitle, int uid, int mid, TLObject photo) {
        notifyMessage(mid, senderTitle, uid,
                application.getString(R.string.st_notification_sent_contact)
                        .replace("{name}", senderTitle)
                , PeerType.PEER_USER, uid, photo);
    }

    public void onNewMessageVideo(String senderTitle, int uid, int mid, TLObject photo) {
        notifyMessage(mid, senderTitle, uid,
                application.getString(R.string.st_notification_sent_video)
                        .replace("{name}", senderTitle)
                , PeerType.PEER_USER, uid, photo);
    }

    public void onNewMessagePhoto(String senderTitle, int uid, int mid, TLObject photo) {
        notifyMessage(mid, senderTitle, uid,
                application.getString(R.string.st_notification_sent_photo)
                        .replace("{name}", senderTitle)
                , PeerType.PEER_USER, uid, photo);
    }

    public void onNewMessage(String senderTitle, String message, int uid, int mid, TLObject photo) {
        notifyMessage(mid, senderTitle, uid, message, PeerType.PEER_USER, uid, photo);
    }

    public void onNewMessageJoined(String senderTitle, int uid, int mid, TLObject photo) {
        notifyMessage(mid, senderTitle, uid,
                application.getString(R.string.st_notification_joined)
                        .replace("{name}", senderTitle), PeerType.PEER_USER, uid, photo);
    }

    // Group chats

    public void onNewChatMessageGeo(String senderTitle, int senderId, String chatTitle, int chatId, int mid, TLObject photo) {
        notifyMessage(mid, senderTitle, senderId,
                application.getString(R.string.st_notification_group_sent_map)
                        .replace("{name}", senderTitle)
                        .replace("{chat}", chatTitle), PeerType.PEER_CHAT, chatId, photo);
    }

    public void onNewChatMessageDoc(String senderTitle, int senderId, String chatTitle, int chatId, int mid, TLObject photo) {
        notifyMessage(mid, senderTitle, senderId,
                application.getString(R.string.st_notification_group_sent_document)
                        .replace("{name}", senderTitle)
                        .replace("{chat}", chatTitle), PeerType.PEER_CHAT, chatId, photo);
    }

    public void onNewChatMessageAudio(String senderTitle, int senderId, String chatTitle, int chatId, int mid, TLObject photo) {
        notifyMessage(mid, senderTitle, senderId,
                application.getString(R.string.st_notification_group_sent_audio)
                        .replace("{name}", senderTitle)
                        .replace("{chat}", chatTitle), PeerType.PEER_CHAT, chatId, photo);
    }

    public void onNewChatMessageContact(String senderTitle, int senderId, String chatTitle, int chatId, int mid, TLObject photo) {
        notifyMessage(mid, senderTitle, senderId, application.getString(R.string.st_notification_group_sent_contact)
                .replace("{name}", senderTitle)
                .replace("{chat}", chatTitle), PeerType.PEER_CHAT, chatId, photo);
    }

    public void onNewChatMessageVideo(String senderTitle, int senderId, String chatTitle, int chatId, int mid, TLObject photo) {
        notifyMessage(mid, senderTitle, senderId, application.getString(R.string.st_notification_group_sent_video)
                .replace("{name}", senderTitle)
                .replace("{chat}", chatTitle), PeerType.PEER_CHAT, chatId, photo);
    }

    public void onNewChatMessagePhoto(String senderTitle, int senderId, String chatTitle, int chatId, int mid, TLObject photo) {
        notifyMessage(mid, senderTitle, senderId, application.getString(R.string.st_notification_group_sent_photo)
                .replace("{name}", senderTitle)
                .replace("{chat}", chatTitle), PeerType.PEER_CHAT, chatId, photo);
    }

    public void onNewChatMessage(String senderTitle, int senderId, String chatTitle, String message, int chatId, int mid, TLObject photo) {
        notifyMessage(mid, senderTitle + "@" + chatTitle, senderId, message, PeerType.PEER_CHAT, chatId, photo);
    }

    // Secret chats

    public void onNewSecretMessage(String senderTitle, int senderId, int chatId, TLObject photo) {
        notifyMessage(0, senderTitle, senderId, application.getString(R.string.st_notification_secret_sent_message),
                PeerType.PEER_USER_ENCRYPTED, chatId, photo);
    }

    public void onNewSecretChatRequested(String senderTitle, int senderId, int chatId, TLObject photo) {
        notifyMessage(0, senderTitle, senderId, application.getString(R.string.st_notification_secret_requested),
                PeerType.PEER_USER_ENCRYPTED, chatId, photo);
    }

    public void onNewSecretChatEstablished(String senderTitle, int senderId, int chatId, TLObject photo) {
        notifyMessage(0, senderTitle, senderId, application.getString(R.string.st_notification_secret_created),
                PeerType.PEER_USER_ENCRYPTED, chatId, photo);
    }

    public void onNewSecretChatCancelled(String senderTitle, int senderId, int chatId, TLObject photo) {
        notifyMessage(0, senderTitle, senderId, application.getString(R.string.st_notification_secret_cancelled),
                PeerType.PEER_USER_ENCRYPTED, chatId, photo);
    }

    public void onNewSecretMessageVideo(String senderTitle, int senderId, int chatId, TLObject photo) {
        notifyMessage(0, senderTitle, senderId, application.getString(R.string.st_notification_secret_sent_video),
                PeerType.PEER_USER_ENCRYPTED, chatId, photo);
    }

    public void onNewSecretMessagePhoto(String senderTitle, int senderId, int chatId, TLObject photo) {
        notifyMessage(0, senderTitle, senderId, application.getString(R.string.st_notification_secret_sent_photo),
                PeerType.PEER_USER_ENCRYPTED, chatId, photo);
    }

    public void onNewSecretMessageGeo(String senderTitle, int senderId, int chatId, TLObject photo) {
        notifyMessage(0, senderTitle, senderId, application.getString(R.string.st_notification_secret_sent_map),
                PeerType.PEER_USER_ENCRYPTED, chatId, photo);
    }

    public void onNewSecretMessageDoc(String senderTitle, int senderId, int chatId, TLObject photo) {
        notifyMessage(0, senderTitle, senderId, application.getString(R.string.st_notification_secret_sent_doc),
                PeerType.PEER_USER_ENCRYPTED, chatId, photo);
    }

    public void onNewSecretMessageAudio(String senderTitle, int senderId, int chatId, TLObject photo) {
        notifyMessage(0, senderTitle, senderId, application.getString(R.string.st_notification_secret_sent_audio),
                PeerType.PEER_USER_ENCRYPTED, chatId, photo);
    }

    private void notifyApp(final NotificationConfig config, String senderTitle, final int senderId, String message, final int peerType, final int peerId, final TLObject photo) {

        if (senderTitle.length() > MAX_SENDER_LENGTH) {
            senderTitle = senderTitle.substring(MAX_SENDER_LENGTH) + "...";
        }

        if (message.length() > MAX_MESSAGE_LENGTH) {
            message = message.substring(MAX_MESSAGE_LENGTH) + "...";
        }

        message = application.getEmojiProcessor().fixStringCompat(message);

        if (config.useNotification) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(application);
            builder.setSmallIcon(R.drawable.app_notify);

            builder.setTicker(senderTitle + ": " + message);

            builder.setContentTitle(senderTitle);
            builder.setContentText(message);

            Intent intent = new Intent(StartActivity.ACTION_OPEN_CHAT);
            intent.setClass(application, StartActivity.class);
            intent.putExtra("peerType", peerType);
            intent.putExtra("peerId", peerId);
            builder.setContentIntent(PendingIntent.getActivity(application, rnd.nextInt(), intent, 0));
            Bitmap bigPhoto = null;
            if (photo != null) {
//                if (photo instanceof TLLocalAvatarPhoto) {
//                    TLLocalAvatarPhoto profilePhoto = (TLLocalAvatarPhoto) photo;
//                    if (profilePhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
//                        bigPhoto = application.getImageController().addTask(new StelsImageTask((TLLocalFileLocation) profilePhoto.getPreviewLocation()));
//                    }
//                }
                // TODO: Implement
            }

            if (bigPhoto == null) {
                if (peerType == PeerType.PEER_USER || peerType == PeerType.PEER_USER_ENCRYPTED) {
                    BitmapDrawable drawable = (BitmapDrawable)
                            application.getResources().getDrawable(Placeholders.getUserPlaceholder(Math.abs(senderId)));
                    bigPhoto = drawable.getBitmap();
                } else {
                    BitmapDrawable drawable = (BitmapDrawable)
                            application.getResources().getDrawable(Placeholders.getGroupPlaceholder(Math.abs(peerId)));
                    bigPhoto = drawable.getBitmap();
                }
            }
            builder.setLargeIcon(bigPhoto);

            if (peerType == PeerType.PEER_USER || peerType == PeerType.PEER_USER_ENCRYPTED) {
                builder.setLights(Placeholders.USER_PLACEHOLDERS_COLOR[Math.abs(senderId) % Placeholders.USER_PLACEHOLDERS_COLOR.length], 1500, 1500);
            } else {
                builder.setLights(Placeholders.GROUP_PLACEHOLDERS_COLOR[Math.abs(peerId) % Placeholders.GROUP_PLACEHOLDERS_COLOR.length], 1500, 1500);
            }

            int defaults = 0;

            if (config.useSound) {
                if (config.useCustomSound) {
                    builder.setSound(Uri.parse(config.customSoundUri));
                } else {
                    defaults = defaults | Notification.DEFAULT_SOUND;
                }
            }

            if (config.useVibration) {
                builder.setVibrate(VIBRATE_PATTERN);
            }

            builder.setDefaults(defaults);

            manager.notify(NOTIFICATION_MESSAGE, builder.build());
        } else {

            if (config.useSound) {
                if (config.useInAppNotification) {
                    Uri soundUri;
                    if (config.useCustomSound) {
                        soundUri = Uri.parse(config.customSoundUri);
                    } else {
                        soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    }

                    try {
                        Ringtone r = RingtoneManager.getRingtone(application, soundUri);
                        r.play();
                    } catch (Exception e) {
                    }

                } else {
                    pool.play(soundId, 1, 1, 1, 0, 1);
                }
            }
            if (config.useVibration) {
                Vibrator mVibrator = (Vibrator) application.getSystemService(Context.VIBRATOR_SERVICE);
                mVibrator.vibrate(VIBRATE_PATTERN, -1);
            }

            if (config.useInAppNotification) {
                final String finalMessage = message;
                final String finalSenderTitle = senderTitle;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        final Activity activity = application.getUiKernel().getVisibleActivity();
                        if (activity == null) {
                            return;
                        }

                        boolean needAdd = false;
                        if (notificationView == null) {
                            notificationView = activity.getLayoutInflater().inflate(R.layout.notification_inapp, null);
                            notificationView.findViewById(R.id.closeButton).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    hideInApp();
                                }
                            });
                            notificationActivity = activity;
                            needAdd = true;
                        }

                        ((TextView) notificationView.findViewById(R.id.name)).setText(finalSenderTitle);
                        ((TextView) notificationView.findViewById(R.id.title)).setText(finalMessage);

                        AvatarView avatarImage = (AvatarView) notificationView.findViewById(R.id.avatar);
                        if (peerType == PeerType.PEER_USER) {
                            avatarImage.setEmptyDrawable(Placeholders.getUserPlaceholder(peerId));
                            ((TextView) notificationView.findViewById(R.id.name)).setTextColor(Placeholders.getUserTitleColor(peerId));
                            ((TextView) notificationView.findViewById(R.id.name)).setCompoundDrawables(null, null, null, null);
                        } else if (peerType == PeerType.PEER_CHAT) {
                            avatarImage.setEmptyDrawable(Placeholders.getGroupPlaceholder(peerId));
                            ((TextView) notificationView.findViewById(R.id.name)).setTextColor(Placeholders.getGroupTitleColor(peerId));
                            ((TextView) notificationView.findViewById(R.id.name)).setCompoundDrawables(null, null, null, null);
                        } else {
                            avatarImage.setEmptyDrawable(Placeholders.getUserPlaceholder(senderId));
                            ((TextView) notificationView.findViewById(R.id.name)).setTextColor(0xff67b540);
                            ((TextView) notificationView.findViewById(R.id.name)).setCompoundDrawablesWithIntrinsicBounds(R.drawable.st_dialogs_lock, 0, 0, 0);
                        }

                        notificationView.findViewById(R.id.container).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ((RootControllerHolder) activity).getRootController().openDialog(peerType, peerId);
                                hideInApp();
                            }
                        });

                        avatarImage.requestAvatar(null);
                        if (photo != null) {
                            if (photo instanceof TLLocalAvatarPhoto) {
                                TLLocalAvatarPhoto profilePhoto = (TLLocalAvatarPhoto) photo;
                                if (profilePhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                                    avatarImage.requestAvatar(profilePhoto.getPreviewLocation());
                                } else {
                                    avatarImage.requestAvatar(null);
                                }
                            } else {
                                avatarImage.requestAvatar(null);
                            }
                        } else {
                            avatarImage.requestAvatar(null);
                        }


                        if (needAdd) {
                            AlphaAnimation alpha = new AlphaAnimation(0.0F, 1.0f);
                            alpha.setDuration(250);
                            alpha.setFillAfter(true);
                            notificationView.setFocusable(false);
                            notificationView.findViewById(R.id.mainContainer).setFocusable(false);
                            notificationView.findViewById(R.id.mainContainer).startAnimation(alpha);

                            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
                            params.height = (int) (application.getResources().getDisplayMetrics().density * 48);
                            params.format = PixelFormat.TRANSLUCENT;
                            params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
                            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
                            params.gravity = Gravity.CLIP_HORIZONTAL | Gravity.TOP;

                            windowManager.addView(notificationView, params);
                        }

                        handler.removeMessages(0);
                        handler.sendEmptyMessageDelayed(0, IN_APP_TIMEOUT);
                    }
                });
            }
        }
    }

    private synchronized void notifyMessage(int mid, String senderTitle, int senderId, String message, int peerType, int peerId, TLObject photo) {
        if (mid <= lastNotifiedMid && mid != 0) {
            Logger.d(TAG, "Ignoring old message");
            return;
        }

        ChatMessage msg = application.getEngine().getMessagesEngine().getMessageByMid(mid);
        if (msg != null) {
            if (msg.isOut()) {
                Logger.d(TAG, "Ignoring out message");
                return;
            }

            if (msg.getState() == MessageState.READED) {
                Logger.d(TAG, "Ignoring readed message");
                return;
            }
        }

        if (mid != 0) {
            lastNotifiedMid = mid;
        }

        lastPeerId = peerId;
        lastPeerType = peerType;

        NotificationSettings settings = application.getNotificationSettings();

        if (!settings.isEnabled()) {
            Logger.d(TAG, "Notifications disabled");
            return;
        }


        if (peerType == PeerType.PEER_USER || peerType == PeerType.PEER_USER_ENCRYPTED) {
            if (!settings.isEnabledForUser(senderId)) {
                Logger.d(TAG, "Notifications disabled for user");
                return;
            }
            if (senderId == application.getCurrentUid()) {
                return;
            }
        } else {
            if (!settings.isGroupEnabled()) {
                Logger.d(TAG, "Group notifications disabled");
                return;
            }

            if (!settings.isEnabledForChat(peerId)) {
                Logger.d(TAG, "Notifications disabled for chat");
                return;
            }
        }

        NotificationConfig config = new NotificationConfig();

        if (peerType == PeerType.PEER_USER || peerType == PeerType.PEER_USER_ENCRYPTED) {
            config.useVibration = settings.isMessageVibrationEnabled();
            config.useSound = settings.isMessageSoundEnabled();
            if (settings.getUserNotificationSound(senderId) != null) {
                config.customSoundUri = settings.getUserNotificationSound(senderId);
                config.useCustomSound = false;
            } else {
                config.customSoundUri = settings.getNotificationSound();
                config.useCustomSound = config.customSoundUri != null;
            }
        } else {
            config.useVibration = settings.isGroupVibrateEnabled();
            config.useSound = settings.isGroupSoundEnabled();

            if (settings.getChatNotificationSound(peerId) != null) {
                config.customSoundUri = settings.getChatNotificationSound(peerId);
                config.useCustomSound = true;
            } else if (settings.getNotificationGroupSound() != null) {
                config.customSoundUri = settings.getNotificationGroupSound();
                config.useCustomSound = true;
            } else {
                config.customSoundUri = settings.getNotificationSound();
                config.useCustomSound = config.customSoundUri != null;
            }
        }

        boolean isConversationVisible = false;

        if (application.getUiKernel().isAppVisible()) {
            config.useSound = config.useSound & settings.isInAppSoundsEnabled();
            config.useVibration = config.useVibration & settings.isInAppVibrateEnabled();
            if (application.getUiKernel().getOpenedChatPeerType() == peerType && application.getUiKernel().getOpenedChatPeerId() == peerId || application.getUiKernel().isDialogsVisible()) {
                config.useNotification = false;
                config.useInAppNotification = false;
                isConversationVisible = true;
            } else {
                config.useNotification = false;
                config.useInAppNotification = settings.isInAppPreviewEnabled();
            }
        } else {
            config.useNotification = true;
            config.useInAppNotification = false;
        }

        if (SystemClock.uptimeMillis() - lastNotifiedTime < QUITE_PERIOD && !isConversationVisible) {
            config.useVibration = false;
            config.useSound = false;
        } else {
            lastNotifiedTime = SystemClock.uptimeMillis();
        }

        Logger.d(TAG, "Performing notification");
        notifyApp(config, senderTitle, senderId, message, peerType, peerId, photo);
    }

    public void hideChatNotifications(int peerType, int peerId) {
        if (lastPeerType == peerType && lastPeerId == peerId) {
            manager.cancel(NOTIFICATION_MESSAGE);
        }
    }

    public void hideAllNotifications() {
        manager.cancel(NOTIFICATION_MESSAGE);
        manager.cancel(NOTIFICATION_SYSTEM);
    }

    public void reset() {
        hideAllNotifications();
        lastNotifiedMid = 0;
        lastNotifiedTime = 0;
    }
}