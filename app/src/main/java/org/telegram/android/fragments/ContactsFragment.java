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
import org.telegram.android.core.model.update.TLLocalAffectedHistory;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.tasks.ProgressInterface;
import org.telegram.android.ui.FilterMatcher;
import org.telegram.android.ui.Placeholders;
import org.telegram.android.ui.TextUtil;
import org.telegram.api.TLAbsInputUser;
import org.telegram.api.TLInputPeerContact;
import org.telegram.api.TLInputUserContact;
import org.telegram.api.messages.TLAffectedHistory;
import org.telegram.api.requests.TLRequestContactsBlock;
import org.telegram.api.requests.TLRequestContactsDeleteContacts;
import org.telegram.api.requests.TLRequestMessagesDeleteHistory;
import org.telegram.tl.TLVector;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Author: Korshakov Stepan
 * Created: 30.07.13 11:33
 */
public class ContactsFragment extends StelsFragment implements ContactSourceListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private FilterMatcher matcher;

    private boolean isLoaded;
    private ContactsSource.LocalContact[] originalUsers;
    private ContactsSource.LocalContact[] filteredUsers;
    private String[] headers;
    private Integer[] headerStart;
    private ContactsAdapter сontactsAdapter;
    private StickyListHeadersListView contactsList;
    private View empty;
    private View loading;
    private View mainContainer;

    private int selectedIndex = -1;
    private int selectedTop = 0;

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

        if (savedInstanceState != null) {
            selectedTop = savedInstanceState.getInt("selectedTop", 0);
            selectedIndex = savedInstanceState.getInt("selectedIndex", -1);
        }

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

        if (savedInstanceState != null) {
            selectedTop = savedInstanceState.getInt("selectedTop", 0);
            selectedIndex = savedInstanceState.getInt("selectedIndex", -1);
        }

        final Context context = getActivity();

        View res = inflater.inflate(R.layout.contacts_list, container, false);

        loading = res.findViewById(R.id.loading);
        empty = res.findViewById(R.id.empty);
        mainContainer = res.findViewById(R.id.mainContainer);

        contactsList = (StickyListHeadersListView) res.findViewById(R.id.contactsList);
        contactsList.setOnItemClickListener(this);
        contactsList.setOnItemLongClickListener(this);
        View share = inflater.inflate(R.layout.contacts_item_share, null);
        contactsList.addHeaderView(share);

        originalUsers = new ContactsSource.LocalContact[0];
        filteredUsers = new ContactsSource.LocalContact[0];
        isLoaded = false;

        сontactsAdapter = new ContactsAdapter();
        contactsList.setAdapter(сontactsAdapter);
        reloadData();

        if (selectedIndex >= 0) {
            contactsList.setSelectionFromTop(selectedIndex, selectedTop);
        }

        return res;
    }

    @Override
    public void onResume() {
        super.onResume();

        application.getContactsSource().registerListener(this);
        onContactsDataChanged();

        updateHeaderPadding();
    }

    private void saveListPosition() {
        if (contactsList != null) {
            int index = contactsList.getFirstVisiblePosition();
            View v = contactsList.getChildAt(0);
            if (v != null) {
                selectedTop = v.getTop();
                selectedIndex = index;
            } else {
                selectedTop = 0;
                selectedIndex = -1;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        application.getContactsSource().unregisterListener(this);
        saveListPosition();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_contacts_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);

        if (!isLoaded) {
            return;
        }

        inflater.inflate(R.menu.contacts_menu, menu);

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
                reloadData();
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
                reloadData();
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
            reloadData();
            return true;
        } else if (item.getItemId() == R.id.allContacts) {
            application.getUserSettings().setShowOnlyTelegramContacts(false);
            getSherlockActivity().invalidateOptionsMenu();
            reloadData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void reloadData() {
        boolean isContactsLoaded = application.getContactsSource().getContacts() != null;

        if (!isContactsLoaded) {
            originalUsers = new ContactsSource.LocalContact[0];
            filteredUsers = new ContactsSource.LocalContact[0];

            loading.setVisibility(View.VISIBLE);
            empty.setVisibility(View.GONE);
            contactsList.setVisibility(View.GONE);

            return;
        } else {
            loading.setVisibility(View.GONE);
        }

        if (application.getUserSettings().showOnlyTelegramContacts()) {
            originalUsers = application.getContactsSource().getTelegramContacts();
        } else {
            originalUsers = application.getContactsSource().getContacts();
        }

        if (matcher != null && matcher.getQuery().length() > 0) {
            ArrayList<ContactsSource.LocalContact> filtered = new ArrayList<ContactsSource.LocalContact>();
            for (ContactsSource.LocalContact u : originalUsers) {
                if (matcher.isMatched(u.displayName)) {
                    filtered.add(u);
                }
            }
            filteredUsers = filtered.toArray(new ContactsSource.LocalContact[filtered.size()]);
        } else {
            filteredUsers = originalUsers;
        }

        ArrayList<String> nHeaders = new ArrayList<String>();
        ArrayList<Integer> nHeaderStarts = new ArrayList<Integer>();

        char header = '\0';

        for (int i = 0; i < filteredUsers.length; i++) {
            if (filteredUsers[i].header != header) {
                nHeaders.add("" + filteredUsers[i].header);
                nHeaderStarts.add(i);
            }
        }

        headers = nHeaders.toArray(new String[0]);
        headerStart = nHeaderStarts.toArray(new Integer[0]);

        сontactsAdapter.notifyDataSetChanged();

        if (filteredUsers.length == 0) {
            empty.setVisibility(View.VISIBLE);
            contactsList.setVisibility(View.GONE);
        } else {
            empty.setVisibility(View.GONE);
            contactsList.setVisibility(View.VISIBLE);
        }

        if (!isLoaded) {
            isLoaded = true;
            getSherlockActivity().invalidateOptionsMenu();
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveListPosition();
        outState.putInt("selectedIndex", selectedIndex);
        outState.putInt("selectedTop", selectedTop);
    }

    @Override
    public void onContactsDataChanged() {
        reloadData();
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

        final ContactsSource.LocalContact contact = (ContactsSource.LocalContact) adapterView.getItemAtPosition(i);

        AlertDialog contextMenu = new AlertDialog.Builder(getActivity())
                .setTitle(contact.displayName)
                .setItems(new CharSequence[]{
                        "View in People", "Delete contact", "Share contact", "Block contact", "Block and delete contact"
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == 1) {
                            AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                                    .setMessage(getStringSafe(R.string.st_contacts_delete).replace("{0}", contact.displayName))
                                    .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int b) {
                                            final Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, contact.lookupKey);
                                            final Contact[] contacts = application.getEngine().getContactsForLocalId(contact.contactId);
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
                                    }).setNegativeButton(R.string.st_no, null).create();
                            alertDialog.setCanceledOnTouchOutside(true);
                            alertDialog.show();
                        } else if (i == 0) {
                            startActivity(new Intent(Intent.ACTION_VIEW)
                                    .setData(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.contactId + "")));
                        } else if (i == 2) {
                            getRootController().shareContact(contact.user.getUid());
                        } else if (i == 3) {
                            runUiTask(new AsyncAction() {
                                @Override
                                public void execute() throws AsyncException {
                                    rpc(new TLRequestContactsBlock(new TLInputUserContact(contact.user.getUid())));
                                }
                            });
                        } else if (i == 4) {
//                            runUiTask(new AsyncAction() {
//                                @Override
//                                public void execute() throws AsyncException {
//                                    TLAffectedHistory tlAffectedHistory = rpc(new TLRequestMessagesDeleteHistory(new TLInputPeerContact(contact.user.getUid()), 0));
//                                    application.getUpdateProcessor().onMessage(new TLLocalAffectedHistory(tlAffectedHistory));
//                                    while (tlAffectedHistory.getOffset() > 0) {
//                                        tlAffectedHistory = rpc(new TLRequestMessagesDeleteHistory(new TLInputPeerContact(contact.user.getUid()), tlAffectedHistory.getOffset()));
//                                        application.getUpdateProcessor().onMessage(new TLLocalAffectedHistory(tlAffectedHistory));
//                                    }
//                                    rpc(new TLRequestContactsBlock(new TLInputUserContact(contact.user.getUid())));
//                                }
//                            });
                        }
                    }
                }).create();
        contextMenu.setCanceledOnTouchOutside(true);
        contextMenu.show();


        return true;
    }

    private class ContactsAdapter extends BaseAdapter implements StickyListHeadersAdapter, SectionIndexer {
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
                view = newView(getActivity());
            }
            bindView(view, getActivity(), i);
            return view;
        }

        @Override
        public View getHeaderView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = View.inflate(getActivity(), R.layout.contact_item_header, null);
            }
            ((TextView) view.findViewById(R.id.header)).setText(getItem(i).header + "");
            return view;
        }

        @Override
        public long getHeaderId(int i) {
            return getItem(i).header;
        }

        @Override
        public Object[] getSections() {
            return headers;
        }

        @Override
        public int getPositionForSection(int i) {
            return headerStart[i];
        }

        @Override
        public int getSectionForPosition(int ind) {
            for (int i = 0; i < headerStart.length; i++) {
                if (headerStart[i] <= ind) {
                    return i;
                }
            }

            return headerStart.length - 1;
        }
    }
}
