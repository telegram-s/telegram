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
import android.os.SystemClock;
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
import org.telegram.android.core.model.LinkType;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.TLLocalAvatarEmpty;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.log.Logger;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.tasks.ProgressInterface;
import org.telegram.android.ui.DataSourceUpdater;
import org.telegram.android.ui.FilterMatcher;
import org.telegram.android.ui.Placeholders;
import org.telegram.android.ui.TextUtil;
import org.telegram.api.TLAbsInputUser;
import org.telegram.api.TLInputUserContact;
import org.telegram.api.requests.TLRequestContactsDeleteContacts;
import org.telegram.tl.TLVector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

/**
 * Author: Korshakov Stepan
 * Created: 30.07.13 11:33
 */
public class ContactsFragment extends StelsFragment implements ContactSourceListener {

    private static final String TAG = "ContactsFragment";

    private FilterMatcher matcher;

    private String settingSortKey;
    private String settingDisplayOrder;

    private DataSourceUpdater dataSourceUpdater;
    private LocalContact[] originalUsers;
    private LocalContact[] filteredUsers;
    private boolean isLoaded;
    // private BaseAdapter contactsAdapter;
    private BaseAdapter localContactsAdapter;
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

        if (sort_order == 1) {
            settingSortKey = ContactsContract.Contacts.SORT_KEY_PRIMARY;
        } else {
            settingSortKey = ContactsContract.Contacts.SORT_KEY_ALTERNATIVE;
        }

        if (display_order == 1) {
            settingDisplayOrder = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY;
        } else {
            settingDisplayOrder = ContactsContract.Contacts.DISPLAY_NAME_ALTERNATIVE;
        }

        originalUsers = new LocalContact[0];
        filteredUsers = new LocalContact[0];
        isLoaded = false;

        contactsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> adapterView, View view, final int i, long l) {
                if (i == 0) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, application.getDynamicConfig().getInviteMessage());
                    startActivity(shareIntent);
                } else {
                    final LocalContact contact = (LocalContact) adapterView.getItemAtPosition(i);
                    Contact[] contacts = application.getEngine().getContactsForLocalId(contact.contactId);
                    if (contacts.length > 0) {
                        getRootController().openUser(contacts[0].getUid());
                    } else {
                        AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_contacts_not_registered_title)
                                .setMessage(getStringSafe(R.string.st_contacts_not_registered_message).replace("{0}", contact.displayName))
                                .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        try {
                                            Cursor pCur = application.getContentResolver().query(
                                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                                    null,
                                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                                    new String[]{contact.contactId + ""}, null);

                                            if (pCur.moveToFirst()) {
                                                String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                                Intent sendSms = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:" + phoneNo));
                                                sendSms.putExtra("address", phoneNo);
                                                sendSms.putExtra("sms_body", application.getDynamicConfig().getInviteMessage());
                                                sendSms.setType("vnd.android-dir/mms-sms");
                                                startActivity(sendSms);
                                            } else {
                                                Context context1 = getActivity();
                                                if (context1 != null) {
                                                    Toast.makeText(context1, R.string.st_error_unable_sms, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        } catch (Exception e) {
                                            Context context1 = getActivity();
                                            if (context1 != null) {
                                                Toast.makeText(context1, R.string.st_error_unable_sms, Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                                }).setNegativeButton(R.string.st_no, null).create();
                        dialog.setCanceledOnTouchOutside(true);
                        dialog.show();

                    }
                }
            }
        });
        contactsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> adapterView, View view, final int i, long l) {
                if (i == 0) {
                    return false;
                }

                new AlertDialog.Builder(getActivity()).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int b) {
                        Cursor c = (Cursor) adapterView.getItemAtPosition(i);
                        long id = c.getLong(c.getColumnIndex(ContactsContract.Contacts._ID));
                        String lookupKey = c.getString(c.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                        final Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);

                        final Contact[] contacts = application.getEngine().getContactsForLocalId(id);
                        if (contacts.length > 0) {
                            runUiTask(new AsyncAction() {
                                @Override
                                public void execute() throws AsyncException {
                                    TLVector<TLAbsInputUser> inputUsers = new TLVector<TLAbsInputUser>();
                                    for (Contact c : contacts) {
                                        inputUsers.add(new TLInputUserContact(c.getUid()));
                                    }
                                    rpc(new TLRequestContactsDeleteContacts(inputUsers));

                                    for (Contact c : contacts) {
                                        User u = application.getEngine().getUser(c.getUid());
                                        u.setLinkType(LinkType.REQUEST);
                                        application.getEngine().getUsersDao().update(u);
                                    }

                                    application.getContentResolver().delete(uri, null, null);
                                    application.getContactsSource().resetState();
                                }
                            });
                        } else {
                            application.getContentResolver().delete(uri, null, null);
                        }
                    }
                }).setNegativeButton("No", null).show();

                return true;
            }
        });

        final Context context = getActivity();
        localContactsAdapter = new BaseAdapter() {

            public View newView(Context context) {
                return View.inflate(context, R.layout.contacts_item, null);
            }

            public void bindView(View view, final Context context, int index) {
                final LocalContact contact = getItem(index);
                ((TextView) view.findViewById(R.id.name)).setText(contact.displayName);
                TextView onlineView = (TextView) view.findViewById(R.id.status);

                if (contact.user != null) {
                    ((FastWebImageView) view.findViewById(R.id.avatar)).setLoadingDrawable(Placeholders.getUserPlaceholder(contact.user.getUid()));
                    if (contact.user.getPhoto() != null && (contact.user.getPhoto() instanceof TLLocalAvatarPhoto)) {
                        TLLocalAvatarPhoto p = (TLLocalAvatarPhoto) contact.user.getPhoto();
                        ((FastWebImageView) view.findViewById(R.id.avatar)).requestTask(new StelsImageTask((TLLocalFileLocation) p.getPreviewLocation()));
                    } else {
                        ((FastWebImageView) view.findViewById(R.id.avatar)).requestTask(null);
                    }

                    int statusValue = getUserState(contact.user.getStatus());
                    if (statusValue < 0) {
                        onlineView.setText(R.string.st_offline);
                        onlineView.setTextColor(getResources().getColor(R.color.st_grey_text));
                    } else if (statusValue == 0) {
                        onlineView.setText(R.string.st_online);
                        onlineView.setTextColor(getResources().getColor(R.color.st_blue_bright));
                    } else {
                        onlineView.setTextColor(getResources().getColor(R.color.st_grey_text));
                        onlineView.setText(TextUtil.formatHumanReadableLastSeen(statusValue, getStringSafe(R.string.st_lang)));
                    }

                    onlineView.setVisibility(View.VISIBLE);
                    view.findViewById(R.id.shareIcon).setVisibility(View.GONE);
                } else {
                    ((FastWebImageView) view.findViewById(R.id.avatar)).setLoadingDrawable(R.drawable.st_user_placeholder);
                    ((FastWebImageView) view.findViewById(R.id.avatar)).requestTask(null);
                    onlineView.setVisibility(View.GONE);
                    view.findViewById(R.id.shareIcon).setVisibility(View.VISIBLE);
                }


            }

            @Override
            public int getCount() {
                return filteredUsers.length;
            }

            @Override
            public LocalContact getItem(int i) {
                return filteredUsers[i];
            }

            @Override
            public long getItemId(int i) {
                return filteredUsers[i].contactId;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                if (view == null) {
                    view = newView(context);
                }
                bindView(view, context, i);
                return view;
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

//        if (application.getContactsSource().isCacheAlive()) {
//            originalUsers = application.getContactsSource().getSortedUsers();
//            doFilter();
//            isLoaded = true;
//        }

        dataSourceUpdater = new DataSourceUpdater(this) {
            @Override
            protected void doUpdate() {
                long start = SystemClock.uptimeMillis();
                ContentResolver cr = application.getContentResolver();
                Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                        new String[]{
                                ContactsContract.Contacts._ID,
                                ContactsContract.Contacts.LOOKUP_KEY,
                                settingDisplayOrder
                        },
                        ContactsContract.Contacts.HAS_PHONE_NUMBER + " > 0 ", null, settingSortKey);

                final LocalContact[] contacts = new LocalContact[cur.getCount()];

                Contact[] netContacts = application.getEngine().getAllContacts();
                HashSet<Integer> ids = new HashSet<Integer>();
                for (Contact c : netContacts) {
                    ids.add(c.getUid());
                }
                application.getEngine().getUsersById(ids.toArray());

                if (cur.moveToFirst()) {
                    for (int i = 0; i < contacts.length; i++) {
                        final long id = cur.getLong(cur.getColumnIndex(ContactsContract.Contacts._ID));
                        String srcName = cur.getString(cur.getColumnIndex(settingDisplayOrder));

                        User relatedContact = null;
                        for (Contact contact : netContacts) {
                            if (contact.getLocalId() == id) {
                                relatedContact = application.getEngine().getUser(contact.getUid());
                                break;
                            }
                        }
                        if (relatedContact != null) {
                            contacts[i] = new LocalContact(id, srcName, relatedContact);
                        } else {
                            contacts[i] = new LocalContact(id, srcName);
                        }
                        cur.moveToNext();
                    }
                }

                Logger.d(TAG, "Contacts loading time in " + (SystemClock.uptimeMillis() - start) + " ms");

                final User[] users = application.getContactsSource().getSortedUsers();
                secureCallback(new Runnable() {
                    @Override
                    public void run() {
                        isLoaded = true;
                        originalUsers = contacts;
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
        ArrayList<LocalContact> preFiltered = new ArrayList<LocalContact>();
        if (application.getUserSettings().showOnlyTelegramContacts()) {
            for (LocalContact u : originalUsers) {
                if (u.user != null) {
                    preFiltered.add(u);
                }
            }
        } else {
            Collections.addAll(preFiltered, originalUsers);
        }
        if (matcher != null && matcher.getQuery().length() > 0) {
            ArrayList<LocalContact> filtered = new ArrayList<LocalContact>();
            for (LocalContact u : preFiltered) {
                if (matcher.isMatched(u.displayName)) {
                    filtered.add(u);
                }
            }
            filteredUsers = filtered.toArray(new LocalContact[0]);
            // applyFilter = true;
        } else {
            filteredUsers = preFiltered.toArray(new LocalContact[0]);
            // applyFilter = false;
        }
//        allAdapter.notifyDataSetChanged();
//        filteredUsers = originalUsers;
//         applyFilter = false;

        localContactsAdapter.notifyDataSetChanged();
        onDataChanged();
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

        if (application.getUserSettings().showOnlyTelegramContacts()) {
            menu.findItem(R.id.allContacts).setVisible(true);
            menu.findItem(R.id.onlyTContacts).setVisible(false);
        } else {
            menu.findItem(R.id.allContacts).setVisible(false);
            menu.findItem(R.id.onlyTContacts).setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.onlyTContacts) {
            application.getUserSettings().setShowOnlyTelegramContacts(true);
            getSherlockActivity().invalidateOptionsMenu();
            doFilter();
            return true;
        } else if (item.getItemId() == R.id.allContacts) {
            application.getUserSettings().setShowOnlyTelegramContacts(false);
            getSherlockActivity().invalidateOptionsMenu();
            doFilter();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onDataChanged() {
        if (localContactsAdapter.getCount() == 0 || !isLoaded) {
            if (application.getContactsSource().getState() == ContactSourceState.SYNCED && isLoaded) {
                loading.setVisibility(View.GONE);
                empty.setVisibility(View.VISIBLE);
            } else {
                loading.setVisibility(View.VISIBLE);
                empty.setVisibility(View.GONE);
            }
            contactsList.setVisibility(View.GONE);
        } else {
            loading.setVisibility(View.GONE);
            empty.setVisibility(View.GONE);
            contactsList.setVisibility(View.VISIBLE);
        }
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

    private class LocalContact {
        public long contactId;
        public String displayName;
        public User user;

        private LocalContact(long contactId, String displayName) {
            this.contactId = contactId;
            this.displayName = displayName;
            this.user = null;
        }

        private LocalContact(long contactId, String displayName, User user) {
            this.contactId = contactId;
            this.displayName = displayName;
            this.user = user;
        }
    }
}
