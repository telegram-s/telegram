package org.telegram.android.adapters;

import android.widget.BaseAdapter;
import org.telegram.android.core.ContactsSource;
import org.telegram.android.ui.FilterMatcher;

/**
 * Created by ex3ndr on 08.12.13.
 */
public abstract class BaseContactAdapter extends BaseAdapter {

    private FilterMatcher matcher = null;
    private ContactsSource.LocalContact[] contacts = new ContactsSource.LocalContact[0];
    private ContactsSource.LocalContact[] filteredContacts = new ContactsSource.LocalContact[0];

    public BaseContactAdapter() {

    }

    @Override
    public int getCount() {
        return filteredContacts.length;
    }

    @Override
    public ContactsSource.LocalContact getItem(int i) {
        return filteredContacts[i];
    }

    @Override
    public long getItemId(int i) {
        return getItem(i).contactId;
    }
}
