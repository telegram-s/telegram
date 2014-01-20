package org.telegram.android.core.background;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import org.telegram.android.R;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.ApiUtils;
import org.telegram.android.core.EngineUtils;
import org.telegram.android.core.background.common.TaskExecutor;
import org.telegram.android.core.files.UploadResult;
import org.telegram.android.core.model.file.AbsFileSource;
import org.telegram.android.core.model.file.FileSource;
import org.telegram.android.core.model.file.FileUriSource;
import org.telegram.android.core.model.media.TLLocalAvatarEmpty;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.core.model.update.TLLocalUpdateChatPhoto;
import org.telegram.android.media.Optimizer;
import org.telegram.android.tasks.AsyncException;
import org.telegram.api.*;
import org.telegram.api.messages.TLAbsStatedMessage;
import org.telegram.api.photos.TLPhoto;
import org.telegram.api.requests.TLRequestMessagesEditChatPhoto;
import org.telegram.api.requests.TLRequestPhotosUploadProfilePhoto;
import org.telegram.mtproto.secure.Entropy;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by ex3ndr on 26.12.13.
 */
public class AvatarUploader extends TaskExecutor<AvatarUploader.Task> {

    public static final int STATE_NONE = 0;
    public static final int STATE_IN_PROGRESS = 1;
    public static final int STATE_ERROR = 2;

    public interface AvatarUserUploadListener {
        public void onAvatarUploadingStateChanged();
    }

    public interface AvatarChatUploadListener {
        public void onAvatarUploadingStateChanged(int chatId);
    }

    private TelegramApplication application;
    private Random rnd = new Random();

    private AvatarUserUploadListener listener;
    private AvatarChatUploadListener chatUploadListener;

    private AbsFileSource uploadingSource;
    private boolean isUploadError;

    private HashMap<Integer, AbsFileSource> chatUploadSources = new HashMap<Integer, AbsFileSource>();
    private HashMap<Integer, Boolean> isChatUploadError = new HashMap<Integer, Boolean>();

    private Handler handler = new Handler(Looper.getMainLooper());

    public AvatarUploader(TelegramApplication application) {
        super(1);
        this.application = application;
    }

    public int getGroupUploadState(int chatId) {
        if (isExecuting(chatId)) {
            return STATE_IN_PROGRESS;
        }

        if (isChatUploadError.containsKey(chatId) && isChatUploadError.get(chatId)) {
            return STATE_ERROR;
        }
        return STATE_NONE;
    }

    public int getAvatarUploadState() {
        if (isExecuting(0)) {
            return STATE_IN_PROGRESS;
        }

        if (isUploadError) {
            return STATE_ERROR;
        }
        return STATE_NONE;
    }

    public AbsFileSource getGroupUploadingSource(int chatId) {
        return chatUploadSources.get(chatId);
    }

    public AbsFileSource getAvatarUploadingSource() {
        return uploadingSource;
    }

    public void uploadAvatar(AbsFileSource fileSource) {
        isUploadError = false;
        uploadingSource = fileSource;
        requestTask(0, new AvatarUploadTask(fileSource));
    }

    public void uploadGroup(int chatId, AbsFileSource fileSource) {
        isChatUploadError.remove(chatId);
        chatUploadSources.put(chatId, fileSource);
        requestTask(chatId, new ChatAvatarUploadTask(chatId, fileSource));
    }

    public void cancelUploadGroupAvatar(int chatId) {
        isChatUploadError.remove(chatId);
        removeTask(chatId);
        chatUploadSources.remove(chatId);
        notifyChatListener(chatId);
    }

    public void cancelUploadAvatar() {
        isUploadError = false;
        removeTask(0);
        uploadingSource = null;
        notifyListener();
    }

    public void tryAgainUploadAvatar() {
        isUploadError = false;
        uploadAvatar(uploadingSource);
    }

    public void tryAgainUploadGroup(int chatId) {
        isChatUploadError.remove(chatId);
        uploadGroup(chatId, chatUploadSources.get(chatId));
    }

    @Override
    protected void doTask(Task task) {
        if (task instanceof AvatarUploadTask) {
            try {
                long fileId = rnd.nextLong();

                String destFile = getUploadTempFile("avatar.jpg");

                AbsFileSource fileSource = ((AvatarUploadTask) task).getFileSource();
                if (fileSource instanceof FileSource) {
                    Optimizer.optimize(((FileSource) fileSource).getFileName(), destFile);
                } else {
                    Optimizer.optimize(((FileUriSource) fileSource).getUri(), application, destFile);
                }

                File file = new File(destFile);
                FileInputStream fis = new FileInputStream(file);
                UploadResult res = application.getUploadController().uploadFile(fis, (int) file.length(), fileId);

                TLPhoto photo = application.getApi().doRpcCall(new TLRequestPhotosUploadProfilePhoto(
                        new TLInputFile(fileId, res.getPartsCount(), "photo.jpg", res.getHash()),
                        "MyPhoto", new TLInputGeoPointEmpty(), new TLInputPhotoCropAuto()));

                if (photo.getPhoto() instanceof org.telegram.api.TLPhoto) {
                    org.telegram.api.TLPhoto srcPhoto = (org.telegram.api.TLPhoto) photo.getPhoto();
                    TLPhotoSize smallPhotoSize = (TLPhotoSize) ApiUtils.findSmallest(srcPhoto);
                    TLFileLocation smallLocation = (TLFileLocation) smallPhotoSize.getLocation();
                    TLPhotoSize largePhotoSize = (TLPhotoSize) ApiUtils.findLargest(srcPhoto);
                    TLFileLocation largeLocation = (TLFileLocation) largePhotoSize.getLocation();
                    TLLocalAvatarPhoto profilePhoto = new TLLocalAvatarPhoto();
                    profilePhoto.setPreviewLocation(new TLLocalFileLocation(smallLocation.getDcId(), smallLocation.getVolumeId(), smallLocation.getLocalId(), smallLocation.getSecret(), smallPhotoSize.getSize()));
                    profilePhoto.setFullLocation(new TLLocalFileLocation(largeLocation.getDcId(), largeLocation.getVolumeId(), largeLocation.getLocalId(), largeLocation.getSecret(), largePhotoSize.getSize()));
                    application.getEngine().getUsersEngine().onUserPhotoChanges(application.getCurrentUid(), profilePhoto);
                } else {
                    application.getEngine().getUsersEngine().onUserPhotoChanges(application.getCurrentUid(), new TLLocalAvatarEmpty());
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(application, R.string.st_avatar_changed, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                isUploadError = true;

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(application, R.string.st_avatar_change_error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else if (task instanceof ChatAvatarUploadTask) {
            AbsFileSource fileSource = ((ChatAvatarUploadTask) task).getFileSource();
            int chatId = ((ChatAvatarUploadTask) task).getChatId();
            try {

                long fileId = Entropy.generateRandomId();
                String destFile = getUploadTempFile("group.jpg");

                if (fileSource instanceof FileSource) {
                    Optimizer.optimize(((FileSource) fileSource).getFileName(), destFile);
                } else {
                    Optimizer.optimize(((FileUriSource) fileSource).getUri(), application, destFile);
                }
                File file = new File(destFile);
                int len = (int) file.length();
                FileInputStream stream = new FileInputStream(file);
                UploadResult res = application.getUploadController().uploadFile(stream, len, fileId);
                if (res == null)
                    throw new AsyncException(AsyncException.ExceptionType.CONNECTION_ERROR);

                TLAbsStatedMessage message = application.getApi().doRpcCall(new TLRequestMessagesEditChatPhoto(chatId,
                        new TLInputChatUploadedPhoto(
                                new TLInputFile(fileId, res.getPartsCount(), "photo.jpg", res.getHash()),
                                new TLInputPhotoCropAuto())));
                TLMessageService service = (TLMessageService) message.getMessage();
                TLMessageActionChatEditPhoto editPhoto = (TLMessageActionChatEditPhoto) service.getAction();

                application.getEngine().onUsers(message.getUsers());
                application.getEngine().getGroupsEngine().onGroupsUpdated(message.getChats());
                application.getEngine().onUpdatedMessage(message.getMessage());
                application.getEngine().onChatAvatarChanges(chatId, EngineUtils.convertAvatarPhoto(editPhoto.getPhoto()));
                application.getUpdateProcessor().onMessage(new TLLocalUpdateChatPhoto(message));

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(application, R.string.st_avatar_group_changed, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                isChatUploadError.put(chatId, true);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(application, R.string.st_avatar_group_error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    @Override
    protected void onTaskStart(long id, Task Task) {
        if (id == 0) {
            notifyListener();
        } else {
            notifyChatListener((int) id);
        }
    }

    @Override
    protected void onTaskEnd(long id, Task Task) {
        if (id == 0) {
            notifyListener();
        } else {
            notifyChatListener((int) id);
        }
    }

    public AvatarUserUploadListener getListener() {
        return listener;
    }

    public void setListener(AvatarUserUploadListener listener) {
        this.listener = listener;
    }

    public AvatarChatUploadListener getChatUploadListener() {
        return chatUploadListener;
    }

    public void setChatUploadListener(AvatarChatUploadListener chatUploadListener) {
        this.chatUploadListener = chatUploadListener;
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onAvatarUploadingStateChanged();
        }
    }

    private void notifyChatListener(int chatId) {
        if (chatUploadListener != null) {
            chatUploadListener.onAvatarUploadingStateChanged(chatId);
        }
    }

    private String getUploadTempFile(String fileName) {
        return application.getCacheDir().getAbsolutePath() + "/u_" + rnd.nextInt() + fileName;
    }

    public static abstract class Task {

    }

    public static class AvatarUploadTask extends Task {
        private AbsFileSource fileSource;

        public AvatarUploadTask(AbsFileSource fileSource) {
            this.fileSource = fileSource;
        }

        public AbsFileSource getFileSource() {
            return fileSource;
        }
    }

    public static class ChatAvatarUploadTask extends Task {
        private int chatId;
        private AbsFileSource fileSource;

        public ChatAvatarUploadTask(int chatId, AbsFileSource fileSource) {
            this.chatId = chatId;
            this.fileSource = fileSource;
        }

        public int getChatId() {
            return chatId;
        }

        public AbsFileSource getFileSource() {
            return fileSource;
        }
    }
}