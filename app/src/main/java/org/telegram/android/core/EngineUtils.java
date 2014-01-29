package org.telegram.android.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.local.TLAbsLocalUserStatus;
import org.telegram.android.core.model.local.TLLocalUserStatusEmpty;
import org.telegram.android.core.model.local.TLLocalUserStatusOffline;
import org.telegram.android.core.model.local.TLLocalUserStatusOnline;
import org.telegram.android.core.model.media.*;
import org.telegram.android.core.model.service.*;
import org.telegram.android.ui.BitmapUtils;
import org.telegram.api.*;
import org.telegram.tl.TLObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 19:28
 */
public class EngineUtils {

    public static TLAbsLocalAvatarPhoto convertAvatarPhoto(TLAbsChatPhoto chatPhoto) {
        if (chatPhoto instanceof TLChatPhoto) {
            TLChatPhoto tlChatPhoto = (TLChatPhoto) chatPhoto;
            if (tlChatPhoto.getPhotoSmall() instanceof TLFileLocation) {
                TLFileLocation location = (TLFileLocation) tlChatPhoto.getPhotoSmall();
                TLLocalAvatarPhoto photo = new TLLocalAvatarPhoto();
                photo.setPreviewLocation(new TLLocalFileLocation(location.getDcId(), location.getVolumeId(), location.getLocalId(), location.getSecret(), -1));

                if (tlChatPhoto.getPhotoBig() instanceof TLFileLocation) {
                    TLFileLocation locationBig = (TLFileLocation) tlChatPhoto.getPhotoBig();
                    photo.setFullLocation(new TLLocalFileLocation(locationBig.getDcId(), locationBig.getVolumeId(), locationBig.getLocalId(), locationBig.getSecret(), -1));
                } else {
                    photo.setFullLocation(new TLLocalFileEmpty());
                }

                return photo;
            } else {
                return new TLLocalAvatarEmpty();
            }
        } else {
            return new TLLocalAvatarEmpty();
        }
    }

    public static TLAbsLocalAvatarPhoto convertAvatarPhoto(TLAbsUserProfilePhoto profilePhoto) {
        if (profilePhoto instanceof TLUserProfilePhoto) {
            TLUserProfilePhoto tlUserProfilePhoto = (TLUserProfilePhoto) profilePhoto;
            if (tlUserProfilePhoto.getPhotoSmall() instanceof TLFileLocation) {
                TLFileLocation location = (TLFileLocation) tlUserProfilePhoto.getPhotoSmall();
                TLLocalAvatarPhoto photo = new TLLocalAvatarPhoto();
                photo.setPreviewLocation(new TLLocalFileLocation(location.getDcId(), location.getVolumeId(), location.getLocalId(), location.getSecret(), -1));

                if (tlUserProfilePhoto.getPhotoBig() instanceof TLFileLocation) {
                    TLFileLocation locationBig = (TLFileLocation) tlUserProfilePhoto.getPhotoBig();
                    photo.setFullLocation(new TLLocalFileLocation(locationBig.getDcId(), locationBig.getVolumeId(), locationBig.getLocalId(), locationBig.getSecret(), -1));
                } else {
                    photo.setFullLocation(new TLLocalFileEmpty());
                }

                return photo;
            } else {
                return new TLLocalAvatarEmpty();
            }
        } else {
            return new TLLocalAvatarEmpty();
        }
    }

    public static TLAbsLocalAvatarPhoto convertAvatarPhoto(TLAbsPhoto photo) {
        if (photo instanceof TLPhoto) {
            TLPhoto tlPhoto = (TLPhoto) photo;
            TLFileLocation size = ApiUtils.getAvatarPhoto(tlPhoto);
            if (size != null) {
                TLLocalAvatarPhoto res = new TLLocalAvatarPhoto();
                res.setPreviewLocation(new TLLocalFileLocation(size.getDcId(), size.getVolumeId(), size.getLocalId(), size.getSecret(), -1));

                TLPhotoSize photoSize = ApiUtils.findDownloadSize(tlPhoto);
                if (photoSize != null) {
                    if (photoSize.getLocation() instanceof TLFileLocation) {
                        TLFileLocation bigLocation = (TLFileLocation) photoSize.getLocation();
                        res.setPreviewLocation(new TLLocalFileLocation(bigLocation.getDcId(), bigLocation.getVolumeId(), bigLocation.getLocalId(), bigLocation.getSecret(), photoSize.getSize()));
                    } else {
                        res.setFullLocation(new TLLocalFileEmpty());
                    }
                } else {
                    res.setFullLocation(new TLLocalFileEmpty());
                }
                return res;
            }
            return new TLLocalAvatarEmpty();
        } else {
            return new TLLocalAvatarEmpty();
        }
    }

    public static TLAbsLocalAction convertAction(TLAbsMessageAction action) {
        if (action instanceof TLMessageActionChatAddUser) {
            return new TLLocalActionChatAddUser(((TLMessageActionChatAddUser) action).getUserId());
        } else if (action instanceof TLMessageActionChatDeleteUser) {
            return new TLLocalActionChatDeleteUser(((TLMessageActionChatDeleteUser) action).getUserId());
        } else if (action instanceof TLMessageActionChatCreate) {
            return new TLLocalActionChatCreate();
        } else if (action instanceof TLMessageActionChatDeletePhoto) {
            return new TLLocalActionChatDeletePhoto();
        } else if (action instanceof TLMessageActionChatEditPhoto) {
            TLLocalActionChatEditPhoto photo = new TLLocalActionChatEditPhoto();
            photo.setPhoto((TLLocalAvatarPhoto) convertAvatarPhoto((((TLMessageActionChatEditPhoto) action).getPhoto())));
            return photo;
        } else if (action instanceof TLMessageActionChatEditTitle) {
            return new TLLocalActionChatEditTitle(((TLMessageActionChatEditTitle) action).getTitle());
        }

        return new TLLocalActionUnknown();
    }

    public static TLObject convertMedia(TLAbsMessageMedia messageMedia) {
        if (messageMedia instanceof TLMessageMediaPhoto) {
            return convertPhoto((TLMessageMediaPhoto) messageMedia);
        } else if (messageMedia instanceof TLMessageMediaVideo) {
            return convertVideo((TLMessageMediaVideo) messageMedia);
        } else if (messageMedia instanceof TLMessageMediaContact) {
            TLLocalContact contact = new TLLocalContact();
            contact.setFirstName(((TLMessageMediaContact) messageMedia).getFirstName());
            contact.setLastName(((TLMessageMediaContact) messageMedia).getLastName());
            contact.setPhoneNumber(((TLMessageMediaContact) messageMedia).getPhoneNumber());
            contact.setUserId(((TLMessageMediaContact) messageMedia).getUserId());
            return contact;
        } else if (messageMedia instanceof TLMessageMediaGeo) {
            if (((TLMessageMediaGeo) messageMedia).getGeo() instanceof TLGeoPointEmpty) {
                return new TLLocalGeo(0, 0);
            } else {
                TLGeoPoint point = (TLGeoPoint) ((TLMessageMediaGeo) messageMedia).getGeo();
                return new TLLocalGeo(point.getLat(), point.getLon());
            }
        } else if (messageMedia instanceof TLMessageMediaUnsupported) {
            return new TLLocalUnknown(((TLMessageMediaUnsupported) messageMedia).getBytes());
        } else if (messageMedia instanceof TLMessageMediaDocument) {
            TLAbsDocument document = ((TLMessageMediaDocument) messageMedia).getDocument();
            if (document instanceof TLDocument) {
                TLDocument doc = (TLDocument) document;
                TLLocalDocument res = new TLLocalDocument(
                        new TLLocalFileDocument(doc.getId(), doc.getAccessHash(), doc.getSize(), doc.getDcId()),
                        doc.getUserId(), doc.getDate(), doc.getFileName(), doc.getMimeType());

                if (doc.getThumb() instanceof TLPhotoCachedSize) {
                    TLPhotoCachedSize cachedSize = (TLPhotoCachedSize) doc.getThumb();
                    res.setFastPreview(cachedSize.getBytes(), cachedSize.getW(), cachedSize.getH());
                } else if (doc.getThumb() instanceof TLPhotoSize) {
                    TLPhotoSize photoSize = (TLPhotoSize) doc.getThumb();
                    if (photoSize.getLocation() instanceof TLFileLocation) {
                        TLFileLocation location = (TLFileLocation) photoSize.getLocation();
                        res.setPreview(new TLLocalFileLocation(location.getDcId(), location.getVolumeId(), location.getLocalId(), location.getSecret(), photoSize.getSize()),
                                photoSize.getW(), photoSize.getH());
                    }
                }

                return res;
            } else {
                return new TLLocalDocument();
            }
        } else if (messageMedia instanceof TLMessageMediaAudio) {
            TLMessageMediaAudio mediaAudio = (TLMessageMediaAudio) messageMedia;
            if (mediaAudio.getAudio() instanceof TLAudio) {
                TLAudio audio = (TLAudio) mediaAudio.getAudio();
                return new TLLocalAudio(
                        new TLLocalFileAudio(audio.getId(), audio.getAccessHash(), audio.getSize(), audio.getDcId()),
                        audio.getDuration());
            }
            return new TLLocalAudio();
        } else if (messageMedia instanceof TLMessageMediaEmpty) {
            return new TLLocalEmpty();
        } else {
            try {
                return new TLLocalUnknown(messageMedia.serialize());
            } catch (IOException e) {
                e.printStackTrace();
                return new TLLocalEmpty();
            }
        }
    }

    public static TLLocalVideo convertVideo(TLMessageMediaVideo video) {
        if (video.getVideo() instanceof TLVideo) {
            TLVideo tlVideo = (TLVideo) video.getVideo();
            TLLocalVideo res = new TLLocalVideo();
            if (tlVideo.getThumb() instanceof TLPhotoCachedSize) {
                TLPhotoCachedSize cachedSize = (TLPhotoCachedSize) tlVideo.getThumb();
                res.setFastPreview(cachedSize.getBytes());
                res.setPreviewW(cachedSize.getW());
                res.setPreviewH(cachedSize.getH());
                if (cachedSize.getLocation() instanceof TLFileLocation) {
                    TLFileLocation location = (TLFileLocation) cachedSize.getLocation();
                    res.setPreviewLocation(new TLLocalFileLocation(location.getDcId(), location.getVolumeId(), location.getLocalId(), location.getSecret(), cachedSize.getBytes().length));
                    res.setPreviewKey(location.getVolumeId() + "_" + location.getLocalId());
                } else if (cachedSize.getLocation() instanceof TLFileLocationUnavailable) {
                    TLFileLocationUnavailable location = (TLFileLocationUnavailable) cachedSize.getLocation();
                    res.setPreviewLocation(new TLLocalFileEmpty());
                    res.setPreviewKey(location.getVolumeId() + "_" + location.getLocalId());
                } else {
                    res.setPreviewLocation(new TLLocalFileEmpty());
                    res.setPreviewKey("");
                }
            } else if (tlVideo.getThumb() instanceof TLPhotoSize) {
                TLPhotoSize photoSize = (TLPhotoSize) tlVideo.getThumb();
                res.setFastPreview(new byte[0]);
                res.setPreviewW(photoSize.getW());
                res.setPreviewH(photoSize.getH());
                if (photoSize.getLocation() instanceof TLFileLocation) {
                    TLFileLocation location = (TLFileLocation) photoSize.getLocation();
                    res.setPreviewLocation(new TLLocalFileLocation(location.getDcId(), location.getVolumeId(), location.getLocalId(), location.getSecret(), photoSize.getSize()));
                    res.setPreviewKey(location.getVolumeId() + "_" + location.getLocalId());
                } else if (photoSize.getLocation() instanceof TLFileLocationUnavailable) {
                    TLFileLocationUnavailable location = (TLFileLocationUnavailable) photoSize.getLocation();
                    res.setPreviewLocation(new TLLocalFileEmpty());
                    res.setPreviewKey(location.getVolumeId() + "_" + location.getLocalId());
                } else {
                    res.setPreviewLocation(new TLLocalFileEmpty());
                    res.setPreviewKey("");
                }
            } else {
                res.setFastPreview(new byte[0]);
                res.setPreviewH(0);
                res.setPreviewW(0);
                res.setPreviewLocation(new TLLocalFileEmpty());
                res.setPreviewKey("");
            }

            res.setDuration(tlVideo.getDuration());
            res.setVideoLocation(new TLLocalFileVideoLocation(tlVideo.getDcId(), tlVideo.getId(), tlVideo.getAccessHash(), tlVideo.getSize()));
            return res;
        } else {
            TLLocalVideo res = new TLLocalVideo();
            res.setPreviewH(0);
            res.setPreviewW(0);
            res.setDuration(0);
            res.setFastPreview(new byte[0]);
            res.setPreviewKey("");
            res.setPreviewLocation(new TLLocalFileEmpty());
            res.setVideoLocation(new TLLocalFileEmpty());
            return res;
        }
    }

    public static TLLocalPhoto convertPhoto(TLMessageMediaPhoto src) {
        if (src.getPhoto() instanceof TLPhoto) {
            TLLocalPhoto res = new TLLocalPhoto();
            TLPhotoCachedSize cachedSize = ApiUtils.findCachedSize((TLPhoto) src.getPhoto());
            if (cachedSize != null) {
                res.setFastPreview(cachedSize.getBytes());
                res.setFastPreviewW(cachedSize.getW());
                res.setFastPreviewH(cachedSize.getH());

                if (cachedSize.getLocation() instanceof TLFileLocation) {
                    TLFileLocation location = (TLFileLocation) cachedSize.getLocation();
                    res.setFastPreviewKey(location.getVolumeId() + "." + location.getLocalId());
                } else {
                    TLFileLocationUnavailable location = (TLFileLocationUnavailable) cachedSize.getLocation();
                    res.setFastPreviewKey(location.getVolumeId() + "." + location.getLocalId());
                }
            } else {
                res.setFastPreview(new byte[0]);
                res.setFastPreviewKey("");
                res.setFastPreviewH(0);
                res.setFastPreviewW(0);
                res.setOptimized(false);
            }

            TLPhotoSize downloadSize = ApiUtils.findDownloadSize((TLPhoto) src.getPhoto());
            if (downloadSize != null) {
                if (downloadSize.getLocation() instanceof TLFileLocation) {
                    res.setFullW(downloadSize.getW());
                    res.setFullH(downloadSize.getH());
                    TLFileLocation location = (TLFileLocation) downloadSize.getLocation();
                    res.setFullLocation(new TLLocalFileLocation(location.getDcId(), location.getVolumeId(), location.getLocalId(), location.getSecret(), downloadSize.getSize()));
                } else {
                    res.setFullH(0);
                    res.setFullW(0);
                    res.setFullLocation(new TLLocalFileEmpty());
                }
            } else {
                res.setFullH(0);
                res.setFullW(0);
                res.setFullLocation(new TLLocalFileEmpty());
            }
            return res;
        } else {
            TLLocalPhoto res = new TLLocalPhoto();
            res.setFastPreview(new byte[0]);
            res.setFastPreviewKey("");
            res.setFastPreviewH(0);
            res.setFastPreviewW(0);
            res.setOptimized(false);
            res.setFullH(0);
            res.setFullW(0);
            res.setFullLocation(new TLLocalFileEmpty());
            return res;
        }
    }

    public static ChatMessage fromTlMessage(TLAbsMessage absMessage, TelegramApplication application) {
        ChatMessage res = new ChatMessage();

        if (absMessage instanceof TLMessage) {
            TLMessage message = (TLMessage) absMessage;
            res.setMid(message.getId());
            res.setMessage(message.getMessage());
            res.setDate(message.getDate());
            res.setOut(message.getOut());
            if (message.getToId() instanceof TLPeerUser) {
                TLPeerUser peer = (TLPeerUser) message.getToId();
                res.setPeerType(PeerType.PEER_USER);
                if (peer.getUserId() == application.getCurrentUid()) {
                    res.setPeerId(message.getFromId());
                    res.setSenderId(message.getFromId());
                } else {
                    res.setPeerId(((TLPeerUser) message.getToId()).getUserId());
                    res.setSenderId(message.getFromId());
                }
                if (peer.getUserId() == application.getCurrentUid() && message.getFromId() == application.getCurrentUid()) {
                    res.setOut(true);
                }
            } else {
                res.setPeerType(PeerType.PEER_CHAT);
                res.setPeerId(((TLPeerChat) message.getToId()).getChatId());
                res.setSenderId(message.getFromId());
            }
            res.setState(message.getUnread() ? MessageState.SENT : MessageState.READED);

            res.setExtras(convertMedia(message.getMedia()));

            if (res.getExtras() instanceof TLLocalPhoto) {
                res.setContentType(ContentType.MESSAGE_PHOTO);
            } else if (res.getExtras() instanceof TLLocalVideo) {
                res.setContentType(ContentType.MESSAGE_VIDEO);
            } else if (res.getExtras() instanceof TLLocalGeo) {
                res.setContentType(ContentType.MESSAGE_GEO);
            } else if (res.getExtras() instanceof TLLocalContact) {
                res.setContentType(ContentType.MESSAGE_CONTACT);
            } else if (res.getExtras() instanceof TLLocalUnknown) {
                res.setContentType(ContentType.MESSAGE_UNKNOWN);
            } else if (res.getExtras() instanceof TLLocalDocument) {
                TLLocalDocument doc = (TLLocalDocument) res.getExtras();
                if (doc.getPreviewH() != 0 && doc.getPreviewW() != 0) {
                    if ("image/gif".equals(doc.getMimeType())) {
                        res.setContentType(ContentType.MESSAGE_DOC_ANIMATED);
                    } else {
                        res.setContentType(ContentType.MESSAGE_DOC_PREVIEW);
                    }
                } else {
                    if (doc.getMimeType().equals("application/ogg")
                            || doc.getMimeType().equals("audio/ogg")
                            || doc.getMimeType().equals("audio/mp4")
                            || doc.getMimeType().equals("audio/mpeg")
                            || doc.getMimeType().equals("audio/vorbis")) {
                        res.setContentType(ContentType.MESSAGE_AUDIO);
                    } else {
                        res.setContentType(ContentType.MESSAGE_DOCUMENT);
                    }
                }
            } else if (res.getExtras() instanceof TLLocalAudio) {
                res.setContentType(ContentType.MESSAGE_AUDIO);
            } else {
                res.setContentType(ContentType.MESSAGE_TEXT);
            }
        } else if (absMessage instanceof TLMessageForwarded) {
            TLMessageForwarded message = (TLMessageForwarded) absMessage;
            res.setForwardDate(message.getFwdDate());
            res.setForwardSenderId(message.getFwdFromId());
            res.setMid(message.getId());
            res.setMessage(message.getMessage());
            res.setDate(message.getDate());
            res.setOut(message.getOut());
            if (message.getToId() instanceof TLPeerUser) {
                TLPeerUser peer = (TLPeerUser) message.getToId();
                res.setPeerType(PeerType.PEER_USER);
                if (peer.getUserId() == application.getCurrentUid()) {
                    res.setPeerId(message.getFromId());
                    res.setSenderId(message.getFromId());
                } else {
                    res.setPeerId(((TLPeerUser) message.getToId()).getUserId());
                    res.setSenderId(message.getFromId());
                }
                if (peer.getUserId() == application.getCurrentUid() && message.getFromId() == application.getCurrentUid()) {
                    res.setOut(true);
                }
            } else {
                res.setPeerType(PeerType.PEER_CHAT);
                res.setPeerId(((TLPeerChat) message.getToId()).getChatId());
                res.setSenderId(message.getFromId());
            }

            res.setState(message.getUnread() ? MessageState.SENT : MessageState.READED);

            res.setExtras(convertMedia(message.getMedia()));

            if (res.getExtras() instanceof TLLocalPhoto) {
                res.setContentType(ContentType.MESSAGE_PHOTO | ContentType.MESSAGE_FORWARDED);
            } else if (res.getExtras() instanceof TLLocalVideo) {
                res.setContentType(ContentType.MESSAGE_VIDEO | ContentType.MESSAGE_FORWARDED);
            } else if (res.getExtras() instanceof TLLocalGeo) {
                res.setContentType(ContentType.MESSAGE_GEO | ContentType.MESSAGE_FORWARDED);
            } else if (res.getExtras() instanceof TLLocalContact) {
                res.setContentType(ContentType.MESSAGE_CONTACT | ContentType.MESSAGE_FORWARDED);
            } else if (res.getExtras() instanceof TLLocalDocument) {
                TLLocalDocument doc = (TLLocalDocument) res.getExtras();
                if (doc.getPreviewH() != 0 && doc.getPreviewW() != 0) {
                    if ("image/gif".equals(doc.getMimeType())) {
                        res.setContentType(ContentType.MESSAGE_DOC_ANIMATED | ContentType.MESSAGE_FORWARDED);
                    } else {
                        res.setContentType(ContentType.MESSAGE_DOC_PREVIEW | ContentType.MESSAGE_FORWARDED);
                    }
                } else {
                    if (doc.getMimeType().equals("application/ogg")
                            || doc.getMimeType().equals("audio/ogg")
                            || doc.getMimeType().equals("audio/mp4")
                            || doc.getMimeType().equals("audio/mpeg")
                            || doc.getMimeType().equals("audio/vorbis")) {
                        res.setContentType(ContentType.MESSAGE_AUDIO | ContentType.MESSAGE_FORWARDED);
                    } else {
                        res.setContentType(ContentType.MESSAGE_DOCUMENT | ContentType.MESSAGE_FORWARDED);
                    }
                }
            } else if (res.getExtras() instanceof TLLocalUnknown) {
                res.setContentType(ContentType.MESSAGE_UNKNOWN | ContentType.MESSAGE_FORWARDED);
            } else if (res.getExtras() instanceof TLLocalAudio) {
                res.setContentType(ContentType.MESSAGE_AUDIO | ContentType.MESSAGE_FORWARDED);
            } else {
                res.setContentType(ContentType.MESSAGE_TEXT | ContentType.MESSAGE_FORWARDED);
            }
        } else if (absMessage instanceof TLMessageService) {
            TLMessageService message = (TLMessageService) absMessage;
            res.setMid(message.getId());

            res.setDate(message.getDate());
            res.setOut(message.getOut());
            if (message.getToId() instanceof TLPeerUser) {
                TLPeerUser peer = (TLPeerUser) message.getToId();
                res.setPeerType(PeerType.PEER_USER);
                if (peer.getUserId() == application.getCurrentUid()) {
                    res.setPeerId(message.getFromId());
                    res.setSenderId(message.getFromId());
                } else {
                    res.setPeerId(((TLPeerUser) message.getToId()).getUserId());
                    res.setSenderId(message.getFromId());
                }
                if (peer.getUserId() == application.getCurrentUid() && message.getFromId() == application.getCurrentUid()) {
                    res.setOut(true);
                }
            } else {
                res.setPeerType(PeerType.PEER_CHAT);
                res.setPeerId(((TLPeerChat) message.getToId()).getChatId());
                res.setSenderId(message.getFromId());
            }
            res.setContentType(ContentType.MESSAGE_SYSTEM);

            res.setState(message.getUnread() ? MessageState.SENT : MessageState.READED);
            res.setExtras(convertAction(message.getAction()));
            res.setSenderId(message.getFromId());

            res.setMessage("####");
        }
        return res;
    }

    public static TLAbsLocalUserStatus convertStatus(TLAbsUserStatus status) {
        if (status instanceof TLUserStatusOnline) {
            return new TLLocalUserStatusOnline(((TLUserStatusOnline) status).getExpires());
        }
        if (status instanceof TLUserStatusOffline) {
            return new TLLocalUserStatusOffline(((TLUserStatusOffline) status).getWasOnline());
        }
        return new TLLocalUserStatusEmpty();
    }

    public static User userFromTlUser(TLAbsUser user) {
        if (user instanceof TLUserSelf) {
            TLUserSelf self = (TLUserSelf) user;
            User res = new User();
            res.setUid(self.getId());
            res.setFirstName(self.getFirstName());
            res.setLastName(self.getLastName());
            res.setLinkType(LinkType.SELF);
            res.setPhoto(convertAvatarPhoto(self.getPhoto()));
            res.setPhone(self.getPhone());
            res.setStatus(convertStatus(self.getStatus()));
            return res;
        } else if (user instanceof TLUserContact) {
            TLUserContact contact = (TLUserContact) user;
            User res = new User();
            res.setUid(contact.getId());
            res.setFirstName(contact.getFirstName());
            res.setLastName(contact.getLastName());
            res.setLinkType(LinkType.CONTACT);
            res.setPhoto(convertAvatarPhoto(contact.getPhoto()));
            res.setPhone(contact.getPhone());
            res.setAccessHash(contact.getAccessHash());
            res.setStatus(convertStatus(contact.getStatus()));
            return res;
        } else if (user instanceof TLUserRequest) {
            TLUserRequest request = (TLUserRequest) user;
            User res = new User();
            res.setUid(request.getId());
            res.setFirstName(request.getFirstName());
            res.setLastName(request.getLastName());
            res.setLinkType(LinkType.REQUEST);
            res.setPhoto(convertAvatarPhoto(request.getPhoto()));
            res.setPhone(request.getPhone());
            res.setAccessHash(request.getAccessHash());
            res.setStatus(convertStatus(request.getStatus()));
            return res;
        } else if (user instanceof TLUserForeign) {
            TLUserForeign foreign = (TLUserForeign) user;
            User res = new User();
            res.setUid(foreign.getId());
            res.setFirstName(foreign.getFirstName());
            res.setLastName(foreign.getLastName());
            res.setLinkType(LinkType.FOREIGN);
            res.setPhoto(convertAvatarPhoto(foreign.getPhoto()));
            res.setPhone(null);
            res.setAccessHash(foreign.getAccessHash());
            res.setStatus(convertStatus(foreign.getStatus()));
            return res;
        } else if (user instanceof TLUserDeleted) {
            TLUserDeleted deleted = (TLUserDeleted) user;
            User res = new User();
            res.setUid(deleted.getId());
            res.setFirstName(deleted.getFirstName());
            res.setLastName(deleted.getLastName());
            res.setLinkType(LinkType.DELETED);
            res.setPhoto(new TLLocalAvatarEmpty());
            res.setPhone(null);
            res.setAccessHash(0);
            res.setStatus(new TLLocalUserStatusEmpty());
            return res;
        } else if (user instanceof TLUserEmpty) {
            TLUserEmpty empty = (TLUserEmpty) user;
            User res = new User();
            res.setUid(empty.getId());
            res.setFirstName("Unknown");
            res.setLastName("User");
            res.setLinkType(LinkType.UNKNOWN);
            res.setPhoto(new TLLocalAvatarEmpty());
            res.setPhone(null);
            res.setAccessHash(0);
            res.setStatus(new TLLocalUserStatusEmpty());
            return res;
        }

        return null;
    }

    public static ChatMessage searchMessage(ChatMessage[] messages, long id) {
        for (ChatMessage message : messages) {
            if (message.getMid() == id)
                return message;
        }

        return null;
    }

    public static User searchUser(User[] users, long id) {
        for (User user : users) {
            if (user.getUid() == id)
                return user;
        }

        return null;
    }

    public static TLAbsChat findChat(List<TLAbsChat> chats, int id) {
        for (TLAbsChat chat : chats) {
            if (chat instanceof TLChat && ((TLChat) chat).getId() == id) {
                return chat;
            } else if (chat instanceof TLChatForbidden && ((TLChatForbidden) chat).getId() == id) {
                return chat;
            } else if (chat instanceof TLChatEmpty && ((TLChatEmpty) chat).getId() == id) {
                return chat;
            }
        }
        throw new RuntimeException("couldn't find chat");
    }

    public static TLDialog findDialog(List<TLDialog> chats, int peerType, int peerId) {
        for (TLDialog chat : chats) {
            if (chat.getPeer() instanceof TLPeerChat && peerType == PeerType.PEER_CHAT) {
                TLPeerChat peer = (TLPeerChat) chat.getPeer();
                if (peer.getChatId() == peerId)
                    return chat;
            }

            if (chat.getPeer() instanceof TLPeerUser && peerType == PeerType.PEER_USER) {
                TLPeerUser peer = (TLPeerUser) chat.getPeer();
                if (peer.getUserId() == peerId)
                    return chat;
            }
        }
        return null;
    }

    public static TLAbsUser findUser(List<TLAbsUser> users, int id) {
        for (TLAbsUser user : users) {
            if (user.getId() == id) {
                return user;
            }
        }
        throw new RuntimeException("couldn't find user");
    }

    public static boolean hasUser(List<TLAbsUser> users, int id) {
        for (TLAbsUser user : users) {
            if (user.getId() == id) {
                return true;
            }
        }
        return false;
    }
}