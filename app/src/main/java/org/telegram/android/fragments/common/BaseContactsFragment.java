package org.telegram.android.fragments.common;

import android.content.Context;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.telegram.android.R;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.core.ContactSourceListener;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.wireframes.ContactWireframe;
import org.telegram.android.preview.AvatarView;
import org.telegram.android.tasks.ProgressInterface;
import org.telegram.android.ui.FilterMatcher;
import org.telegram.android.ui.Placeholders;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import java.util.ArrayList;

/**
 * Created by ex3ndr on 08.12.13.
 */
public abstract class BaseContactsFragment extends TelegramFragment implements ContactSourceListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    private boolean isLoaded;

    private FilterMatcher matcher;

    private ContactWireframe[] allContacts;
    private ContactWireframe[] filteredContacts;
    private String[] headers;
    private Integer[] headerStart;

    private View empty;
    private View loading;

    private StickyListHeadersListView listView;
    private ContactsAdapter contactsAdapter;

    private boolean isMultiple;

    private int selectedTop;
    private int selectedIndex;

    protected StickyListHeadersListView getListView() {
        return listView;
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
                showList();
                BaseContactsFragment.this.showContent();
            }

            @Override
            public void hideContent() {
                hideList();
                BaseContactsFragment.this.hideContent();
            }

            @Override
            public void showProgress() {
                showLoading();
            }

            @Override
            public void hideProgress() {
                hideLoading();
            }
        });
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            selectedTop = savedInstanceState.getInt("selectedTop", 0);
            selectedIndex = savedInstanceState.getInt("selectedIndex", -1);
        }

        View res = wrap(inflater).inflate(getLayout(), container, false);
        listView = (StickyListHeadersListView) res.findViewById(R.id.contactsList);
        loading = res.findViewById(R.id.loading);
        empty = res.findViewById(R.id.empty);
        isMultiple = useMultipleSelection();
        onCreateView(res, inflater, container, savedInstanceState);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);

        listView.setPadding(
                (int) listView.getResources().getDimension(R.dimen.fast_scroll_padding_left), 0,
                (int) listView.getResources().getDimension(R.dimen.fast_scroll_padding_right), 0);
//        listView.getWrappedList().invalidateViews();
//        listView.post(new Runnable() {
//            @Override
//            public void run() {
//                listView.setPadding(
//                        (int) listView.getResources().getDimension(R.dimen.fast_scroll_padding_left), 0,
//                        (int) listView.getResources().getDimension(R.dimen.fast_scroll_padding_right), 0);
//                listView.getWrappedList().invalidateViews();
//                listView.invalidate();
//            }
//        });

        isLoaded = false;
        allContacts = new ContactWireframe[0];
        filteredContacts = new ContactWireframe[0];
        contactsAdapter = new ContactsAdapter();
        listView.setAdapter(contactsAdapter);

        reloadData();

        if (selectedIndex != -1) {
            if (selectedIndex < filteredContacts.length) {
                listView.setSelectionFromTop(selectedIndex, selectedTop);
            }
        }

        return res;
    }

    public boolean filterItem(ContactWireframe contact) {
        return true;
    }

    public ContactWireframe getContactAt(int index) {
        return filteredContacts[index];
    }

    public int getContactsCount() {
        return filteredContacts.length;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    protected void reloadData() {
        boolean isContactsLoaded = application.getContactsSource().getContacts() != null;
        if (!isContactsLoaded) {
            hideList();
            hideContent();
            hideEmpty();
            showLoading();
            return;
        } else {
            showContent();
            hideLoading();
        }

        if (showOnlyTelegramContacts()) {
            allContacts = application.getContactsSource().getTelegramContacts();
        } else {
            allContacts = application.getContactsSource().getContacts();
        }

        if (matcher != null && matcher.getQuery().length() > 0) {
            ArrayList<ContactWireframe> filtered = new ArrayList<ContactWireframe>();
            for (ContactWireframe u : allContacts) {
                if (!filterItem(u)) {
                    continue;
                }
                if (matcher.isMatched(u.getDisplayName())) {
                    filtered.add(u);
                }
            }
            filteredContacts = filtered.toArray(new ContactWireframe[filtered.size()]);
        } else {
            ArrayList<ContactWireframe> filtered = new ArrayList<ContactWireframe>();
            for (ContactWireframe u : allContacts) {
                if (!filterItem(u)) {
                    continue;
                }
                filtered.add(u);
            }
            filteredContacts = filtered.toArray(new ContactWireframe[filtered.size()]);
        }

        ArrayList<String> nHeaders = new ArrayList<String>();
        ArrayList<Integer> nHeaderStarts = new ArrayList<Integer>();

        char header = '\0';

        for (int i = 0; i < filteredContacts.length; i++) {
            if (filteredContacts[i].getSection() != header) {
                header = filteredContacts[i].getSection();
                nHeaders.add("" + filteredContacts[i].getSection());
                nHeaderStarts.add(i);
            }
        }

        headers = nHeaders.toArray(new String[0]);
        headerStart = nHeaderStarts.toArray(new Integer[0]);


        contactsAdapter.notifyDataSetChanged();

        if (filteredContacts.length == 0) {
            showEmpty();
            hideList();
        } else {
            hideEmpty();
            showList();
        }

        if (!isLoaded) {
            isLoaded = true;
            getSherlockActivity().invalidateOptionsMenu();
        }
    }

    protected void doFilter(String query) {
        if (query == null || query.trim().length() == 0) {
            matcher = null;
        } else {
            matcher = new FilterMatcher(query.trim().toLowerCase());
        }
        reloadData();
        onFilterChanged();
        if (matcher != null) {
            contactsAdapter.notifyDataSetInvalidated();
            listView.invalidateViews();
            listView.post(new Runnable() {
                @Override
                public void run() {
                    if (filteredContacts.length > 0) {
                        listView.setSelection(0);
                    }
                }
            });
        }
    }

    protected void onFilterChanged() {

    }

    protected boolean isFiltering() {
        return matcher != null;
    }

    protected void hideContent() {

    }

    protected void showContent() {

    }

    protected void hideList() {
        listView.setVisibility(View.GONE);
    }

    protected void showList() {
        listView.setVisibility(View.VISIBLE);
    }

    protected void showEmpty() {
        if (empty != null) {
            empty.setVisibility(View.VISIBLE);
        }
    }

    protected void hideEmpty() {
        if (empty != null) {
            empty.setVisibility(View.GONE);
        }
    }

    protected void showLoading() {
        if (loading != null) {
            loading.setVisibility(View.VISIBLE);
        }
    }

    protected void hideLoading() {
        if (loading != null) {
            loading.setVisibility(View.GONE);
        }
    }

    protected abstract boolean showOnlyTelegramContacts();

    protected abstract int getLayout();

    protected abstract void onCreateView(View view, LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

    protected boolean useMultipleSelection() {
        return false;
    }

    protected boolean isSelected(int index) {
        return false;
    }

    private void saveListPosition() {
        if (listView != null) {
            int index = listView.getFirstVisiblePosition();
            View v = listView.getChildAt(0);
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
    public void onResume() {
        super.onResume();
        application.getContactsSource().registerListener(this);
        onContactsDataChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        application.getContactsSource().unregisterListener(this);
        saveListPosition();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listView = null;
        empty = null;
        loading = null;
        matcher = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveListPosition();
        outState.putInt("selectedTop", selectedTop);
        outState.putInt("selectedIndex", selectedIndex);
    }

    @Override
    public void onContactsDataChanged() {
        reloadData();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        return false;
    }

    private class ContactsAdapter extends BaseAdapter implements StickyListHeadersAdapter, SectionIndexer {
        public View newView(Context context) {
            return View.inflate(context, isMultiple ? R.layout.contacts_item_multiple : R.layout.contacts_item_single, null);
        }

        public void bindView(View view, final Context context, int index) {
            final ContactWireframe contact = getItem(index);

            if (matcher != null) {
                SpannableString spannableString = new SpannableString(contact.getDisplayName());
                matcher.highlight(context, spannableString);
                ((TextView) view.findViewById(R.id.name)).setText(spannableString);
            } else {
                ((TextView) view.findViewById(R.id.name)).setText(contact.getDisplayName());
            }

            TextView onlineView = (TextView) view.findViewById(R.id.status);

            if (contact.getRelatedUsers().length > 0) {
                User user = contact.getRelatedUsers()[0];
                ((AvatarView) view.findViewById(R.id.avatar)).setEmptyUser(user.getDisplayName(), user.getUid());
                if (user.getPhoto() != null && (user.getPhoto() instanceof TLLocalAvatarPhoto)) {
                    TLLocalAvatarPhoto p = (TLLocalAvatarPhoto) user.getPhoto();
                    ((AvatarView) view.findViewById(R.id.avatar)).requestAvatar(p.getPreviewLocation());
                } else {
                    ((AvatarView) view.findViewById(R.id.avatar)).requestAvatar(null);
                }

                int statusValue = getUserState(user.getStatus());
                if (statusValue < 0) {
                    onlineView.setText(R.string.st_offline);
                    onlineView.setTextColor(context.getResources().getColor(R.color.st_grey_text));
                } else if (statusValue == 0) {
                    onlineView.setText(R.string.st_online);
                    onlineView.setTextColor(context.getResources().getColor(R.color.st_blue_bright));
                } else {
                    onlineView.setTextColor(context.getResources().getColor(R.color.st_grey_text));
                    onlineView.setText(formatLastSeen(statusValue));
                }

                onlineView.setVisibility(View.VISIBLE);
                if (!isMultiple) {
                    view.findViewById(R.id.shareIcon).setVisibility(View.GONE);
                }
            } else {
                ((AvatarView) view.findViewById(R.id.avatar)).setEmptyGreyUser();
                ((AvatarView) view.findViewById(R.id.avatar)).requestAvatar(null);
                onlineView.setVisibility(View.GONE);
                if (!isMultiple) {
                    view.findViewById(R.id.shareIcon).setVisibility(View.VISIBLE);
                }
            }

            if (isMultiple) {
                if (isSelected(index)) {
                    ((ImageView) view.findViewById(R.id.contactSelected)).setImageResource(R.drawable.holo_btn_check_on);
                } else {
                    ((ImageView) view.findViewById(R.id.contactSelected)).setImageResource(R.drawable.holo_btn_check_off);
                }
            }
        }

        @Override
        public int getCount() {
            return filteredContacts.length;
        }

        @Override
        public ContactWireframe getItem(int i) {
            return filteredContacts[i];
        }

        @Override
        public long getItemId(int i) {
            if (filteredContacts[i].getContactId() == 0) {
                return -filteredContacts[i].getRelatedUsers()[0].getUid();
            }
            return filteredContacts[i].getContactId();
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = newView(application);
            }
            bindView(view, application, i);
            return view;
        }

        @Override
        public View getHeaderView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = View.inflate(application, R.layout.contact_item_header, null);
            }
            ((TextView) view.findViewById(R.id.header)).setText(getItem(i).getHeader() + "");
            return view;
        }

        @Override
        public long getHeaderId(int i) {
            return getItem(i).getHeader().charAt(0);
        }

        @Override
        public Object[] getSections() {
            return headers;
        }

        @Override
        public int getPositionForSection(int i) {
            if (i >= headerStart.length || i < 0) {
                return 0;
            }
            return headerStart[i];
        }

        @Override
        public int getSectionForPosition(int ind) {
            for (int i = 0; i < headerStart.length - 1; i++) {
                if (headerStart[i] <= ind && ind < headerStart[i + 1]) {
                    return i;
                }
            }

            return headerStart.length - 1;
        }
    }
}
