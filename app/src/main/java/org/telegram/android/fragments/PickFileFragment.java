package org.telegram.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
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

    private Object[] currentFolder = new Object[0];
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
            public Object getItem(int i) {
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

                Object item = getItem(i);

                if (item instanceof Directory) {
                    ((TextView) view.findViewById(R.id.fileName)).setText(((Directory) getItem(i)).getPath());
                } else {
                    ((TextView) view.findViewById(R.id.fileName)).setText(((DFile) getItem(i)).getPath());
                }

                return view;
            }
        };
        listView.setAdapter(adapter);

        updatePath();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Object o = adapterView.getItemAtPosition(i);
                if (o instanceof Directory) {
                    gotoFolder(currentPath + "/" + ((Directory) o).getPath());
                } else if (o instanceof DFile) {
                    setResult(Activity.RESULT_OK, currentPath + "/" + ((DFile) o).getPath());
                    history.clear();
                    getRootController().doBack();
                }
            }
        });
        return res;
    }

    private void gotoFolder(String path) {
        history.add(currentPath);
        currentPath = path;
        updatePath();
    }

    private void updatePath() {
        ArrayList<Object> items = new ArrayList<Object>();

        File file = new File(currentPath);

        if (file.list() != null) {
            noAccess = false;

            for (String f : file.list(new FilenameFilter() {
                @Override
                public boolean accept(java.io.File file, String s) {
                    return new File(file, s).isDirectory();
                }
            })) {
                items.add(new Directory(f));
            }

            for (String f : file.list(new FilenameFilter() {
                @Override
                public boolean accept(java.io.File file, String s) {
                    return !(new File(file, s).isDirectory());
                }
            })) {
                items.add(new DFile(f));
            }
        } else {
            noAccess = true;
        }

        currentFolder = items.toArray();

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

    private class Directory {
        private String path;

        private Directory(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        @Override
        public String toString() {
            return "<" + path + ">";
        }
    }

    private class DFile {
        private String path;

        private DFile(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        @Override
        public String toString() {
            return path;
        }
    }
}
