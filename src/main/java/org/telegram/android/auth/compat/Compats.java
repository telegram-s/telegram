package org.telegram.android.auth.compat;

import org.telegram.android.auth.compat.v1.*;
import org.telegram.android.auth.compat.v2.*;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 18.09.13
 * Time: 1:56
 */
public class Compats {
    public static HashMap<String, String> VER1 = new HashMap<String, String>();
    public static HashMap<String, String> VER2 = new HashMap<String, String>();

    static {
        VER1.put("com.extradea.framework.persistence.PersistenceObject", CompatPersistence.class.getCanonicalName());
        VER1.put("com.extradea.framework.persistence.ContextPersistence", CompatContextPersistence.class.getCanonicalName());
        VER1.put("org.telegram.android.api.TLFileLocation", TLFileLocationCompat.class.getCanonicalName());
        VER1.put("org.telegram.android.api.TLFileLocationUnavailable", TLFileLocationEmptyCompat.class.getCanonicalName());

        VER1.put("org.telegram.android.api.TLUserProfilePhotoEmpty", TLUserProfilePhotoEmptyCompat.class.getCanonicalName());
        VER1.put("org.telegram.android.api.TLUserProfilePhoto", TLUserProfilePhotoCompat.class.getCanonicalName());

        VER1.put("org.telegram.android.api.TLBoolTrue", TLBoolTrueCompat.class.getCanonicalName());
        VER1.put("org.telegram.android.api.TLBoolFalse", TLBoolFalseCompat.class.getCanonicalName());

        VER1.put("org.telegram.android.api.TLUserStatus", TLUserStatusCompat.class.getCanonicalName());
        VER1.put("org.telegram.android.api.TLUserStatusOffline", TLUserStatusOfflineCompat.class.getCanonicalName());
        VER1.put("org.telegram.android.api.TLUserStatusOnline", TLUserStatusOnlineCompat.class.getCanonicalName());
        VER1.put("org.telegram.android.api.TLUserStatusEmpty", TLUserStatusEmptyCompat.class.getCanonicalName());

        VER1.put("org.telegram.android.api.TLUser", TLUserCompat.class.getCanonicalName());
        VER1.put("org.telegram.android.api.TLUserSelf", TLUserSelfCompat.class.getCanonicalName());
        VER1.put("org.telegram.android.api.TLUserSelf", TLUserSelfCompat.class.getCanonicalName());
        VER1.put("org.telegram.android.auth.AuthCredentials", CompatCredentials.class.getCanonicalName());

        //////////////////////////////////////////////////////////

        VER2.put("com.extradea.framework.persistence.PersistenceObject", CompatPersistence.class.getCanonicalName());
        VER2.put("com.extradea.framework.persistence.ContextPersistence", CompatContextPersistence.class.getCanonicalName());
        VER2.put("org.telegram.android.api.TLFileLocation", TLFileLocationCompat.class.getCanonicalName());
        VER2.put("org.telegram.android.api.TLFileLocationUnavailable", TLFileLocationEmptyCompat.class.getCanonicalName());

        VER2.put("org.telegram.android.api.TLUserProfilePhotoEmpty", TLUserProfilePhotoEmptyCompat2.class.getCanonicalName());
        VER2.put("org.telegram.android.api.TLUserProfilePhoto", TLUserProfilePhotoCompat2.class.getCanonicalName());

        VER2.put("org.telegram.android.api.TLBoolTrue", TLBoolTrueCompat.class.getCanonicalName());
        VER2.put("org.telegram.android.api.TLBoolFalse", TLBoolFalseCompat.class.getCanonicalName());

        VER2.put("org.telegram.android.api.TLUserStatus", TLUserStatusCompat.class.getCanonicalName());
        VER2.put("org.telegram.android.api.TLUserStatusOffline", TLUserStatusOfflineCompat.class.getCanonicalName());
        VER2.put("org.telegram.android.api.TLUserStatusOnline", TLUserStatusOnlineCompat.class.getCanonicalName());
        VER2.put("org.telegram.android.api.TLUserStatusEmpty", TLUserStatusEmptyCompat.class.getCanonicalName());

        VER2.put("org.telegram.android.api.TLUser", TLUserCompat2.class.getCanonicalName());
        VER2.put("org.telegram.android.api.TLUserSelf", TLUserSelfCompat2.class.getCanonicalName());
        VER2.put("org.telegram.android.api.TLUserSelf", TLUserSelfCompat2.class.getCanonicalName());
        VER2.put("org.telegram.android.auth.AuthCredentials", CompatCredentials2.class.getCanonicalName());
    }
}
