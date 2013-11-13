package org.telegram.android.auth;

import android.os.SystemClock;
import org.telegram.android.StelsApplication;
import org.telegram.android.tasks.AsyncException;
import org.telegram.api.engine.TelegramApi;
import org.telegram.mtproto.pq.Authorizer;
import org.telegram.mtproto.pq.PqAuth;
import org.telegram.mtproto.state.ConnectionInfo;

import java.io.IOException;

/**
 * Author: Korshakov Stepan
 * Created: 23.07.13 3:21
 */
public class AuthController {

    private StelsApplication application;
    private AuthState state;
    private Thread authThread;

    public AuthController(StelsApplication application) {
        this.application = application;
    }

    private void notifyStateChange(AuthState newState) {
        state = newState;
    }

    public void switchToDC(int dc) {
        application.getApiStorage().switchToPrimaryDc(dc);
        application.updateApi();
        check();
    }

    public void check() {
        if (application.getApiStorage().getAuthKey(application.getApiStorage().getPrimaryDc()) == null) {
            if (authThread == null) {
                notifyStateChange(AuthState.IN_PROGRESS);
                authThread = new Thread() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                Authorizer authorizer = new Authorizer();
                                ConnectionInfo info = application.getApiStorage().getConnectionInfo(application.getApiStorage().getPrimaryDc());
                                PqAuth auth = authorizer.doAuth(info.getAddress(), info.getPort());
                                if (application.getApiStorage().getAuthKey(application.getApiStorage().getPrimaryDc()) != null) {
                                    notifyStateChange(AuthState.COMPLETED);
                                    return;
                                }
                                application.getApiStorage().putAuthKey(application.getApiStorage().getPrimaryDc(), auth.getAuthKey());
                                application.updateApi();
                                notifyStateChange(AuthState.COMPLETED);
                                authThread = null;
                                return;
                            } catch (Exception e) {
                                e.printStackTrace();
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                }
                            }
//                            try {
//                                Authorizer authorizer = new Authorizer();
//
//                                authorizer.doAuth()
//                                if (application.getConnector().getOverlord().hasAliveChannels()) {
//                                    if (!(application.getConnectionContext() instanceof ConnectionContext)) {
//                                        AuthKeyHelper helper = new AuthKeyHelper(application.getConnector());
//                                        AuthConfiguration configuration = helper.requestNewAuthKey(application.getConnectionContext().getDc());
//                                        application.getKeyStorage().writeNewAuthKey(configuration.getDc(), configuration.getAuthKey(), configuration.getServerSalt());
//
//                                        application.switchContext(application.getContextStorage().createNewLinkedContext(
//                                                configuration.getDc(),
//                                                configuration.getAuthKey(),
//                                                configuration.getServerSalt(),
//                                                CryptoUtils.newSecureSeed(8),
//                                                0, -1
//                                        ));
//                                    }
//
//                                    if (!application.getTechSyncer().foregroundCheckDc()) {
//                                        Thread.sleep(1000);
//                                        continue;
//                                    }
//
//                                    notifyStateChange(AuthState.COMPLETED);
//                                    authThread = null;
//                                    return;
//                                } else {
//                                    Thread.sleep(1000);
//                                }
//                            } catch (FailureException e) {
//                                e.printStackTrace();
//                                notifyStateChange(AuthState.FAILURE);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                                notifyStateChange(AuthState.CONNECTIVITY_WARNING);
//                            } catch (TransportSecurityException e) {
//                                e.printStackTrace();
//                                notifyStateChange(AuthState.SECURITY_ERROR);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                                notifyStateChange(AuthState.FAILURE);
//                                return;
//                            }
                        }
                    }
                };
                authThread.setName("AuthControllerThread#" + authThread.hashCode());
                authThread.start();
            }
        } else {
            notifyStateChange(AuthState.COMPLETED);
        }
    }

    public AuthState getState() {
        return state;
    }

    public void waitAuth(long delay) throws AsyncException {
        long start = SystemClock.uptimeMillis();
        while (getState() != AuthState.COMPLETED) {
            if (SystemClock.uptimeMillis() - start > delay) {
                throw new AsyncException(AsyncException.ExceptionType.CONNECTION_ERROR);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    public void logout() {
        application.getApiStorage().resetAuth();
        application.updateApi();
        check();
    }
}
