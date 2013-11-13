package org.telegram.android.auth;

import android.content.Context;
import com.crittercism.app.Crittercism;
import com.extradea.framework.persistence.ContextPersistence;
import org.telegram.android.auth.compat.Compats;
import org.telegram.android.auth.compat.OldAuthCompatStream;
import org.telegram.android.auth.compat.v1.CompatCredentials;
import org.telegram.android.auth.compat.v1.TLUserSelfCompat;
import org.telegram.android.auth.compat.v2.CompatCredentials2;
import org.telegram.android.auth.compat.v2.TLUserSelfCompat2;
import org.telegram.android.log.Logger;

import java.io.*;

/**
 * Author: Korshakov Stepan
 * Created: 23.07.13 3:18
 */
public class AuthCredentials extends ContextPersistence {

    public static AuthCredentials loadCredentials(Context context) {
        AuthCredentials res = new AuthCredentials(context);

        if (!res.isLoginned()) {
            File src = new File(context.getFilesDir().getAbsolutePath() + "/org.telegram.android.auth.AuthCredentials.sav");
            if (src.exists()) {
                // Try V1
                try {
                    OldAuthCompatStream stream = new OldAuthCompatStream(context.openFileInput("org.telegram.android.auth.AuthCredentials.sav"), Compats.VER1);
                    CompatCredentials compatCredentials = (CompatCredentials) stream.readObject();
                    if (compatCredentials.getUser() != null) {
                        int uid = ((TLUserSelfCompat) compatCredentials.getUser()).getId();
                        Logger.d("Credentials", "Auth UserId: " + uid);
                        res.setAuth(uid, compatCredentials.getExpires());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                    Crittercism.logHandledException(e);
                }

                if (!res.isLoginned()) {
                    // Try V2
                    try {
                        OldAuthCompatStream stream = new OldAuthCompatStream(context.openFileInput("org.telegram.android.auth.AuthCredentials.sav"), Compats.VER2);
                        CompatCredentials2 compatCredentials = (CompatCredentials2) stream.readObject();
                        if (compatCredentials.getUser() != null) {
                            int uid = ((TLUserSelfCompat2) compatCredentials.getUser()).getId();
                            Logger.d("Credentials", "Auth UserId: " + uid);
                            res.setAuth(uid, compatCredentials.getExpires());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Crittercism.logHandledException(e);
                    }
                }
            }
        }
        res.trySave();
        return res;
    }

    private static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private int uid;
    private int expires;
    private boolean isLoginned;

    public AuthCredentials(Context context) {
        super(context);
        tryLoad();
    }

    public int getUid() {
        return uid;
    }

    public int getExpires() {
        return expires;
    }

    public boolean isLoginned() {
        return isLoginned;
    }

    public void setAuth(int uid, int expires) {
        this.uid = uid;
        this.expires = expires;
        this.isLoginned = true;
        trySave();
    }

    public void clearAuth() {
        this.uid = 0;
        this.expires = 0;
        this.isLoginned = false;
        trySave();
    }
}
