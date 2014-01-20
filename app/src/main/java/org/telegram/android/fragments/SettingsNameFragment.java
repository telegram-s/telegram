package org.telegram.android.fragments;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import org.telegram.android.R;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.core.model.User;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.api.TLAbsUser;
import org.telegram.api.engine.RpcException;
import org.telegram.api.requests.TLRequestAccountUpdateProfile;

import java.util.ArrayList;

/**
 * Author: Korshakov Stepan
 * Created: 16.08.13 18:23
 */
public class SettingsNameFragment extends TelegramFragment {

    private int uid;
    private User user;

    private EditText firstName;
    private EditText lastName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.settings_name, container, false);
        firstName = (EditText) res.findViewById(R.id.firstName);
        lastName = (EditText) res.findViewById(R.id.lastName);
        lastName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    onApplyName();
                    return true;
                }
                return false;
            }
        });

        if (savedInstanceState == null) {
            User user = getEngine().getUser(application.getCurrentUid());
            if (user != null) {
                firstName.setText(user.getFirstName());
                lastName.setText(user.getLastName());
            } else {
                firstName.setText("");
                lastName.setText("");
            }
        }

        return res;
    }

    @Override
    public void onResume() {
        super.onResume();
        showKeyboard(firstName);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSherlockActivity().getSupportActionBar().setCustomView(R.layout.settings_name_bar);
        getSherlockActivity().getSupportActionBar().getCustomView().findViewById(R.id.dialogCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancel();
            }
        });
        getSherlockActivity().getSupportActionBar().getCustomView().findViewById(R.id.dialogDone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onApplyName();
            }
        });
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
    }

    private void onCancel() {
        hideKeyboard(firstName);
        hideKeyboard(lastName);
        getActivity().onBackPressed();
    }

    private void onApplyName() {
        final String newFirstName = firstName.getText().toString().trim();
        final String newLastName = lastName.getText().toString().trim();

        if (newFirstName.length() == 0) {
            Toast.makeText(getActivity(), R.string.st_settings_name_empty_first, Toast.LENGTH_SHORT).show();
            return;
        }

        if (newLastName.length() == 0) {
            Toast.makeText(getActivity(), R.string.st_settings_name_empty_last, Toast.LENGTH_SHORT).show();
            return;
        }

        runUiTask(new AsyncAction() {
            private boolean changed = true;

            @Override
            public void execute() throws AsyncException {
                try {
                    TLAbsUser user = rpcRaw(new TLRequestAccountUpdateProfile(newFirstName, newLastName));
                    ArrayList<TLAbsUser> users = new ArrayList<TLAbsUser>();
                    users.add(user);
                    application.getEngine().onUsers(users);
                } catch (RpcException e) {
                    e.printStackTrace();
                    if (!"NAME_NOT_MODIFIED".equals(e.getErrorTag())) {
                        throw new AsyncException(e);
                    } else {
                        changed = false;
                    }
                }
            }

            @Override
            public void afterExecute() {
                super.afterExecute();
                if (changed) {
                    Toast.makeText(getActivity(), R.string.st_settings_name_changed, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), R.string.st_settings_name_not_changed, Toast.LENGTH_SHORT).show();
                }
                hideKeyboard(firstName);
                hideKeyboard(lastName);
                getActivity().onBackPressed();
            }
        });
    }
}
