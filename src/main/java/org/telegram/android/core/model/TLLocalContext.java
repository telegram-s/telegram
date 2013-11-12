package org.telegram.android.core.model;

import org.telegram.android.core.model.local.TLLocalUserStatusEmpty;
import org.telegram.android.core.model.local.TLLocalUserStatusOffline;
import org.telegram.android.core.model.local.TLLocalUserStatusOnline;
import org.telegram.android.core.model.media.*;
import org.telegram.android.core.model.service.*;
import org.telegram.android.core.model.storage.*;
import org.telegram.tl.TLContext;

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

        // Media
        registerClass(TLLocalAvatarEmpty.CLASS_ID, TLLocalAvatarEmpty.class);
        registerClass(TLLocalAvatarPhoto.CLASS_ID, TLLocalAvatarPhoto.class);
        registerClass(TLLocalContact.CLASS_ID, TLLocalContact.class);
        registerClass(TLLocalEmpty.CLASS_ID, TLLocalEmpty.class);
        registerClass(TLLocalEncryptedFileLocation.CLASS_ID, TLLocalEncryptedFileLocation.class);
        registerClass(TLLocalFileEmpty.CLASS_ID, TLLocalFileEmpty.class);
        registerClass(TLLocalFileLocation.CLASS_ID, TLLocalFileLocation.class);
        registerClass(TLLocalFileVideoLocation.CLASS_ID, TLLocalFileVideoLocation.class);
        registerClass(TLLocalGeo.CLASS_ID, TLLocalGeo.class);
        registerClass(TLLocalPhoto.CLASS_ID, TLLocalPhoto.class);
        registerClass(TLLocalUnknown.CLASS_ID, TLLocalUnknown.class);
        registerClass(TLLocalVideo.CLASS_ID, TLLocalVideo.class);
        registerClass(TLUploadingPhoto.CLASS_ID, TLUploadingPhoto.class);
        registerClass(TLUploadingVideo.CLASS_ID, TLUploadingVideo.class);

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
    }
}
