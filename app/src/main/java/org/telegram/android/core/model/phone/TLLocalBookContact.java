package org.telegram.android.core.model.phone;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;
import org.telegram.tl.TLVector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created by ex3ndr on 07.12.13.
 */
public class TLLocalBookContact extends TLObject {

    public static final int CLASS_ID = 0xbc6bd10d;

    private long contactId;
    private String firstName;
    private String lastName;
    private TLVector<TLLocalBookPhone> phones;

    public TLLocalBookContact(long contactId, String firstName, String lastName) {
        this.contactId = contactId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phones = new TLVector<TLLocalBookPhone>();
    }

    public TLLocalBookContact() {
    }

    public long getContactId() {
        return contactId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public TLVector<TLLocalBookPhone> getPhones() {
        return phones;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeLong(contactId, stream);
        writeTLString(firstName, stream);
        writeTLString(lastName, stream);
        writeTLVector(phones, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        contactId = readLong(stream);
        firstName = readTLString(stream);
        lastName = readTLString(stream);
        phones = readTLVector(stream, context);
    }
}
