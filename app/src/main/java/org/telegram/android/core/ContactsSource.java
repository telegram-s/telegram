package org.telegram.android.core;

import android.accounts.Account;
import android.content.*;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Pair;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import org.telegram.android.Configuration;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.background.sync.ContactsSync;
import org.telegram.android.core.model.Contact;
import org.telegram.android.core.model.LinkType;
import org.telegram.android.core.model.TLLocalContext;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.local.TLAbsLocalUserStatus;
import org.telegram.android.core.model.local.TLLocalUserStatusOffline;
import org.telegram.android.core.model.local.TLLocalUserStatusOnline;
import org.telegram.android.core.model.phone.TLLocalBook;
import org.telegram.android.core.model.phone.TLLocalBookContact;
import org.telegram.android.core.model.phone.TLLocalBookPhone;
import org.telegram.android.core.model.storage.TLStorage;
import org.telegram.android.critical.TLPersistence;
import org.telegram.android.log.Logger;
import org.telegram.api.*;
import org.telegram.api.contacts.TLAbsContacts;
import org.telegram.api.contacts.TLContacts;
import org.telegram.api.contacts.TLImportedContacts;
import org.telegram.api.requests.TLRequestContactsGetContacts;
import org.telegram.api.requests.TLRequestContactsImportContacts;
import org.telegram.mtproto.secure.CryptoUtils;
import org.telegram.mtproto.time.TimeOverlord;
import org.telegram.tl.TLObject;
import org.telegram.tl.TLVector;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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
                        settingDisplayOrder
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

                User relatedContact = null;
                for (Contact contact : netContacts) {
                    if (contact.getLocalId() == id) {
                        relatedContact = application.getEngine().getUser(contact.getUid());
                        break;
                    }
                }
                if (relatedContact != null) {
                    contacts[i] = new LocalContact(id, srcName, relatedContact);
                    tContacts.add(contacts[i]);
                } else {
                    contacts[i] = new LocalContact(id, srcName);
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
        public char header;
        public User user;

        private LocalContact(long contactId, String displayName) {
            this.contactId = contactId;
            this.displayName = displayName;
            this.user = null;
            setHeader();
        }

        private LocalContact(long contactId, String displayName, User user) {
            this.contactId = contactId;
            this.displayName = displayName;
            this.user = user;
            setHeader();
        }

        private void setHeader() {
            if (displayName.length() == 0) {
                header = '#';
            } else if (Character.isLetter(displayName.charAt(0))) {
                header = (displayName.charAt(0) + "").toUpperCase().charAt(0);
            } else {
                header = '#';
            }
        }
    }
}