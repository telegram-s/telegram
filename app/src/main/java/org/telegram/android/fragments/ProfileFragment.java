package org.telegram.android.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.MenuItem;
import com.extradea.framework.images.ui.FastWebImageView;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;
import org.telegram.android.core.UserSourceListener;
import org.telegram.android.core.model.Contact;
import org.telegram.android.core.model.LinkType;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.tasks.ProgressInterface;
import org.telegram.android.ui.Placeholders;
import org.telegram.android.ui.TextUtil;
import org.telegram.api.*;
import org.telegram.api.engine.RpcException;
import org.telegram.api.engine.TimeoutException;
import org.telegram.api.requests.TLRequestContactsBlock;
import org.telegram.api.requests.TLRequestUsersGetFullUser;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Author: Korshakov Stepan
 * Created: 05.08.13 11:01
 */
public class ProfileFragment extends StelsFragment implements UserSourceListener {

    private static final int PICK_NOTIFICATION_SOUND = 1;

    private int userId;
    // private TLUserFull user;
    private User user;
    private boolean loadingStarted;

    private View progress;
    private TextView nameView;
    private TextView onlineView;
    private FastWebImageView avatarView;
    private TextView phoneView;
    private ImageView enabledView;
    private View notifications;
    private View notificationsSound;
    private TextView notificationSoundTitle;
    private View mediaContainer;
    private TextView mediaCounter;
    private View mainContainer;

    public ProfileFragment(int userId) {
        this.userId = userId;
    }

    public ProfileFragment() {

    }

    public int getUserId() {
        return userId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            userId = savedInstanceState.getInt("userId");
        }
        View res = inflater.inflate(R.layout.profile_fragment, container, false);
        mainContainer = res.findViewById(R.id.mainContainer);
        progress = res.findViewById(R.id.progress);
        avatarView = (FastWebImageView) res.findViewById(R.id.avatar);
        nameView = (TextView) res.findViewById(R.id.name);
        onlineView = (TextView) res.findViewById(R.id.online);
        phoneView = (TextView) res.findViewById(R.id.phone);
        enabledView = (ImageView) res.findViewById(R.id.notificationsCheck);
        notifications = res.findViewById(R.id.notificationsButton);
        notificationsSound = res.findViewById(R.id.notificationSound);
        notificationSoundTitle = (TextView) res.findViewById(R.id.notificationSoundTitle);
        mediaContainer = res.findViewById(R.id.mediaContainer);
        mediaCounter = (TextView) res.findViewById(R.id.mediaCoutner);

        if (application.isRTL()) {
            nameView.setGravity(Gravity.RIGHT);
            phoneView.setGravity(Gravity.RIGHT);
            ((TextView) res.findViewById(R.id.secretChatTitle)).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.st_ic_lock_gray, 0);
        } else {
            nameView.setGravity(Gravity.LEFT);
            phoneView.setGravity(Gravity.LEFT);
            ((TextView) res.findViewById(R.id.secretChatTitle)).setCompoundDrawablesWithIntrinsicBounds(R.drawable.st_ic_lock_gray, 0, 0, 0);
        }

        res.findViewById(R.id.mediaButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRootController().openMedia(PeerType.PEER_USER, userId);
            }
        });
        res.findViewById(R.id.sendMessage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRootController().openDialog(PeerType.PEER_USER, userId);
            }
        });

        res.findViewById(R.id.secureChat).setVisibility(View.VISIBLE);
        res.findViewById(R.id.secureChatDiv).setVisibility(View.VISIBLE);

        res.findViewById(R.id.secureChat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runUiTask(new AsyncAction() {
                    private int chatId;

                    @Override
                    public void execute() throws AsyncException {
                        try {
                            chatId = application.getEncryptionController().requestEncryption(userId);
                        } catch (RpcException e) {
                            throw new AsyncException(e);
                        } catch (TimeoutException e) {
                            throw new AsyncException(AsyncException.ExceptionType.CONNECTION_ERROR);
                        } catch (IOException e) {
                            throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
                        }
                    }

                    @Override
                    public void afterExecute() {
                        getRootController().openDialog(PeerType.PEER_USER_ENCRYPTED, chatId);
                    }
                });
            }
        });

        setDefaultProgressInterface(new ProgressInterface() {
            @Override
            public void showContent() {

            }

            @Override
            public void hideContent() {
            }

            @Override
            public void showProgress() {
                showView(progress);
            }

            @Override
            public void hideProgress() {
                goneView(progress);
            }
        });
        progress.setVisibility(View.GONE);

        user = getEngine().getUser(userId);
        if (user == null) {
            if (!loadingStarted) {
                loadingStarted = true;
                runUiTask(new AsyncAction() {
                    @Override
                    public void execute() throws AsyncException {
                        try {
                            TLUserFull tlUser = application.getApi().doRpcCall(new TLRequestUsersGetFullUser(new TLInputUserContact(userId)));
                            ArrayList<TLAbsUser> users = new ArrayList<TLAbsUser>();
                            users.add(tlUser.getUser());
                            getEngine().onUsers(users);
                            user = getEngine().getUser(userId);
                        } catch (RpcException e) {
                            throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
                        } catch (TimeoutException e) {
                            throw new AsyncException(AsyncException.ExceptionType.CONNECTION_ERROR);
                        } catch (IOException e) {
                            throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
                        }
                    }

                    @Override
                    public void afterExecute() {
                        bindUi();
                    }
                });
            }
        } else {
            bindUi();
        }

        return res;
    }

    private void bindUi() {
        if (user.getPhoto() instanceof TLLocalAvatarPhoto && ((TLLocalAvatarPhoto) user.getPhoto()).getPreviewLocation() instanceof TLLocalFileLocation) {
            avatarView.setLoadingDrawable(Placeholders.getUserPlaceholder(userId));
            avatarView.requestTask(new StelsImageTask((TLLocalFileLocation) ((TLLocalAvatarPhoto) user.getPhoto()).getPreviewLocation()));
            if (((TLLocalAvatarPhoto) user.getPhoto()).getFullLocation() instanceof TLLocalFileLocation) {
                avatarView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getRootController().openImage((TLLocalFileLocation) ((TLLocalAvatarPhoto) user.getPhoto()).getFullLocation());
                    }
                });
            } else {
                avatarView.setOnClickListener(null);
            }
        } else {
            avatarView.setLoadingDrawable(Placeholders.getUserPlaceholder(userId));
            avatarView.requestTask(null);
            avatarView.setOnClickListener(null);
        }

        String name = user.getFirstName() + " " + user.getLastName();
        final String phone = user.getPhone();
        int statusValue = getUserState(user.getStatus());
        if (statusValue < 0) {
            onlineView.setText(R.string.st_offline);
            onlineView.setTextColor(getResources().getColor(R.color.st_grey_text));
        } else if (statusValue == 0) {
            onlineView.setText(R.string.st_online);
            onlineView.setTextColor(getResources().getColor(R.color.st_blue_bright));
        } else {
            onlineView.setTextColor(getResources().getColor(R.color.st_grey_text));
            onlineView.setText(TextUtil.formatHumanReadableLastSeen(statusValue, getStringSafe(R.string.st_lang)));
        }

        nameView.setText(name);
        if (phone != null && phone.length() > 0 && !phone.equals("null")) {
            phoneView.setText(unicodeWrap(TextUtil.formatPhone(phone)));
            phoneView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    copyToPastebin("+" + phone);
                    Toast.makeText(getActivity(), R.string.st_profile_phone_copied, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        } else {
            phoneView.setText(R.string.st_profile_phone_hidden);
            phoneView.setOnLongClickListener(null);
        }

        int mediaCount = getEngine().getMediaCount(PeerType.PEER_USER, userId);
        if (mediaCount == 0) {
            mediaContainer.setVisibility(View.GONE);
        } else {
            mediaContainer.setVisibility(View.VISIBLE);
            mediaCounter.setText("" + mediaCount);
        }

        if (application.getNotificationSettings().isEnabledForUser(userId)) {
            enabledView.setImageResource(R.drawable.holo_btn_check_on);
        } else {
            enabledView.setImageResource(R.drawable.holo_btn_check_off);
        }

        notifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (application.getNotificationSettings().isEnabledForUser(userId)) {
                    application.getNotificationSettings().disableForUser(userId);
                    enabledView.setImageResource(R.drawable.holo_btn_check_off);
                } else {
                    application.getNotificationSettings().enableForUser(userId);
                    enabledView.setImageResource(R.drawable.holo_btn_check_on);
                }
            }
        });

        notificationsSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getStringSafe(R.string.st_select_tone));
                if (application.getNotificationSettings().getUserNotificationSound(userId) != null) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            Uri.parse(application.getNotificationSettings().getUserNotificationSound(userId)));
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                }
                startActivityForResult(intent, PICK_NOTIFICATION_SOUND);
            }
        });

        updateNotificationSound();
    }

    private void updateNotificationSound() {
        if (notificationSoundTitle != null) {
            String title = application.getNotificationSettings().getUserNotificationSoundTitle(userId);
            if (title == null) {
                notificationSoundTitle.setText(R.string.st_default);
            } else {
                notificationSoundTitle.setText(title);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        application.getUserSource().registerListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        application.getUserSource().unregisterListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_NOTIFICATION_SOUND && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), uri);
                String title = ringtone.getTitle(getActivity());
                application.getNotificationSettings().setUserNotificationSound(userId, uri.toString(), title);
            } else {
                application.getNotificationSettings().setUserNotificationSound(userId, null, null);
            }
            updateNotificationSound();
        }
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.user_menu, menu);
        menu.findItem(R.id.blockUserMenu).setTitle(highlightMenuText(R.string.st_profile_menu_block));
        menu.findItem(R.id.addContact).setTitle(highlightMenuText(R.string.st_profile_menu_add));
        if (application.getEngine().getUsersEngine().getContactsForUid(userId).length > 0) {
            menu.findItem(R.id.addContact).setVisible(false);
            if (!application.getTechKernel().getTechReflection().isOnSdCard()) {
                menu.findItem(R.id.viewBook).setVisible(true);
                menu.findItem(R.id.viewBook).setTitle(highlightMenuText(R.string.st_profile_menu_view));
            } else {
                menu.findItem(R.id.viewBook).setVisible(false);
            }
        } else {
            menu.findItem(R.id.viewBook).setVisible(false);
            if (user != null && user.getPhone() != null && user.getPhone().length() > 0 && user.getLinkType() != LinkType.CONTACT) {
                menu.findItem(R.id.addContact).setVisible(true);
            } else {
                menu.findItem(R.id.addContact).setVisible(false);
            }
        }
        if (user != null && user.getPhone() != null && user.getPhone().length() > 0) {
            menu.findItem(R.id.shareContact).setVisible(true);
            menu.findItem(R.id.shareContact).setTitle(highlightMenuText(R.string.st_profile_menu_share));
        } else {
            menu.findItem(R.id.shareContact).setVisible(false);
        }
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_profile_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.blockUserMenu) {
            runUiTask(new AsyncAction() {
                @Override
                public void execute() throws AsyncException {
                    try {
                        application.getApi().doRpcCall(new TLRequestContactsBlock(new TLInputUserForeign(user.getUid(), user.getAccessHash())));
                    } catch (RpcException e) {
                        e.printStackTrace();
                        throw new AsyncException(e);
                    } catch (TimeoutException e) {
                        throw new AsyncException(AsyncException.ExceptionType.CONNECTION_ERROR);
                    } catch (IOException e) {
                        throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
                    }
                }

                @Override
                public void afterExecute() {
                    Toast.makeText(getActivity(), R.string.st_profile_blocked, Toast.LENGTH_SHORT).show();
                }
            });
            return true;
        } else if (item.getItemId() == R.id.viewBook) {
            final long[] contacts = application.getEngine().getUsersEngine().getContactsForUid(userId);
            if (contacts.length == 1) {
                startActivity(new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contacts[0] + "")));
            } else {
                CharSequence[] sequences = new CharSequence[contacts.length];
                for (int i = 0; i < contacts.length; i++) {
                    User user = getEngine().getUser(userId);
                    sequences[i] = user.getDisplayName();
                }
                AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).setItems(sequences, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(Intent.ACTION_VIEW)
                                .setData(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contacts[i] + "")));
                    }
                }).create();
                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();
            }
            return true;
        } else if (item.getItemId() == R.id.addContact) {
            getRootController().addContact(userId);
        } else if (item.getItemId() == R.id.shareContact) {
            getRootController().shareContact(userId);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("userId", userId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        progress = null;
        nameView = null;
        onlineView = null;
        avatarView = null;
        phoneView = null;
        enabledView = null;
        notifications = null;
        notificationSoundTitle = null;
        notificationsSound = null;
    }

    @Override
    public void onUsersChanged(User[] users) {
        for (User u : users) {
            if (u.getUid() == userId) {
                this.user = u;
                bindUi();
                return;
            }
        }
    }
}
