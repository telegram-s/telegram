package org.telegram.android.core.contacts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ex3ndr on 27.01.14.
 */
public class ContactsUploadState {
    private ArrayList<SyncContact> contacts = new ArrayList<SyncContact>();
    private HashMap<String, Integer> importedPhones = new HashMap<String, Integer>();

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
