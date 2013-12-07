package org.telegram.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.extradea.framework.images.ui.FastWebImageView;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;
import org.telegram.android.core.ContactSourceListener;
import org.telegram.android.core.ContactsSource;
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

import java.util.ArrayList;

/**
 * Author: Korshakov Stepan
 * Created: 10.08.13 19:15
 */
public class PickUserFragment extends StelsFragment implements ContactSourceListener {
    private ListView contactsList;
    private BaseAdapter usersAdapter;
    private FilterMatcher matcher;

    private boolean isLoaded;
    private ContactsSource.LocalContact[] originalUsers;
    private ContactsSource.LocalContact[] filteredUsers;

    private View empty;
    private View progress;
    private View mainContainer;

    public PickUserFragment() {

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getSherlockActivity().invalidateOptionsMenu();
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
        if (savedInstanceState == null) {
            setResult(Activity.RESULT_CANCELED, null);
        }

        final Context context = getActivity();

        View res = inflater.inflate(R.layout.pick_user, container, false);
        mainContainer = res.findViewById(R.id.mainContainer);
        empty = res.findViewById(R.id.empty);
        progress = res.findViewById(R.id.loading);

        contactsList = (ListView) res.findViewById(R.id.contacts);

        filteredUsers = new ContactsSource.LocalContact[0];
        originalUsers = new ContactsSource.LocalContact[0];
        isLoaded = false;
        usersAdapter = new BaseAdapter() {
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
                return getItem(i).contactId;
            }

            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @Override
            public boolean isEnabled(int position) {
                return false;
            }

            @Override
            public View getView(int index, View view, ViewGroup viewGroup) {
                if (view == null) {
                    view = View.inflate(context, R.layout.contacts_item_select, null);
                }

                final ContactsSource.LocalContact object = getItem(index);
                boolean showHeader = index == 0;
                if (index > 0) {
                    ContactsSource.LocalContact prev = getItem(index - 1);
                    showHeader = !object.displayName.substring(0, 1).equalsIgnoreCase(prev.displayName.substring(0, 1));
                }

                if (showHeader) {
                    view.findViewById(R.id.separatorContainer).setVisibility(View.VISIBLE);
                    if (object.displayName.length() == 0) {
                        ((TextView) view.findViewById(R.id.separator)).setText("#");
                    } else {
                        ((TextView) view.findViewById(R.id.separator)).setText(object.displayName.substring(0, 1));
                    }
                } else {
                    view.findViewById(R.id.separatorContainer).setVisibility(View.GONE);
                }

                if (matcher != null) {
                    SpannableString string = new SpannableString(object.displayName);
                    matcher.highlight(context, string);
                    ((TextView) view.findViewById(R.id.name)).setText(string);
                } else {
                    ((TextView) view.findViewById(R.id.name)).setText(object.displayName);
                }

                FastWebImageView imageView = (FastWebImageView) view.findViewById(R.id.avatar);
                imageView.setLoadingDrawable(Placeholders.USER_PLACEHOLDERS[object.user.getUid() % Placeholders.USER_PLACEHOLDERS.length]);

                if (object.user.getPhoto() instanceof TLLocalAvatarPhoto) {
                    TLLocalAvatarPhoto userPhoto = (TLLocalAvatarPhoto) object.user.getPhoto();
                    if (userPhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                        imageView.requestTask(new StelsImageTask((TLLocalFileLocation) userPhoto.getPreviewLocation()));
                    } else {
                        imageView.requestTask(null);
                    }
                } else {
                    imageView.requestTask(null);
                }

                int status = getUserState(object.user.getStatus());
                if (status < 0) {
                    ((TextView) view.findViewById(R.id.status)).setText(R.string.st_offline);
                    ((TextView) view.findViewById(R.id.status)).setTextColor(getResources().getColor(R.color.st_grey_text));
                } else if (status == 0) {
                    ((TextView) view.findViewById(R.id.status)).setText(R.string.st_online);
                    ((TextView) view.findViewById(R.id.status)).setTextColor(getResources().getColor(R.color.st_blue_bright));
                } else {
                    ((TextView) view.findViewById(R.id.status)).setTextColor(getResources().getColor(R.color.st_grey_text));
                    ((TextView) view.findViewById(R.id.status)).setText(
                            TextUtil.formatHumanReadableLastSeen(status, getStringSafe(R.string.st_lang)));
                }

                view.findViewById(R.id.contactSelected).setVisibility(View.GONE);

                view.findViewById(R.id.contactContainer).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setResult(Activity.RESULT_OK, object);
                        getActivity().onBackPressed();
                    }
                });

                return view;
            }
        };
        contactsList.setAdapter(usersAdapter);

        onContactsDataChanged();

        return res;
    }

    private void reloadData() {
        if (matcher != null) {
            ArrayList<ContactsSource.LocalContact> filtered = new ArrayList<ContactsSource.LocalContact>();
            for (ContactsSource.LocalContact u : originalUsers) {
                if (matcher.isMatched(u.displayName)) {
                    filtered.add(u);
                }
            }
            filteredUsers = filtered.toArray(new ContactsSource.LocalContact[0]);
        } else {
            filteredUsers = originalUsers;
        }

        if (isLoaded) {
            if (filteredUsers.length == 0) {
                empty.setVisibility(View.VISIBLE);
            } else {
                empty.setVisibility(View.GONE);
            }
        }
        usersAdapter.notifyDataSetChanged();
    }

    private void doFilter(String filter) {
        if (filter == null || filter.length() == 0) {
            matcher = null;
        } else {
            matcher = new FilterMatcher(filter);
        }
        reloadData();
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.pick_user_menu, menu);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_pick_user_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);


        MenuItem searchItem = menu.findItem(R.id.searchMenu);

        searchItem.setVisible(isLoaded);

        SearchView searchView = (SearchView) searchItem.getActionView();
        // searchView.setQueryHint(getStringSafe(R.string.st_pick_user_filter));
        searchView.setQueryHint("");

        ((ImageView) searchView.findViewById(R.id.abs__search_button)).setImageResource(R.drawable.st_bar_ic_search);

        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                return false;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                doFilter(newText.trim().toLowerCase());
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
                filteredUsers = originalUsers;
                reloadData();
                return true;
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        updateHeaderPadding();

        application.getContactsSource().registerListener(this);
        onContactsDataChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        application.getContactsSource().unregisterListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        contactsList = null;
        empty = null;
        progress = null;
    }

    @Override
    public void onContactsDataChanged() {
        ContactsSource.LocalContact[] contacts = application.getContactsSource().getTelegramContacts();
        if (contacts != null) {
            originalUsers = contacts;

            progress.setVisibility(View.GONE);
            if (contacts.length == 0) {
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
        } else {
            originalUsers = new ContactsSource.LocalContact[0];
            progress.setVisibility(View.VISIBLE);
            empty.setVisibility(View.GONE);
            contactsList.setVisibility(View.GONE);
            isLoaded = false;
        }
        reloadData();
    }
}
