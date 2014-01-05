package org.telegram.android.core.sec;

import android.os.SystemClock;
import org.telegram.android.kernel.ApplicationKernel;

/**
 * Created by ex3ndr on 05.01.14.
 */
public class LockState {
    private ApplicationKernel kernel;
    private long lastAppHideTime;
    private boolean isLocked;

    public LockState(ApplicationKernel kernel) {
        this.kernel = kernel;
        this.isLocked = this.kernel.getSettingsKernel().getSecuritySettings().isLockEnabled();
    }

    public boolean isLocked() {
        if (!kernel.getAuthKernel().isLoggedIn()) {
            isLocked = false;
        }
        if (!kernel.getSettingsKernel().getSecuritySettings().isLockEnabled()) {
            isLocked = false;
        }
        return isLocked;
    }

    public void onApplicationVisible() {
        if (kernel.getSettingsKernel().getSecuritySettings().isLockEnabled()) {
            if (!isLocked) {
                if ((SystemClock.uptimeMillis() - lastAppHideTime) / 1000 > kernel.getSettingsKernel().getSecuritySettings().getLockTimeout()) {
                    isLocked = true;
                }
            }
        } else {
            isLocked = false;
        }
    }

    public void onApplicationHidden() {
        lastAppHideTime = SystemClock.uptimeMillis();
    }

    public void unlock() {
        isLocked = false;
    }
}
