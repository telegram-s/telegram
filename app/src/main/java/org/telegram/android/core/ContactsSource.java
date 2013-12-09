package org.telegram.android.core;

import android.content.*;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.Settings;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.background.sync.ContactsSync;
import org.telegram.android.core.model.Contact;
import org.telegram.android.core.model.User;
import org.telegram.android.log.Logger;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Author: Korshakov Stepan
 * Created: 31.07.13 16:08
 */
public class ContactsSource implements ContactsSync.ContactSyncListener {

    private static final String TAG = "ContactsSource";

    private ContactSourceState state;

    private Handler handler = new Handler(Looper.getMainLooper());

    private StelsApplication application;

    private final CopyOnWriteArrayList<ContactSourceListener> listeners = new CopyOnWriteArrayList<ContactSourceListener>();

    private String settingSortKey;
    private String settingDisplayOrder;

    private LocalContact[] contacts;
    private LocalContact[] telegramContacts;

    public LocalContact[] getContacts() {
        return contacts;
    }

    public LocalContact[] getTelegramContacts() {
        return telegramContacts;
    }

    public ContactsSource(StelsApplication application) {
        this.state = ContactSourceState.UNSYNCED;
        this.application = application;
        loadSettings();
    }

    private void loadSettings() {
        int sort_order = 1;
        int display_order = 1;
        try {
            sort_order = Settings.System.getInt(application.getContentResolver(), "android.contacts.SORT_ORDER");
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        try {
            display_order = Settings.System.getInt(application.getContentResolver(), "android.contacts.DISPLAY_ORDER");
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        if (sort_order == 1) {
            settingSortKey = ContactsContract.Contacts.SORT_KEY_PRIMARY;
        } else {
            settingSortKey = ContactsContract.Contacts.SORT_KEY_ALTERNATIVE;
        }

        if (display_order == 1) {
            settingDisplayOrder = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY;
        } else {
            settingDisplayOrder = ContactsContract.Contacts.DISPLAY_NAME_ALTERNATIVE;
        }
    }

    public void run() {
        this.application.getSyncKernel().getContactsSync().setListener(this);
    }

    public ContactSourceState getState() {
        return state;
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

        ContentResolver cr = application.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.LOOKUP_KEY,
                        settingDisplayOrder,
                        settingSortKey
                },
                ContactsContract.Contacts.HAS_PHONE_NUMBER + " > 0 ", null, settingSortKey);

        final LocalContact[] contacts = new LocalContact[cur.getCount()];

        Contact[] netContacts = application.getEngine().getAllContacts();
        HashSet<Integer> ids = new HashSet<Integer>();
        for (Contact c : netContacts) {
            ids.add(c.getUid());
        }
        application.getEngine().getUsersById(ids.toArray());

        ArrayList<LocalContact> tContacts = new ArrayList<LocalContact>();

        if (cur.moveToFirst()) {
            for (int i = 0; i < contacts.length; i++) {
                final long id = cur.getLong(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String srcName = cur.getString(cur.getColumnIndex(settingDisplayOrder));
                String sortName = cur.getString(cur.getColumnIndex(settingSortKey));
                String lookupKey = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));

                User relatedContact = null;
                for (Contact contact : netContacts) {
                    if (contact.getLocalId() == id) {
                        relatedContact = application.getEngine().getUser(contact.getUid());
                        break;
                    }
                }
                if (relatedContact != null) {
                    contacts[i] = new LocalContact(id, lookupKey, srcName, sortName, relatedContact);
                    tContacts.add(contacts[i]);
                } else {
                    contacts[i] = new LocalContact(id, lookupKey, srcName, sortName);
                }
                cur.moveToNext();
            }
        }

        cur.close();

        this.telegramContacts = tContacts.toArray(new LocalContact[tContacts.size()]);
        this.contacts = contacts;

        notifyDataChanged();
    }


    public class LocalContact {
        public long contactId;
        public String displayName;
        public String lookupKey;
        public char header;
        public User user;

        private LocalContact(long contactId, String lookupKey, String displayName, String sortName) {
            this.contactId = contactId;
            this.displayName = displayName;
            this.lookupKey = lookupKey;
            this.user = null;
            setHeader(sortName);
        }

        private LocalContact(long contactId, String lookupKey, String displayName, String sortName, User user) {
            this.contactId = contactId;
            this.displayName = displayName;
            this.lookupKey = lookupKey;
            this.user = user;
            setHeader(sortName);
        }

        private void setHeader(String s) {
            if (s == null || s.length() == 0) {
                header = '#';
            } else if (Character.isLetter(s.charAt(0))) {
                header = (s.charAt(0) + "").toUpperCase().charAt(0);
            } else {
                header = '#';
            }
        }
    }
}