package org.telegram.android.core.engines;

import com.j256.ormlite.stmt.DeleteBuilder;
import org.telegram.android.core.model.Contact;
import org.telegram.dao.ContactDao;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by ex3ndr on 14.01.14.
 */
public class ContactsDatabase {
    private ContactDao contactDao;
    private ArrayList<Contact> cache = null;

    public ContactsDatabase(ModelEngine engine) {
        contactDao = engine.getDaoSession().getContactDao();
    }

    public Contact[] getContactsForLocalId(long localId) {
        ArrayList<Contact> contacts = new ArrayList<Contact>();
        for (Contact cached : readAllContacts()) {
            if (cached.getLocalId() == localId) {
                contacts.add(cached);
            }
        }

        return contacts.toArray(new Contact[contacts.size()]);
    }

    public int[] getUidsForLocalId(long localId) {
        ArrayList<Contact> contacts = new ArrayList<Contact>();
        for (Contact cached : readAllContacts()) {
            if (cached.getLocalId() == localId) {
                contacts.add(cached);
            }
        }

        int[] res = new int[contacts.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = contacts.get(i).getUid();
        }
        return res;
    }

    public long[] getContactsForUid(int uid) {
        ArrayList<Contact> contacts = new ArrayList<Contact>();
        for (Contact cached : readAllContacts()) {
            if (cached.getUid() == uid) {
                contacts.add(cached);
            }
        }

        long[] res = new long[contacts.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = contacts.get(i).getLocalId();
        }
        return res;
    }

    public synchronized Contact[] readAllContacts() {
        if (cache == null) {
            ArrayList<Contact> res = new ArrayList<Contact>();
            for (org.telegram.dao.Contact contact : contactDao.queryBuilder().list()) {
                res.add(new Contact(contact.getUid(), contact.getLocalId()));
            }
            cache = res;
        }
        return cache.toArray(new Contact[0]);
    }

    public synchronized void deleteContactsForLocalId(long localId) {
        contactDao.queryBuilder()
                .where(ContactDao.Properties.LocalId.eq(localId))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
        for (Contact cached : readAllContacts()) {
            if (cached.getLocalId() == localId) {
                cache.remove(cached);
            }
        }
    }

    public synchronized void writeContacts(Contact[] contacts) {
        Contact[] saved = readAllContacts();
        ArrayList<org.telegram.dao.Contact> toAdd = new ArrayList<org.telegram.dao.Contact>();
        outer:
        for (Contact c : contacts) {
            for (Contact s : saved) {
                if (c.getUid() == s.getUid() && c.getLocalId() == s.getLocalId()) {
                    continue outer;
                }
            }
            toAdd.add(new org.telegram.dao.Contact(null, c.getUid(), c.getLocalId()));
            cache.add(c);
        }

        if (toAdd.size() > 0) {
            contactDao.insertInTx(toAdd, true);
        }
    }
}
