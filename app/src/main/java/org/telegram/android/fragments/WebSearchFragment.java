package org.telegram.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import org.telegram.android.R;
import org.telegram.android.base.TelegramActivity;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.core.WebSearchSource;
import org.telegram.android.core.model.WebSearchResult;
import org.telegram.android.core.model.web.TLSearchResult;
import org.telegram.android.log.Logger;
import org.telegram.android.preview.PreviewConfig;
import org.telegram.android.preview.SmallPreviewView;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.ui.source.ViewSourceListener;
import org.telegram.android.ui.source.ViewSourceState;
import org.telegram.android.util.IOUtils;

import java.io.File;
import java.io.IOException;
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
    private BaseAdapter adapter;
    private WebSearchSource webSearchSource;
    private boolean isInSearchMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.web_search, container, false);

        isInSearchMode = false;

        webSearchSource = application.getDataSourceKernel().getWebSearchSource();
        lastSearchResults = webSearchSource.getLastResults();
        for (int i = 0; i < lastSearchResults.length; i++) {
            searchResults.add(new WebSearchResult(i, lastSearchResults[i]));
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

                runUiTask(new AsyncAction() {

                    private String fileName;

                    @Override
                    public void execute() throws AsyncException {
                        try {
                            byte[] data = IOUtils.downloadFile(result.getFullUrl());
                            fileName = getUploadTempFile("upload.jpg");
                            IOUtils.writeAll(fileName, data);
                            webSearchSource.onSearchResultSent(result.toTlResult());
                        } catch (IOException e) {
                            throw new AsyncException(AsyncException.ExceptionType.CONNECTION_ERROR);
                        }
                    }

                    @Override
                    public void afterExecute() {
                        webSearchSource.cancelQuery();
                        getActivity().setResult(Activity.RESULT_OK, new Intent().setData(Uri.fromFile(new File(fileName))));
                        getActivity().finish();
                    }
                });
            }
        });

        final Context context = getActivity();
        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return searchResults.size();
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
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    SmallPreviewView previewView = new SmallPreviewView(context);
                    GridView.LayoutParams imageParams = new GridView.LayoutParams(PreviewConfig.MEDIA_PREVIEW, PreviewConfig.MEDIA_PREVIEW);
                    previewView.setLayoutParams(imageParams);
                    convertView = previewView;
                }

                SmallPreviewView previewView = (SmallPreviewView) convertView;
                previewView.requestSearchThumb(getItem(position));
                return previewView;
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
        if (webSearchSource.getViewSource() != null) {
            onSourceDataChanged();
        }
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
        // searchItem.expandActionView();
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                isInSearchMode = true;
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                isInSearchMode = false;
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
    }

    @Override
    public void onSourceStateChanged() {
        if (webSearchSource.getViewSource() != null) {
            if (webSearchSource.getViewSource().getState() == ViewSourceState.IN_PROGRESS) {
                goneView(gridView);
                goneView(empty);
                showView(progress);
            } else {
                if (webSearchSource.getViewSource().getItemsCount() == 0) {
                    showView(empty);
                    goneView(gridView);
                } else {
                    goneView(empty);
                    showView(gridView);
                }
                goneView(progress);
            }
        } else {
            if (lastSearchResults.length > 0) {
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
        searchResults = webSearchSource.getViewSource().getCurrentWorkingSet();
        adapter.notifyDataSetChanged();
    }
}
