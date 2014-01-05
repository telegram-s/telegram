package org.telegram.android.kernel;

import org.telegram.android.config.SecuritySettings;
import org.telegram.android.config.UserSettings;
import org.telegram.android.core.NotificationSettings;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class SettingsKernel {
    private NotificationSettings notificationSettings;
    private UserSettings userSettings;
    private SecuritySettings securitySettings;

    public SettingsKernel(ApplicationKernel kernel) {
        notificationSettings = new NotificationSettings(kernel.getApplication());
        userSettings = new UserSettings(kernel.getApplication());
        securitySettings = new SecuritySettings(kernel.getApplication());
    }

    public SecuritySettings getSecuritySettings() {
        return securitySettings;
    }

    public NotificationSettings getNotificationSettings() {
        return notificationSettings;
    }

    public UserSettings getUserSettings() {
        return userSettings;
    }

    public void logIn() {
        securitySettings.clearSettings();
        // clearSettings();
    }

    public void logOut() {
        securitySettings.clearSettings();
        // clearSettings();
    }

    public void clearSettings() {
        securitySettings.clearSettings();
        notificationSettings.clearSettings();
        userSettings.clearSettings();
    }
}

