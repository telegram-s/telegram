package org.telegram.android.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;

/**
 * Created by ex3ndr on 05.01.14.
 */
public class SettingsSecurityFragment extends StelsFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.settings_security, container, false);
        return res;
    }

    @Override
    public void onCreateOptionsMenuChecked(Menu menu, MenuInflater inflater) {
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_settings_security_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
    }
}
