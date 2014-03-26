package org.telegram.android.core.model;

import org.telegram.android.core.model.local.*;
import org.telegram.android.core.model.media.*;
import org.telegram.android.core.model.notifications.TLNotificationRecord;
import org.telegram.android.core.model.notifications.TLNotificationState;
import org.telegram.android.core.model.phone.TLImportedPhone;
import org.telegram.android.core.model.phone.TLSyncContact;
import org.telegram.android.core.model.phone.TLSyncPhone;
import org.telegram.android.core.model.phone.TLSyncState;
import org.telegram.android.core.model.service.*;
import org.telegram.android.core.model.storage.*;
import org.telegram.android.core.model.web.TLLastSearchResults;
import org.telegram.android.core.model.web.TLSearchResult;
import org.telegram.android.kernel.compat.v5.TLDcInfoCompat;
import org.telegram.android.kernel.compat.v8.TLLocalPhotoCompat8;
import org.telegram.android.kernel.compat.v9.TLLocalPhotoCompat9;
import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 07.11.13
 * Time: 21:16
 */
public class TLLocalContext extends TLContext {
    private static TLLocalContext instance = new TLLocalContext();

    public static TLLocalContext getInstance() {
        return instance;
    }

    @Override
    protected void init() {
        // Local
        registerClass(TLLocalUserStatusOnline.CLASS_ID, TLLocalUserStatusOnline.class);
        registerClass(TLLocalUserStatusOffline.CLASS_ID, TLLocalUserStatusOffline.class);
        registerClass(TLLocalUserStatusEmpty.CLASS_ID, TLLocalUserStatusEmpty.class);
        registerClass(TLLocalFullChatInfo.CLASS_ID, TLLocalFullChatInfo.class);
        registerClass(TLLocalChatParticipant.CLASS_ID, TLLocalChatParticipant.class);

        // Media
        registerClass(TLLocalAvatarEmpty.CLASS_ID, TLLocalAvatarEmpty.class);
        registerClass(TLLocalAvatarPhoto.CLASS_ID, TLLocalAvatarPhoto.class);
        registerClass(TLLocalContact.CLASS_ID, TLLocalContact.class);
        registerClass(TLLocalEmpty.CLASS_ID, TLLocalEmpty.class);
        registerClass(TLLocalEncryptedFileLocation.CLASS_ID, TLLocalEncryptedFileLocation.class);
        registerClass(TLLocalFileEmpty.CLASS_ID, TLLocalFileEmpty.class);
        registerClass(TLLocalFileLocation.CLASS_ID, TLLocalFileLocation.class);
        registerClass(TLLocalFileVideoLocation.CLASS_ID, TLLocalFileVideoLocation.class);
        registerClass(TLLocalFileDocument.CLASS_ID, TLLocalFileDocument.class);
        registerClass(TLLocalFileAudio.CLASS_ID, TLLocalFileAudio.class);
        registerClass(TLLocalGeo.CLASS_ID, TLLocalGeo.class);
        registerClass(TLLocalPhoto.CLASS_ID, TLLocalPhoto.class);
        registerClass(TLLocalUnknown.CLASS_ID, TLLocalUnknown.class);
        registerClass(TLLocalVideo.CLASS_ID, TLLocalVideo.class);
        registerClass(TLLocalDocument.CLASS_ID, TLLocalDocument.class);
        registerClass(TLLocalAudio.CLASS_ID, TLLocalAudio.class);
        registerClass(TLUploadingPhoto.CLASS_ID, TLUploadingPhoto.class);
        registerClass(TLUploadingVideo.CLASS_ID, TLUploadingVideo.class);
        registerClass(TLUploadingDocument.CLASS_ID, TLUploadingDocument.class);
        registerClass(TLUploadingAudio.CLASS_ID, TLUploadingAudio.class);
        registerClass(TLSearchResult.CLASS_ID, TLSearchResult.class);
        registerClass(TLLastSearchResults.CLASS_ID, TLLastSearchResults.class);

        // Service
        registerClass(TLLocalActionChatAddUser.CLASS_ID, TLLocalActionChatAddUser.class);
        registerClass(TLLocalActionChatCreate.CLASS_ID, TLLocalActionChatCreate.class);
        registerClass(TLLocalActionChatDeletePhoto.CLASS_ID, TLLocalActionChatDeletePhoto.class);
        registerClass(TLLocalActionChatDeleteUser.CLASS_ID, TLLocalActionChatDeleteUser.class);
        registerClass(TLLocalActionChatEditPhoto.CLASS_ID, TLLocalActionChatEditPhoto.class);
        registerClass(TLLocalActionChatEditTitle.CLASS_ID, TLLocalActionChatEditTitle.class);
        registerClass(TLLocalActionEncryptedCancelled.CLASS_ID, TLLocalActionEncryptedCancelled.class);
        registerClass(TLLocalActionEncryptedCreated.CLASS_ID, TLLocalActionEncryptedCreated.class);
        registerClass(TLLocalActionEncryptedMessageDestructed.CLASS_ID, TLLocalActionEncryptedMessageDestructed.class);
        registerClass(TLLocalActionEncryptedRequested.CLASS_ID, TLLocalActionEncryptedRequested.class);
        registerClass(TLLocalActionEncryptedTtl.CLASS_ID, TLLocalActionEncryptedTtl.class);
        registerClass(TLLocalActionEncryptedWaiting.CLASS_ID, TLLocalActionEncryptedWaiting.class);
        registerClass(TLLocalActionUnknown.CLASS_ID, TLLocalActionUnknown.class);
        registerClass(TLLocalActionUserEditPhoto.CLASS_ID, TLLocalActionUserEditPhoto.class);
        registerClass(TLLocalActionUserRegistered.CLASS_ID, TLLocalActionUserRegistered.class);

        // Storage
        registerClass(TLDcInfo.CLASS_ID, TLDcInfo.class);
        registerClass(TLKey.CLASS_ID, TLKey.class);
        registerClass(TLLastKnownSalt.CLASS_ID, TLLastKnownSalt.class);
        registerClass(TLOldSession.CLASS_ID, TLOldSession.class);
        registerClass(TLStorage.CLASS_ID, TLStorage.class);

        // Compat
        registerCompatClass(TLDcInfoCompat.CLASS_ID, TLDcInfoCompat.class);
        registerCompatClass(TLLocalPhotoCompat8.CLASS_ID, TLLocalPhotoCompat8.class);
        registerCompatClass(TLLocalPhotoCompat9.CLASS_ID, TLLocalPhotoCompat9.class);

        // UpdateState
        registerClass(TLUpdateState.CLASS_ID, TLUpdateState.class);

        // PhoneBook
        registerClass(TLImportedPhone.CLASS_ID, TLImportedPhone.class);
        registerClass(TLSyncContact.CLASS_ID, TLSyncContact.class);
        registerClass(TLSyncPhone.CLASS_ID, TLSyncPhone.class);
        registerClass(TLSyncState.CLASS_ID, TLSyncState.class);

        // Notifications
        registerClass(TLNotificationRecord.CLASS_ID, TLNotificationRecord.class);
        registerClass(TLNotificationState.CLASS_ID, TLNotificationState.class);
    }

    @Override
    protected TLObject convertCompatClass(TLObject src) {
        if (src instanceof TLDcInfoCompat) {
            TLDcInfoCompat compat = (TLDcInfoCompat) src;
            return new TLDcInfo(compat.getDcId(), compat.getAddress(), compat.getPort(), 0);
        } else if (src instanceof TLLocalPhotoCompat8) {
            TLLocalPhotoCompat8 localPhoto = (TLLocalPhotoCompat8) src;
            TLLocalPhoto res = new TLLocalPhoto();
            res.setFastPreview(localPhoto.getFastPreview());
            res.setFastPreviewH(localPhoto.getFastPreviewH());
            res.setFastPreviewW(localPhoto.getFastPreviewW());
            res.setFastPreviewKey(localPhoto.getFastPreviewKey());
            res.setFullH(localPhoto.getFullH());
            res.setFullW(localPhoto.getFullW());
            res.setFullLocation(localPhoto.getFullLocation());
            res.setOptimization(TLLocalPhoto.OPTIMIZATION_NONE);
            return res;
        } else if (src instanceof TLLocalPhotoCompat9) {
            TLLocalPhotoCompat9 localPhoto = (TLLocalPhotoCompat9) src;
            TLLocalPhoto res = new TLLocalPhoto();
            res.setFastPreview(localPhoto.getFastPreview());
            res.setFastPreviewH(localPhoto.getFastPreviewH());
            res.setFastPreviewW(localPhoto.getFastPreviewW());
            res.setFastPreviewKey(localPhoto.getFastPreviewKey());
            res.setFullH(localPhoto.getFullH());
            res.setFullW(localPhoto.getFullW());
            res.setFullLocation(localPhoto.getFullLocation());
            res.setOptimization(localPhoto.isOptimized() ? TLLocalPhoto.OPTIMIZATION_RESIZE : TLLocalPhoto.OPTIMIZATION_NONE);
            return res;
        }

        return src;
    }
}
