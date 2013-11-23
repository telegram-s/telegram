package org.telegram.android.kernel.compat.v4;

import org.telegram.android.kernel.compat.v1.TLObjectCompat;

import java.io.Serializable;

/**
 * Created by ex3ndr on 23.11.13.
 */
public class CompatTLLocalContact extends TLObjectCompat implements Serializable {
    protected String phoneNumber;
    protected String firstName;
    protected String lastName;
    protected int userId;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public int getUserId() {
        return userId;
    }
}
