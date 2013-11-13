package org.telegram.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import com.extradea.framework.images.ImageController;
import com.extradea.framework.images.ImageSupport;
import com.extradea.framework.images.workers.*;
import org.telegram.android.auth.AuthController;
import org.telegram.android.config.DebugSettings;
import org.telegram.android.config.WallpaperHolder;
import org.telegram.android.core.*;
import org.telegram.android.core.background.*;
import org.telegram.android.core.files.DownloadController;
import org.telegram.android.core.files.UploadController;
import org.telegram.android.core.model.ChatMessage;
import org.telegram.android.config.UserSettings;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.storage.TLDcInfo;
import org.telegram.android.core.model.storage.TLKey;
import org.telegram.android.critical.ApiStorage;
import org.telegram.android.cursors.ViewSource;
import org.telegram.android.log.Logger;
import org.telegram.android.media.CachedImageWorker;
import org.telegram.android.media.DownloadManager;
import org.telegram.android.media.StelsImageWorker;
import org.telegram.android.core.ApiUtils;
import org.telegram.android.reflection.CrashHandler;
import org.telegram.android.reflection.TechReflection;
import org.telegram.android.screens.ScreenLogicType;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.ui.*;
import org.telegram.android.core.GlobalSearcher;
import org.telegram.api.auth.TLAuthorization;
import org.telegram.api.engine.ApiCallback;
import org.telegram.api.engine.AppInfo;
import org.telegram.api.engine.LoggerInterface;
import org.telegram.api.engine.TelegramApi;
import org.telegram.i18n.I18nUtil;
import org.telegram.mtproto.log.LogInterface;

import java.util.HashMap;

/**
 * Author: Korshakov Stepan
 * Created: 22.07.13 0:55
 */
public class StelsApplication extends Application implements ImageSupport {

    private static final String TAG = "App";

    private static final String SHARED_SETTINGS = "org.telegram.android.Settings";
    private static final String ACCOUNT_TYPE = "org.telegram.android.account";

    private static final int ACTIVE_TIMEOUT = 1000;

    private TechReflection techReflection;
    private ApiStorage apiStorage;
    private ModelEngine engine;

    private AuthController authController;
    private TelegramApi telegramApi;
    private UpdateProcessor updateProcessor;

    private DialogSource dialogSource;
    private HashMap<Long, MessageSource> messageSources = new HashMap<Long, MessageSource>();
    private UserSource userSource;
    private ContactsSource contactsSource;
    private ChatSource chatSource;
    private EncryptedChatSource encryptedChatSource;

    private GlobalSearcher searcher;

    private VersionHolder versionHolder;
    private MessageSender messageSender;
    private FastActions actions;
    private EncryptionController encryptionController;
    private EmojiProcessor emojiProcessor;
    private TypingStates typingStates;
    private ImageController controller;
    private NotificationSettings notificationSettings;
    private Notifications notifications;
    private UploadController uploadController;
    private DownloadController downloadController;
    private DownloadManager downloadManager;
    private TechSyncer techSyncer;

    private LastEmojiProcessor lastEmoji;
    private DebugSettings debugSettings;
    private DynamicConfig dynamicConfig;
    private UiResponsibility responsibility;
    private WallpaperHolder wallpaperHolder;
    private UserSettings userSettings;
    private ConnectionMonitor monitor;
    private TextSaver textSaver;
    private SelfDestructProcessor selfDestructProcessor;
    private EncryptedChatProcessor encryptedChatProcessor;

    private int openedChatPeerId;
    private int openedChatPeerType;

    private boolean isDialogsVisible = false;

    public boolean isAppVisible = false;
    public StelsActivity visibleActivity;

    public boolean isAppActive = true;
    public long lastStartTime;

    private Account account;

    private SharedPreferences appSettings;

    private ScreenLogicType screenLogicType;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                checkGoesOffline();
            }
        }
    };

    private boolean isSaveToGalleryEnabled = true;

    @Override
    public void onCreate() {
        CrashHandler.init(this);
        super.onCreate();

        Logger.init(this);
        Logger.d(TAG, "--------------- Start App ------------------");
        Logger.resetPerformanceTimer();
        org.telegram.api.engine.Logger.registerInterface(new LoggerInterface() {
            @Override
            public void w(String tag, String message) {
                Logger.w("API|" + tag, message);
            }

            @Override
            public void d(String tag, String message) {
                Logger.d("API|" + tag, message);
            }

            @Override
            public void e(String tag, Throwable t) {
                Logger.t("API|" + tag, t);
            }
        });
        org.telegram.mtproto.log.Logger.registerInterface(new LogInterface() {
            @Override
            public void w(String tag, String message) {
                Logger.w(tag, message);
            }

            @Override
            public void d(String tag, String message) {
                Logger.d(tag, message);
            }

            @Override
            public void e(String tag, Throwable t) {
                Logger.t(tag, t);
            }
        });
        techReflection = new TechReflection(this);
        debugSettings = new DebugSettings(this);

        if (debugSettings.isSaveLogs()) {
            Logger.enableDiskLog();
        } else {
            Logger.disableDiskLog();
        }

        I18nUtil.init(this);
        TextUtil.init(this);
        AsyncException.initLocalisation(this);
        ApiUtils.init(this, techReflection.getScreenSize());
        Logger.recordTme(TAG, "Contextual init");

        apiStorage = new ApiStorage(this);
        checkLoginStates();
        Logger.recordTme(TAG, "ApiStorage loading");

        monitor = new ConnectionMonitor(this);
        Logger.recordTme(TAG, "ConnectionMonitor");

        authController = new AuthController(this);
        Logger.recordTme(TAG, "Auth loading");

        engine = new ModelEngine(this);
        Logger.recordTme(TAG, "Model loading");

        messageSender = new MessageSender(this);
        Logger.recordTme(TAG, "MessageSender");

        encryptedChatProcessor = new EncryptedChatProcessor(this);
        Logger.recordTme(TAG, "EncryptedChatProcessor");

        // updateProcessor = new UpdateProcessor(this);
        // Logger.recordTme(TAG, "UpdateProcessor");

        actions = new FastActions(this);
        Logger.recordTme(TAG, "FastActions");

        emojiProcessor = new EmojiProcessor(this);
        Logger.recordTme(TAG, "EmojiProcessor");

        typingStates = new TypingStates(this);
        Logger.recordTme(TAG, "TypingStates");

        controller = new ImageController(this, new ImageWorker[]{
                new FileSystemWorker(StelsApplication.this),
                new DownloadWorker(),
                new CornersWorker(),
                new StelsImageWorker(this),
                new StelsImageWorker(this),
                new CachedImageWorker()
        });

        Logger.recordTme(TAG, "ImageController");
        dialogSource = new DialogSource(this);
        Logger.recordTme(TAG, "DialogSource");
        userSource = new UserSource(this);
        Logger.recordTme(TAG, "UserSource");
        contactsSource = new ContactsSource(this);
        Logger.recordTme(TAG, "ContactsSource");
        chatSource = new ChatSource(this);
        Logger.recordTme(TAG, "ChatSource");
        encryptedChatSource = new EncryptedChatSource(this);
        Logger.recordTme(TAG, "EncryptedChatSource");
        notificationSettings = new NotificationSettings(this);
        Logger.recordTme(TAG, "NotificationSettings");
        notifications = new Notifications(this);
        Logger.recordTme(TAG, "Notifications");
        uploadController = new UploadController(this);
        Logger.recordTme(TAG, "UploadController");
        downloadController = new DownloadController(this);
        Logger.recordTme(TAG, "DownloadController");
        downloadManager = new DownloadManager(this);
        Logger.recordTme(TAG, "DownloadManager");
        techSyncer = new TechSyncer(this);
        Logger.recordTme(TAG, "TechSyncer");
        searcher = new GlobalSearcher(this);
        Logger.recordTme(TAG, "GlobalSearcher");
        lastEmoji = new LastEmojiProcessor(this);
        Logger.recordTme(TAG, "LastEmojiProcessor");
        dynamicConfig = new DynamicConfig(this);
        Logger.recordTme(TAG, "DynamicConfig");
        userSettings = new UserSettings(this);
        Logger.recordTme(TAG, "UserSettings");
        wallpaperHolder = new WallpaperHolder(this);
        Logger.recordTme(TAG, "WallpaperHolder");
        encryptionController = new EncryptionController(this);
        Logger.recordTme(TAG, "EncryptionController");
        textSaver = new TextSaver(this);
        Logger.recordTme(TAG, "TextSaver");
        selfDestructProcessor = new SelfDestructProcessor(this);
        Logger.recordTme(TAG, "SelfDestructProcessor");
        versionHolder = new VersionHolder(this);
        versionHolder.tryLoad();
        Logger.recordTme(TAG, "VersionHolder");
        responsibility = new UiResponsibility();

        appSettings = getSharedPreferences(SHARED_SETTINGS, MODE_PRIVATE);
        isSaveToGalleryEnabled = appSettings.getBoolean("save_to_gallery", true);

        isAppVisible = false;
        isAppActive = false;

        updateApi();
        authController.check();

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            int versionCode = pInfo.versionCode;
            if (!versionHolder.isWasUpgraded()) {
                if (versionHolder.getCurrentVersionInstalled() == 0) {
                    if (isLoggedIn()) {
                        versionHolder.setWasUpgraded(true);
                    }
                } else {
                    if (versionHolder.getCurrentVersionInstalled() != versionCode) {
                        versionHolder.setWasUpgraded(true);
                    }
                }
            }
            versionHolder.saveCurrentVersion(versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (isLoggedIn()) {
            // Checked in previous steps
            AccountManager am = AccountManager.get(this);
            Account[] accounts = am.getAccountsByType(ACCOUNT_TYPE);
            if (accounts.length == 0) {
                Account account = new Account(apiStorage.getObj().getPhone(), ACCOUNT_TYPE);
                am.addAccountExplicitly(account, "", null);
                this.account = account;
            } else {
                account = accounts[0];
            }

            if (engine.getDatabase().isWasUpgraded()) {
                dialogSource.resetSync();
                dialogSource.startSync();
            } else {
                dialogSource.startSyncIfRequired();
            }

            if (techReflection.isAppUpgraded() || engine.getDatabase().isWasUpgraded()) {
                contactsSource.resetState();
            }

            if (engine.getDatabase().isWasUpgraded()) {
                MessageSource.clearData(this);
            }

            contactsSource.startSync();
            engine.markUnsentAsFailured();
            updateProcessor = new UpdateProcessor(this);
            updateProcessor.checkState();
            actions.checkHistory();
            techSyncer.checkPush();
            techSyncer.onLogin();
            actions.checkForDeletions();
            selfDestructProcessor.checkInitialDeletions();
        }

        techSyncer.checkDC();

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                dropLogin();
            }
        }, new IntentFilter("org.telegram.android.LOGOUT"));

        screenLogicType = ScreenLogicType.SINGLE_STATIC;

        Logger.recordTme(TAG, "Misc loading");

        onAppPause();
    }

    public void updateApi() {
        if (getApiStorage().getAuthKey(getApiStorage().getPrimaryDc()) != null) {
            setApi(new TelegramApi(getApiStorage(), new AppInfo(5, Build.MODEL, Build.VERSION.RELEASE, getAppVersion(),
                    getString(R.string.st_lang)), new ApiCallback() {
                @Override
                public void onApiDies(TelegramApi api) {
                    if (api == StelsApplication.this.telegramApi) {
                        dropLogin();
                    }
                }

                @Override
                public void onUpdatesInvalidated(TelegramApi api) {
                    if (api == StelsApplication.this.telegramApi) {
                        if (isLoggedIn() && updateProcessor != null) {
                            updateProcessor.invalidateUpdates();
                        }
                    }
                }
            }));
        } else {
            setApi(null);
        }
    }

    public boolean isOnSdCard() {
        return techReflection.isOnSdCard();
    }

    public boolean isRTL() {
        return techReflection.isRtl();
    }

    public DebugSettings getDebugSettings() {
        return debugSettings;
    }

    public ScreenLogicType getScreenLogicType() {
        return screenLogicType;
    }

    private void checkLoginStates() {
        Logger.d(TAG, "Authenticated: " + apiStorage.isAuthenticated());
        Logger.d(TAG, "Uid: " + apiStorage.getObj().getUid());
        Logger.d(TAG, "Phone: " + apiStorage.getObj().getPhone());
        for (TLKey key : apiStorage.getObj().getKeys()) {
            Logger.d(TAG, "Key: " + key.getDcId() + ":" + key.isAuthorised());
        }
        for (TLDcInfo dc : apiStorage.getObj().getDcInfos()) {
            Logger.d(TAG, "Key: " + dc.getDcId() + " " + dc.getAddress() + ":" + dc.getPort());
        }
//        if (isLoggedIn()) {
//            if (!(connectionContext instanceof ConnectionContext)) {
//                dropLogin();
//                return;
//            }
//
//            ConnectionContext context = (ConnectionContext) connectionContext;
//
//            if (getDataCenters().getConfigs(context.getDc()).length == 0) {
//                dropLogin();
//                return;
//            }
//
//            if (getKeyStorage().getKey(context.getDc()) == null) {
//                dropLogin();
//                return;
//            }
//
//            AccountManager am = AccountManager.get(this);
//            Account[] accounts = am.getAccountsByType("org.telegram.android.account");
//            if (accounts.length != 1) {
//                dropLogin();
//            }
//        }
    }

    public boolean isSaveToGalleryEnabled() {
        return isSaveToGalleryEnabled;
    }

    public void setSaveToGalleryEnabled(boolean value) {
        isSaveToGalleryEnabled = value;
        appSettings.edit().putBoolean("save_to_gallery", value).commit();
    }

    private String getAppVersion() {
        return techReflection.getAppVersion();
    }

    public boolean isSlow() {
        return techReflection.isSlow();
    }

    public boolean isLoggedIn() {
        return apiStorage.isAuthenticated();
    }

    public int getCurrentUid() {
        return apiStorage.getObj().getUid();
    }

    private void checkGoesOnline() {
        Logger.d(TAG, "checkGoesOnline");
        if (!isAppActive) {
            Logger.d(TAG, "yep it goes foreground");
            isAppActive = true;
            onAppGoesForeground();
        }
    }

    private void checkGoesOffline() {
        Logger.d(TAG, "checkGoesOffline: " + isAppActive + ", " + isAppVisible + ", " + (SystemClock.uptimeMillis() - lastStartTime));
        if (isAppActive && SystemClock.uptimeMillis() - lastStartTime >= ACTIVE_TIMEOUT && !isAppVisible) {
            Logger.d(TAG, "yep it goes background");
            isAppActive = false;
            onAppGoesBackground();
        }
    }

    public StelsActivity getVisibleActivity() {
        return visibleActivity;
    }

    public void onAppResume(StelsActivity activity) {
        visibleActivity = activity;
        isAppVisible = true;
        lastStartTime = SystemClock.uptimeMillis();
        checkGoesOnline();
    }

    public void onAppPause() {
        isAppVisible = false;
        lastStartTime = SystemClock.uptimeMillis();
        checkGoesOffline();
        handler.removeMessages(0);
        handler.sendEmptyMessageDelayed(0, ACTIVE_TIMEOUT);
        notifications.onActivityPaused();
    }

    public void onConfigurationChanged() {
        notifications.hideInApp();
    }

    public void onDialogsResume() {
        isDialogsVisible = true;
        getNotifications().hideAllNotifications();
    }

    public void onDialogPaused() {
        isDialogsVisible = false;
    }

    public boolean isDialogsVisible() {
        return isDialogsVisible;
    }

    public boolean isAppVisible() {
        return isAppVisible;
    }

    public boolean isAppActive() {
        return isAppActive;
    }

    public void onAppGoesForeground() {
        emojiProcessor.loadEmoji();
        actions.onAppGoesForeground();
        encryptedChatProcessor.onUserGoesOnline();
    }

    public void onAppGoesBackground() {
        actions.onAppGoesBackground();
        encryptedChatProcessor.onUserGoesOffline();
    }

    public void onOpenedChat(int peerType, int peerId) {
        this.openedChatPeerType = peerType;
        this.openedChatPeerId = peerId;
        getEngine().markDialogAsNonFailed(peerType, peerId);

        if (peerType != PeerType.PEER_USER_ENCRYPTED) {
            int maxMid = getEngine().getMaxMsgInId(peerType, peerId);
            getEngine().onMaxLocalViewed(peerType, peerId, maxMid);
        } else {
            int maxDate = getEngine().getMaxDateInDialog(peerType, peerId);
            getEngine().onMaxLocalViewed(peerType, peerId, maxDate);
        }

        getActions().readHistory(peerType, peerId);
        getDialogSource().getViewSource().invalidateData();
        getNotifications().hideChatNotifications(peerType, peerId);
    }

    public void onClosedChat(int peerType, int peerId) {
        if (openedChatPeerId == peerId && openedChatPeerType == peerType) {
            this.openedChatPeerType = -1;
            this.openedChatPeerId = -1;
        }
    }

    public int getOpenedChatPeerId() {
        return openedChatPeerId;
    }

    public int getOpenedChatPeerType() {
        return openedChatPeerType;
    }

    private void dropData() {
        getEngine().getDatabase().clearData();
        getEngine().clearCache();
        getChatSource().clear();
    }

    public void prelogin() {
        dropData();
    }

    public void onSuccessAuth(TLAuthorization auth) {
        apiStorage.doAuth(auth);

        dialogSource = new DialogSource(this);
        dialogSource.startSync();
        contactsSource = new ContactsSource(this);
        contactsSource.startSync();
        updateProcessor = new UpdateProcessor(this);
        updateProcessor.invalidateUpdates();

        techSyncer.onLogin();

        Account account = new Account(apiStorage.getObj().getPhone(), ACCOUNT_TYPE);
        AccountManager am = AccountManager.get(this);
        am.addAccountExplicitly(account, "", null);
        this.account = account;
    }

    public void clearLoginState() {
        authController.logout();
        this.account = null;

        AccountManager am = AccountManager.get(this);
        Account[] accounts = am.getAccountsByType(ACCOUNT_TYPE);
        for (Account c : accounts) {
            am.removeAccount(c, null, null);
        }

        // Clearing all messages states
        for (MessageSource source : messageSources.values()) {
            source.destroy();
        }
        messageSources.clear();
        MessageSource.clearData(this);

        // Clearing dialogs states
        dialogSource.destroy();
        DialogSource.clearData(this);

        // Clearing contacts states
        contactsSource.destroy();
        ContactsSource.clearData(this);

        updateProcessor.destroy();
        updateProcessor.clearData();

        techSyncer.onLogout();

        notifications.reset();

        dropData();

        apiStorage.resetAuth();
    }

    public void dropLogin() {
        if (!isLoggedIn())
            return;

        apiStorage.reset();

        clearLoginState();

        if (isAppVisible) {
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("org.telegram.android.ACTION_LOGOUT");
            sendBroadcast(broadcastIntent);
        } else {
            NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
            notification.setTicker("Required login in Telegram");
            notification.setContentTitle("Telegram");
            notification.setContentText("Please login again to Telegram");
            notification.setContentIntent(PendingIntent.getActivity(this, 0, new Intent().setClass(this, StartActivity.class), 0));
            notification.setSmallIcon(R.drawable.app_notify);
            notification.setAutoCancel(true);
            Notification not = notification.build();
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.notify(1, not);
        }
    }

    public UploadController getUploadController() {
        return uploadController;
    }

    public UserSource getUserSource() {
        return userSource;
    }

    public FastActions getActions() {
        return actions;
    }

    public TypingStates getTypingStates() {
        return typingStates;
    }

    public MessageSender getMessageSender() {
        return messageSender;
    }

    public DialogSource getDialogSource() {
        return dialogSource;
    }

    public ContactsSource getContactsSource() {
        return contactsSource;
    }

    public EmojiProcessor getEmojiProcessor() {
        return emojiProcessor;
    }

    public Notifications getNotifications() {
        return notifications;
    }

    public NotificationSettings getNotificationSettings() {
        return notificationSettings;
    }

    public UpdateProcessor getUpdateProcessor() {
        return updateProcessor;
    }

    public synchronized MessageSource getMessageSource(int peerType, int peerId) {
        long id = peerType + peerId * 10;
        if (messageSources.containsKey(id)) {
            return messageSources.get(id);
        } else {
            MessageSource source = new MessageSource(peerType, peerId, this);
            responsibility.doPause(50);
            source.startSyncIfRequired();
            messageSources.put(id, source);
            return source;
        }
    }

    public synchronized void removeMessageSource(int peerType, int peerId) {
        long id = peerType + peerId * 10;
        if (messageSources.containsKey(id)) {
            messageSources.remove(id);
        }
    }

    public synchronized ViewSource<ChatMessage> getMessagesViewSource(int peerType, int peerId) {
        long id = peerType + peerId * 10;
        if (messageSources.containsKey(id)) {
            return messageSources.get(id).getMessagesSource();
        }

        return null;
    }

    public void onSourceAddMessage(ChatMessage message) {
        ViewSource<ChatMessage> res = getMessagesViewSource(message.getPeerType(), message.getPeerId());
        if (res == null)
            return;
        res.addItem(message);
    }

    public void onSourceAddMessageHacky(ChatMessage message) {
        ViewSource<ChatMessage> res = getMessagesViewSource(message.getPeerType(), message.getPeerId());
        if (res == null)
            return;
        res.addToEndHacky(message);
    }

    public void onSourceRemoveMessage(ChatMessage message) {
        ViewSource<ChatMessage> res = getMessagesViewSource(message.getPeerType(), message.getPeerId());
        if (res == null)
            return;
        res.removeItem(message);
    }

    public void onSourceUpdateMessage(ChatMessage message) {
        ViewSource<ChatMessage> res = getMessagesViewSource(message.getPeerType(), message.getPeerId());
        if (res == null)
            return;
        res.updateItem(message);
    }

    public synchronized void notifyUIUpdate() {
        Logger.w(TAG, "notifyUIUpdate");
        long start = System.currentTimeMillis();
        if (dialogSource != null && dialogSource.getViewSource() != null) {
            dialogSource.getViewSource().invalidateDataIfRequired();
        }
        if (messageSources != null) {
            for (MessageSource source : messageSources.values()) {
                if (source.getMessagesSource() != null) {
                    source.getMessagesSource().invalidateDataIfRequired();
                }
            }
        }
        Logger.w(TAG, "notifyUIUpdate: " + (System.currentTimeMillis() - start) + " ms");
    }

    public ModelEngine getEngine() {
        return engine;
    }

    public ApiStorage getApiStorage() {
        return apiStorage;
    }

    public void setApi(TelegramApi api) {
        this.telegramApi = api;
    }

    public TelegramApi getApi() {
        return telegramApi;
    }

    public AuthController getAuthController() {
        return authController;
    }

    public ChatSource getChatSource() {
        return chatSource;
    }

    public DownloadManager getDownloadManager() {
        return downloadManager;
    }

    public TechSyncer getTechSyncer() {
        return techSyncer;
    }

    public Account getAccount() {
        return account;
    }

    public GlobalSearcher getSearcher() {
        return searcher;
    }

    public LastEmojiProcessor getLastEmoji() {
        return lastEmoji;
    }

    public DynamicConfig getDynamicConfig() {
        return dynamicConfig;
    }

    public UiResponsibility getResponsibility() {
        return responsibility;
    }

    public UserSettings getUserSettings() {
        return userSettings;
    }

    public WallpaperHolder getWallpaperHolder() {
        return wallpaperHolder;
    }

    public ConnectionMonitor getMonitor() {
        return monitor;
    }

    public EncryptionController getEncryptionController() {
        return encryptionController;
    }

    public EncryptedChatSource getEncryptedChatSource() {
        return encryptedChatSource;
    }

    public DownloadController getDownloadController() {
        return downloadController;
    }

    public TextSaver getTextSaver() {
        return textSaver;
    }

    public SelfDestructProcessor getSelfDestructProcessor() {
        return selfDestructProcessor;
    }

    public EncryptedChatProcessor getEncryptedChatProcessor() {
        return encryptedChatProcessor;
    }

    public VersionHolder getVersionHolder() {
        return versionHolder;
    }

    public TechReflection getTechReflection() {
        return techReflection;
    }

    @Override
    public ImageController getImageController() {
        return controller;
    }
}