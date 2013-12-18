package org.telegram.android.core.background.sync;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.Contact;
import org.telegram.android.core.model.TLLocalContext;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.phone.TLLocalBook;
import org.telegram.android.core.model.phone.TLLocalImportedPhone;
import org.telegram.android.critical.TLPersistence;
import org.telegram.android.log.Logger;
import org.telegram.api.TLImportedContact;
import org.telegram.api.TLInputContact;
import org.telegram.api.contacts.TLAbsContacts;
import org.telegram.api.contacts.TLContacts;
import org.telegram.api.contacts.TLImportedContacts;
import org.telegram.api.requests.TLRequestContactsGetContacts;
import org.telegram.api.requests.TLRequestContactsImportContacts;
import org.telegram.mtproto.secure.CryptoUtils;
import org.telegram.tl.TLVector;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by ex3ndr on 07.12.13.
 */
public class ContactsSync extends BaseSync {

    public static interface ContactSyncListener {
        public void onBookUpdated();
    }

    private static final String SETTINGS_NAME = "org.telegram.android.contacts";

    private static final int SYNC_CONTACTS_PRE = 1;
    private static final int SYNC_CONTACTS = 2;
    private static final int SYNC_CONTACTS_INTEGRATION = 3;
    private static final int SYNC_CONTACTS_TWO_SIDE = 4;
    private static final int SYNC_CONTACTS_TWO_SIDE_TIMEOUT = 12 * HOUR;

    private static final int OP_LIMIT = 20;

    private static final boolean TWO_SIDE_SYNC = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;

    private static final String TAG = "ContactsSync";

    private StelsApplication application;

    private TLPersistence<TLLocalBook> bookPersistence;

    private String isoCountry;

    private PhoneNumberUtil phoneUtil;

    private final Object phoneBookSync = new Object();

    private PhoneBookRecord[] currentPhoneBook;

    private ArrayList<Contact> contacts;

    private boolean isLoaded = true;

    private boolean isSynced = false;

    private ContactSyncListener listener;

    private ContentObserver contentObserver;
    private HandlerThread observerUpdatesThread;

    private final Object contactsSync = new Object();

    private long lastContactsChanged = 0;

    public ContactsSync(StelsApplication application) {
        super(application, SETTINGS_NAME);

        long start = SystemClock.uptimeMillis();
        this.application = application;
        this.bookPersistence = new TLPersistence<TLLocalBook>(application, "book_sync.bin", TLLocalBook.class, TLLocalContext.getInstance());
        Logger.d(TAG, "Persistence loaded in " + (SystemClock.uptimeMillis() - start) + " ms");

        start = SystemClock.uptimeMillis();
        registerSyncSingleOffline(SYNC_CONTACTS_PRE, "contactsPreSync");
        registerSyncSingleOffline(SYNC_CONTACTS_INTEGRATION, "updateIntegration");
        registerSyncEvent(SYNC_CONTACTS, "contactsSync");
        registerSync(SYNC_CONTACTS_TWO_SIDE, "updateContacts", SYNC_CONTACTS_TWO_SIDE_TIMEOUT);
        Logger.d(TAG, "Sync registered in " + (SystemClock.uptimeMillis() - start) + " ms");

        start = SystemClock.uptimeMillis();
        TelephonyManager manager = (TelephonyManager) application.getSystemService(Context.TELEPHONY_SERVICE);

        if (manager.getSimCountryIso() != null) {
            isoCountry = manager.getSimCountryIso();
        } else if (manager.getNetworkCountryIso() != null) {
            isoCountry = manager.getNetworkCountryIso();
        } else {
            isoCountry = "us";
        }

        isSynced = preferences.getBoolean("is_synced", false);
        isLoaded = false;
        Logger.d(TAG, "Completed init in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    public void clear() {
        preferences.edit().putBoolean("is_synced", false).commit();
        this.isSynced = false;
        this.isLoaded = false;
        this.bookPersistence.getObj().getImportedPhones().clear();
        this.bookPersistence.write();
    }

    public void addPhoneMapping(int uid, String phone) {
        for (TLLocalImportedPhone importedPhone : bookPersistence.getObj().getImportedPhones()) {
            if (importedPhone.getPhone().equals(phone)) {
                return;
            }
        }

        bookPersistence.getObj().getImportedPhones().add(new TLLocalImportedPhone(phone, uid, false));
        bookPersistence.write();
    }

    public void removeContact(long contactId) {
        Contact[] relatedContacts = application.getEngine().getUsersEngine().getContactsForLocalId(contactId);
        application.getEngine().getUsersEngine().deleteContactsForLocalId(contactId);

        HashSet<Integer> uids = new HashSet<Integer>();
        for (Contact c : relatedContacts) {
            uids.add(c.getUid());
        }

        for (Integer uid : uids) {
            for (TLLocalImportedPhone importedPhone : bookPersistence.getObj().getImportedPhones()) {
                if (importedPhone.getUid() == uid) {
                    bookPersistence.getObj().getImportedPhones().remove(importedPhone);
                    bookPersistence.write();
                    break;
                }
            }
            synchronized (contactsSync) {
                Logger.d(TAG, "Writing integration contacts...");

                Uri rawContactUri = ContactsContract.RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, application.getKernel().getAuthKernel().getAccount().name).appendQueryParameter(
                        ContactsContract.RawContacts.ACCOUNT_TYPE, application.getKernel().getAuthKernel().getAccount().type).build();
                application.getContentResolver().delete(rawContactUri, ContactsContract.RawContacts.SYNC2 + " = " + uid, null);
            }
        }
    }

    public void resetSync() {
        Logger.d(TAG, "resetSync");
        resetSync(SYNC_CONTACTS_PRE);
        resetSync(SYNC_CONTACTS_TWO_SIDE);
    }

    public ContactSyncListener getListener() {
        return listener;
    }

    public void setListener(ContactSyncListener listener) {
        this.listener = listener;
    }

    private synchronized void notifyChanged() {
        if (!isSynced) {
            Logger.d(TAG, "notifyChanged: ignored, not synced");
            return;
        } else {
            Logger.d(TAG, "notifyChanged");
        }
        if (this.listener != null) {
            this.listener.onBookUpdated();
        }
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public ArrayList<Contact> getContacts() {
        return contacts;
    }

    public PhoneBookRecord[] getCurrentPhoneBook() {
        return currentPhoneBook;
    }

    private String currentPhoneBookHash;

    public void run() {
        init();
        this.observerUpdatesThread = new HandlerThread("ObserverHandlerThread") {
            @Override
            protected void onLooperPrepared() {

                contentObserver = new ContentObserver(new Handler(observerUpdatesThread.getLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        if (SystemClock.uptimeMillis() - lastContactsChanged < 1000) {
                            return;
                        }

                        if (currentPhoneBookHash == null) {
                            invalidateContactsSync();
                        } else {
                            PhoneBookRecord[] records = loadPhoneBook();
                            String hash = phoneBookHash(records);
                            if (!hash.equals(currentPhoneBookHash)) {
                                currentPhoneBookHash = hash;
                                invalidateContactsSync();
                            } else {
                                lastContactsChanged = SystemClock.uptimeMillis();
                            }
                        }
                    }
                };
                application.getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, false, contentObserver);
            }
        };
        this.observerUpdatesThread.setPriority(Thread.MIN_PRIORITY);
        this.observerUpdatesThread.start();
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

    public void invalidateContactsSync() {
        resetSync(SYNC_CONTACTS_PRE);
        resetSync(SYNC_CONTACTS_TWO_SIDE);
        Logger.d(TAG, "invalidateContactsSync");
    }

    private void invalidateIntegration() {
        resetSync(SYNC_CONTACTS_INTEGRATION);
        Logger.d(TAG, "invalidateIntegration");
    }

    protected void contactsPreSync() throws Exception {
        Logger.d(TAG, "PreSync:" + isLoaded);
        if (!isLoaded) {
            PhoneBookRecord[] freshPhoneBook = loadPhoneBook();
            ArrayList<Contact> freshContacts = new ArrayList<Contact>();
            Collections.addAll(freshContacts, application.getEngine().getUsersEngine().getAllContacts());
            synchronized (phoneBookSync) {
                currentPhoneBook = freshPhoneBook;
                currentPhoneBookHash = phoneBookHash(currentPhoneBook);
                contacts = freshContacts;
                isLoaded = true;
            }
        } else {
            PhoneBookRecord[] freshPhoneBook = loadPhoneBook();
            synchronized (phoneBookSync) {
                currentPhoneBook = freshPhoneBook;
                currentPhoneBookHash = phoneBookHash(currentPhoneBook);
            }
        }

        notifyChanged();

        updateMapping();

        notifyChanged();

        resetSync(SYNC_CONTACTS);
    }

    protected void updateContacts() throws Exception {
        if (!isSynced) {
            Logger.d(TAG, "Ignoring contacts loading: not synced");
            return;
        }

        String hash;
        User[] contacts = application.getEngine().getUsersEngine().getContacts();
        if (contacts.length == 0) {
            hash = "";
        } else {
            ArrayList<Integer> uids = new ArrayList<Integer>();
            for (User c : contacts) {
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
            TLContacts contactsResponse = (TLContacts) response;
            application.getEngine().getUsersEngine().onContacts(contactsResponse.getUsers(), contactsResponse.getContacts());
        }

        if (TWO_SIDE_SYNC) {
            // updateIntegration();
            invalidateIntegration();
        }
    }

    protected void contactsSync() throws Exception {

        PhoneBookRecord[] phoneBookRecords;
        synchronized (phoneBookSync) {
            phoneBookRecords = currentPhoneBook;
        }
        if (phoneBookRecords == null) {
            Logger.w(TAG, "Cancelling contacts sync: preloaded phonebook is empty");
            return;
        }

        PhonesForImport[] phonesForImports = filterPhones(phoneBookRecords);
        PhonesForImport[] resultImports = diff(phonesForImports);
        Logger.d(TAG, "Phone book contacts: " + phoneBookRecords.length);
        Logger.d(TAG, "Phone book uniq phones: " + phonesForImports.length);
        Logger.d(TAG, "Diff phones count: " + resultImports.length);

        if (resultImports.length > 0) {
            TLVector<TLInputContact> inputContacts = new TLVector<TLInputContact>();
            for (PhonesForImport phonesForImport : resultImports) {
                inputContacts.add(new TLInputContact(phonesForImport.baseId, phonesForImport.value, phonesForImport.firstName, phonesForImport.lastName));
            }

            TLImportedContacts importedContacts = application.getApi().doRpcCallGzip(new TLRequestContactsImportContacts(inputContacts, false), 60000);

            application.getEngine().onUsers(importedContacts.getUsers());

            Logger.d(TAG, "Imported phones count: " + importedContacts.getImported().size());

            outer:
            for (PhonesForImport phonesForImport : resultImports) {
                int uid = 0;
                for (TLImportedContact contact : importedContacts.getImported()) {
                    if (phonesForImport.baseId == contact.getClientId()) {
                        uid = contact.getUserId();
                        break;
                    }
                }

                for (TLLocalImportedPhone importedPhone : bookPersistence.getObj().getImportedPhones()) {
                    if (importedPhone.getPhone().equals(phonesForImport.value)) {
                        if (!importedPhone.isImported()) {
                            bookPersistence.getObj().getImportedPhones().remove(importedPhone);
                            break;
                        } else {
                            continue outer;
                        }
                    }
                }

                bookPersistence.getObj().getImportedPhones().add(new TLLocalImportedPhone(phonesForImport.value, uid, true));
            }
            updateMapping();
            isSynced = true;
            preferences.edit().putBoolean("is_synced", true).commit();
            notifyChanged();
            invalidateIntegration();
            bookPersistence.write();
        } else {
            updateMapping();
            isSynced = true;
            preferences.edit().putBoolean("is_synced", true).commit();
            notifyChanged();
            invalidateIntegration();
        }
    }

    protected void updateIntegration() {
        if (!isSynced) {
            Logger.d(TAG, "Ignoring integration: not synced");
            return;
        }

        if (!TWO_SIDE_SYNC) {
            return;
        }

        long start = SystemClock.uptimeMillis();
        User[] users = application.getEngine().getUsersEngine().getContacts();
        HashMap<Integer, Long> localContacts = new HashMap<Integer, Long>();
        synchronized (contactsSync) {
            Logger.d(TAG, "Writing integration contacts...");

            Uri rawContactUri = ContactsContract.RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, application.getKernel().getAuthKernel().getAccount().name).appendQueryParameter(
                    ContactsContract.RawContacts.ACCOUNT_TYPE, application.getKernel().getAuthKernel().getAccount().type).build();
            Cursor c1 = application.getContentResolver().query(rawContactUri, new String[]{BaseColumns._ID, ContactsContract.RawContacts.SYNC2}, null, null, null);
            if (c1 != null) {
                while (c1.moveToNext()) {
                    localContacts.put(c1.getInt(1), c1.getLong(0));
                }
                c1.close();
            }
        }

        Logger.d(TAG, "Readed saved contacts in " + (SystemClock.uptimeMillis() - start) + " ms");
        start = SystemClock.uptimeMillis();

        synchronized (contactsSync) {
            ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
            for (User u : users) {
                if (!localContacts.containsKey(u.getUid())) {

                    addContact(false, application.getKernel().getAuthKernel().getAccount(), u, u.getDisplayName(), u.getPhone(), operationList);
                    //if (operationList.size() > OP_LIMIT) {
                    complete(operationList);
                    operationList.clear();
                    //}
                }
            }

            if (operationList.size() > 0) {
                complete(operationList);
            }
        }
        Logger.d(TAG, "Changes applied in " + (SystemClock.uptimeMillis() - start) + " ms");
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


    protected void updateMapping() {
        Logger.d(TAG, "updatingMapping...");
        long start = SystemClock.uptimeMillis();

        PhoneBookRecord[] phoneBookRecords;
        synchronized (phoneBookSync) {
            phoneBookRecords = currentPhoneBook;
        }
        if (phoneBookRecords == null) {
            Logger.w(TAG, "Cancelling contact mapping: preloaded phonebook is empty");
            return;
        }

        HashMap<Long, HashSet<Integer>> imported = new HashMap<Long, HashSet<Integer>>();
        HashSet<Integer> uids = new HashSet<Integer>();
        for (PhoneBookRecord bookRecord : phoneBookRecords) {
            phoneLoop:
            for (Phone phone : bookRecord.getPhones()) {
                for (TLLocalImportedPhone importedPhone : bookPersistence.getObj().getImportedPhones()) {
                    if (importedPhone.getUid() == 0) {
                        continue;
                    }
                    if (phone.getNumber().equals(importedPhone.getPhone())) {
                        uids.add(importedPhone.getUid());
                        continue phoneLoop;
                    }
                }
            }

            if (uids.size() > 0) {
                imported.put(bookRecord.contactId, uids);
                uids = new HashSet<Integer>();
            }
        }

        Logger.d(TAG, "build map in " + (SystemClock.uptimeMillis() - start) + " ms");

        application.getEngine().getUsersEngine().onImportedContacts(imported);

        Logger.d(TAG, "updatedMapping in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    private PhonesForImport[] diff(PhonesForImport[] phones) {
        ArrayList<PhonesForImport> res = new ArrayList<PhonesForImport>();
        outer:
        for (PhonesForImport p : phones) {
            for (TLLocalImportedPhone importedPhone : bookPersistence.getObj().getImportedPhones()) {
                if (!importedPhone.isImported()) {
                    break;
                }

                if (importedPhone.getPhone().equals(p.value)) {
                    continue outer;
                }
            }
            res.add(p);
        }
        return res.toArray(new PhonesForImport[res.size()]);
    }

    private PhonesForImport[] filterPhones(PhoneBookRecord[] book) {
        ArrayList<PhonesForImport> res = new ArrayList<PhonesForImport>();

        HashSet<String> foundedPhones = new HashSet<String>();

        for (PhoneBookRecord record : book) {
            for (Phone phone : record.getPhones()) {
                if (foundedPhones.contains(phone.getNumber())) {
                    continue;
                }
                foundedPhones.add(phone.getNumber());

                res.add(new PhonesForImport(phone.id, phone.getNumber(), record.getFirstName(), record.getLastName()));
            }
        }

        return res.toArray(new PhonesForImport[0]);
    }

    public PhoneBookRecord[] loadPhoneBook() {
        synchronized (contactsSync) {
            Logger.d(TAG, "Loading phone book");
            long start = SystemClock.uptimeMillis();
            HashMap<Long, PhoneBookRecord> recordsMap = new HashMap<Long, PhoneBookRecord>();
            ArrayList<PhoneBookRecord> records = new ArrayList<PhoneBookRecord>();
            ContentResolver cr = application.getContentResolver();
            if (cr == null) {
                return new PhoneBookRecord[0];
            }
            Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                    new String[]
                            {
                                    ContactsContract.Contacts._ID,
                                    ContactsContract.Contacts.DISPLAY_NAME
                            },
                    ContactsContract.Contacts.HAS_PHONE_NUMBER + " > 0", null, ContactsContract.Contacts._ID + " desc");
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
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone._ID,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null,
                    null, ContactsContract.CommonDataKinds.Phone._ID + " desc");
            if (pCur == null) {
                return new PhoneBookRecord[0];
            }
            int idContactIndex = pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
            int idPhoneIndex = pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID);
            int idNumberIndex = pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

            Logger.d(TAG, "Phone book phase 1 in " + (SystemClock.uptimeMillis() - start) + " ms");

            if (phoneUtil == null) {
                phoneUtil = PhoneNumberUtil.getInstance();
            }

            Logger.d(TAG, "Phone book phase 2 in " + (SystemClock.uptimeMillis() - start) + " ms");

            while (pCur.moveToNext()) {
                long contactId = pCur.getLong(idContactIndex);
                PhoneBookRecord record = recordsMap.get(contactId);
                if (record != null) {
                    long phoneId = pCur.getLong(idPhoneIndex);
                    String phoneNo = pCur.getString(idNumberIndex);

                    try {
                        Phonenumber.PhoneNumber phonenumber = phoneUtil.parse(phoneNo, isoCountry);
                        phoneNo = phonenumber.getCountryCode() + "" + phonenumber.getNationalNumber();
                    } catch (NumberParseException e) {
                        phoneNo = phoneNo.replaceAll("[^\\d]", "");
                    }

                    record.getPhones().add(new Phone(phoneNo, phoneId));
                }
            }
            pCur.close();
            Logger.d(TAG, "Phone book loaded in " + (SystemClock.uptimeMillis() - start) + " ms");
            return records.toArray(new PhoneBookRecord[records.size()]);
        }
    }

    private class PhonesForImport {
        private long baseId;
        private String value;

        private String firstName;
        private String lastName;

        public PhonesForImport(long baseId, String value, String firstName, String lastName) {
            this.baseId = baseId;
            this.value = value;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public long getBaseId() {
            return baseId;
        }

        public String getValue() {
            return value;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }
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
}
