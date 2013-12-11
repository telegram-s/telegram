package org.telegram.android.fragments.common;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.extradea.framework.images.ui.FastWebImageView;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;
import org.telegram.android.core.ContactSourceListener;
import org.telegram.android.core.ContactsSource;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.tasks.ProgressInterface;
import org.telegram.android.ui.FilterMatcher;
import org.telegram.android.ui.Placeholders;
import org.telegram.android.ui.TextUtil;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ex3ndr on 08.12.13.
 */
public abstract class BaseContactsFragment extends StelsFragment implements ContactSourceListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    private boolean isLoaded;

    private FilterMatcher matcher;

    private ContactsSource.LocalContact[] allContacts;
    private ContactsSource.LocalContact[] filteredContacts;
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

        View res = inflater.inflate(getLayout(), container, false);
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
        allContacts = new ContactsSource.LocalContact[0];
        filteredContacts = new ContactsSource.LocalContact[0];
        contactsAdapter = new ContactsAdapter();
        listView.setAdapter(contactsAdapter);

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {

            boolean isScrolling = false;

            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                if (i == SCROLL_STATE_IDLE) {
                    application.getImageController().doResume();
                    isScrolling = false;
                } else {
                    application.getImageController().doPause();
                    isScrolling = true;
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i2, int i3) {
                if (isScrolling) {
                    application.getImageController().doPause();
                }
            }
        });

        reloadData();

        if (selectedIndex != -1) {
            if (selectedIndex < filteredContacts.length) {
                listView.setSelectionFromTop(selectedIndex, selectedTop);
            }
        }

        return res;
    }

    public boolean filterItem(ContactsSource.LocalContact contact) {
        return true;
    }

    public ContactsSource.LocalContact getContactAt(int index) {
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
            ArrayList<ContactsSource.LocalContact> filtered = new ArrayList<ContactsSource.LocalContact>();
            for (ContactsSource.LocalContact u : allContacts) {
                if (!filterItem(u)) {
                    continue;
                }
                if (matcher.isMatched(u.displayName)) {
                    filtered.add(u);
                }
            }
            filteredContacts = filtered.toArray(new ContactsSource.LocalContact[filtered.size()]);
        } else {
            ArrayList<ContactsSource.LocalContact> filtered = new ArrayList<ContactsSource.LocalContact>();
            for (ContactsSource.LocalContact u : allContacts) {
                if (!filterItem(u)) {
                    continue;
                }
                filtered.add(u);
            }
            filteredContacts = filtered.toArray(new ContactsSource.LocalContact[filtered.size()]);
        }

        ArrayList<String> nHeaders = new ArrayList<String>();
        ArrayList<Integer> nHeaderStarts = new ArrayList<Integer>();

        char header = '\0';

        for (int i = 0; i < filteredContacts.length; i++) {
            if (filteredContacts[i].header != header) {
                nHeaders.add("" + filteredContacts[i].header);
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
                if (!isMultiple) {
                    view.findViewById(R.id.shareIcon).setVisibility(View.GONE);
                }
            } else {
                ((FastWebImageView) view.findViewById(R.id.avatar)).setLoadingDrawable(R.drawable.st_user_placeholder);
                ((FastWebImageView) view.findViewById(R.id.avatar)).requestTask(null);
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
        public ContactsSource.LocalContact getItem(int i) {
            return filteredContacts[i];
        }

        @Override
        public long getItemId(int i) {
            return filteredContacts[i].contactId;
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
            if (i > headerStart.length || i < 0) {
                return 0;
            }
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
