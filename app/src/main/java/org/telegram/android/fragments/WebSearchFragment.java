package org.telegram.android.fragments;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import org.telegram.android.R;
import org.telegram.android.activity.PickWebImageActivity;
import org.telegram.android.base.TelegramActivity;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.core.WebSearchSource;
import org.telegram.android.core.model.WebSearchResult;
import org.telegram.android.core.model.web.TLSearchResult;
import org.telegram.android.log.Logger;
import org.telegram.android.preview.PreviewConfig;
import org.telegram.android.preview.SmallPreviewView;
import org.telegram.android.ui.TextUtil;
import org.telegram.android.ui.source.ViewSourceListener;
import org.telegram.android.ui.source.ViewSourceState;

import java.util.ArrayList;

/**
 * Created by ex3ndr on 14.03.14.
 */
public class WebSearchFragment extends TelegramFragment implements ViewSourceListener {
    private static final String TAG = "WebSearchFragment";

    private View progress;
    private View empty;
    private View emptyHint;
    private GridView gridView;
    private TLSearchResult[] lastSearchResults;
    private ArrayList<WebSearchResult> searchResults = new ArrayList<WebSearchResult>();
    private ArrayList<WebSearchResult> lastResults = new ArrayList<WebSearchResult>();
    private BaseAdapter adapter;
    private WebSearchSource webSearchSource;
    private boolean isInSearchMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.web_search, container, false);

        webSearchSource = application.getDataSourceKernel().getWebSearchSource();
        if (lastSearchResults == null) {
            lastSearchResults = webSearchSource.getLastResults();
            for (int i = 0; i < lastSearchResults.length; i++) {
                lastResults.add(new WebSearchResult(i, lastSearchResults[i]));
            }
        }

        gridView = (GridView) res.findViewById(R.id.mediaGrid);
        progress = res.findViewById(R.id.loading);
        empty = res.findViewById(R.id.empty);
        emptyHint = res.findViewById(R.id.hint);
        gridView.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        empty.setVisibility(View.GONE);
        emptyHint.setVisibility(View.GONE);

        gridView.setPadding(0, PreviewConfig.MEDIA_SPACING, 0, PreviewConfig.MEDIA_SPACING);
        gridView.setNumColumns(PreviewConfig.MEDIA_ROW_COUNT);
        gridView.setColumnWidth(PreviewConfig.MEDIA_PREVIEW);
        gridView.setVerticalSpacing(PreviewConfig.MEDIA_SPACING);
        gridView.setHorizontalSpacing(PreviewConfig.MEDIA_SPACING);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final WebSearchResult result = (WebSearchResult) parent.getItemAtPosition(position);
                ((PickWebImageActivity) getActivity()).openPreview(result);
            }
        });

        final Context context = getActivity();
        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return searchResults.size() + 1;
            }

            @Override
            public WebSearchResult getItem(int position) {
                return searchResults.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public int getItemViewType(int position) {
                if (position < searchResults.size()) {
                    return 0;
                } else {
                    return 1;
                }
            }

            @Override
            public int getViewTypeCount() {
                return 2;
            }

            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @Override
            public boolean isEnabled(int position) {
                return position < searchResults.size();
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (position < searchResults.size()) {
                    if (isInSearchMode) {
                        webSearchSource.getViewSource().onItemsShown(position);
                    }

                    if (convertView == null) {
                        FrameLayout res = new FrameLayout(context);
                        GridView.LayoutParams params = new GridView.LayoutParams(
                                PreviewConfig.MEDIA_PREVIEW,
                                PreviewConfig.MEDIA_PREVIEW);
                        res.setLayoutParams(params);

                        SmallPreviewView previewView = new SmallPreviewView(context);
                        previewView.setEmptyDrawable(new ColorDrawable(0xffdfe4ea));
                        previewView.setLayoutParams(new FrameLayout.LayoutParams(
                                PreviewConfig.MEDIA_PREVIEW,
                                PreviewConfig.MEDIA_PREVIEW));
                        res.addView(previewView);

                        TextView size = new TextView(context);
                        size.setBackgroundResource(R.drawable.st_bubble_media_info);
                        size.setTextColor(Color.WHITE);
                        size.setTextSize(12);
                        FrameLayout.LayoutParams sizeParams = new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                Gravity.BOTTOM | Gravity.LEFT);
                        size.setLayoutParams(sizeParams);
                        size.setGravity(Gravity.CENTER_VERTICAL);
                        size.setCompoundDrawablePadding(getPx(4));
                        res.addView(size);

                        ImageView animationIcon = new ImageView(context);
                        animationIcon.setImageResource(R.drawable.st_ic_websearch_animation);
                        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                Gravity.CENTER);
                        animationIcon.setLayoutParams(iconParams);
                        res.addView(animationIcon);

                        convertView = res;
                    }

                    WebSearchResult searchResult = getItem(position);

                    SmallPreviewView previewView = (SmallPreviewView) ((ViewGroup) convertView).getChildAt(0);
                    previewView.requestSearchThumb(searchResult);

                    TextView size = (TextView) ((ViewGroup) convertView).getChildAt(1);
                    size.setText(TextUtil.formatFileSize(searchResult.getSize()));

                    View animationIcon = ((ViewGroup) convertView).getChildAt(2);

                    if (searchResult.getContentType().equals("image/gif")) {
                        animationIcon.setVisibility(View.VISIBLE);
                    } else {
                        animationIcon.setVisibility(View.GONE);
                    }

                    return convertView;
                } else {
                    ProgressBar res = new ProgressBar(context);
                    GridView.LayoutParams params = new GridView.LayoutParams(
                            PreviewConfig.MEDIA_PREVIEW,
                            PreviewConfig.MEDIA_PREVIEW);
                    res.setLayoutParams(params);

                    res.setVisibility(View.INVISIBLE);
                    if (isInSearchMode) {
                        if (webSearchSource.getViewSource() != null) {
                            if (webSearchSource.getViewSource().getState() == ViewSourceState.IN_PROGRESS) {
                                res.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                    return res;
                }
            }
        };
        gridView.setAdapter(adapter);
        return res;
    }

    private void doSearch(String query) {
        Logger.d(TAG, "Searching: " + query);
        webSearchSource.newQuery(query);
        onSourceDataChanged();
        onSourceStateChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        webSearchSource.setListener(this);
        onSourceStateChanged();
        onSourceDataChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        webSearchSource.setListener(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.web_menu, menu);

        final MenuItem searchItem = menu.findItem(R.id.searchMenu);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        if (isInSearchMode) {
            searchItem.expandActionView();
            searchView.setQuery(webSearchSource.getQuery(), false);
            searchView.clearFocus();
        }

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                isInSearchMode = true;
                onSourceDataChanged();
                onSourceStateChanged();
                adapter.notifyDataSetInvalidated();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                isInSearchMode = false;
                webSearchSource.cancelQuery();
                onSourceDataChanged();
                onSourceStateChanged();

                ((TelegramActivity) getActivity()).fixBackButton();
                return true;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String s) {
                secureCallback(new Runnable() {
                    @Override
                    public void run() {
                        doSearch(s);
                    }
                });
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_web_search_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
        ((TelegramActivity) getActivity()).fixBackButton();
    }

    @Override
    public void onSourceStateChanged() {
        if (isInSearchMode) {
            if (webSearchSource.getViewSource() != null) {

                if (webSearchSource.getViewSource().getItemsCount() == 0) {
                    if (webSearchSource.getViewSource().getState() == ViewSourceState.IN_PROGRESS) {
                        goneView(gridView);
                        goneView(empty);
                        goneView(emptyHint);
                        showView(progress);
                    } else {
                        showView(empty);
                        goneView(gridView);
                        goneView(emptyHint);
                        goneView(progress);
                    }
                } else {
                    goneView(empty);
                    showView(gridView);
                    goneView(emptyHint);
                    goneView(progress);
                }
            } else {
                goneView(gridView);
                showView(emptyHint);
                goneView(progress);
                goneView(empty);
            }
        } else {
            if (lastResults.size() > 0) {
                showView(gridView);
                goneView(emptyHint);
            } else {
                goneView(gridView);
                showView(emptyHint);
            }
            goneView(progress);
            goneView(empty);
        }

    }

    @Override
    public void onSourceDataChanged() {
        if (isInSearchMode) {
            if (webSearchSource.getViewSource() != null) {
                searchResults = webSearchSource.getViewSource().getCurrentWorkingSet();
            } else {
                searchResults.clear();
            }
            adapter.notifyDataSetChanged();
        } else {
            searchResults = new ArrayList<WebSearchResult>(lastResults);
            adapter.notifyDataSetChanged();
        }
    }
}
