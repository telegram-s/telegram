package org.telegram.android.fragments;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.Html;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.extradea.framework.images.ui.FastWebImageView;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;
import org.telegram.android.core.ContactSourceListener;
import org.telegram.android.core.ContactSourceState;
import org.telegram.android.core.model.Contact;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.tasks.ProgressInterface;
import org.telegram.android.ui.DataSourceUpdater;
import org.telegram.android.ui.FilterMatcher;
import org.telegram.android.ui.Placeholders;
import org.telegram.android.ui.TextUtil;

import java.util.ArrayList;

/**
 * Author: Korshakov Stepan
 * Created: 30.07.13 11:33
 */
public class ContactsFragment extends StelsFragment implements ContactSourceListener {

    private FilterMatcher matcher;

    private DataSourceUpdater dataSourceUpdater;
    private User[] originalUsers;
    private User[] filteredUsers;
    private boolean isLoaded;
    // private BaseAdapter contactsAdapter;
    private CursorAdapter localContactsAdapter;
    private ListView contactsList;
    private View empty;
    private View loading;
    private View mainContainer;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateHeaderPadding();
    }

    private void updateHeaderPadding() {
        if (mainContainer == null) {
            return;
        }
        mainContainer.setPadding(0, getBarHeight(), 0, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.contacts_list, container, false);

        loading = res.findViewById(R.id.loading);
        empty = res.findViewById(R.id.empty);
        contactsList = (ListView) res.findViewById(R.id.contactsList);
        mainContainer = res.findViewById(R.id.mainContainer);

        updateHeaderPadding();

        setDefaultProgressInterface(new ProgressInterface() {
            @Override
            public void showContent() {
                contactsList.setVisibility(View.VISIBLE);
            }

            @Override
            public void hideContent() {
                contactsList.setVisibility(View.GONE);
            }

            @Override
            public void showProgress() {
                loading.setVisibility(View.VISIBLE);
            }

            @Override
            public void hideProgress() {
                loading.setVisibility(View.GONE);
            }
        });

        contactsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> adapterView, View view, final int i, long l) {
                if (i == 0) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, application.getDynamicConfig().getInviteMessage());
                    startActivity(shareIntent);
                } else {
                    Cursor c = (Cursor) adapterView.getItemAtPosition(i);
                    long id = c.getLong(c.getColumnIndex(ContactsContract.Contacts._ID));
//                    new AlertDialog.Builder(getActivity()).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int b) {
//                            Cursor c = (Cursor) adapterView.getItemAtPosition(i);
//                            String lookupKey = c.getString(c.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
//                            Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
//                            application.getContentResolver().delete(uri, null, null);
//                        }
//                    }).setNegativeButton("No", null).show();

                    Contact[] contacts = application.getEngine().getContactsForLocalId(id);
                    if (contacts.length > 0) {
                        getRootController().openUser(contacts[0].getUid());
                    }


//                    User user = (User) adapterView.getItemAtPosition(i);
//                    getRootController().openDialog(PeerType.PEER_USER, user.getUid());
                }
            }
        });

        originalUsers = new User[0];
        filteredUsers = new User[0];
        isLoaded = false;

        int sort_order = 1;
        int display_order = 1;
        try {
            sort_order = Settings.System.getInt(application.getContentResolver(), "android.contacts.SORT_ORDER");
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        try {
            display_order = Settings.System.getInt(application.getContentResolver(), "android.contacts.DISPLAY_ORDER");
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        final String sortKey;
        if (sort_order == 1) {
            sortKey = ContactsContract.Contacts.SORT_KEY_PRIMARY;
        } else {
            sortKey = ContactsContract.Contacts.SORT_KEY_ALTERNATIVE;
        }

        final String displayName;
        if (display_order == 1) {
            displayName = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY;
        } else {
            displayName = ContactsContract.Contacts.DISPLAY_NAME_ALTERNATIVE;
        }

        ContentResolver cr = application.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.LOOKUP_KEY,
                        displayName
                },
                ContactsContract.Contacts.HAS_PHONE_NUMBER + " > 0 ", null, sortKey);

        localContactsAdapter = new CursorAdapter(getActivity(), cur, false) {

            @Override
            public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
                return View.inflate(context, R.layout.contacts_item, null);
            }

            @Override
            public void bindView(View view, final Context context, Cursor cursor) {
                final String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String srcName = cursor.getString(cursor.getColumnIndex(displayName));

                ((TextView) view.findViewById(R.id.name)).setText(srcName);
            }
        };

        View share = inflater.inflate(R.layout.contacts_item_share, null);
//        share.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent shareIntent = new Intent(Intent.ACTION_SEND);
//                shareIntent.setType("text/plain");
//                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, application.getDynamicConfig().getInviteMessage());
//                startActivity(shareIntent);
//            }
//        });
        contactsList.addHeaderView(share);
        contactsList.setAdapter(localContactsAdapter);

        if (application.getContactsSource().isCacheAlive()) {
            originalUsers = application.getContactsSource().getSortedUsers();
            doFilter();
            isLoaded = true;
        }

        dataSourceUpdater = new DataSourceUpdater(this) {
            @Override
            protected void doUpdate() {
                final User[] users = application.getContactsSource().getSortedUsers();
                secureCallback(new Runnable() {
                    @Override
                    public void run() {
                        isLoaded = true;
                        originalUsers = users;
                        doFilter();
                    }
                });
            }
        };

        onDataChanged();

        loading.setVisibility(View.GONE);
        empty.setVisibility(View.GONE);
        contactsList.setVisibility(View.VISIBLE);

        return res;
    }

    private void doFilter() {
//        if (matcher != null && matcher.getQuery().length() > 0) {
//            ArrayList<User> filtered = new ArrayList<User>();
//            for (User u : originalUsers) {
//                if (matcher.isMatched(u.getDisplayName())) {
//                    filtered.add(u);
//                }
//            }
//            filteredUsers = filtered.toArray(new User[0]);
//            applyFilter = true;
//        } else {
//            filteredUsers = originalUsers;
//            applyFilter = false;
//        }
//        allAdapter.notifyDataSetChanged();
//        onDataChanged();
    }

    @Override
    public void onResume() {
        super.onResume();

        application.getContactsSource().registerListener(this);
        dataSourceUpdater.invalidate();

        updateHeaderPadding();
    }

    @Override
    public void onPause() {
        super.onPause();
        application.getContactsSource().unregisterListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.contacts_menu, menu);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_contacts_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);


        MenuItem searchItem = menu.findItem(R.id.searchMenu);
        com.actionbarsherlock.widget.SearchView searchView = (com.actionbarsherlock.widget.SearchView) searchItem.getActionView();
        // searchView.setQueryHint(getStringSafe(R.string.st_contacts_filter));
        searchView.setQueryHint("");

        ((ImageView) searchView.findViewById(R.id.abs__search_button)).setImageResource(R.drawable.st_bar_ic_search);

        searchView.setOnSuggestionListener(new com.actionbarsherlock.widget.SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                return false;
            }
        });

        searchView.setOnQueryTextListener(new com.actionbarsherlock.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                matcher = new FilterMatcher(newText.trim().toLowerCase());
                doFilter();
                return true;
            }
        });

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                matcher = null;
                doFilter();
                return true;
            }
        });
    }

    private void onDataChanged() {
//        if (allAdapter.getCount() == 0 || !isLoaded) {
//            if (application.getContactsSource().getState() == ContactSourceState.SYNCED && isLoaded) {
//                loading.setVisibility(View.GONE);
//                empty.setVisibility(View.VISIBLE);
//            } else {
//                loading.setVisibility(View.VISIBLE);
//                empty.setVisibility(View.GONE);
//            }
//            contactsList.setVisibility(View.GONE);
//        } else {
//            loading.setVisibility(View.GONE);
//            empty.setVisibility(View.GONE);
//            contactsList.setVisibility(View.VISIBLE);
//        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        matcher = null;
        if (dataSourceUpdater != null) {
            dataSourceUpdater.onDetach();
        }
        loading = null;
        empty = null;
        contactsList = null;
    }

    @Override
    public void onContactsStateChanged() {
        dataSourceUpdater.invalidate();
    }

    @Override
    public void onContactsDataChanged() {
        dataSourceUpdater.invalidate();
    }
}
