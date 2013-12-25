package org.telegram.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.extradea.framework.images.ui.FastWebImageView;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;
import org.telegram.android.core.EngineUtils;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.tasks.ProgressInterface;
import org.telegram.android.ui.Placeholders;
import org.telegram.api.TLInputUserForeign;
import org.telegram.api.contacts.TLAbsBlocked;
import org.telegram.api.requests.TLRequestContactsBlock;
import org.telegram.api.requests.TLRequestContactsGetBlocked;
import org.telegram.api.requests.TLRequestContactsUnblock;

import java.util.List;

/**
 * Author: Korshakov Stepan
 * Created: 18.08.13 17:32
 */
public class BlockedFragment extends StelsFragment {
    private View loading;
    private View empty;
    private ListView usersList;
    private User[] users = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.blocked_list, container, false);
        loading = res.findViewById(R.id.loading);
        empty = res.findViewById(R.id.empty);
        usersList = (ListView) res.findViewById(R.id.usersList);
        View bottomPadding = View.inflate(getActivity(), R.layout.blocked_bottom, null);
        usersList.addFooterView(bottomPadding, null, false);
        usersList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                Object item = adapterView.getItemAtPosition(i);
                if (item instanceof User) {
                    final User u = (User) item;
                    runUiTask(new AsyncAction() {
                        @Override
                        public void execute() throws AsyncException {
                            rpc(new TLRequestContactsUnblock(new TLInputUserForeign(u.getUid(), u.getAccessHash())));
                        }

                        @Override
                        public void afterExecute() {
                            Toast.makeText(getActivity(), R.string.st_blocked_unblocked, Toast.LENGTH_SHORT).show();
                            loadUsers();
                        }
                    });
                }

                return true;
            }
        });
        empty.setVisibility(View.GONE);
        setDefaultProgressInterface(new ProgressInterface() {
            @Override
            public void showContent() {

            }

            @Override
            public void hideContent() {

            }

            @Override
            public void showProgress() {
                showView(loading, false);
            }

            @Override
            public void hideProgress() {
                goneView(loading, false);
            }
        });
        check();
        return res;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.blocked_menu, menu);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_blocked_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.addUser) {
            pickUserAll();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onFragmentResult(int resultCode, Object data) {
        if (resultCode == Activity.RESULT_OK) {
            Integer uid = (Integer) data;
            final User user = application.getEngine().getUser(uid);
            runUiTask(new AsyncAction() {
                User[] nUsers;

                @Override
                public void execute() throws AsyncException {
                    rpc(new TLRequestContactsBlock(new TLInputUserForeign(user.getUid(), user.getAccessHash())));
                    nUsers = requestForUsers();
                }

                @Override
                public void afterExecute() {
                    Toast.makeText(getActivity(), R.string.st_blocked_blocked, Toast.LENGTH_SHORT).show();
                    users = nUsers;
                    bindUi();
                }
            });
        }
    }

    private User[] requestForUsers() throws AsyncException {
        TLAbsBlocked blocked = rpc(new TLRequestContactsGetBlocked(0, 300));

        User[] nUsers = new User[blocked.getUsers().size()];
        for (int i = 0; i < blocked.getUsers().size(); i++) {
            nUsers[i] = EngineUtils.userFromTlUser(blocked.getUsers().get(i));
        }
        return nUsers;
    }

    private void loadUsers() {
        runUiTask(new AsyncAction() {

            private User[] nUsers;

            @Override
            public void execute() throws AsyncException {
                nUsers = requestForUsers();
            }

            @Override
            public void afterExecute() {
                users = nUsers;
                bindUi();
            }
        });
    }

    private void check() {
        if (users == null) {
            loadUsers();
        } else {
            bindUi();
        }
    }

    private void bindUi() {
        loading.setVisibility(View.GONE);
        if (users.length == 0) {
            empty.setVisibility(View.VISIBLE);
            usersList.setVisibility(View.GONE);
        } else {
            empty.setVisibility(View.GONE);
            usersList.setVisibility(View.VISIBLE);

            final Context context = getActivity();
            usersList.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return users.length;
                }

                @Override
                public User getItem(int i) {
                    return users[i];
                }

                @Override
                public long getItemId(int i) {
                    return getItem(i).getUid();
                }

                @Override
                public View getView(int i, View view, ViewGroup viewGroup) {
                    if (view == null) {
                        view = View.inflate(context, R.layout.blocked_item, null);
                    }
                    User object = getItem(i);

                    ((TextView) view.findViewById(R.id.name)).setText(object.getDisplayName());

                    FastWebImageView imageView = (FastWebImageView) view.findViewById(R.id.avatar);
                    imageView.setLoadingDrawable(Placeholders.USER_PLACEHOLDERS[object.getUid() % Placeholders.USER_PLACEHOLDERS.length]);
                    if (object.getPhoto() instanceof TLLocalAvatarPhoto) {
                        TLLocalAvatarPhoto userPhoto = (TLLocalAvatarPhoto) object.getPhoto();
                        if (userPhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                            imageView.requestTask(new StelsImageTask((TLLocalFileLocation) userPhoto.getPreviewLocation()));
                        } else {
                            imageView.requestTask(null);
                        }
                    } else {
                        imageView.requestTask(null);
                    }

                    if (object.getPhone() == null || object.getPhone().trim().length() == 0) {
                        ((TextView) view.findViewById(R.id.phone)).setText(R.string.st_blocked_phone_hidden);
                    } else {
                        ((TextView) view.findViewById(R.id.phone)).setText(org.telegram.android.ui.TextUtil.formatPhone(object.getPhone()));
                    }
                    return view;
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        empty = null;
        usersList = null;
        loading = null;
    }
}