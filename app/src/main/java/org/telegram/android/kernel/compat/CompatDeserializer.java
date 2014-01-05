package org.telegram.android.kernel.compat;

import android.net.Uri;
import org.telegram.android.core.model.TLLocalContext;
import org.telegram.android.core.model.local.TLLocalUserStatusEmpty;
import org.telegram.android.core.model.local.TLLocalUserStatusOffline;
import org.telegram.android.core.model.local.TLLocalUserStatusOnline;
import org.telegram.android.core.model.media.*;
import org.telegram.android.kernel.compat.v1.TLUserStatusEmptyCompat;
import org.telegram.android.kernel.compat.v1.TLUserStatusOfflineCompat;
import org.telegram.android.kernel.compat.v1.TLUserStatusOnlineCompat;
import org.telegram.android.kernel.compat.v4.*;
import org.telegram.mtproto.log.Logger;
import org.telegram.tl.StreamingUtils;
import org.telegram.tl.TLObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created by ex3ndr on 22.11.13.
 */
public class CompatDeserializer {

    private static final String TAG = "CompatDeserializer";

    private static Object convert(Object object) {
        if (object instanceof TLUserStatusEmptyCompat) {
            return new TLLocalUserStatusEmpty();
        } else if (object instanceof TLUserStatusOnlineCompat) {
            return new TLLocalUserStatusOnline(((TLUserStatusOnlineCompat) object).getExpires());
        } else if (object instanceof TLUserStatusOfflineCompat) {
            return new TLLocalUserStatusOffline(((TLUserStatusOfflineCompat) object).getWasOnline());
        } else if (object instanceof CompatTLLocalAvatarEmpty) {
            return new TLLocalAvatarEmpty();
        } else if (object instanceof CompatTLLocalAvatarPhoto) {
            CompatTLLocalAvatarPhoto avatarPhoto = (CompatTLLocalAvatarPhoto) object;
            TLLocalAvatarPhoto res = new TLLocalAvatarPhoto();
            res.setPreviewLocation((TLAbsLocalFileLocation) convert(avatarPhoto.getPreviewLocation()));
            res.setFullLocation((TLAbsLocalFileLocation) convert(avatarPhoto.getFullLocation()));
            return res;
        } else if (object instanceof CompatTLLocalFileLocation) {
            CompatTLLocalFileLocation fileLocation = (CompatTLLocalFileLocation) object;
            return new TLLocalFileLocation(fileLocation.getDcId(), fileLocation.getVolumeId(), fileLocation.getLocalId(), fileLocation.getSecret(), fileLocation.getSize());
        } else if (object instanceof CompatTLLocalFileVideoLocation) {
            CompatTLLocalFileVideoLocation videoLocation = (CompatTLLocalFileVideoLocation) object;
            return new TLLocalFileVideoLocation(videoLocation.getDcId(), videoLocation.getVideoId(), videoLocation.getAccessHash(), videoLocation.getSize());
        } else if (object instanceof CompatTLLocalEncryptedFileLocation) {
            CompatTLLocalEncryptedFileLocation encryptedFileLocation = (CompatTLLocalEncryptedFileLocation) object;
            return new TLLocalEncryptedFileLocation(encryptedFileLocation.getId(), encryptedFileLocation.getAccessHash(), encryptedFileLocation.getSize(), encryptedFileLocation.getDcId(), encryptedFileLocation.getKey(), encryptedFileLocation.getIv());
        } else if (object instanceof CompatTLLocalFileEmpty) {
            return new TLLocalFileEmpty();
        } else if (object instanceof CompatTLLocalEmpty) {
            return new TLLocalEmpty();
        } else if (object instanceof CompatTLLocalUnknown) {
            return new TLLocalUnknown(((CompatTLLocalUnknown) object).getData());
        } else if (object instanceof CompatTLLocalContact) {
            CompatTLLocalContact localContact = (CompatTLLocalContact) object;
            return new TLLocalContact(localContact.getPhoneNumber(), localContact.getFirstName(), localContact.getLastName(), localContact.getUserId());
        } else if (object instanceof CompatTLLocalGeo) {
            CompatTLLocalGeo localGeo = (CompatTLLocalGeo) object;
            return new TLLocalGeo(localGeo.getLatitude(), localGeo.getLongitude());
        } else if (object instanceof CompatTLLocalPhoto) {
            CompatTLLocalPhoto localPhoto = (CompatTLLocalPhoto) object;
            TLLocalPhoto res = new TLLocalPhoto();
            res.setFastPreview(localPhoto.getFastPreview());
            res.setFastPreviewH(localPhoto.getFastPreviewH());
            res.setFastPreviewW(localPhoto.getFastPreviewW());
            res.setFastPreviewKey(localPhoto.getFastPreviewKey());
            res.setFullH(localPhoto.getFullH());
            res.setFullW(localPhoto.getFullW());
            res.setFullLocation((TLAbsLocalFileLocation) convert(localPhoto.getFullLocation()));
            return res;
        } else if (object instanceof CompatTLLocalVideo) {
            CompatTLLocalVideo localVideo = (CompatTLLocalVideo) object;
            TLLocalVideo res = new TLLocalVideo();
            res.setDuration(localVideo.getDuration());
            res.setFastPreview(localVideo.getFastPreview());
            res.setPreviewH(localVideo.getPreviewH());
            res.setPreviewW(localVideo.getPreviewW());
            res.setPreviewKey(localVideo.getPreviewKey());
            res.setPreviewLocation((TLAbsLocalFileLocation) convert(localVideo.getPreviewLocation()));
            res.setVideoLocation((TLAbsLocalFileLocation) convert(localVideo.getVideoLocation()));
            return res;
        } else if (object instanceof CompatTLUploadingPhoto) {
            CompatTLUploadingPhoto photo = (CompatTLUploadingPhoto) object;
            if (photo.getFileName() != null && photo.getFileName().length() > 0) {
                return new TLUploadingPhoto(photo.getWidth(), photo.getHeight(), photo.getFileName());
            } else {
                return new TLUploadingPhoto(photo.getWidth(), photo.getHeight(), Uri.parse(photo.getFileUri()));
            }
        } else if (object instanceof CompatTLUploadingVideo) {
            CompatTLUploadingVideo video = (CompatTLUploadingVideo) object;
            TLUploadingVideo res = new TLUploadingVideo(video.getFileName(), video.getPreviewWidth(), video.getPreviewHeight());
            return res;
        } else if (object == null) {
            return null;
        } else {
            Logger.d(TAG, "Unknown java object:" + object.toString());
            return null;
        }
    }

    public static TLObject deserialize(byte[] data) {
        if (data.length < 4) {
            return null;
        }
        if ((data[0] & 0xFF) == 0xAC && (data[1] & 0xFF) == 0xED) {
            Logger.d(TAG, "Founded java object");
            try {
                CompatObjectInputStream inputStream = new CompatObjectInputStream(new ByteArrayInputStream(data), Compats.MIGRATE_DB_1);
                Object object = inputStream.readObject();
                return (TLObject) convert(object);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            // MIGRATE_DB_1
            return null;
        }
        try {
            return TLLocalContext.getInstance().deserializeMessage(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
