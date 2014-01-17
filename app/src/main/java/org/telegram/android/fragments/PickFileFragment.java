package org.telegram.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

/**
 * Created by ex3ndr on 14.12.13.
 */
public class PickFileFragment extends StelsFragment {
    private String currentPath = "/";
    private ArrayList<String> history = new ArrayList<String>();

    private Record[] currentFolder = new Record[0];
    private boolean noAccess = false;
    private BaseAdapter adapter;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.file_list, container, false);
        setResult(Activity.RESULT_CANCELED, null);

        ListView listView = (ListView) res.findViewById(R.id.itemsList);
        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return currentFolder.length;
            }

            @Override
            public Record getItem(int i) {
                return currentFolder[i];
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                if (view == null) {
                    view = inflater.inflate(R.layout.file_item, viewGroup, false);
                }

                Record item = getItem(i);

                ((TextView) view.findViewById(R.id.fileName)).setText(item.getPath());

                switch (item.getType()) {
                    default:
                    case RECORD_FILE:
                        ((ImageView) view.findViewById(R.id.icon)).setImageDrawable(null);
                        break;
                    case RECORD_DIRECTORY:
                        ((ImageView) view.findViewById(R.id.icon)).setImageResource(R.drawable.st_ic_directory);
                        break;
                    case RECORD_STORAGE:
                        ((ImageView) view.findViewById(R.id.icon)).setImageResource(R.drawable.st_ic_storage);
                        break;
                    case RECORD_STORAGE_EX:
                        ((ImageView) view.findViewById(R.id.icon)).setImageResource(R.drawable.st_ic_external_storage);
                        break;
                }

                switch (item.getType()) {
                    case RECORD_DIRECTORY:
                    case RECORD_STORAGE:
                    case RECORD_STORAGE_EX:
                        view.findViewById(R.id.icon).setBackgroundColor(
                                application.getResources().getColor(R.color.st_file_dir_bg));
                        break;
                    default:
                        view.findViewById(R.id.icon).setBackgroundColor(0);
                        break;
                }

                return view;
            }
        };
        listView.setAdapter(adapter);

        updatePath();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Record o = (Record) adapterView.getItemAtPosition(i);
                if (o.getType() == RECORD_DIRECTORY) {
                    gotoFolder(currentPath + "/" + o.getPath());
                } else if (o.getType() == RECORD_FILE) {
                    setResult(Activity.RESULT_OK, currentPath + "/" + o.getPath());
                    history.clear();
                    getRootController().doBack();
                }
            }
        });
        return res;
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_pick_file_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
    }

    private void gotoFolder(String path) {
        history.add(currentPath);
        currentPath = path;
        updatePath();
    }

    private void updatePath() {
        ArrayList<Record> items = new ArrayList<Record>();

        if (currentPath.length() == 0) {

        } else {
            File file = new File(currentPath);

            if (file.list() != null) {
                noAccess = false;

                for (String f : file.list(new FilenameFilter() {
                    @Override
                    public boolean accept(java.io.File file, String s) {
                        return new File(file, s).isDirectory();
                    }
                })) {
                    items.add(new Record(f, RECORD_DIRECTORY));
                }

                for (String f : file.list(new FilenameFilter() {
                    @Override
                    public boolean accept(java.io.File file, String s) {
                        return !(new File(file, s).isDirectory());
                    }
                })) {
                    items.add(new Record(f, RECORD_FILE));
                }
            } else {
                noAccess = true;
            }
        }

        currentFolder = items.toArray(new Record[0]);

        adapter.notifyDataSetInvalidated();
    }

    @Override
    public boolean onBackPressed() {
        if (history.size() > 0) {
            String path = history.get(history.size() - 1);
            history.remove(history.size() - 1);
            currentPath = path;
            updatePath();
            return true;
        }
        return false;
    }

    private static final int RECORD_FILE = 0;
    private static final int RECORD_DIRECTORY = 1;
    private static final int RECORD_STORAGE = 2;
    private static final int RECORD_STORAGE_EX = 3;

    private class Record {
        private String path;

        private int type;

        private Record(String path, int type) {
            this.path = path;
            this.type = type;
        }

        public String getPath() {
            return path;
        }

        public int getType() {
            return type;
        }
    }
}
