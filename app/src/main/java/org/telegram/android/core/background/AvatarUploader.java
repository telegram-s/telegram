package org.telegram.android.core.background;

import android.widget.Toast;
import org.telegram.android.R;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.ApiUtils;
import org.telegram.android.core.background.common.TaskExecutor;
import org.telegram.android.core.files.UploadResult;
import org.telegram.android.core.model.file.AbsFileSource;
import org.telegram.android.core.model.file.FileSource;
import org.telegram.android.core.model.file.FileUriSource;
import org.telegram.android.core.model.media.TLLocalAvatarEmpty;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.media.Optimizer;
import org.telegram.api.*;
import org.telegram.api.photos.TLPhoto;
import org.telegram.api.requests.TLRequestPhotosUploadProfilePhoto;

import java.io.File;
import java.io.FileInputStream;
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

    private StelsApplication application;
    private Random rnd = new Random();

    private AvatarUserUploadListener listener;

    private AbsFileSource uploadingSource;

    private boolean isUploadError;

    public AvatarUploader(StelsApplication application) {
        super(1);
        this.application = application;
    }

    protected String getUploadTempFile(String fileName) {
        return application.getCacheDir().getAbsolutePath() + "/u_" + rnd.nextInt() + fileName;
    }

    public AvatarUserUploadListener getListener() {
        return listener;
    }

    public void setListener(AvatarUserUploadListener listener) {
        this.listener = listener;
    }

    public int getState() {
        if (isExecuting(0)) {
            return STATE_IN_PROGRESS;
        }

        if (isUploadError) {
            return STATE_ERROR;
        }
        return STATE_NONE;
    }

    public AbsFileSource getUploadingSource() {
        return uploadingSource;
    }

    public void uploadAvatar(AbsFileSource fileName) {
        isUploadError = false;
        uploadingSource = fileName;
        requestTask(0, new Task(true, fileName));
    }

    public void cancelUpload() {
        isUploadError = false;
        removeTask(0);
        uploadingSource = null;
        notifyListener();
    }

    public void tryAgain() {
        isUploadError = false;
        uploadAvatar(uploadingSource);
    }

    @Override
    protected void doTask(Task task) {
        if (task.isUserAvatar) {
            try {
                long fileId = rnd.nextLong();

                String destFile = getUploadTempFile("avatar.jpg");

                if (task.getFileSource() instanceof FileSource) {
                    Optimizer.optimize(((FileSource) task.getFileSource()).getFileName(), destFile);
                } else {
                    Optimizer.optimize(((FileUriSource) task.getFileSource()).getUri(), application, destFile);
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

                Toast.makeText(application, R.string.st_avatar_changed, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                isUploadError = true;
                Toast.makeText(application, R.string.st_avatar_change_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onTaskStart(long id, Task Task) {
        if (id == 0) {
            notifyListener();
        }
    }

    @Override
    protected void onTaskEnd(long id, Task Task) {
        if (id == 0) {
            notifyListener();
        }
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onAvatarUploadingStateChanged();
        }
    }

    public static class Task {
        private boolean isUserAvatar;
        private AbsFileSource fileSource;

        public Task(boolean isUserAvatar, AbsFileSource fileSource) {
            this.isUserAvatar = isUserAvatar;
            this.fileSource = fileSource;
        }

        public boolean isUserAvatar() {
            return isUserAvatar;
        }

        public AbsFileSource getFileSource() {
            return fileSource;
        }
    }
}