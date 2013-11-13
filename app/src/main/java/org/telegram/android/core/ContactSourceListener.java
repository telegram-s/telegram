package org.telegram.android.core;

/**
 * Author: Korshakov Stepan
 * Created: 31.07.13 16:12
 */
public interface ContactSourceListener {
    public void onContactsStateChanged();

    public void onContactsDataChanged();
}
