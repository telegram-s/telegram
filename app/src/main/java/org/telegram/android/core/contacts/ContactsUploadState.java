package org.telegram.android.core.contacts;

import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.TLLocalContext;
import org.telegram.android.core.model.phone.TLImportedPhone;
import org.telegram.android.core.model.phone.TLSyncContact;
import org.telegram.android.core.model.phone.TLSyncPhone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ex3ndr on 27.01.14.
 */
public class ContactsUploadState {

    private StatePersistence persistence;

    private ArrayList<SyncContact> contacts = new ArrayList<SyncContact>();
    private HashMap<String, Integer> importedPhones = new HashMap<String, Integer>();

    public ContactsUploadState(TelegramApplication application) {
        persistence = new StatePersistence(application, "contacts.bin", TLLocalContext.getInstance());
        for (TLSyncContact tlc : persistence.getObj().getContacts()) {
            SyncContact contact = new SyncContact(tlc.getContactId(), tlc.getFirstName(), tlc.getLastName());
            for (TLSyncPhone tlp : tlc.getPhones()) {
                SyncPhone syncPhone = new SyncPhone(tlp.getPhoneId(), tlp.getNumber(), contact);
                syncPhone.setPrimary(tlp.isPrimary());
                syncPhone.setImported(tlp.isImported());
                contact.getSyncPhones().add(syncPhone);
            }
            contacts.add(contact);
        }

        for (TLImportedPhone tlp : persistence.getObj().getImportedPhones()) {
            importedPhones.put(tlp.getPhone(), tlp.getUid());
        }
    }

    public void write() {
        persistence.getObj().getContacts().clear();
        persistence.getObj().getImportedPhones().clear();
        for (SyncContact contact : contacts) {
            ArrayList<TLSyncPhone> phones = new ArrayList<TLSyncPhone>();
            for (SyncPhone phone : contact.getSyncPhones()) {
                phones.add(new TLSyncPhone(phone.getPhoneId(), phone.getNumber(), phone.isPrimary(), phone.isImported()));
            }
            persistence.getObj().getContacts().add(new TLSyncContact(contact.getContactId(), contact.getFirstName(), contact.getLastName(), phones));
        }

        for (Map.Entry<String, Integer> keys : importedPhones.entrySet()) {
            persistence.getObj().getImportedPhones().add(new TLImportedPhone(keys.getKey(), keys.getValue()));
        }
        persistence.write();
    }

    public ArrayList<SyncContact> getContacts() {
        return contacts;
    }

    public List<SyncPhone> buildImportPhones() {
        ArrayList<SyncPhone> res = new ArrayList<SyncPhone>();
        for (SyncContact contact : contacts) {
            for (SyncPhone phone : contact.getSyncPhones()) {
                if (!phone.isImported() && phone.isPrimary()) {
                    res.add(phone);
                }
            }
        }
        return res;
    }

    public HashMap<String, Integer> getImportedPhones() {
        return importedPhones;
    }
}
