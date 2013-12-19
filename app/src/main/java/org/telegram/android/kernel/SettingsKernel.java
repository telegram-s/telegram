package org.telegram.android.kernel;

import org.telegram.android.config.UserSettings;
import org.telegram.android.core.NotificationSettings;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class SettingsKernel {
    private NotificationSettings notificationSettings;
    private UserSettings userSettings;

    public SettingsKernel(ApplicationKernel kernel) {
        notificationSettings = new NotificationSettings(kernel.getApplication());
        userSettings = new UserSettings(kernel.getApplication());
    }

    public NotificationSettings getNotificationSettings() {
        return notificationSettings;
    }

    public UserSettings getUserSettings() {
        return userSettings;
    }

    public void logIn() {
        // clearSettings();
    }

    public void logOut() {
        // clearSettings();
    }

    public void clearSettings() {
        notificationSettings.clearSettings();
        userSettings.clearSettings();
    }
}

