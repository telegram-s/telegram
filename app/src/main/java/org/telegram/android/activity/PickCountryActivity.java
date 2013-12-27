package org.telegram.android.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.*;
import com.actionbarsherlock.widget.SearchView;
import org.telegram.android.R;
import org.telegram.android.StelsActivity;
import org.telegram.android.countries.Countries;
import org.telegram.android.countries.CountryRecord;
import org.telegram.android.ui.FilterMatcher;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import java.util.ArrayList;

/**
 * Created by ex3ndr on 23.12.13.
 */
public class PickCountryActivity extends StelsActivity {
    private StickyListHeadersListView listView;
    private CountriesAdapter adapter;
    private CountryRecord[] countries;
    private FilterMatcher matcher;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.st_bar_bg));
        getSupportActionBar().setLogo(R.drawable.st_bar_logo);
        getSupportActionBar().setIcon(R.drawable.st_bar_logo);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Choose a country");

        countries = Countries.COUNTRIES;

        setContentView(R.layout.pick_country);

        listView = (StickyListHeadersListView) findViewById(R.id.listView);
        adapter = new CountriesAdapter();
        listView.setAdapter(adapter);
        listView.setFastScrollEnabled(true);
        listView.setFastScrollAlwaysVisible(true);
        listView.setPadding((int) getResources().getDimension(R.dimen.fast_scroll_padding_left), 0,
                (int) getResources().getDimension(R.dimen.fast_scroll_padding_right), 0);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                hideKeyboard();
                application.getKernel().getActivationController().setCurrentCountry(countries[i]);
                finish();
            }
        });

        doFilter(null);
    }

    private void hideKeyboard() {
        View view = findViewById(R.id.focuser);
        if (view != null) {
            view.requestFocus();
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return false;
    }

    private void doFilter(String query) {
        if (query == null || query.trim().length() == 0) {
            matcher = null;
        } else {
            matcher = new FilterMatcher(query);
        }
        ArrayList<CountryRecord> records = new ArrayList<CountryRecord>();
        for (int i = 0; i < Countries.COUNTRIES.length - 1; i++) {
            if (matcher != null) {
                if (matcher.isMatched(Countries.COUNTRIES[i].getTitle())) {
                    records.add(Countries.COUNTRIES[i]);
                }
            } else {
                records.add(Countries.COUNTRIES[i]);
            }
        }
        countries = records.toArray(new CountryRecord[0]);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.pick_country, menu);
        MenuItem item = menu.findItem(R.id.searchMenu);
        com.actionbarsherlock.widget.SearchView searchView = (SearchView) item.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                doFilter(s);
                return true;
            }
        });

        item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                doFilter(null);
                hideKeyboard();
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    private class CountriesAdapter extends BaseAdapter implements SectionIndexer, StickyListHeadersAdapter {

        private Integer[] sectionStart;
        private Object[] sectionValue;

        public CountriesAdapter() {
            notifyDataSetChanged();
        }

        @Override
        public void notifyDataSetChanged() {
            char lastStart = '#';
            ArrayList<Object> sectionTitles = new ArrayList<Object>();
            ArrayList<Integer> sections = new ArrayList<Integer>();
            for (int i = 0; i < countries.length; i++) {
                if (countries[i].getTitle().charAt(0) != lastStart) {
                    sections.add(i);
                    lastStart = countries[i].getTitle().charAt(0);
                    sectionTitles.add("" + lastStart);
                }
            }

            sectionStart = sections.toArray(new Integer[0]);
            sectionValue = sectionTitles.toArray();
            super.notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return countries.length;
        }

        @Override
        public CountryRecord getItem(int i) {
            return countries[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.country_record, viewGroup, false);
            }
            CountryRecord record = getItem(i);

            if (matcher != null) {
                SpannableString title = new SpannableString(record.getTitle());
                matcher.highlight(PickCountryActivity.this, title);
                ((TextView) view.findViewById(R.id.country)).setText(title);
            } else {
                ((TextView) view.findViewById(R.id.country)).setText(record.getTitle());
            }

            ((TextView) view.findViewById(R.id.code)).setText("+" + record.getCallPrefix());

            return view;
        }

        @Override
        public Object[] getSections() {
            return sectionValue;
        }

        @Override
        public int getPositionForSection(int i) {
            if (i >= 0 && i < sectionStart.length) {
                return sectionStart[i];
            } else {
                return 0;
            }
        }

        @Override
        public int getSectionForPosition(int index) {
            for (int i = 1; i < sectionStart.length; i++) {
                if (sectionStart[i] > index) {
                    return i - 1;
                }
            }
            if (sectionStart.length > 0) {
                return sectionStart.length - 1;
            } else {
                return 0;
            }
        }

        @Override
        public View getHeaderView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = View.inflate(application, R.layout.contact_item_header, null);
            }
            ((TextView) view.findViewById(R.id.header)).setText(getSections()[getSectionForPosition(i)] + "");

            return view;
        }

        @Override
        public long getHeaderId(int i) {
            return getSectionForPosition(i);
        }
    }
}