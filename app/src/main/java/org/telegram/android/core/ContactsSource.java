package org.telegram.android.core;

import android.content.*;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.Settings;
import org.telegram.android.R;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.background.sync.ContactsSync;
import org.telegram.android.core.model.Contact;
import org.telegram.android.core.model.User;
import org.telegram.android.core.wireframes.ContactWireframe;
import org.telegram.android.log.Logger;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Author: Korshakov Stepan
 * Created: 31.07.13 16:08
 */
public class ContactsSource implements ContactsSync.ContactSyncListener {

    private static final String TAG = "ContactsSource";

    private static final int SORT_ORDER_FIRST_NAME = 0;
    private static final int SORT_ORDER_LAST_NAME = 1;

    private static final int DISPLAY_FIRST_NAME = 0;
    private static final int DISPLAY_LAST_NAME = 1;

    private Handler handler = new Handler(Looper.getMainLooper());

    private TelegramApplication application;

    private final CopyOnWriteArrayList<ContactSourceListener> listeners = new CopyOnWriteArrayList<ContactSourceListener>();

    private String settingSortKey;
    private String settingDisplayOrder;

    private int sortOrder = SORT_ORDER_FIRST_NAME;
    private int displayOrder = DISPLAY_FIRST_NAME;

    private ContactWireframe[] contacts;
    private ContactWireframe[] telegramContacts;

    public ContactWireframe[] getContacts() {
        return contacts;
    }

    public ContactWireframe[] getTelegramContacts() {
        return telegramContacts;
    }

    public ContactsSource(TelegramApplication application) {
        this.application = application;
        loadSettings();
    }

    private void loadSettings() {
        sortOrder = SORT_ORDER_FIRST_NAME;
        displayOrder = DISPLAY_FIRST_NAME;

        try {
            if (Settings.System.getInt(application.getContentResolver(), "android.contacts.SORT_ORDER") == 2) {
                sortOrder = SORT_ORDER_LAST_NAME;
            }
        } catch (Settings.SettingNotFoundException e) {
        }
        try {
            if (Settings.System.getInt(application.getContentResolver(), "android.contacts.DISPLAY_ORDER") == 2) {
                displayOrder = DISPLAY_LAST_NAME;
            }
        } catch (Settings.SettingNotFoundException e) {
        }

        if (sortOrder == SORT_ORDER_FIRST_NAME) {
            settingSortKey = ContactsContract.Contacts.SORT_KEY_PRIMARY;
        } else {
            settingSortKey = ContactsContract.Contacts.SORT_KEY_ALTERNATIVE;
        }

        if (displayOrder == DISPLAY_FIRST_NAME) {
            settingDisplayOrder = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY;
        } else {
            settingDisplayOrder = ContactsContract.Contacts.DISPLAY_NAME_ALTERNATIVE;
        }
    }

    public void run() {
        this.application.getSyncKernel().getContactsSync().setListener(this);
    }

    public void registerListener(ContactSourceListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregisterListener(ContactSourceListener listener) {
        listeners.remove(listener);
    }

    public void notifyDataChanged() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Logger.d(TAG, "notify");
                for (ContactSourceListener listener : listeners) {
                    listener.onContactsDataChanged();
                }
            }
        });
    }

    public synchronized void destroy() {
        Logger.d(TAG, "Destroying contacts");
    }

    @Override
    public void onBookUpdated() {
        Logger.d(TAG, "onBookUpdated");


        Contact[] netContacts = application.getEngine().getUsersEngine().getAllContacts();
        HashSet<Integer> ids = new HashSet<Integer>();
        for (Contact c : netContacts) {
            ids.add(c.getUid());
        }
        application.getEngine().getUsersEngine().getUsersById(ids.toArray());

        ArrayList<ContactWireframe> allContacts = new ArrayList<ContactWireframe>();
        ArrayList<ContactWireframe> telegramContacts = new ArrayList<ContactWireframe>();

        User[] myContacts = application.getEngine().getUsersEngine().getContacts();
        Arrays.sort(myContacts, new Comparator<User>() {
            @Override
            public int compare(User user, User user2) {
                if (sortOrder == SORT_ORDER_FIRST_NAME) {
                    return user.getFirstName().compareTo(user2.getFirstName());
                } else {
                    return user.getLastName().compareTo(user2.getLastName());
                }
            }
        });

        HashSet<Integer> addedUids = new HashSet<Integer>();

        for (int i = 0; i < myContacts.length; i++) {
            addedUids.add(myContacts[i].getUid());
            allContacts.add(new ContactWireframe(myContacts[i], sortOrder == SORT_ORDER_FIRST_NAME));
            telegramContacts.add(new ContactWireframe(myContacts[i], sortOrder == SORT_ORDER_FIRST_NAME));
        }

        ContentResolver cr = application.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.LOOKUP_KEY,
                        settingDisplayOrder,
                        settingSortKey
                },
                ContactsContract.Contacts.HAS_PHONE_NUMBER + " > 0 ", null, settingSortKey);
        if (cur != null && cur.moveToFirst()) {
            do {
                final long id = cur.getLong(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String srcName = cur.getString(cur.getColumnIndex(settingDisplayOrder));
                String sortName = cur.getString(cur.getColumnIndex(settingSortKey));
                String lookupKey = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));

                User relatedContact = null;
                for (Contact contact : netContacts) {
                    if (addedUids.contains(contact.getUid())) {
                        continue;
                    }
                    if (contact.getLocalId() == id) {
                        relatedContact = application.getEngine().getUser(contact.getUid());
                        break;
                    }
                }
                if (relatedContact == null) {
                    allContacts.add(new ContactWireframe(id, lookupKey, srcName, application.getString(R.string.st_contacts_header_unregistered)));
                }
            } while (cur.moveToNext());
            cur.close();
        }

        this.telegramContacts = telegramContacts.toArray(new ContactWireframe[telegramContacts.size()]);
        this.contacts = allContacts.toArray(new ContactWireframe[allContacts.size()]);

        notifyDataChanged();
    }
}