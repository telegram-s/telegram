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
import org.telegram.android.fragments.common.BaseContactsFragment;
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
public class PickUserFragment extends BaseContactsFragment {
    private View mainContainer;

    public PickUserFragment() {

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getSherlockActivity().invalidateOptionsMenu();
    }

    @Override
    protected boolean showOnlyTelegramContacts() {
        return true;
    }

    @Override
    protected int getLayout() {
        return R.layout.pick_user;
    }

    @Override
    protected void onCreateView(View view, LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setResult(Activity.RESULT_CANCELED, null);
        mainContainer = view.findViewById(R.id.mainContainer);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        ContactsSource.LocalContact contact = (ContactsSource.LocalContact) adapterView.getItemAtPosition(i);
        setResult(Activity.RESULT_OK, contact.user.getUid());
        getActivity().onBackPressed();
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_pick_user_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);

        if (!isLoaded()) {
            return;
        }

        inflater.inflate(R.menu.pick_user_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.searchMenu);

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
                doFilter(newText);
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
                doFilter(null);
                return true;
            }
        });

    }
}
