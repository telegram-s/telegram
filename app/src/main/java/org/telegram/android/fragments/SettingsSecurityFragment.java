package org.telegram.android.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;
import org.telegram.android.config.SecuritySettings;
import org.telegram.android.ui.TextUtil;

/**
 * Created by ex3ndr on 05.01.14.
 */
public class SettingsSecurityFragment extends StelsFragment {

    private TextView lockTypeTitle;
    private TextView lockTimeoutTitle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.settings_security, container, false);
        lockTypeTitle = (TextView) res.findViewById(R.id.lockTypeTitle);
        lockTimeoutTitle = (TextView) res.findViewById(R.id.lockTimeoutTitle);

        res.findViewById(R.id.changeLock).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int[] ids = new int[]{
                        SecuritySettings.LOCK_TYPE_DEBUG,
                        SecuritySettings.LOCK_TYPE_NONE
                };
                CharSequence[] titles = new CharSequence[]{
                        "Debug",
                        "None"
                };
                AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setItems(titles, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                application.getSettingsKernel().getSecuritySettings().setLockType(ids[i]);
                                bindUi();
                            }
                        }).create();
                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();
            }
        });

        res.findViewById(R.id.keepUnlocked).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int[] ids = new int[]{
                        60,
                        5 * 60,
                        15 * 60,
                        30 * 60,
                        60 * 60,
                        8 * 60 * 60,
                        0
                };
                CharSequence[] titles = new CharSequence[]{
                        "1 Minute",
                        "5 Minutes",
                        "15 Minutes",
                        "30 Minutes",
                        "1 Hour",
                        "8 Hours",
                        "None"
                };
                AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setItems(titles, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                application.getSettingsKernel().getSecuritySettings().setLockTimeout(ids[i]);
                                bindUi();
                            }
                        }).create();
                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();
            }
        });

        bindUi();
        return res;
    }

    private void bindUi() {
        switch (application.getSettingsKernel().getSecuritySettings().getLockType()) {
            default:
            case SecuritySettings.LOCK_TYPE_NONE:
                lockTypeTitle.setText(R.string.st_settings_security_lock_disabled);
                break;
            case SecuritySettings.LOCK_TYPE_DEBUG:
                lockTypeTitle.setText(R.string.st_settings_security_lock_debug);
        }

        if (application.getSettingsKernel().getSecuritySettings().getLockTimeout() <= 0) {
            lockTimeoutTitle.setText(R.string.st_settings_security_lock_none);
        } else {
            lockTimeoutTitle.setText(
                    getStringSafe(R.string.st_settings_security_lock_template)
                            .replace("{time}", TextUtil.formatHumanReadableDuration(
                                    application.getSettingsKernel().getSecuritySettings().getLockTimeout())));
        }
    }

    @Override
    public void onCreateOptionsMenuChecked(Menu menu, MenuInflater inflater) {
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_settings_security_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
    }
}
