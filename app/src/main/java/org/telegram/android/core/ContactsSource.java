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
import android.util.Pair;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import org.telegram.android.Configuration;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.Contact;
import org.telegram.android.core.model.LinkType;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.local.TLAbsLocalUserStatus;
import org.telegram.android.core.model.local.TLLocalUserStatusOffline;
import org.telegram.android.core.model.local.TLLocalUserStatusOnline;
import org.telegram.android.log.Logger;
import org.telegram.api.*;
import org.telegram.api.contacts.TLAbsContacts;
import org.telegram.api.contacts.TLContacts;
import org.telegram.api.contacts.TLImportedContacts;
import org.telegram.api.requests.TLRequestContactsGetContacts;
import org.telegram.api.requests.TLRequestContactsImportContacts;
import org.telegram.mtproto.secure.CryptoUtils;
import org.telegram.mtproto.time.TimeOverlord;
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
public class ContactsSource {

    private static final String TAG = "ContactsSource";

    public static void clearData(StelsApplication application) {
        SharedPreferences preferences = application.getSharedPreferences("org.telegram.android.contacts", Context.MODE_PRIVATE);
        preferences.edit().remove("max_sync_id").remove("sync_hash").commit();
    }


    public class PhoneBookRecord {
        private long contactId;
        private String firstName;
        private String lastName;
        private ArrayList<Phone> phones = new ArrayList<Phone>();

        public ArrayList<Phone> getPhones() {
            return phones;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        private void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public long getContactId() {
            return contactId;
        }

        private void setContactId(long contactId) {
            this.contactId = contactId;
        }
    }

    public class Phone {
        private String number;
        private long id;

        private Phone(String number, long id) {
            this.number = number;
            this.id = id;
        }

        public String getNumber() {
            return number;
        }

        public long getId() {
            return id;
        }
    }

    private ContactSourceState state;

    private ExecutorService service = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread res = new Thread(runnable);
            res.setName("ContactsSourceService#" + res.hashCode());
            res.setPriority(Configuration.SOURCES_THREAD_PRIORITY);
            return res;
        }
    });

    private HashSet<Integer> contactsCache = null;

    private final Object stateSync = new Object();

    private Handler handler = new Handler(Looper.getMainLooper());

    private HandlerThread observerUpdatesThread;

    private StelsApplication application;

    private final CopyOnWriteArrayList<ContactSourceListener> listeners = new CopyOnWriteArrayList<ContactSourceListener>();

    private long maxContactId;

    private String lastSyncHash;

    private SharedPreferences preferences;

    private boolean isClosed = false;

    private ContentObserver contentObserver;

    public ContactsSource(StelsApplication application) {
        this.state = ContactSourceState.UNSYNCED;
        this.application = application;
        this.preferences = application.getSharedPreferences("org.telegram.android.contacts", Context.MODE_PRIVATE);
        this.maxContactId = preferences.getLong("max_sync_id", 0);
        this.lastSyncHash = preferences.getString("sync_hash", null);
        this.observerUpdatesThread = new HandlerThread("ObserverHandlerThread");
        this.observerUpdatesThread.start();
    }

    public synchronized boolean isCacheAlive() {
        return contactsCache != null;
    }

    public synchronized User[] getUsers() {
        while (contactsCache == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return application.getEngine().getContacts();
    }

    public User[] getSortedUsers() {
        User[] res = getUsers();
        Arrays.sort(res, new Comparator<User>() {
            @Override
            public int compare(User user, User user2) {
                return -(getUserOnlineSorkKey(user.getStatus()) - getUserOnlineSorkKey(user2.getStatus()));
            }
        });
        return res;
    }

    public User[] getSortedByNameUsers() {
        User[] res = getUsers();
        Arrays.sort(res, new Comparator<User>() {
            @Override
            public int compare(User user, User user2) {
                return user.getSortingName().compareTo(user2.getSortingName());
            }
        });
        return res;
    }

    protected int getUserOnlineSorkKey(TLAbsLocalUserStatus status) {
        if (status != null) {
            if (status instanceof TLLocalUserStatusOnline) {
                TLLocalUserStatusOnline online = (TLLocalUserStatusOnline) status;
                if (getServerTime() > online.getExpires()) {
                    return online.getExpires(); // Last seen
                } else {
                    return Integer.MAX_VALUE - 1; // Online
                }
            } else if (status instanceof TLLocalUserStatusOffline) {
                TLLocalUserStatusOffline offline = (TLLocalUserStatusOffline) status;
                return offline.getWasOnline(); // Last seen
            } else {
                return 0; // Offline
            }
        } else {
            return 0; // Offline
        }
    }

    protected int getServerTime() {
        return (int) (TimeOverlord.getInstance().getServerTime() / 1000);
    }


    private boolean checkPhonesChanged() {
        PhoneBookRecord[] book = loadPhoneBook();
        String bookHash = phoneBookHash(book);
        return !(lastSyncHash != null && lastSyncHash.equals(bookHash));
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

    public void notifyStateChanged() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (ContactSourceListener listener : listeners) {
                    listener.onContactsStateChanged();
                }
            }
        });
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

    public synchronized void resetState() {
        this.preferences.edit().remove("max_sync_id").remove("sync_hash").commit();
        this.lastSyncHash = null;
        startSync();
    }

    public synchronized void startSync() {
        if (isClosed)
            return;

        if (this.contentObserver == null) {
            while (observerUpdatesThread.getLooper() == null) {
                Thread.yield();
            }
            this.contentObserver = new ContentObserver(new Handler(observerUpdatesThread.getLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    if (!application.isLoggedIn()) {
                        return;
                    }
                    if (!selfChange && state != ContactSourceState.IN_PROGRESS) {
                        Logger.d(TAG, "Contacts changed in observer.");

                        if (checkPhonesChanged()) {
                            Logger.d(TAG, "New phones added. Starting new sync.");
                            startSync();
                        }
                    }
                }
            };
            this.application.getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contentObserver);
        }

        synchronized (stateSync) {
            if (state != ContactSourceState.IN_PROGRESS) {
                state = ContactSourceState.IN_PROGRESS;
                notifyStateChanged();
                service.execute(new Runnable() {
                    @Override
                    public void run() {
                        while (application.isLoggedIn()) {
                            if (contactsCache == null) {
                                User[] users = application.getEngine().getContacts();
                                HashSet<Integer> nCache = new HashSet<Integer>();
                                for (User u : users) {
                                    nCache.add(u.getUid());
                                }
                                contactsCache = nCache;
                            }
                            application.getMonitor().waitForConnection();
                            try {
                                doSync();
                                state = ContactSourceState.SYNCED;
                                notifyDataChanged();
                                return;
                            } catch (Exception e) {
                                Logger.t(TAG, e);
                            }
                        }
                        state = ContactSourceState.UNSYNCED;
                    }
                });
            }
        }
    }

    public void editOrCreateUserContactName(int uid, String firstName, String lastName) {
        application.getEngine().onUserNameChanges(uid, firstName, lastName);
        application.getEngine().onUserLinkChanged(uid, LinkType.CONTACT);
        application.getUserSource().notifyUserChanged(uid);

//        if (!application.getTechKernel().getTechReflection().isOnSdCard()) {
//            if (application.getEngine().getContactsForUid(uid).length > 0) {
//                User user = application.getEngine().getUser(uid);
//                long nid = addContact(true, application.getKernel().getAuthKernel().getAccount(), user, firstName + " " + lastName, user.getPhone());
//                application.getEngine().updateContact(uid, nid);
//            } else {
//                User user = application.getEngine().getUser(uid);
//                long id = addContact(false, application.getKernel().getAuthKernel().getAccount(), user, firstName + " " + lastName, user.getPhone());
//                application.getEngine().addContact(uid, id);
//            }
//        }

        resetState();
    }

    private String phoneBookHash(PhoneBookRecord[] book) {
        MessageDigest crypt = null;
        try {
            crypt = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            // Never happens
            return "";
        }

        for (PhoneBookRecord record : book) {
            crypt.update(record.getFirstName().getBytes());
            crypt.update(record.getLastName().getBytes());
            for (Phone phone : record.getPhones()) {
                crypt.update(phone.getNumber().getBytes());
            }
        }

        return CryptoUtils.ToHex(crypt.digest());
    }

    public PhoneBookRecord[] loadPhoneBook() {
        Logger.d(TAG, "Contacts: loading phone book");
        HashMap<Long, PhoneBookRecord> recordsMap = new HashMap<Long, PhoneBookRecord>();
        ArrayList<PhoneBookRecord> records = new ArrayList<PhoneBookRecord>();
        ContentResolver cr = application.getContentResolver();
        if (cr == null) {
            return new PhoneBookRecord[0];
        }
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, ContactsContract.Contacts.HAS_PHONE_NUMBER + " > 0", null, ContactsContract.Contacts._ID + " desc");
        if (cur == null) {
            return new PhoneBookRecord[0];
        }
        int idIndex = cur.getColumnIndex(ContactsContract.Contacts._ID);
        int nameIndex = cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        while (cur.moveToNext()) {
            long id = cur.getLong(idIndex);
            String name = cur.getString(nameIndex);
            if (name == null)
                continue;

            String[] nameParts = name.split(" ", 2);
            PhoneBookRecord record = new PhoneBookRecord();
            record.setContactId(id);
            if (nameParts.length == 2) {
                record.setFirstName(nameParts[0]);
                record.setLastName(nameParts[1]);
            } else {
                record.setFirstName(name);
                record.setLastName("");
            }
            records.add(record);
            recordsMap.put(id, record);
        }
        cur.close();

        Cursor pCur = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null, ContactsContract.CommonDataKinds.Phone._ID + " desc");
        if (pCur == null) {
            return new PhoneBookRecord[0];
        }
        int idContactIndex = pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
        int idPhoneIndex = pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID);
        int idNumberIndex = pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
        while (pCur.moveToNext()) {
            long contactId = pCur.getLong(idContactIndex);
            PhoneBookRecord record = recordsMap.get(contactId);
            if (record != null) {
                long phoneId = pCur.getLong(idPhoneIndex);
                String phoneNo = pCur.getString(idNumberIndex);
                record.getPhones().add(new Phone(phoneNo, phoneId));
            }
        }
        pCur.close();
        Logger.d(TAG, "Contacts: loading phone book end");
        return records.toArray(new PhoneBookRecord[records.size()]);
    }

    private String unifyPhone(String s) {
        return s.replaceAll("[^\\d]", "");
    }

    private void doUploadContacts() throws Exception {
        long startFetchingContacts = System.currentTimeMillis();
        long newMaxId = maxContactId;
        TLVector<TLInputContact> inputContacts = new TLVector<TLInputContact>();

        PhoneBookRecord[] book = loadPhoneBook();
        String bookHash = phoneBookHash(book);
        Logger.d(TAG, "Contacts fetching time: " + (System.currentTimeMillis() - startFetchingContacts) + " ms");

        HashMap<Long, Long> idMap = new HashMap<Long, Long>();

        HashMap<String, Long> phoneMap = new HashMap<String, Long>();
        HashMap<Long, HashSet<Long>> realPhoneMap = new HashMap<Long, HashSet<Long>>();

        for (PhoneBookRecord record : book) {
            for (Phone phone : record.getPhones()) {
                idMap.put(phone.getId(), record.getContactId());

                String phoneVal = unifyPhone(phone.getNumber());
                if (phoneMap.containsKey(phoneVal)) {
                    realPhoneMap.get(phoneMap.get(phoneVal)).add(phone.getId());
                } else {
                    HashSet<Long> phoneIdSet = new HashSet<Long>();
                    phoneIdSet.add(phone.getId());
                    realPhoneMap.put(phone.getId(), phoneIdSet);
                    phoneMap.put(phoneVal, phone.getId());
                    inputContacts.add(new TLInputContact(phone.getId(), phoneVal, record.getFirstName(), record.getLastName()));
                }
            }
        }

        TLImportedContacts importedContacts = application.getApi().doRpcCall(new TLRequestContactsImportContacts(inputContacts, false), 15000);
        application.getEngine().onUsers(importedContacts.getUsers());

        HashMap<Long, HashSet<Integer>> importedUsers = new HashMap<Long, HashSet<Integer>>();

        for (TLImportedContact contact : importedContacts.getImported()) {
            for (Long phoneId : realPhoneMap.get(contact.getClientId())) {
                long contactId = idMap.get(phoneId);
                int userId = contact.getUserId();
                if (importedUsers.containsKey(contactId)) {
                    importedUsers.get(contactId).add(userId);
                } else {
                    HashSet<Integer> set = new HashSet<Integer>();
                    set.add(userId);
                    importedUsers.put(contactId, set);
                }
            }
        }
        application.getEngine().onImportedContacts(importedUsers);

        for (User user : application.getEngine().getContacts()) {
            contactsCache.add(user.getUid());
        }

        maxContactId = newMaxId;
        lastSyncHash = bookHash;
        if (isClosed) {
            return;
        }
        preferences.edit().putLong("max_sync_id", maxContactId).putString("sync_hash", bookHash).commit();
        notifyDataChanged();
    }

    private void doSyncContacts() throws Exception {
        String hash;
        List<Contact> savedContacts = application.getEngine().getContactsDao().queryForAll();

        if (savedContacts.size() == 0) {
            hash = "";
        } else {
            ArrayList<Integer> uids = new ArrayList<Integer>();
            for (Contact c : savedContacts) {
                uids.add(c.getUid());
            }
            Collections.sort(uids);
            String uidString = "";
            for (Integer u : uids) {
                if (uidString.length() != 0) {
                    uidString += ",";
                }
                uidString += u;
            }
            hash = CryptoUtils.MD5(uidString.getBytes());
        }

        TLAbsContacts response = application.getApi().doRpcCall(new TLRequestContactsGetContacts(hash));
        if (response instanceof TLContacts) {
            TLContacts contacts = (TLContacts) response;
            application.getEngine().onContacts(contacts.getUsers(), contacts.getContacts());
            User[] users = application.getEngine().getContacts();
            for (User u : users) {
                contactsCache.add(u.getUid());
            }
            notifyDataChanged();
        }
    }

    private void applyDiff() {
        if (application.getTechKernel().getTechReflection().isOnSdCard()) {
            return;
        }

        Logger.d(TAG, "Contacts: applying changes to phonebook");
        User[] users = getUsers();

        Uri rawContactUri = ContactsContract.RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, application.getKernel().getAuthKernel().getAccount().name).appendQueryParameter(
                ContactsContract.RawContacts.ACCOUNT_TYPE, application.getKernel().getAuthKernel().getAccount().type).build();
        Cursor c1 = application.getContentResolver().query(rawContactUri, new String[]{BaseColumns._ID, ContactsContract.RawContacts.SYNC2}, null, null, null);
        HashMap<Integer, Long> localContacts = new HashMap<Integer, Long>();
        if (c1 != null) {
            while (c1.moveToNext()) {
                localContacts.put(c1.getInt(1), c1.getLong(0));
            }
            c1.close();
        }

        Logger.d(TAG, "Readed saved contacts");

        HashSet<Integer> founded = new HashSet<Integer>();
        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        for (User u : users) {
            founded.add(u.getUid());
            if (!localContacts.containsKey(u.getUid())) {
                addContact(false, application.getKernel().getAuthKernel().getAccount(), u, u.getDisplayName(), u.getPhone(), operationList);
                if (operationList.size() > 200) {
                    complete(operationList);
                    operationList.clear();
                }
            }
        }
        if (operationList.size() > 0) {
            complete(operationList);
        }
        Logger.d(TAG, "applying changes ends");
        /*HashSet<String> removed = new HashSet<String>();
        for (String key : localContacts.keySet()) {
            if (!founded.contains(key)) {
                removed.add(key);
            }
        }*/
    }

    private void addContact(boolean withCheck, Account account, User user, String name, String phone, ArrayList<ContentProviderOperation> operationList) {
        if (withCheck) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI);
            builder.withSelection(
                    ContactsContract.RawContacts.ACCOUNT_NAME + " = ? AND " +
                            ContactsContract.RawContacts.ACCOUNT_TYPE + " = ? AND " +
                            ContactsContract.RawContacts.SYNC2 + " = ?",
                    new String[]{account.name, account.type, "" + user.getUid()});
            operationList.add(builder.build());
        }

        int first = operationList.size();

        //Create our RawContact
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI);
        builder.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name);
        builder.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type);
        builder.withValue(ContactsContract.RawContacts.SYNC1, phone);
        builder.withValue(ContactsContract.RawContacts.SYNC2, user.getUid());
        operationList.add(builder.build());

        //Create a Data record of common type 'StructuredName' for our RawContact
        builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder.withValueBackReference(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, first);
        builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name);
        operationList.add(builder.build());

        builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder.withValueBackReference(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, first);
        builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        builder.withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, "+" + phone);
        builder.withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        operationList.add(builder.build());

        //Create a Data record of custom type "vnd.android.cursor.item/vnd.fm.last.android.profile" to display a link to the Last.fm profile
        builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, first);
        builder.withValue(ContactsContract.Data.MIMETYPE, "vnd.android.cursor.item/vnd.org.telegram.android.profile");
        builder.withValue(ContactsContract.Data.DATA1, "+" + phone);
        builder.withValue(ContactsContract.Data.DATA2, "Telegram Profile");
        builder.withValue(ContactsContract.Data.DATA3, "+" + phone);
        builder.withValue(ContactsContract.Data.DATA4, user.getUid());
        operationList.add(builder.build());

//        try {
//            ContentProviderResult[] results = application.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
//            if (results.length < 4) {
//                return 0;
//            }
//            return Long.parseLong(results[results.length - 4].uri.getLastPathSegment());
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        } catch (OperationApplicationException e) {
//            e.printStackTrace();
//        }
//        return 0;
    }

    private void complete(ArrayList<ContentProviderOperation> operationList) {
        try {
            application.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    private void doSync() throws Exception {
        if (checkPhonesChanged()) {
            Logger.d(TAG, "Performing sync");
            Logger.d(TAG, "Downloading contacts");
            doSyncContacts();
            applyDiff();

            Logger.d(TAG, "Uploading contacts");
            doUploadContacts();
            Logger.d(TAG, "Downloading updated contacts");
            doSyncContacts();
            applyDiff();
        } else {
            Logger.d(TAG, "Sync not required");
            Logger.d(TAG, "Downloading contacts");
            doSyncContacts();
            applyDiff();
        }
    }

    public synchronized void destroy() {
        Logger.d(TAG, "Destroying contacts");
        this.isClosed = true;
        service.shutdownNow();
        if (this.contentObserver != null) {
            application.getContentResolver().unregisterContentObserver(this.contentObserver);
            this.contentObserver = null;
        }
        this.maxContactId = 0;
        this.contactsCache = null;
        this.lastSyncHash = null;
    }
}