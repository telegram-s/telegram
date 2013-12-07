package org.telegram.android.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
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
import org.telegram.android.core.ContactsSource;
import org.telegram.android.core.model.Contact;
import org.telegram.android.core.model.LinkType;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.tasks.ProgressInterface;
import org.telegram.android.ui.FilterMatcher;
import org.telegram.android.ui.Placeholders;
import org.telegram.android.ui.TextUtil;
import org.telegram.api.TLAbsInputUser;
import org.telegram.api.TLInputUserContact;
import org.telegram.api.requests.TLRequestContactsDeleteContacts;
import org.telegram.tl.TLVector;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Author: Korshakov Stepan
 * Created: 30.07.13 11:33
 */
public class ContactsFragment extends StelsFragment implements ContactSourceListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final String TAG = "ContactsFragment";

    private FilterMatcher matcher;

    private ContactsSource.LocalContact[] originalUsers;
    private ContactsSource.LocalContact[] filteredUsers;
    private boolean isLoaded;
    private BaseAdapter сontactsAdapter;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final Context context = getActivity();

        View res = inflater.inflate(R.layout.contacts_list, container, false);

        loading = res.findViewById(R.id.loading);
        empty = res.findViewById(R.id.empty);
        mainContainer = res.findViewById(R.id.mainContainer);

        contactsList = (ListView) res.findViewById(R.id.contactsList);
        contactsList.setOnItemClickListener(this);
        contactsList.setOnItemLongClickListener(this);
        View share = inflater.inflate(R.layout.contacts_item_share, null);
        contactsList.addHeaderView(share);

        originalUsers = new ContactsSource.LocalContact[0];
        filteredUsers = new ContactsSource.LocalContact[0];
        isLoaded = false;

        сontactsAdapter = new BaseAdapter() {

            public View newView(Context context) {
                return View.inflate(context, R.layout.contacts_item, null);
            }

            public void bindView(View view, final Context context, int index) {
                final ContactsSource.LocalContact contact = getItem(index);

                if (matcher != null) {
                    SpannableString spannableString = new SpannableString(contact.displayName);
                    matcher.highlight(context, spannableString);
                    ((TextView) view.findViewById(R.id.name)).setText(spannableString);
                } else {
                    ((TextView) view.findViewById(R.id.name)).setText(contact.displayName);
                }

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
                        onlineView.setTextColor(context.getResources().getColor(R.color.st_grey_text));
                    } else if (statusValue == 0) {
                        onlineView.setText(R.string.st_online);
                        onlineView.setTextColor(context.getResources().getColor(R.color.st_blue_bright));
                    } else {
                        onlineView.setTextColor(context.getResources().getColor(R.color.st_grey_text));
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
            public ContactsSource.LocalContact getItem(int i) {
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
        contactsList.setAdapter(сontactsAdapter);

        onDataChanged();
        return res;
    }


    private void doFilter() {
        ArrayList<ContactsSource.LocalContact> preFiltered = new ArrayList<ContactsSource.LocalContact>();
        if (application.getUserSettings().showOnlyTelegramContacts()) {
            for (ContactsSource.LocalContact u : originalUsers) {
                if (u.user != null) {
                    preFiltered.add(u);
                }
            }
        } else {
            Collections.addAll(preFiltered, originalUsers);
        }
        if (matcher != null && matcher.getQuery().length() > 0) {
            ArrayList<ContactsSource.LocalContact> filtered = new ArrayList<ContactsSource.LocalContact>();
            for (ContactsSource.LocalContact u : preFiltered) {
                if (matcher.isMatched(u.displayName)) {
                    filtered.add(u);
                }
            }
            filteredUsers = filtered.toArray(new ContactsSource.LocalContact[0]);
            // applyFilter = true;
        } else {
            filteredUsers = preFiltered.toArray(new ContactsSource.LocalContact[0]);
            // applyFilter = false;
        }

        сontactsAdapter.notifyDataSetChanged();
        onDataChanged();
    }

    @Override
    public void onResume() {
        super.onResume();

        application.getContactsSource().registerListener(this);
        onContactsDataChanged();

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

        searchItem.setVisible(isLoaded);

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

        if (isLoaded) {
            menu.findItem(R.id.allContacts).setVisible(false);
            menu.findItem(R.id.onlyTContacts).setVisible(false);
        } else {
            if (application.getUserSettings().showOnlyTelegramContacts()) {
                menu.findItem(R.id.allContacts).setVisible(true);
                menu.findItem(R.id.onlyTContacts).setVisible(false);
            } else {
                menu.findItem(R.id.allContacts).setVisible(false);
                menu.findItem(R.id.onlyTContacts).setVisible(true);
            }
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
        if (сontactsAdapter.getCount() == 0 || !isLoaded) {
            if (isLoaded) {
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
        loading = null;
        empty = null;
        contactsList = null;
    }

    @Override
    public void onContactsDataChanged() {
        ContactsSource.LocalContact[] nContacts = application.getContactsSource().getContacts();
        if (nContacts != null) {
            originalUsers = nContacts;
            if (!isLoaded) {
                isLoaded = true;
                getSherlockActivity().invalidateOptionsMenu();
            }
        } else {
            originalUsers = new ContactsSource.LocalContact[0];
        }
        doFilter();
        onDataChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (i == 0) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, application.getDynamicConfig().getInviteMessage());
            startActivity(shareIntent);
        } else {
            final ContactsSource.LocalContact contact = (ContactsSource.LocalContact) adapterView.getItemAtPosition(i);
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
                        }
                    });
                } else {
                    application.getContentResolver().delete(uri, null, null);
                }
            }
        }).setNegativeButton("No", null).show();

        return true;
    }
}
