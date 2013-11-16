package org.telegram.android.kernel;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import com.extradea.framework.images.ImageController;
import com.extradea.framework.images.workers.CornersWorker;
import com.extradea.framework.images.workers.DownloadWorker;
import com.extradea.framework.images.workers.FileSystemWorker;
import com.extradea.framework.images.workers.ImageWorker;
import org.telegram.android.StelsActivity;
import org.telegram.android.StelsApplication;
import org.telegram.android.config.WallpaperHolder;
import org.telegram.android.core.ApiUtils;
import org.telegram.android.core.Notifications;
import org.telegram.android.core.TextSaver;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.log.Logger;
import org.telegram.android.media.CachedImageWorker;
import org.telegram.android.media.StelsImageWorker;
import org.telegram.android.screens.ScreenLogicType;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.ui.EmojiProcessor;
import org.telegram.android.ui.LastEmojiProcessor;
import org.telegram.android.ui.TextUtil;
import org.telegram.android.ui.UiResponsibility;
import org.telegram.i18n.I18nUtil;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class UiKernel {
    private static final String TAG = "UiKernel";

    private static final int ACTIVE_TIMEOUT = 1000;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                checkGoesOffline();
            }
        }
    };

    private StelsApplication application;

    private ImageController imageController;

    private Notifications notifications;

    private EmojiProcessor emojiProcessor;

    private UiResponsibility responsibility;

    private ScreenLogicType screenLogicType;

    private LastEmojiProcessor lastEmoji;

    private TextSaver textSaver;

    private WallpaperHolder wallpaperHolder;

    private int openedChatPeerId;
    private int openedChatPeerType;

    private boolean isDialogsVisible = false;

    public boolean isAppVisible = false;
    public StelsActivity visibleActivity;

    public boolean isAppActive = true;
    public long lastStartTime;

    public UiKernel(ApplicationKernel kernel) {
        this.application = kernel.getApplication();
        Logger.d(TAG, "Creating ui kernel");
        this.responsibility = new UiResponsibility();
        this.notifications = new Notifications(application);
        this.emojiProcessor = new EmojiProcessor(application);
        this.lastEmoji = new LastEmojiProcessor(application);
        this.textSaver = new TextSaver(application);
        this.wallpaperHolder = new WallpaperHolder(application);
        this.screenLogicType = ScreenLogicType.SINGLE_STATIC;
        this.isAppActive = false;
        this.isAppActive = false;

        imageController = new ImageController(application, new ImageWorker[]{
                new FileSystemWorker(application),
                new DownloadWorker(),
                new CornersWorker(),
                new StelsImageWorker(application),
                new StelsImageWorker(application),
                new CachedImageWorker()
        });

        I18nUtil.init(application);
        TextUtil.init(application);
        AsyncException.initLocalisation(application);
        ApiUtils.init(application, kernel.getTechKernel().getTechReflection().getScreenSize());
    }

    public UiResponsibility getResponsibility() {
        return responsibility;
    }

    public ScreenLogicType getScreenLogicType() {
        return screenLogicType;
    }

    public Notifications getNotifications() {
        return notifications;
    }

    public EmojiProcessor getEmojiProcessor() {
        return emojiProcessor;
    }

    public LastEmojiProcessor getLastEmoji() {
        return lastEmoji;
    }

    public TextSaver getTextSaver() {
        return textSaver;
    }

    public ImageController getImageController() {
        return imageController;
    }

    public WallpaperHolder getWallpaperHolder() {
        return wallpaperHolder;
    }

    public boolean isAppVisible() {
        return isAppVisible;
    }

    public boolean isDialogsVisible() {
        return isDialogsVisible;
    }

    public boolean isAppActive() {
        return isAppActive;
    }

    public void onOpenedChat(int peerType, int peerId) {
        this.openedChatPeerType = peerType;
        this.openedChatPeerId = peerId;
        application.getEngine().markDialogAsNonFailed(peerType, peerId);

        if (peerType != PeerType.PEER_USER_ENCRYPTED) {
            int maxMid = application.getEngine().getMaxMsgInId(peerType, peerId);
            application.getEngine().onMaxLocalViewed(peerType, peerId, maxMid);
        } else {
            int maxDate = application.getEngine().getMaxDateInDialog(peerType, peerId);
            application.getEngine().onMaxLocalViewed(peerType, peerId, maxDate);
        }

        application.getActions().readHistory(peerType, peerId);
        application.getDialogSource().getViewSource().invalidateData();
        getNotifications().hideChatNotifications(peerType, peerId);
    }

    public void onClosedChat(int peerType, int peerId) {
        if (openedChatPeerId == peerId && openedChatPeerType == peerType) {
            this.openedChatPeerType = -1;
            this.openedChatPeerId = -1;
        }
    }

    public StelsActivity getVisibleActivity() {
        return visibleActivity;
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

    public int getOpenedChatPeerId() {
        return openedChatPeerId;
    }

    public int getOpenedChatPeerType() {
        return openedChatPeerType;
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

    private void onAppGoesForeground() {
        emojiProcessor.loadEmoji();
        if (application.getActions() != null) {
            application.getActions().onAppGoesForeground();
        }
        if (application.getEncryptedChatProcessor() != null) {
            application.getEncryptedChatProcessor().onUserGoesOnline();
        }
    }

    private void onAppGoesBackground() {
        if (application.getActions() != null) {
            application.getActions().onAppGoesBackground();
        }
        if (application.getEncryptedChatProcessor() != null) {
            application.getEncryptedChatProcessor().onUserGoesOffline();
        }
    }

    public void logIn() {
        clearState();
    }

    public void logOut() {
        clearState();
    }

    private void clearState() {
        lastEmoji.clearLastSmileys();
        notifications.reset();
        textSaver.reset();
    }
}
