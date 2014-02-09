package org.telegram.android.core;

import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.local.TLAbsLocalUserStatus;
import org.telegram.android.core.model.local.TLLocalUserStatusOffline;
import org.telegram.android.core.model.local.TLLocalUserStatusOnline;
import org.telegram.api.*;
import org.telegram.mtproto.time.TimeOverlord;
import org.telegram.tl.TLObject;

import java.util.List;

/**
 * Author: Korshakov Stepan
 * Created: 22.08.13 0:47
 */
public class ApiUtils {
    protected static final String[] AVATAR_IMAGE_PRIORITY = new String[]{"a", "s", "b", "m", "c", "x"};

    protected static final String[] SMALLEST_PRIORITY = new String[]{"s", "a", "m", "b", "c", "x", "y", "d", "w"};

    protected static final String[] LARGEST_PRIORITY = new String[]{"w", "d", "y", "x", "c", "b", "m", "a", "s"};

    protected static final String[] FULL_IMAGE_PRIORITY_DEFAULT = new String[]{"x", "m", "s"};
    protected static final String[] FULL_IMAGE_PRIORITY_DEFAULT_320 = new String[]{"x", "m", "s"};
    protected static final String[] FULL_IMAGE_PRIORITY_DEFAULT_800 = new String[]{"y", "x", "m", "s"};

    protected static final String[] WALLPAPER_PREVIEW_PRIORITY = new String[]{"b", "m", "c", "x"};
    protected static String[] FULL_IMAGE_PRIORITY = FULL_IMAGE_PRIORITY_DEFAULT;

    public static int MAX_SIZE;

    private static TelegramApplication application;

    public static void init(TelegramApplication _application, int screenSize) {
        application = _application;
        if (screenSize <= 320) {
            MAX_SIZE = 800;
            FULL_IMAGE_PRIORITY = FULL_IMAGE_PRIORITY_DEFAULT_320;
        } else {
            MAX_SIZE = 1280;
            FULL_IMAGE_PRIORITY = FULL_IMAGE_PRIORITY_DEFAULT_800;
        }
    }

    public static TLAbsPhotoSize findLargest(org.telegram.api.TLPhoto photo) {
        return getPhotoSize(LARGEST_PRIORITY, photo);
    }

    public static TLAbsPhotoSize findSmallest(org.telegram.api.TLPhoto photo) {
        return getPhotoSize(SMALLEST_PRIORITY, photo);
    }

    public static TLPhotoSize findDownloadSize(org.telegram.api.TLPhoto photo) {
        return getRawPhotoSize(FULL_IMAGE_PRIORITY, photo);
    }

    public static TLFileLocation getAvatarPhoto(TLObject object) {
        return getPhoto(object, AVATAR_IMAGE_PRIORITY);
    }

    public static TLPhotoCachedSize findCachedSize(org.telegram.api.TLPhoto photo) {
        for (TLAbsPhotoSize size : photo.getSizes()) {
            if (size instanceof TLPhotoCachedSize) {
                return (TLPhotoCachedSize) size;
            }
        }
        return null;
    }

    private static TLAbsPhotoSize getPhotoSize(String[] priority, org.telegram.api.TLPhoto src) {
        for (String aPriority : priority) {
            for (TLAbsPhotoSize size : src.getSizes()) {
                if (size instanceof TLPhotoSize) {
                    TLPhotoSize sz = (TLPhotoSize) size;
                    if (sz.getType().equals(aPriority)) {
                        //Log.d("STELS_IMAGE", "Selected abs photo size: " + sz.getW() + "x" + sz.getH());
                        return sz;
                    }
                } else if (size instanceof TLPhotoCachedSize) {
                    TLPhotoCachedSize sz = (TLPhotoCachedSize) size;
                    if (sz.getType().equals(aPriority)) {
                        //Log.d("STELS_IMAGE", "Selected abs photo size: " + sz.getW() + "x" + sz.getH());
                        return sz;
                    }
                }
            }
        }
        return null;
    }

    private static TLPhotoSize getRawPhotoSize(String[] priority, List<TLAbsPhotoSize> src) {
        for (String aPriority : priority) {
            for (TLAbsPhotoSize size : src) {
                if (size instanceof TLPhotoSize) {
                    TLPhotoSize sz = (TLPhotoSize) size;
                    if (sz.getType().equals(aPriority)) {
                        return sz;
                    }
                }
            }
        }
        return null;
    }

    private static TLPhotoSize getRawPhotoSize(String[] priority, org.telegram.api.TLPhoto src) {
        for (String aPriority : priority) {
            for (TLAbsPhotoSize size : src.getSizes()) {
                if (size instanceof TLPhotoSize) {
                    TLPhotoSize sz = (TLPhotoSize) size;
                    if (sz.getType().equals(aPriority)) {
                        //Log.d("STELS_IMAGE", "Selected abs photo size: " + sz.getW() + "x" + sz.getH());
                        return sz;
                    }
                }
            }
        }
        return null;
    }

    private static TLFileLocation getPhoto(TLObject object, String[] priority) {
        if (object == null)
            return null;

        if (object instanceof TLChatPhoto) {
            TLChatPhoto chatPhoto = (TLChatPhoto) object;
            return (TLFileLocation) chatPhoto.getPhotoSmall();
        }
        if (object instanceof TLPhoto) {
            TLPhoto photo = (TLPhoto) object;
            TLAbsPhotoSize size = getPhotoSize(priority, photo);
            if (size instanceof TLPhotoSize) {
                //Log.d("STELS_IMAGE", "Selected photo size: " + ((TLPhotoSize) size).getW() + "x" + ((TLPhotoSize) size).getH());
                return (TLFileLocation) ((TLPhotoSize) size).getLocation();
            } else if (size instanceof TLPhotoCachedSize) {
                //Log.d("STELS_IMAGE", "Selected photo size: " + ((TLPhotoSizePhotoCachedSize) size).getW() + "x" + ((TLPhotoSizePhotoCachedSize) size).getH());
                // TODO: use cache
                return (TLFileLocation) ((TLPhotoCachedSize) size).getLocation();
            }
        }
        if (object instanceof TLUserProfilePhoto) {
            TLUserProfilePhoto userProfilePhoto = (TLUserProfilePhoto) object;
            return (TLFileLocation) userProfilePhoto.getPhotoSmall();
        }

        return null;
    }

    public static TLPhotoSize getWallpaperPreview(List<TLAbsPhotoSize> sizes) {
        return getRawPhotoSize(WALLPAPER_PREVIEW_PRIORITY, sizes);
    }

    public static TLPhotoSize getWallpaperFull(List<TLAbsPhotoSize> sizes) {
        return getRawPhotoSize(FULL_IMAGE_PRIORITY, sizes);
    }

    public static int getUserState(TLAbsLocalUserStatus status) {
        if (status != null) {
            if (status instanceof TLLocalUserStatusOnline) {
                TLLocalUserStatusOnline online = (TLLocalUserStatusOnline) status;
                if ((TimeOverlord.getInstance().getServerTime() / 1000) > online.getExpires()) {
                    return online.getExpires(); // Last seen
                } else {
                    return 0; // Online
                }
            } else if (status instanceof TLLocalUserStatusOffline) {
                TLLocalUserStatusOffline offline = (TLLocalUserStatusOffline) status;
                return offline.getWasOnline(); // Last seen
            } else {
                return -1; // Offline
            }
        } else {
            return -1; // Offline
        }
    }
}
