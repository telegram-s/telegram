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
    private boolean applyFilter;
    private BaseAdapter contactsAdapter;
    private CursorAdapter localContactsAdapter;
    private BaseAdapter allAdapter;
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
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, application.getDynamicConfig().getInviteMessage());
                    startActivity(shareIntent);
                } else {
                    User user = (User) adapterView.getItemAtPosition(i);
                    getRootController().openDialog(PeerType.PEER_USER, user.getUid());
                }
            }
        });

        originalUsers = new User[0];
        filteredUsers = new User[0];
        isLoaded = false;

        final Context context = getActivity();
        contactsAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return filteredUsers.length;
            }

            @Override
            public User getItem(int i) {
                return filteredUsers[i];
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @Override
            public long getItemId(int i) {
                return getItem(i).getUid();
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                if (view == null) {
                    view = View.inflate(context, R.layout.contacts_item, null);
                }

                final User object = getItem(i);

                if (matcher != null) {
                    SpannableString string = new SpannableString(object.getDisplayName());
                    matcher.highlight(context, string);
                    ((TextView) view.findViewById(R.id.name)).setText(string);
                } else {
                    ((TextView) view.findViewById(R.id.name)).setText(object.getDisplayName());
                }

                FastWebImageView imageView = (FastWebImageView) view.findViewById(R.id.avatar);
                imageView.setLoadingDrawable(Placeholders.USER_PLACEHOLDERS[object.getUid() % Placeholders.USER_PLACEHOLDERS.length]);
                if (object.getPhoto() instanceof TLLocalAvatarPhoto) {
                    TLLocalAvatarPhoto userPhoto = (TLLocalAvatarPhoto) object.getPhoto();
                    if (userPhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                        imageView.requestTask(new StelsImageTask((TLLocalFileLocation) userPhoto.getPreviewLocation()));
                    } else {
                        imageView.requestTask(null);
                    }
                } else {
                    imageView.requestTask(null);
                }

                int status = getUserState(object.getStatus());
                if (status < 0) {
                    ((TextView) view.findViewById(R.id.status)).setText(R.string.st_offline);
                    ((TextView) view.findViewById(R.id.status)).setTextColor(context.getResources().getColor(R.color.st_grey_text));
                } else if (status == 0) {
                    ((TextView) view.findViewById(R.id.status)).setText(R.string.st_online);
                    ((TextView) view.findViewById(R.id.status)).setTextColor(context.getResources().getColor(R.color.st_blue_bright));
                } else {
                    ((TextView) view.findViewById(R.id.status)).setTextColor(context.getResources().getColor(R.color.st_grey_text));
                    ((TextView) view.findViewById(R.id.status)).setText(
                            TextUtil.formatHumanReadableLastSeen(status, getStringSafe(R.string.st_lang)));
                }

                return view;
            }
        };

        ContentResolver cr = application.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME
                },
                ContactsContract.Contacts.HAS_PHONE_NUMBER + " > 0 and " + ContactsContract.Contacts.DISPLAY_NAME + " NOTNULL", null, ContactsContract.Contacts.SORT_KEY_ALTERNATIVE);

        localContactsAdapter = new CursorAdapter(context, cur, false) {

            private boolean visible(Cursor cursor) {
                return cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)) != null;
            }

            @Override
            public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
                return View.inflate(context, R.layout.contacts_item_local, null);
            }

            @Override
            public void bindView(View view, final Context context, Cursor cursor) {
                final String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String srcName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                final String name = srcName == null ? "???" : srcName;
                String[] items = name.split(" ");
                String key = items[items.length - 1];

                boolean separatorVisible = false;
                if (cursor.getPosition() == 0) {
                    separatorVisible = true;
                } else {
                    // Prev item
                    cursor.move(-1);
                    String prevName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    cursor.move(1);
                    if (prevName == null) {
                        prevName = "???";
                    }
                    String[] prevItems = prevName.split(" ");
                    String prevKey = prevItems[prevItems.length - 1];

                    if (!prevKey.substring(0, 1).equalsIgnoreCase(key.substring(0, 1))) {
                        separatorVisible = true;
                    }
                }
                String res = "";
                for (int i = 0; i < items.length; i++) {
                    if (i != 0) {
                        res += " ";
                    }
                    if (i == items.length - 1) {
                        res += "<b>" + items[i] + "</b>";
                    } else {
                        res += items[i];
                    }
                }
                ((TextView) view.findViewById(R.id.title)).setText(Html.fromHtml(res));

                view.findViewById(R.id.title).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Contact contact = application.getEngine().getContactUid(Integer.parseInt(id));
                        if (contact != null) {
                            getRootController().openUser(contact.getUid());
                        } else {
                            new AlertDialog.Builder(context).setTitle(R.string.st_contacts_not_registered_title)
                                    .setMessage(getStringSafe(R.string.st_contacts_not_registered_message).replace("{0}", name))
                                    .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            try {
                                                Cursor pCur = application.getContentResolver().query(
                                                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                                        null,
                                                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                                        new String[]{id}, null);

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
                                    }).setNegativeButton(R.string.st_no, null).show();
                        }
                    }
                });
                if (separatorVisible) {
                    ((TextView) view.findViewById(R.id.separator)).setText(key.substring(0, 1).toUpperCase());
                    view.findViewById(R.id.separatorContainer).setVisibility(View.VISIBLE);
                } else {
                    view.findViewById(R.id.separatorContainer).setVisibility(View.GONE);
                }
            }
        };

        allAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                if (applyFilter) {
                    return contactsAdapter.getCount();
                } else {
                    return contactsAdapter.getCount() + localContactsAdapter.getCount();
                }
            }

            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @Override
            public boolean isEnabled(int position) {
                return position < contactsAdapter.getCount();
            }

            @Override
            public Object getItem(int i) {
                if (i < contactsAdapter.getCount()) {
                    return contactsAdapter.getItem(i);
                } else {
                    return localContactsAdapter.getItem(i - contactsAdapter.getCount());
                }
            }

            @Override
            public long getItemId(int i) {
                if (i < contactsAdapter.getCount()) {
                    return contactsAdapter.getItemId(i);
                } else {
                    return localContactsAdapter.getItemId(i - contactsAdapter.getCount());
                }
            }

            @Override
            public int getItemViewType(int position) {
                if (position < contactsAdapter.getCount()) {
                    return contactsAdapter.getItemViewType(position);
                } else {
                    return localContactsAdapter.getItemViewType(position - contactsAdapter.getCount())
                            + contactsAdapter.getViewTypeCount();
                }
            }

            @Override
            public int getViewTypeCount() {
                return localContactsAdapter.getViewTypeCount() + contactsAdapter.getViewTypeCount();
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                if (i < contactsAdapter.getCount()) {
                    return contactsAdapter.getView(i, view, viewGroup);
                } else {
                    return localContactsAdapter.getView(i - contactsAdapter.getCount(), view, viewGroup);
                }
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
        contactsList.setAdapter(allAdapter);

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

        return res;
    }

    private void doFilter() {
        if (matcher != null && matcher.getQuery().length() > 0) {
            ArrayList<User> filtered = new ArrayList<User>();
            for (User u : originalUsers) {
                if (matcher.isMatched(u.getDisplayName())) {
                    filtered.add(u);
                }
            }
            filteredUsers = filtered.toArray(new User[0]);
            applyFilter = true;
        } else {
            filteredUsers = originalUsers;
            applyFilter = false;
        }
        allAdapter.notifyDataSetChanged();
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
        searchView.setQueryHint(getStringSafe(R.string.st_contacts_filter));

        ((ImageView) searchView.findViewById(R.id.abs__search_button)).setImageResource(R.drawable.st_bar_ic_search);

        AutoCompleteTextView searchText = (AutoCompleteTextView) searchView.findViewById(R.id.abs__search_src_text);
        searchText.setHintTextColor(0xccB8B8B8);
        searchText.setTextColor(0xff010101);

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
        if (allAdapter.getCount() == 0 || !isLoaded) {
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
}
