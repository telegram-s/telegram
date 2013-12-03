package org.telegram.android.kernel.api;

import android.os.SystemClock;
import org.telegram.android.critical.ApiStorage;
import org.telegram.android.kernel.ApiKernel;
import org.telegram.android.tasks.AsyncException;
import org.telegram.mtproto.pq.Authorizer;
import org.telegram.mtproto.pq.PqAuth;
import org.telegram.mtproto.state.ConnectionInfo;

/**
 * Author: Korshakov Stepan
 * Created: 23.07.13 3:21
 */
public class AuthController {

    private ApiKernel kernel;
    private AuthState state;
    private Thread authThread;

    public AuthController(ApiKernel kernel) {
        this.kernel = kernel;
    }

    private void notifyStateChange(AuthState newState) {
        state = newState;
    }

    private ApiStorage getApiStorage() {
        return kernel.getKernel().getAuthKernel().getApiStorage();
    }

    public void check() {
        if (getApiStorage().getAuthKey(getApiStorage().getPrimaryDc()) == null) {
            if (authThread == null) {
                notifyStateChange(AuthState.IN_PROGRESS);
                authThread = new Thread() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                Authorizer authorizer = new Authorizer();
                                ConnectionInfo[] info = getApiStorage().getAvailableConnections(getApiStorage().getPrimaryDc());
                                PqAuth auth = authorizer.doAuth(info);
                                if (getApiStorage().getAuthKey(getApiStorage().getPrimaryDc()) != null) {
                                    notifyStateChange(AuthState.COMPLETED);
                                    return;
                                }
                                getApiStorage().putAuthKey(getApiStorage().getPrimaryDc(), auth.getAuthKey());
                                kernel.updateTelegramApi();
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
}
