package org.telegram.android.core.background.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.contacts.ContactsUploadState;
import org.telegram.android.core.contacts.SyncContact;
import org.telegram.android.core.contacts.SyncPhone;
import org.telegram.android.core.model.Contact;
import org.telegram.android.core.model.LinkType;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.phone.TLImportedPhone;
import org.telegram.android.kernel.ApplicationKernel;
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

import java.util.*;

/**
 * Created by ex3ndr on 07.12.13.
 */
public class ContactsSync extends BaseSync {

    public static interface ContactSyncListener {
        public void onBookUpdated();
    }

    private static final String SETTINGS_NAME = "org.telegram.android.contacts";

    private static final int SYNC_CONTACTS_PRE = 1;

    private static final int SYNC_DOWNLOAD_REQUEST_TIMEOUT = 30000;
    private static final int SYNC_UPLOAD_REQUEST_TIMEOUT = 60000;

    private static final int IMPORT_LIMIT = 150;

    private static final String TAG = "ContactsSync";

    private String isoCountry;

    private PhoneNumberUtil phoneUtil;

    private PhoneBookRecord[] currentPhoneBook;

    private boolean isSynced = false;

    private ContactSyncListener listener;

    private ContentObserver contentObserver;
    private HandlerThread observerUpdatesThread;

    private final Object contactsSync = new Object();

    private long lastContactsChanged = 0;

    private ContactsUploadState uploadState;

    private ApplicationKernel kernel;

    public ContactsSync(ApplicationKernel kernel) {
        super(kernel.getApplication(), SETTINGS_NAME);

        this.kernel = kernel;

        long start = SystemClock.uptimeMillis();
        registerSyncSingleOffline(SYNC_CONTACTS_PRE, "contactsPreSync");
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

        isSynced = kernel.getStorageKernel().getModel().getSyncStateEngine().isContactSynced();
        Logger.d(TAG, "Completed init in " + (SystemClock.uptimeMillis() - start) + " ms");
    }


    public ContactSyncListener getListener() {
        return listener;
    }

    public void setListener(ContactSyncListener listener) {
        this.listener = listener;
    }

    public void run() {
        init();
        this.observerUpdatesThread = new HandlerThread("ObserverHandlerThread") {
            @Override
            protected void onLooperPrepared() {
                contentObserver = new ContentObserver(new Handler(observerUpdatesThread.getLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        if (SystemClock.uptimeMillis() - lastContactsChanged < 500) {
                            return;
                        }
                        invalidateContactsSync();
                        lastContactsChanged = SystemClock.uptimeMillis();

                    }
                };
                application.getContentResolver().
                        registerContentObserver(ContactsContract.Contacts.CONTENT_URI, false, contentObserver);
            }
        }

        ;
        this.observerUpdatesThread.setPriority(Thread.MIN_PRIORITY);
        this.observerUpdatesThread.start();
    }

    public void clear() {
        this.kernel.getStorageKernel().getModel().getSyncStateEngine().setSynced(false);
        this.isSynced = false;
        this.uploadState.getImportedPhones().clear();
        this.uploadState.getContacts().clear();
        this.uploadState.write();
    }

    public void invalidateContactsSync() {
        resetSync(SYNC_CONTACTS_PRE);
        Logger.d(TAG, "invalidateContactsSync");
    }


    public void resetSync() {
        Logger.d(TAG, "resetSync");
        resetSync(SYNC_CONTACTS_PRE);
    }

    public void addPhoneMapping(int uid, String phone) {
        uploadState.getImportedPhones().put(phone, uid);
        uploadState.write();
    }

    public void removeContact(long contactId) {
        application.getEngine().getUsersEngine().deleteContactsForLocalId(contactId);
    }

    public void removeContactLinks(int uid) {
        application.getEngine().getUsersEngine().deleteContactsForUid(uid);
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

    protected void contactsPreSync() throws Exception {
        reloadPhoneBook();

        notifyChanged();

        updateUploadingState();

        contactsOffline();

        contactsUpload();

        contactsDownload();

        updateMapping();

        notifyChanged();
    }

    private void reloadPhoneBook() {
        PhoneBookDiff diff = null;
        PhoneBookRecord[] freshPhoneBook = loadPhoneBook();
        if (currentPhoneBook != null) {
            diff = diffPhoneBook(currentPhoneBook, freshPhoneBook);
        }
        currentPhoneBook = freshPhoneBook;

        // Log difference
        if (diff != null && !diff.isEmpty()) {
            if (diff.getAddedContacts().size() != 0) {
                for (PhoneBookRecord record : diff.getAddedContacts()) {
                    Logger.d(TAG, "DIFF: Added:  #" + record.getContactId() + " " + record.getFirstName() + " " + record.getLastName());
                }
            }
            if (diff.getRemovedContacts().size() != 0) {
                for (PhoneBookRecord record : diff.getRemovedContacts()) {
                    Logger.d(TAG, "DIFF: Removed: #" + record.getContactId() + " " + record.getFirstName() + " " + record.getLastName());
                }
            }

            if (diff.getUpdatedContacts().size() != 0) {
                for (UpdatedRecord record : diff.getUpdatedContacts()) {
                    Logger.d(TAG, "DIFF: Updated: #" + record.getOldRecord().getContactId() + " " + record.getOldRecord().getFirstName() + " " + record.getOldRecord().getLastName());
                    if (record.isChangedName()) {
                        Logger.d(TAG, "DIFF: New name: " + record.getNewRecord().getFirstName() + " " + record.getNewRecord().getLastName());
                    }
                    for (Phone removed : record.getRemovedPhones()) {
                        Logger.d(TAG, "DIFF: Phone remove: #" + removed.getId() + " " + removed.getNumber());
                    }
                    for (Phone added : record.getAddedPhones()) {
                        Logger.d(TAG, "DIFF: Phone added: #" + added.getId() + " " + added.getNumber());
                    }
                    for (UpdatedPhone updatedPhone : record.getUpdatedPhones()) {
                        Logger.d(TAG, "DIFF: Phone update: #" + updatedPhone.getPhoneId() + " " + updatedPhone.getOldPhone() + " -> " + updatedPhone.getNewPhone());
                    }
                }
            }
        }
    }

    private void updateUploadingState() {
        if (uploadState == null) {
            uploadState = new ContactsUploadState(application);
        }
        applyUploadingState(uploadState, currentPhoneBook);
        uploadState.write();
    }

    private void contactsOffline() {
        List<SyncPhone> syncPhones = uploadState.buildImportPhones();
        for (SyncPhone phone : syncPhones) {
            Integer uid = uploadState.getImportedPhones().get(phone.getNumber());
            if (uid != null) {
                application.getEngine().getUsersEngine().onUserLinkChanged(uid, LinkType.CONTACT);
            }
        }
        updateMapping();
    }

    protected void contactsUpload() throws Exception {

        List<SyncPhone> syncPhones = uploadState.buildImportPhones();

        Logger.d(TAG, "Phone book contacts for import: " + syncPhones.size());

        if (syncPhones.size() > 0) {

            int offset = 0;

            while (offset < syncPhones.size()) {
                TLVector<TLInputContact> inputContacts = new TLVector<TLInputContact>();
                for (int i = 0; i < IMPORT_LIMIT && (i + offset < syncPhones.size()); i++) {
                    SyncPhone phone = syncPhones.get(i + offset);
                    inputContacts.add(new TLInputContact(phone.getPhoneId(), phone.getNumber(), phone.getContact().getFirstName(), phone.getContact().getLastName()));

                }
                offset += IMPORT_LIMIT;

                TLImportedContacts importedContacts = application.getApi().doRpcCallGzip(
                        new TLRequestContactsImportContacts(inputContacts, false), SYNC_UPLOAD_REQUEST_TIMEOUT);

                for (TLInputContact contact : inputContacts) {
                    for (SyncPhone phone : syncPhones) {
                        if (phone.getPhoneId() == contact.getClientId()) {
                            phone.setImported(true);
                        }
                    }
                }

                application.getEngine().onUsers(importedContacts.getUsers());

                Logger.d(TAG, "Imported phones count: " + importedContacts.getImported().size());

                for (SyncPhone phone : syncPhones) {
                    for (TLImportedContact contact : importedContacts.getImported()) {
                        if (phone.getPhoneId() == contact.getClientId()) {
                            uploadState.getImportedPhones().put(phone.getNumber(), contact.getUserId());
                            break;
                        }
                    }
                }
                uploadState.write();
                updateMapping();
            }
            isSynced = true;
            this.kernel.getStorageKernel().getModel().getSyncStateEngine().setSynced(true);
            notifyChanged();
            uploadState.write();
        } else {
            updateMapping();
            isSynced = true;
            this.kernel.getStorageKernel().getModel().getSyncStateEngine().setSynced(true);
            notifyChanged();
        }
    }

    protected void contactsDownload() throws Exception {
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

        TLAbsContacts response = application.getApi().doRpcCall(new TLRequestContactsGetContacts(hash), SYNC_DOWNLOAD_REQUEST_TIMEOUT);
        if (response instanceof TLContacts) {
            TLContacts contactsResponse = (TLContacts) response;
            application.getEngine().getUsersEngine().onContacts(contactsResponse.getUsers(), contactsResponse.getContacts());
        }
    }

    protected void updateMapping() {
        Logger.d(TAG, "updatingMapping...");
        long start = SystemClock.uptimeMillis();

        HashMap<Long, HashSet<Integer>> imported = new HashMap<Long, HashSet<Integer>>();
        HashSet<Integer> uids = new HashSet<Integer>();
        for (PhoneBookRecord bookRecord : currentPhoneBook) {
            for (Phone phone : bookRecord.getPhones()) {
                if (uploadState.getImportedPhones().containsKey(phone.getNumber())) {
                    uids.add(uploadState.getImportedPhones().get(phone.getNumber()));
                }
            }

            if (uids.size() > 0) {
                imported.put(bookRecord.contactId, uids);
                uids = new HashSet<Integer>();
            }
        }

        Logger.d(TAG, "build map in " + (SystemClock.uptimeMillis() - start) + " ms");

        application.getEngine().getUsersEngine().updateContactMapping(imported);

        Logger.d(TAG, "updatedMapping in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Helper objects and methods
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void applyUploadingState(ContactsUploadState uploadState, PhoneBookRecord[] phoneBook) {
        HashMap<String, SyncPhone> primaryPhones = new HashMap<String, SyncPhone>();
        for (SyncContact contact : uploadState.getContacts()) {
            for (SyncPhone phone : contact.getSyncPhones()) {
                if (phone.isPrimary()) {
                    primaryPhones.put(phone.getNumber(), phone);
                }
            }
        }

        for (PhoneBookRecord record : phoneBook) {
            SyncContact syncContact = findSyncContact(uploadState, record.contactId);
            if (syncContact != null) {
                if (!syncContact.getFirstName().equals(record.getFirstName()) ||
                        !syncContact.getLastName().equals(record.getLastName())) {

                    for (SyncPhone phone : syncContact.getSyncPhones()) {
                        phone.setImported(false);
                        markAsPrimary(phone, primaryPhones);
                    }

                    Log.d(TAG, "UDIFF: contact renamed #" + syncContact.getContactId() + " from '" + syncContact.getFirstName() + " " + syncContact.getLastName() +
                            "' to '" + record.getFirstName() + " " + record.getLastName() + "'");
                    syncContact.setFirstName(record.getFirstName());
                    syncContact.setLastName(record.getLastName());
                }
                for (Phone phone : record.getPhones()) {
                    SyncPhone syncPhone = findSyncPhone(syncContact, phone.getId());
                    if (syncPhone == null) {
                        syncPhone = new SyncPhone(phone.id, phone.getNumber(), syncContact);
                        syncPhone.setImported(false);
                        markAsPrimary(syncPhone, primaryPhones);
                        syncContact.getSyncPhones().add(syncPhone);
                        Log.d(TAG, "UDIFF: Added phone to contact #" + syncContact.getContactId() + ": " + syncPhone.getNumber());
                    } else {
                        if (!syncPhone.getNumber().equals(phone.getNumber())) {
                            Log.d(TAG, "UDIFF: Changed phone in contact #" + syncContact.getContactId() + ": " + syncPhone.getNumber() + " -> " + phone.getNumber());

                            if (primaryPhones.get(syncPhone.getNumber()) == syncPhone) {
                                boolean founded = false;
                                outer:
                                for (SyncContact tc : uploadState.getContacts()) {
                                    for (SyncPhone tp : tc.getSyncPhones()) {
                                        if (tp.getNumber().equals(syncPhone.getNumber()) && tp.getPhoneId() != syncPhone.getPhoneId()) {
                                            markAsPrimary(tp, primaryPhones);
                                            founded = true;
                                            break outer;
                                        }
                                    }
                                }

                                if (!founded) {
                                    Log.d(TAG, "UDIFF: Deleted primary phone #" + syncPhone.getNumber());
                                    primaryPhones.remove(syncPhone.getNumber());
                                }
                            }

                            syncPhone.setNumber(phone.getNumber());
                            syncPhone.setImported(false);
                            markAsPrimary(syncPhone, primaryPhones);


                        }
                    }
                }
            } else {
                syncContact = new SyncContact(record.getContactId(), record.getFirstName(), record.getLastName());
                for (Phone phone : record.getPhones()) {
                    SyncPhone syncPhone = new SyncPhone(phone.id, phone.getNumber(), syncContact);
                    syncPhone.setImported(false);
                    markAsPrimary(syncPhone, primaryPhones);
                    syncContact.getSyncPhones().add(syncPhone);
                }
                uploadState.getContacts().add(syncContact);
                Log.d(TAG, "UDIFF: Added contact #" + syncContact.getContactId());
            }
        }
    }

    private void markAsPrimary(SyncPhone phone, HashMap<String, SyncPhone> primaryPhones) {
        phone.setPrimary(true);
        if (primaryPhones.get(phone.getNumber()) != phone) {
            SyncPhone oldPrimaryPhone = primaryPhones.get(phone.getNumber());
            if (oldPrimaryPhone != null) {
                oldPrimaryPhone.setPrimary(false);

                if (oldPrimaryPhone.getContact() != phone.getContact()) {
                    SyncContact oldConcact = oldPrimaryPhone.getContact();
                    SyncContact syncContact = phone.getContact();
                    Log.d(TAG, "UDIFF: Phone primary moved from #" + oldConcact.getContactId() + " " + oldConcact.getFirstName() + " " + oldConcact.getLastName() + " to #" +
                            syncContact.getContactId() + " " + syncContact.getFirstName() + " " + syncContact.getLastName());
                }
            }
            primaryPhones.put(phone.getNumber(), phone);
        }
    }

    private PhoneBookDiff diffPhoneBook(PhoneBookRecord[] original, PhoneBookRecord[] updated) {
        PhoneBookDiff res = new PhoneBookDiff();
        for (PhoneBookRecord record : updated) {
            PhoneBookRecord orig = findRecord(original, record.contactId);
            if (orig != null) {
                if (!isRecordsEquals(orig, record)) {
                    res.getUpdatedContacts().add(new UpdatedRecord(orig, record));
                }
            } else {
                res.getAddedContacts().add(record);
            }
        }
        for (PhoneBookRecord record : original) {
            PhoneBookRecord upd = findRecord(updated, record.contactId);
            if (upd == null) {
                res.getRemovedContacts().add(record);
            }
        }
        return res;
    }

    private boolean isRecordsEquals(PhoneBookRecord a, PhoneBookRecord b) {
        if (!a.getFirstName().equals(b.getFirstName())) {
            return false;
        }
        if (!a.getLastName().equals(b.getLastName())) {
            return false;
        }
        if (a.getPhones().size() != b.getPhones().size()) {
            return false;
        }
        outer:
        for (int i = 0; i < a.getPhones().size(); i++) {
            Phone aphone = a.getPhones().get(i);
            for (int j = 0; j < b.getPhones().size(); j++) {
                Phone bphone = b.getPhones().get(j);
                if (bphone.getId() == aphone.getId()) {
                    if (bphone.getNumber().equals(aphone.getNumber())) {
                        continue outer;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        }

        return true;
    }

    private PhoneBookRecord findRecord(PhoneBookRecord[] records, long id) {
        for (PhoneBookRecord record : records) {
            if (record.getContactId() == id) {
                return record;
            }
        }
        return null;
    }

    private SyncContact findSyncContact(ContactsUploadState uploadState, long id) {
        for (SyncContact contact : uploadState.getContacts()) {
            if (contact.getContactId() == id) {
                return contact;
            }
        }
        return null;
    }

    private SyncPhone findSyncPhone(SyncContact syncContact, long id) {
        for (SyncPhone phone : syncContact.getSyncPhones()) {
            if (phone.getPhoneId() == id) {
                return phone;
            }
        }
        return null;
    }

    private PhoneBookRecord[] loadPhoneBook() {
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

    private class PhoneBookRecord {
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

    private class Phone {
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

    private class UpdatedPhone {
        private long phoneId;
        private String oldPhone;
        private String newPhone;

        private UpdatedPhone(long phoneId, String oldPhone, String newPhone) {
            this.phoneId = phoneId;
            this.oldPhone = oldPhone;
            this.newPhone = newPhone;
        }

        public long getPhoneId() {
            return phoneId;
        }

        public String getOldPhone() {
            return oldPhone;
        }

        public String getNewPhone() {
            return newPhone;
        }
    }

    private class UpdatedRecord {
        private boolean isChangedName;

        private PhoneBookRecord oldRecord;
        private PhoneBookRecord newRecord;
        private ArrayList<Phone> addedPhones = new ArrayList<Phone>();
        private ArrayList<Phone> removedPhones = new ArrayList<Phone>();
        private ArrayList<UpdatedPhone> updatedPhones = new ArrayList<UpdatedPhone>();

        private UpdatedRecord(PhoneBookRecord oldRecord, PhoneBookRecord newRecord) {
            this.oldRecord = oldRecord;
            this.newRecord = newRecord;
            this.isChangedName = !(this.oldRecord.getFirstName().equals(newRecord.getFirstName()) &&
                    this.oldRecord.getLastName().equals(newRecord.getLastName()));
            outer:
            for (Phone oPhone : oldRecord.getPhones()) {
                for (Phone uPhone : newRecord.getPhones()) {
                    if (uPhone.getId() == oPhone.getId()) {
                        if (!uPhone.getNumber().equals(oPhone.getNumber())) {
                            updatedPhones.add(new UpdatedPhone(oPhone.id, oPhone.getNumber(), uPhone.getNumber()));
                        }
                        continue outer;
                    }
                }
                removedPhones.add(oPhone);
            }

            outer:
            for (Phone uPhone : newRecord.getPhones()) {
                for (Phone oPhone : oldRecord.getPhones()) {
                    if (uPhone.getId() == oPhone.getId()) {
                        continue outer;
                    }
                }
                addedPhones.add(uPhone);
            }
        }

        public PhoneBookRecord getOldRecord() {
            return oldRecord;
        }

        public PhoneBookRecord getNewRecord() {
            return newRecord;
        }

        public boolean isChangedName() {
            return isChangedName;
        }

        public ArrayList<Phone> getAddedPhones() {
            return addedPhones;
        }

        public ArrayList<Phone> getRemovedPhones() {
            return removedPhones;
        }

        public ArrayList<UpdatedPhone> getUpdatedPhones() {
            return updatedPhones;
        }
    }

    private class PhoneBookDiff {
        private ArrayList<UpdatedRecord> updatedContacts = new ArrayList<UpdatedRecord>();
        private ArrayList<PhoneBookRecord> removedContacts = new ArrayList<PhoneBookRecord>();
        private ArrayList<PhoneBookRecord> addedContacts = new ArrayList<PhoneBookRecord>();

        public ArrayList<UpdatedRecord> getUpdatedContacts() {
            return updatedContacts;
        }

        public ArrayList<PhoneBookRecord> getRemovedContacts() {
            return removedContacts;
        }

        public ArrayList<PhoneBookRecord> getAddedContacts() {
            return addedContacts;
        }

        public boolean isEmpty() {
            return updatedContacts.isEmpty() && removedContacts.isEmpty() && addedContacts.isEmpty();
        }
    }
}
