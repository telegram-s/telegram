package org.telegram.android.core;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.support.v4.text.BidiFormatter;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;
import org.telegram.android.R;
import org.telegram.android.StartActivity;
import org.telegram.android.TelegramApplication;
import org.telegram.android.app.NotificationRepeat;
import org.telegram.android.config.NotificationSettings;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.media.*;
import org.telegram.android.core.model.notifications.TLNotificationRecord;
import org.telegram.android.core.model.notifications.TLNotificationState;
import org.telegram.android.core.model.service.*;
import org.telegram.android.critical.TLPersistence;
import org.telegram.android.log.Logger;
import org.telegram.android.media.Optimizer;
import org.telegram.android.preview.AvatarView;
import org.telegram.android.screens.RootControllerHolder;
import org.telegram.android.ui.Placeholders;
import org.telegram.android.ui.TextUtil;
import org.telegram.android.ui.UiMeasure;
import org.telegram.i18n.I18nUtil;
import org.telegram.tl.TLObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Author: Korshakov Stepan
 * Created: 01.08.13 9:43
 */
public class Notifications {

    private class NotificationConfig {
        public boolean useSound;
        public boolean useVibration;
        public boolean useCustomSound;
        public String customSoundUri;
        public int ledColor;
    }

    private static final String TAG = "Notificagtions";

    private static final long QUITE_PERIOD = 300;
    private static final long IN_APP_TIMEOUT = 3000;

    private static final int MAX_MESSAGE_LENGTH = 200;

    private static final long[] VIBRATE_PATTERN = new long[]{0, 200};
    private static final int NOTIFICATION_MESSAGE = 0;
    private static final int NOTIFICATION_SYSTEM = 1;

    private static final int NOTIFICATION_REPEAT = 3 * 60 * 60 * 1000;// 3 hrs

    private static final int MAX_SIZE = 5;

    private TelegramApplication application;
    private NotificationManager manager;

    private long lastNotifiedTime = -1;
    private long lastSentNotifiedTime = -1;

    private Random rnd = new Random();

    private NotificationRecord[] lastRecords = new NotificationRecord[0];

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Logger.d(TAG, "notify");
            hideInApp();
        }
    };

    private SoundPool pool;
    private int soundId;
    private int soundSentId;

    private View notificationView;
    private Activity notificationActivity;
    private WindowManager windowManager;

    private TLPersistence<TLNotificationState> persistence;
    private int unreadMessages;

    public Notifications(TelegramApplication application) {
        this.application = application;
        this.windowManager = (WindowManager) application.getSystemService(Context.WINDOW_SERVICE);
        this.manager = (NotificationManager) application.getSystemService(Context.NOTIFICATION_SERVICE);
        this.pool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
        this.soundId = this.pool.load(application, R.raw.message, 0);
        this.soundSentId = this.pool.load(application, R.raw.sent, 0);
        this.persistence = new TLPersistence<TLNotificationState>(application, "notifications.tl", TLNotificationState.class, TLLocalContext.getInstance());
        loadPersistence();
    }

    private void savePersistence() {
        persistence.getObj().getRecords().clear();
        for (int i = 0; i < lastRecords.length; i++) {
            persistence.getObj().getRecords().add(
                    new TLNotificationRecord(
                            lastRecords[i].getPeerType(),
                            lastRecords[i].getPeerId(),
                            lastRecords[i].getSender().getUid(),
                            lastRecords[i].getContentMessage(),
                            lastRecords[i].getContentShortMessage())
            );
        }
        persistence.getObj().setUnreadCount(unreadMessages);
        persistence.write();
    }

    private void loadPersistence() {
        List<TLNotificationRecord> r = persistence.getObj().getRecords();

        ArrayList<NotificationRecord> nRecords = new ArrayList<NotificationRecord>();

        for (int i = 0; i < lastRecords.length; i++) {
            TLNotificationRecord pr = r.get(i);
            NotificationRecord record = new NotificationRecord();
            record.setPeerId(pr.getPeerId());
            record.setPeerType(pr.getPeerType());
            record.setContentMessage(pr.getContentMessage());
            record.setContentShortMessage(pr.getContentShortMessage());

            record.setSender(application.getEngine().getUser(pr.getSenderId()));
            if (record.getSender() == null) {
                continue;
            }
            if (record.getPeerType() == PeerType.PEER_CHAT) {
                record.setGroup(application.getEngine().getGroupsEngine().getGroup(record.getPeerId()));
                if (record.getGroup() == null) {
                    continue;
                }
            }
            if (record.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                record.setSecretChat(application.getEngine().getSecretEngine().loadChat(record.getPeerId()));
                if (record.getSecretChat() == null) {
                    continue;
                }
            }
            nRecords.add(record);
        }
        lastRecords = nRecords.toArray(new NotificationRecord[0]);
        unreadMessages = persistence.getObj().getUnreadCount();
    }

    public void notifySent(int peerType, int peerId) {
        if (peerType == application.getUiKernel().getOpenedChatPeerType() &&
                peerId == application.getUiKernel().getOpenedChatPeerId()) {
            if (SystemClock.uptimeMillis() - lastSentNotifiedTime > QUITE_PERIOD) {
                pool.play(soundSentId, 1, 1, 1, 0, 1);
                lastSentNotifiedTime = SystemClock.uptimeMillis();
            }
        }
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

    public void renotify() {
        if (application.getUiKernel().isAppVisible()) {
            return;
        }
        performNotify(lastRecords);
    }

    private void onNewNotifications(NotificationRecord... msgs) {
        if (msgs.length == 0) {
            return;
        }

        NotificationSettings settings = application.getNotificationSettings();
        if (!settings.isEnabled()) {
            return;
        }

        ArrayList<NotificationRecord> filtered = new ArrayList<NotificationRecord>();
        for (NotificationRecord msg : msgs) {
            if (msg.getPeerType() == PeerType.PEER_USER || msg.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                if (!settings.isEnabledForUser(msg.getPeerId())) {
                    Logger.d(TAG, "Notifications disabled for user");
                    continue;
                }
            } else if (msg.getPeerType() == PeerType.PEER_CHAT) {
                if (!settings.isGroupEnabled()) {
                    Logger.d(TAG, "Group notifications disabled");
                    continue;
                }

                if (!settings.isEnabledForChat(msg.getPeerId())) {
                    Logger.d(TAG, "Notifications disabled for chat");
                    continue;
                }

                if (!settings.isEnabledForUser(msg.getSender().getUid())) {
                    Logger.d(TAG, "Notifications disabled for user");
                    continue;
                }
            } else {
                continue;
            }

            filtered.add(msg);

            if (lastRecords.length < MAX_SIZE) {
                NotificationRecord[] nRecords = new NotificationRecord[lastRecords.length + 1];
                nRecords[0] = msg;
                for (int i = 0; i < lastRecords.length; i++) {
                    nRecords[i + 1] = lastRecords[i];
                }
                lastRecords = nRecords;
            } else {
                boolean cleaned = false;
                for (int i = lastRecords.length - 1; i > 0; i--) {
                    if (lastRecords[i].getPeerType() == lastRecords[i - 1].getPeerType() &&
                            lastRecords[i].getPeerId() == lastRecords[i - 1].getPeerId()) {
                        cleaned = true;
                        for (int j = i; j > 0; j--) {
                            lastRecords[j] = lastRecords[j - 1];
                        }
                        break;
                    }
                }
                if (!cleaned) {
                    for (int j = lastRecords.length - 1; j > 0; j--) {
                        lastRecords[j] = lastRecords[j - 1];
                    }
                }
                lastRecords[0] = msg;
            }
        }

        savePersistence();

        performNotify(filtered.toArray(new NotificationRecord[0]));
    }

    private synchronized void performNotify(NotificationRecord[] newNotifications) {
        if (newNotifications.length == 0) {
            return;
        }

        NotificationSettings settings = application.getNotificationSettings();

        NotificationConfig inAppConfig = null;
        NotificationConfig systemConfig = null;
        NotificationConfig inChatConfig = null;

        boolean isSilent = false;
        if (SystemClock.uptimeMillis() - lastNotifiedTime < QUITE_PERIOD) {
            isSilent = true;
        } else {
            lastNotifiedTime = SystemClock.uptimeMillis();
        }

        for (NotificationRecord msg : newNotifications) {
            int peerType = msg.getPeerType();
            int peerId = msg.getPeerId();
            int senderId = msg.getSender().getUid();

            NotificationConfig config = new NotificationConfig();
            if (peerType == PeerType.PEER_USER || peerType == PeerType.PEER_USER_ENCRYPTED) {
                config.useVibration = settings.isMessageVibrationEnabled();
                config.useSound = settings.isMessageSoundEnabled();

                if (settings.getCustomUserColor(senderId) != NotificationSettings.LED_DEFAULT) {
                    config.ledColor = getLedColor(senderId, settings.getCustomUserColor(senderId), 0);
                } else {
                    config.ledColor = getLedColor(senderId, settings.getLedMode(), 0);
                }

                if (settings.getUserNotificationSound(senderId) != null) {
                    config.customSoundUri = settings.getUserNotificationSound(senderId);
                    config.useCustomSound = true;
                } else {
                    config.customSoundUri = settings.getNotificationSound();
                    config.useCustomSound = config.customSoundUri != null;
                }
            } else {
                config.useVibration = settings.isGroupVibrateEnabled();
                config.useSound = settings.isGroupSoundEnabled();
                if (settings.getCustomGroupColor(peerId) != NotificationSettings.LED_DEFAULT) {
                    config.ledColor = getLedColor(peerId, settings.getCustomGroupColor(peerId), settings.getLedMode());
                } else {
                    config.ledColor = getLedColor(peerId, settings.getLedGroupMode(), settings.getLedMode());
                }

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

            if (application.getUiKernel().isAppVisible()) {
                config.useSound = config.useSound & settings.isInAppSoundsEnabled();
                config.useVibration = config.useVibration & settings.isInAppVibrateEnabled();
                if (application.getUiKernel().getOpenedChatPeerType() == peerType && application.getUiKernel().getOpenedChatPeerId() == peerId || application.getUiKernel().isDialogsVisible()) {
                    inChatConfig = mergeConfigs(inChatConfig, config);
                } else {
                    inAppConfig = mergeConfigs(inAppConfig, config);
                }
            } else {
                systemConfig = mergeConfigs(systemConfig, config);
            }
        }

        if (isSilent) {
            if (inChatConfig != null) {
                inChatConfig.useVibration = false;
                inChatConfig.useSound = false;
            }

            if (inAppConfig != null) {
                inAppConfig.useVibration = false;
                inAppConfig.useSound = false;
            }

            if (systemConfig != null) {
                systemConfig.useVibration = false;
                systemConfig.useSound = false;
            }
        }

        if (inChatConfig != null) {
            performNotifyInChat(inChatConfig);
            if (inAppConfig != null) {
                inAppConfig.useVibration = inAppConfig.useVibration & !inChatConfig.useVibration;
                inAppConfig.useSound = inAppConfig.useSound & !inChatConfig.useSound;
            }
        }
        if (inAppConfig != null) {
            performNotifyInApp(lastRecords, inAppConfig);
        }
        if (systemConfig != null) {
            performNotifySystem(lastRecords, systemConfig);
            AlarmManager alarmManager = (AlarmManager) application.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + NOTIFICATION_REPEAT, PendingIntent.getService(application,
                    0, new Intent().setClass(application, NotificationRepeat.class), 0));
        }
    }

    private int getLedColor(int peerId, int mode, int baseMode) {
        switch (mode) {
            case NotificationSettings.LED_NONE:
                return 0;
            case NotificationSettings.LED_DEFAULT:
                if (baseMode != 0) {
                    return getLedColor(peerId, baseMode, 0);
                }
            case NotificationSettings.LED_COLORFUL:
            default:
                return Placeholders.getLedColor(peerId);
            case NotificationSettings.LED_CYAN:
                return Placeholders.CYAN_LED;
            case NotificationSettings.LED_PURPLE:
                return Placeholders.PURPLE_LED;
            case NotificationSettings.LED_PINK:
                return Placeholders.PINK_LED;
            case NotificationSettings.LED_ORANGE:
                return Placeholders.ORANGE_LED;
            case NotificationSettings.LED_YELLOW:
                return Placeholders.YELLOW_LED;
            case NotificationSettings.LED_BLUE:
                return Placeholders.BLUE_LED;
            case NotificationSettings.LED_GREEN:
                return Placeholders.GREEN_LED;
            case NotificationSettings.LED_RED:
                return Placeholders.RED_LED;
            case NotificationSettings.LED_WHITE:
                return Placeholders.WHITE_LED;
        }
    }

    private NotificationConfig mergeConfigs(NotificationConfig source, NotificationConfig config) {
        if (source == null) {
            source = config;
        } else {
            source.useSound |= config.useSound;
            source.useVibration |= config.useVibration;
            if (config.ledColor != 0) {
                if (source.ledColor == 0) {
                    source.ledColor = config.ledColor;
                } else if (source.ledColor != config.ledColor) {
                    source.ledColor = Placeholders.WHITE_LED;
                }
            }
            if (config.useCustomSound) {
                source.customSoundUri = config.customSoundUri;
                source.useCustomSound = true;
            }
        }
        return source;
    }

    private void performNotifyInApp(final NotificationRecord[] records, NotificationConfig config) {
        if (config.useSound) {
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
        }

        if (config.useVibration) {
            Vibrator mVibrator = (Vibrator) application.getSystemService(Context.VIBRATOR_SERVICE);
            mVibrator.vibrate(VIBRATE_PATTERN, -1);
        }

        final int peerType = records[0].getPeerType();
        final int peerId = records[0].getPeerId();
        final String finalMessage = records[0].getContentMessage();
        final String finalSenderTitle = records[0].getSenderTitle();
        final int senderId = records[0].getSender().getUid();
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
                    avatarImage.setEmptyUser(finalSenderTitle, peerId);
                    ((TextView) notificationView.findViewById(R.id.name)).setTextColor(Placeholders.getTitleColor(peerId));
                    ((TextView) notificationView.findViewById(R.id.name)).setCompoundDrawables(null, null, null, null);
                } else if (peerType == PeerType.PEER_CHAT) {
                    avatarImage.setEmptyGroup(finalSenderTitle, peerId);
                    ((TextView) notificationView.findViewById(R.id.name)).setTextColor(Placeholders.getTitleColor(peerId));
                    ((TextView) notificationView.findViewById(R.id.name)).setCompoundDrawables(null, null, null, null);
                } else if (peerType == PeerType.PEER_USER_ENCRYPTED) {
                    avatarImage.setEmptyUser(finalSenderTitle, senderId);
                    ((TextView) notificationView.findViewById(R.id.name)).setTextColor(0xff4aa152);
                    ((TextView) notificationView.findViewById(R.id.name)).setCompoundDrawablesWithIntrinsicBounds(R.drawable.st_dialogs_lock, 0, 0, 0);
                } else {
                    throw new UnsupportedOperationException("Unknown peer type: " + peerType);
                }

                notificationView.findViewById(R.id.container).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((RootControllerHolder) activity).getRootController().openDialog(peerType, peerId);
                        hideInApp();
                    }
                });

                TLAbsLocalAvatarPhoto photo = null;
                if (peerType == PeerType.PEER_CHAT) {
                    photo = records[0].getGroup().getAvatar();
                } else if ((peerType == PeerType.PEER_USER_ENCRYPTED) || (peerType == PeerType.PEER_USER)) {
                    photo = records[0].getSender().getPhoto();
                }

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

    private void performNotifyInChat(NotificationConfig config) {
        if (config.useSound) {
            pool.play(soundId, 1, 1, 1, 0, 1);
        }
        if (config.useVibration) {
            Vibrator mVibrator = (Vibrator) application.getSystemService(Context.VIBRATOR_SERVICE);
            mVibrator.vibrate(VIBRATE_PATTERN, -1);
        }
    }

    private void performNotifySystem(NotificationRecord[] records, NotificationConfig config) {

        boolean isHomogenous = true;

        int lPeerId = records[0].peerId;
        int lPeerType = records[0].peerType;
        for (NotificationRecord record : records) {
            if (record.getPeerId() != lPeerId || record.getPeerType() != lPeerType) {
                isHomogenous = false;
                break;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(application);
        builder.setSmallIcon(R.drawable.app_notify);
        builder.setLights(config.ledColor, 500, 500);

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

        builder.setTicker(records[records.length - 1].getSenderTitle() + ": " + records[records.length - 1].getContentMessage());

        Intent intent;
        if (isHomogenous) {
            intent = new Intent(StartActivity.ACTION_OPEN_CHAT);
            intent.setClass(application, StartActivity.class);
            intent.putExtra("peerType", records[0].getPeerType());
            intent.putExtra("peerId", records[0].getPeerId());
        } else {
            intent = new Intent();
            intent.setClass(application, StartActivity.class);
        }

        builder.setContentIntent(PendingIntent.getActivity(application, rnd.nextInt(), intent, 0));

        String summary;
        if (records.length > 1) {
            builder.setContentTitle(application.getString(R.string.st_app_name));
            summary = I18nUtil.getInstance().getPluralFormatted(R.plurals.st_new_messages_s, records.length);
            builder.setContentText(summary);
        } else {
            if (records[0].getPeerType() == PeerType.PEER_CHAT) {
                builder.setContentTitle(records[0].getSenderTitle());
            } else {
                builder.setContentTitle(records[0].getSender().getDisplayName());
            }

            builder.setContentText(records[0].getContentMessage());
            summary = I18nUtil.getInstance().getPluralFormatted(R.plurals.st_new_messages_s, 1);
        }

        Notification res = null;
        if (Build.VERSION.SDK_INT >= 11) {
            if (isHomogenous) {
                int size = (int) (UiMeasure.DENSITY * 64);
                Bitmap notificationBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                boolean loaded = false;

                TLAbsLocalAvatarPhoto photo = null;
                if (records[0].getPeerType() == PeerType.PEER_USER || records[0].getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                    photo = records[0].getSender().getPhoto();
                } else if (records[0].getPeerType() == PeerType.PEER_CHAT) {
                    photo = records[0].getGroup().getAvatar();
                }

                if (photo != null) {
                    if (photo instanceof TLLocalAvatarPhoto) {
                        TLLocalAvatarPhoto profilePhoto = (TLLocalAvatarPhoto) photo;
                        if (profilePhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                            Bitmap avatar = application.getUiKernel().getAvatarLoader().loadFromStorage(profilePhoto.getPreviewLocation());
                            if (avatar != null) {
                                Canvas canvas = new Canvas(notificationBitmap);
                                canvas.drawBitmap(avatar, new Rect(0, 0, avatar.getWidth(), avatar.getHeight()), new Rect(0, 0, size, size),
                                        new Paint(Paint.FILTER_BITMAP_FLAG));
                                loaded = true;
                            }
                        }
                    }
                }

                if (!loaded) {
                    int color = Placeholders.getBgColor(records[0].getPeerId());
                    notificationBitmap.eraseColor(color);
                    Canvas canvas = new Canvas(notificationBitmap);
                    if (records[0].getPeerType() == PeerType.PEER_CHAT) {
                        Drawable mask = application.getResources().getDrawable(R.drawable.st_group_placeholder_big);
                        mask.setBounds(0, 0, size, size);
                        mask.draw(canvas);
                    } else {
                        Drawable mask = application.getResources().getDrawable(R.drawable.st_user_placeholder_dialog_big);
                        mask.setBounds(0, 0, size, size);
                        mask.draw(canvas);
                    }
                }
                builder.setLargeIcon(notificationBitmap);
            }

            if (records.length > 1) {
                NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle(builder);
                for (NotificationRecord record : records) {
                    if (!isHomogenous) {
                        inboxStyle.addLine(record.getSenderTitle() + ": " + record.getContentShortMessage());
                    } else {
                        if (records[0].getPeerType() == PeerType.PEER_CHAT) {
                            inboxStyle.addLine(record.getShortSender() + ": " + record.getContentShortMessage());
                        } else {
                            inboxStyle.addLine(record.getContentMessage());
                        }
                    }
                }
                if (isHomogenous) {
                    if (records[0].getPeerType() == PeerType.PEER_USER || records[0].getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                        inboxStyle.setBigContentTitle(records[0].getSender().getDisplayName());
                    } else if (records[0].getPeerType() == PeerType.PEER_CHAT) {
                        inboxStyle.setBigContentTitle(records[0].getGroup().getTitle());
                    } else {
                        inboxStyle.setBigContentTitle(application.getString(R.string.st_app_name));
                    }
                } else {
                    inboxStyle.setBigContentTitle(application.getString(R.string.st_app_name));
                }

                inboxStyle.setSummaryText(summary);
                res = inboxStyle.build();
            } else {
                if (records[0].getContentPreview() != null && records[0].getContentPreview().length > 0) {
                    try {
                        Bitmap preview = Optimizer.load(records[0].getContentPreview());
                        Optimizer.blur(preview);
                        res = new NotificationCompat.BigPictureStyle(builder)
                                .bigPicture(preview)
                                .build();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (res != null) {
                    res = builder.build();
                }
            }
        } else {
            res = builder.build();
        }


        manager.notify(NOTIFICATION_MESSAGE, res);
    }

    public void hideAllNotifications() {
        manager.cancel(NOTIFICATION_MESSAGE);
        manager.cancel(NOTIFICATION_SYSTEM);
        lastRecords = new NotificationRecord[0];
        AlarmManager alarmManager = (AlarmManager) application.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(PendingIntent.getService(application,
                0, new Intent().setClass(application, NotificationRepeat.class), 0));
    }

    public void reset() {
        hideAllNotifications();
        lastNotifiedTime = 0;
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

    public void onNewMessages(ChatMessage... msgs) {
        ArrayList<NotificationRecord> records = new ArrayList<NotificationRecord>();
        for (int i = 0; i < msgs.length; i++) {
            if (msgs[i] == null) {
                continue;
            }
            if (!msgs[i].isOut() && msgs[i].getState() == MessageState.SENT) {
                NotificationRecord record = convertToRecord(msgs[i]);
                if (record != null) {
                    records.add(record);
                }
            }
        }

        onNewNotifications(records.toArray(new NotificationRecord[0]));
    }

    private NotificationRecord convertToRecord(ChatMessage msg) {
        NotificationRecord record = new NotificationRecord();
        record.setPeerId(msg.getPeerId());
        record.setPeerType(msg.getPeerType());
        record.setSender(application.getEngine().getUser(msg.getSenderId()));
        if (record.getSender() == null) {
            return null;
        }
        if (msg.getPeerType() == PeerType.PEER_CHAT) {
            record.setGroup(application.getEngine().getGroupsEngine().getGroup(msg.getPeerId()));
            if (record.getGroup() == null) {
                return null;
            }
        }
        if (msg.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
            record.setSecretChat(application.getEngine().getSecretEngine().loadChat(msg.getPeerId()));
            if (record.getSecretChat() == null) {
                return null;
            }
        }

        if (msg.getRawContentType() == ContentType.MESSAGE_TEXT) {
            if (msg.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                record.setContentMessage(application.getString(R.string.st_notification_secret_sent_message));
                record.setContentShortMessage(application.getString(R.string.st_notification_sent_message_short));
            } else {
                String message = msg.getMessage();
                if (message.length() > MAX_MESSAGE_LENGTH) {
                    message = message.substring(0, MAX_MESSAGE_LENGTH);
                }
                record.setContentMessage(message);
                record.setContentShortMessage(message);
            }
        } else if (msg.getRawContentType() == ContentType.MESSAGE_CONTACT) {
            if (msg.getPeerType() == PeerType.PEER_CHAT) {
                record.setContentMessage(application.getString(R.string.st_notification_group_sent_contact)
                        .replace("{name}", BidiFormatter.getInstance().unicodeWrap(record.sender.getDisplayName()))
                        .replace("{chat}", BidiFormatter.getInstance().unicodeWrap(record.group.getTitle())));
            } else if (msg.getPeerType() == PeerType.PEER_USER) {
                record.setContentMessage(application.getString(R.string.st_notification_sent_contact)
                        .replace("{name}", BidiFormatter.getInstance().unicodeWrap(record.sender.getDisplayName())));
            } else {
                return null;
            }
            record.setContentShortMessage(application.getString(R.string.st_notification_sent_contact_short));
        } else if (msg.getRawContentType() == ContentType.MESSAGE_GEO) {
            if (msg.getPeerType() == PeerType.PEER_CHAT) {
                record.setContentMessage(application.getString(R.string.st_notification_group_sent_map)
                        .replace("{name}", BidiFormatter.getInstance().unicodeWrap(record.sender.getDisplayName()))
                        .replace("{chat}", BidiFormatter.getInstance().unicodeWrap(record.group.getTitle())));
            } else if (msg.getPeerType() == PeerType.PEER_USER) {
                record.setContentMessage(application.getString(R.string.st_notification_sent_map)
                        .replace("{name}", BidiFormatter.getInstance().unicodeWrap(record.sender.getDisplayName())));
            } else if (msg.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                record.setContentMessage(application.getString(R.string.st_notification_secret_sent_map));
            } else {
                return null;
            }
            record.setContentShortMessage(application.getString(R.string.st_notification_sent_map_short));
        } else if (msg.getRawContentType() == ContentType.MESSAGE_PHOTO) {
            if (msg.getExtras() instanceof TLLocalPhoto) {
                record.setContentPreview(((TLLocalPhoto) msg.getExtras()).getFastPreview());
            }
            if (msg.getPeerType() == PeerType.PEER_CHAT) {
                record.setContentMessage(application.getString(R.string.st_notification_group_sent_photo)
                        .replace("{name}", BidiFormatter.getInstance().unicodeWrap(record.sender.getDisplayName()))
                        .replace("{chat}", BidiFormatter.getInstance().unicodeWrap(record.group.getTitle())));
            } else if (msg.getPeerType() == PeerType.PEER_USER) {
                record.setContentMessage(application.getString(R.string.st_notification_sent_photo)
                        .replace("{name}", BidiFormatter.getInstance().unicodeWrap(record.sender.getDisplayName())));
            } else if (msg.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                record.setContentMessage(application.getString(R.string.st_notification_secret_sent_photo));
            } else {
                return null;
            }
            record.setContentShortMessage(application.getString(R.string.st_notification_sent_photo_short));
        } else if (msg.getRawContentType() == ContentType.MESSAGE_VIDEO) {
            if (msg.getExtras() instanceof TLLocalVideo) {
                record.setContentPreview(((TLLocalVideo) msg.getExtras()).getFastPreview());
            }
            if (msg.getPeerType() == PeerType.PEER_CHAT) {
                record.setContentMessage(application.getString(R.string.st_notification_group_sent_video)
                        .replace("{name}", BidiFormatter.getInstance().unicodeWrap(record.sender.getDisplayName()))
                        .replace("{chat}", BidiFormatter.getInstance().unicodeWrap(record.group.getTitle())));
            } else if (msg.getPeerType() == PeerType.PEER_USER) {
                record.setContentMessage(application.getString(R.string.st_notification_sent_video)
                        .replace("{name}", BidiFormatter.getInstance().unicodeWrap(record.sender.getDisplayName())));
            } else if (msg.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                record.setContentMessage(application.getString(R.string.st_notification_secret_sent_video));
            } else {
                return null;
            }
            record.setContentShortMessage(application.getString(R.string.st_notification_sent_video_short));
        } else if (msg.getRawContentType() == ContentType.MESSAGE_DOCUMENT || msg.getRawContentType() == ContentType.MESSAGE_DOC_PREVIEW) {
            if (msg.getExtras() instanceof TLLocalDocument) {
                record.setContentPreview(((TLLocalDocument) msg.getExtras()).getFastPreview());
            }
            if (msg.getPeerType() == PeerType.PEER_CHAT) {
                record.setContentMessage(application.getString(R.string.st_notification_group_sent_document)
                        .replace("{name}", BidiFormatter.getInstance().unicodeWrap(record.sender.getDisplayName()))
                        .replace("{chat}", BidiFormatter.getInstance().unicodeWrap(record.group.getTitle())));
            } else if (msg.getPeerType() == PeerType.PEER_USER) {
                record.setContentMessage(application.getString(R.string.st_notification_sent_document)
                        .replace("{name}", BidiFormatter.getInstance().unicodeWrap(record.sender.getDisplayName())));
            } else if (msg.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                record.setContentMessage(application.getString(R.string.st_notification_secret_sent_doc));
            } else {
                return null;
            }
            record.setContentShortMessage(application.getString(R.string.st_notification_sent_document_short));
        } else if (msg.getRawContentType() == ContentType.MESSAGE_DOC_ANIMATED) {
            if (msg.getExtras() instanceof TLLocalDocument) {
                record.setContentPreview(((TLLocalDocument) msg.getExtras()).getFastPreview());
            }
            if (msg.getPeerType() == PeerType.PEER_CHAT) {
                record.setContentMessage(application.getString(R.string.st_notification_group_sent_animation)
                        .replace("{name}", BidiFormatter.getInstance().unicodeWrap(record.sender.getDisplayName()))
                        .replace("{chat}", BidiFormatter.getInstance().unicodeWrap(record.group.getTitle())));
            } else if (msg.getPeerType() == PeerType.PEER_USER) {
                record.setContentMessage(application.getString(R.string.st_notification_sent_animation)
                        .replace("{name}", BidiFormatter.getInstance().unicodeWrap(record.sender.getDisplayName())));
            } else if (msg.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                record.setContentMessage(application.getString(R.string.st_notification_secret_sent_animation));
            } else {
                return null;
            }
            record.setContentShortMessage(application.getString(R.string.st_notification_sent_animation_short));
        } else if (msg.getRawContentType() == ContentType.MESSAGE_AUDIO) {
            if (msg.getPeerType() == PeerType.PEER_CHAT) {
                record.setContentMessage(application.getString(R.string.st_notification_group_sent_audio)
                        .replace("{name}", BidiFormatter.getInstance().unicodeWrap(record.sender.getDisplayName()))
                        .replace("{chat}", BidiFormatter.getInstance().unicodeWrap(record.group.getTitle())));
            } else if (msg.getPeerType() == PeerType.PEER_USER) {
                record.setContentMessage(application.getString(R.string.st_notification_sent_audio)
                        .replace("{name}", BidiFormatter.getInstance().unicodeWrap(record.sender.getDisplayName())));
            } else if (msg.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                record.setContentMessage(application.getString(R.string.st_notification_secret_sent_audio));
            } else {
                return null;
            }
            record.setContentShortMessage(application.getString(R.string.st_notification_sent_audio_short));
        } else if (msg.getContentType() == ContentType.MESSAGE_SYSTEM) {
            TLObject object = msg.getExtras();
            if (object != null && object instanceof TLAbsLocalAction) {
                TLAbsLocalAction action = (TLAbsLocalAction) object;
                if (action instanceof TLLocalActionChatAddUser) {
                    int addedUid = ((TLLocalActionChatAddUser) action).getUserId();
                    String addedName;
                    int resource;
                    if (addedUid == application.getCurrentUid()) {
                        addedName = application.getString(R.string.st_notification_you);
                        resource = R.string.st_notification_added_user_you;
                    } else {
                        User user = application.getEngine().getUser(addedUid);
                        if (user == null) {
                            return null;
                        }
                        addedName = getShortName(user);
                        resource = R.string.st_notification_added_user;
                    }
                    record.setContentMessage(application.getString(resource).replace("{0}", BidiFormatter.getInstance().unicodeWrap(addedName)));
                } else if (action instanceof TLLocalActionChatEditTitle) {
                    record.setContentMessage(application.getString(R.string.st_notification_changed_name));
                } else if (action instanceof TLLocalActionChatCreate) {
                    record.setContentMessage(application.getString(R.string.st_notification_created_group));
                } else if (action instanceof TLLocalActionChatEditPhoto) {
                    if (((TLLocalActionChatEditPhoto) action).getPhoto() instanceof TLLocalAvatarEmpty) {
                        record.setContentMessage(application.getString(R.string.st_notification_removed_photo));
                    } else {
                        record.setContentMessage(application.getString(R.string.st_notification_changed_photo));
                    }
                } else if (action instanceof TLLocalActionChatDeletePhoto) {
                    record.setContentMessage(application.getString(R.string.st_notification_removed_photo));
                } else if (action instanceof TLLocalActionChatDeleteUser) {
                    int deletedUid = ((TLLocalActionChatDeleteUser) action).getUserId();
                    String deletedName;
                    int resource;
                    if (deletedUid == application.getCurrentUid()) {
                        deletedName = application.getString(R.string.st_notification_you);
                        resource = R.string.st_notification_kicked_user_you;
                    } else {
                        User user = application.getEngine().getUser(deletedUid);
                        if (user == null) {
                            return null;
                        }
                        deletedName = getShortName(user);
                        resource = R.string.st_notification_kicked_user;
                    }
                    record.setContentMessage(application.getString(resource).replace("{0}", BidiFormatter.getInstance().unicodeWrap(deletedName)));
                } else if (action instanceof TLLocalActionEncryptedTtl) {
                    int ttl = ((TLLocalActionEncryptedTtl) action).getTtlSeconds();
                    if (ttl <= 0) {
                        record.setContentMessage(application.getString(R.string.st_notification_encrypted_switched_off));
                    } else {
                        record.setContentMessage(application.getString(R.string.st_notification_encrypted_switched).replace("{time}",
                                BidiFormatter.getInstance().unicodeWrap(TextUtil.formatHumanReadableDuration(ttl))));
                    }
                } else {
                    return null;
                }

                record.setContentShortMessage(record.getContentMessage());
            } else {
                return null;
            }
        } else {
            return null;
        }

        return record;
    }

    public void onNewMessageJoined(int uid) {
        NotificationRecord record = new NotificationRecord();
        record.setPeerId(uid);
        record.setPeerType(PeerType.PEER_USER);
        record.setSender(application.getEngine().getUser(uid));
        if (record.getSender() == null) {
            return;
        }
        record.setContentShortMessage(application.getString(R.string.st_notification_joined_short));
        record.setContentMessage(application.getString(R.string.st_notification_joined)
                .replace("{name}", record.getSender().getDisplayName()));
        onNewNotifications(record);
    }

    public void onNewSecretChatRequested(int senderId, int chatId) {
        NotificationRecord record = new NotificationRecord();
        record.setPeerId(chatId);
        record.setPeerType(PeerType.PEER_USER_ENCRYPTED);
        record.setSender(application.getEngine().getUser(senderId));
        if (record.getSender() == null) {
            return;
        }

        record.setContentMessage(application.getString(R.string.st_notification_secret_requested));
        record.setContentShortMessage(record.getContentMessage());
        onNewNotifications(record);
    }

    public void onNewSecretChatEstablished(int uid, int chatId) {
        NotificationRecord record = new NotificationRecord();
        record.setPeerId(chatId);
        record.setPeerType(PeerType.PEER_USER_ENCRYPTED);
        record.setSender(application.getEngine().getUser(uid));
        if (record.getSender() == null) {
            return;
        }
        record.setContentMessage(application.getString(R.string.st_notification_secret_created));
        record.setContentShortMessage(record.getContentMessage());
        onNewNotifications(record);
    }

    public void onNewSecretChatCancelled(int uid, int chatId) {
        NotificationRecord record = new NotificationRecord();
        record.setPeerId(chatId);
        record.setPeerType(PeerType.PEER_USER_ENCRYPTED);
        record.setSender(application.getEngine().getUser(uid));
        if (record.getSender() == null) {
            return;
        }
        record.setContentMessage(application.getString(R.string.st_notification_secret_cancelled));
        record.setContentShortMessage(record.getContentMessage());
        onNewNotifications(record);
    }

    private class NotificationRecord {
        private int peerType;
        private int peerId;
        private String contentShortMessage;
        private String contentMessage;
        private byte[] contentPreview = new byte[0];
        private User sender;
        private Group group;
        private EncryptedChat secretChat;

        public int getPeerType() {
            return peerType;
        }

        public void setPeerType(int peerType) {
            this.peerType = peerType;
        }

        public int getPeerId() {
            return peerId;
        }

        public void setPeerId(int peerId) {
            this.peerId = peerId;
        }

        public String getContentShortMessage() {
            return contentShortMessage;
        }

        public void setContentShortMessage(String contentShortMessage) {
            this.contentShortMessage = contentShortMessage;
        }

        public String getContentMessage() {
            return contentMessage;
        }

        public void setContentMessage(String contentMessage) {
            this.contentMessage = contentMessage;
        }

        public byte[] getContentPreview() {
            return contentPreview;
        }

        public void setContentPreview(byte[] contentPreview) {
            this.contentPreview = contentPreview;
        }

        public User getSender() {
            return sender;
        }

        public void setSender(User sender) {
            this.sender = sender;
        }

        public Group getGroup() {
            return group;
        }

        public void setGroup(Group group) {
            this.group = group;
        }

        public EncryptedChat getSecretChat() {
            return secretChat;
        }

        public void setSecretChat(EncryptedChat secretChat) {
            this.secretChat = secretChat;
        }

        public String getSenderTitle() {
            if (peerType == PeerType.PEER_CHAT) {
                return getShortSender() + "@" + group.getTitle();
            } else {
                return getShortSender();
            }
        }

        public String getShortSender() {
            return getShortName(sender);
        }
    }

    private static String getShortName(User user) {
        String lastName = user.getLastName().trim();
        if (lastName.length() > 0) {
            return user.getFirstName() + " " + user.getLastName().trim().charAt(0);
        } else {
            return user.getFirstName();
        }
    }

}