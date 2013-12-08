package org.telegram.android.core.background.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.Contact;
import org.telegram.android.core.model.TLLocalContext;
import org.telegram.android.core.model.phone.TLLocalBook;
import org.telegram.android.core.model.phone.TLLocalImportedPhone;
import org.telegram.android.critical.TLPersistence;
import org.telegram.android.log.Logger;
import org.telegram.api.TLImportedContact;
import org.telegram.api.TLInputContact;
import org.telegram.api.contacts.TLImportedContacts;
import org.telegram.api.requests.TLRequestContactsImportContacts;
import org.telegram.tl.TLVector;

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

    public ContactsSync(StelsApplication application) {
        super(application, SETTINGS_NAME);

        this.application = application;
        this.bookPersistence = new TLPersistence<TLLocalBook>(application, "book_sync.bin", TLLocalBook.class, TLLocalContext.getInstance());

        registerSyncSingleOffline(SYNC_CONTACTS_PRE, "contactsPreSync");
        registerSyncEvent(SYNC_CONTACTS, "contactsSync");

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
    }

    public void clear() {
        preferences.edit().putBoolean("is_synced", false).commit();
        this.isSynced = false;
        this.isLoaded = false;
        this.bookPersistence.getObj().getImportedPhones().clear();
        this.bookPersistence.getObj().getContacts().clear();
        this.bookPersistence.write();
    }

    public ContactSyncListener getListener() {
        return listener;
    }

    public void setListener(ContactSyncListener listener) {
        this.listener = listener;
    }

    private synchronized void notifyChanged() {
        Logger.d(TAG, "notifyChanged");
        if (!isSynced) {
            Logger.d(TAG, "ignoring notifyChanged: not synced");
            return;
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

    public void run() {
        init();
        this.observerUpdatesThread = new HandlerThread("ObserverHandlerThread") {
            @Override
            protected void onLooperPrepared() {

                contentObserver = new ContentObserver(new Handler(observerUpdatesThread.getLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        invalidateContactsSync();
                    }
                };
                application.getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contentObserver);
            }
        };
        this.observerUpdatesThread.start();
    }

    public void invalidateContactsSync() {
        resetSync(SYNC_CONTACTS_PRE);
    }

    protected void contactsPreSync() throws Exception {
        Logger.d(TAG, "PreSync:" + isLoaded);
        if (!isLoaded) {
            PhoneBookRecord[] freshPhoneBook = loadPhoneBook();
            ArrayList<Contact> freshContacts = new ArrayList<Contact>();
            Collections.addAll(freshContacts, application.getEngine().getAllContacts());
            synchronized (phoneBookSync) {
                currentPhoneBook = freshPhoneBook;
                contacts = freshContacts;
                isLoaded = true;
            }
        } else {
            PhoneBookRecord[] freshPhoneBook = loadPhoneBook();
            synchronized (phoneBookSync) {
                currentPhoneBook = freshPhoneBook;
            }
        }

        notifyChanged();

        updateMapping();

        notifyChanged();

        resetSync(SYNC_CONTACTS);
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

        Thread.sleep(10000);

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
                        continue outer;
                    }
                }

                bookPersistence.getObj().getImportedPhones().add(new TLLocalImportedPhone(phonesForImport.value, uid));
            }
            updateMapping();
            isSynced = true;
            preferences.edit().putBoolean("is_synced", true).commit();
            notifyChanged();
            bookPersistence.write();
        } else {
            updateMapping();
            isSynced = true;
            preferences.edit().putBoolean("is_synced", true).commit();
            notifyChanged();
        }
    }

    protected void updateMapping() {
        Logger.d(TAG, "updateMapping");
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

        application.getEngine().onImportedContacts(imported);
    }

    private PhonesForImport[] diff(PhonesForImport[] phones) {
        ArrayList<PhonesForImport> res = new ArrayList<PhonesForImport>();
        outer:
        for (PhonesForImport p : phones) {
            for (TLLocalImportedPhone importedPhone : bookPersistence.getObj().getImportedPhones()) {
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

        if (phoneUtil == null) {
            phoneUtil = PhoneNumberUtil.getInstance();
        }

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
        Logger.d(TAG, "Contacts: loading phone book end");
        return records.toArray(new PhoneBookRecord[records.size()]);
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
