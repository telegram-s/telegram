package org.telegram.android.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.view.MenuItem;
import com.extradea.framework.images.ui.FastWebImageView;
import org.telegram.android.MediaReceiverFragment;
import org.telegram.android.R;
import org.telegram.android.core.ChatSourceListener;
import org.telegram.android.core.files.UploadResult;
import org.telegram.android.core.model.local.TLAbsLocalUserStatus;
import org.telegram.android.core.model.local.TLLocalUserStatusOffline;
import org.telegram.android.core.model.local.TLLocalUserStatusOnline;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.core.model.update.TLLocalAddChatUser;
import org.telegram.android.core.model.update.TLLocalAffectedHistory;
import org.telegram.android.core.model.update.TLLocalRemoveChatUser;
import org.telegram.android.core.model.update.TLLocalUpdateChatPhoto;
import org.telegram.android.media.Optimizer;
import org.telegram.android.core.model.DialogDescription;
import org.telegram.android.core.model.FullChatInfo;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.User;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.tasks.ProgressInterface;
import org.telegram.android.ui.Placeholders;
import org.telegram.android.ui.TextUtil;
import org.telegram.api.*;
import org.telegram.api.engine.RpcException;
import org.telegram.api.messages.TLAbsStatedMessage;
import org.telegram.api.messages.TLAffectedHistory;
import org.telegram.api.messages.TLStatedMessage;
import org.telegram.api.messages.TLStatedMessageLink;
import org.telegram.api.requests.*;
import org.telegram.i18n.I18nUtil;
import org.telegram.mtproto.secure.Entropy;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Author: Korshakov Stepan
 * Created: 05.08.13 20:36
 */
public class EditChatFragment extends MediaReceiverFragment implements ChatSourceListener {

    private static final int PICK_NOTIFICATION_SOUND = 1;

    private int chatId;
    private boolean loadingStarted;
    private FullChatInfo fullChatInfo;
    private DialogDescription dialogDescription;

    private View headerView;
    private TextView titleView;
    private FastWebImageView avatarView;
    private ImageButton changeAvatarView;
    private ImageView enabledView;
    private View notifications;
    private View notificationsSound;
    private TextView notificationSoundTitle;
    private TextView onlineView;
    private TextView membersTitle;

    private ListView listView;
    // private LinearLayout membersContainer;

    private View mediaContainer;
    private TextView mediaCounter;

    private View forbidden;

    private View progress;

    private Pair<Integer, Integer>[] users;

    private int maxChatSize;

    public EditChatFragment(int chatId) {
        this.chatId = chatId;
    }

    public EditChatFragment() {

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            chatId = savedInstanceState.getInt("chatId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            chatId = savedInstanceState.getInt("chatId");
        }

        maxChatSize = application.getKernel().getTechKernel().getSystemConfig().getMaxChatSize();

        View res = inflater.inflate(R.layout.chat_edit, container, false);

        forbidden = res.findViewById(R.id.forbidden);
        progress = res.findViewById(R.id.progress);
        listView = (ListView) res.findViewById(R.id.mainListView);

        View footer = inflater.inflate(R.layout.chat_edit_footer, null);
        footer.findViewById(R.id.leaveChat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                leaveChat();
            }
        });
        footer.findViewById(R.id.addMember).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickUser();
            }
        });

        headerView = inflater.inflate(R.layout.chat_edit_header, null);
        avatarView = (FastWebImageView) headerView.findViewById(R.id.avatar);
        titleView = (TextView) headerView.findViewById(R.id.name);
        onlineView = (TextView) headerView.findViewById(R.id.online);
        enabledView = (ImageView) headerView.findViewById(R.id.notificationsCheck);
        notifications = headerView.findViewById(R.id.notificationsButton);
        notificationsSound = headerView.findViewById(R.id.notificationSound);
        notificationSoundTitle = (TextView) headerView.findViewById(R.id.notificationSoundTitle);
        membersTitle = (TextView) headerView.findViewById(R.id.membersTitle);
        mediaContainer = headerView.findViewById(R.id.mediaContainer);
        mediaCounter = (TextView) headerView.findViewById(R.id.mediaCoutner);
        changeAvatarView = (ImageButton) headerView.findViewById(R.id.changeAvatar);
        headerView.findViewById(R.id.mediaButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRootController().openMedia(PeerType.PEER_CHAT, chatId);
            }
        });
        headerView.findViewById(R.id.editChat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRootController().openChatEditTitle(chatId);
            }
        });

        listView.addHeaderView(headerView, null, false);
        listView.addFooterView(footer, null, false);

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
                hideView(progress);
            }
        });

        progress.setVisibility(View.GONE);


        fullChatInfo = getEngine().getFullChatInfo(chatId);
        dialogDescription = getEngine().getDescriptionForPeer(PeerType.PEER_CHAT, chatId);
        if (fullChatInfo == null) {
            if (!loadingStarted) {
                loadingStarted = true;
                runUiTask(new AsyncAction() {
                    @Override
                    public void execute() throws AsyncException {
                        org.telegram.api.messages.TLChatFull fullChat = rpc(new TLRequestMessagesGetFullChat(chatId));
                        getEngine().onUsers(fullChat.getUsers());
                        getEngine().onChatParticipants(fullChat.getFullChat().getParticipants());
                        fullChatInfo = getEngine().getFullChatInfo(chatId);
                        dialogDescription = getEngine().getDescriptionForPeer(PeerType.PEER_CHAT, chatId);
                    }

                    @Override
                    public void afterExecute() {
                        bindView();
                    }
                });
            }
        } else {
            bindView();
        }
        return res;
    }

    protected int getUserOnlineSorkKey(TLAbsLocalUserStatus status) {
        if (status != null) {
            if (status instanceof TLLocalUserStatusOnline) {
                TLLocalUserStatusOnline online = (TLLocalUserStatusOnline) status;
                if (getServerTime() > online.getExpires()) {
                    return online.getExpires(); // Last seen
                } else {
                    return Integer.MAX_VALUE - 1; // Online
                }
            } else if (status instanceof TLLocalUserStatusOffline) {
                TLLocalUserStatusOffline offline = (TLLocalUserStatusOffline) status;
                return offline.getWasOnline(); // Last seen
            } else {
                return 0; // Offline
            }
        } else {
            return 0; // Offline
        }
    }

    private void bindView() {
        getSherlockActivity().invalidateOptionsMenu();

        if (fullChatInfo.isForbidden()) {
            forbidden.setVisibility(View.VISIBLE);
            return;
        }

        forbidden.setVisibility(View.GONE);

        avatarView.setLoadingDrawable(Placeholders.getGroupPlaceholder(chatId));
        if (dialogDescription.getPhoto() instanceof TLLocalAvatarPhoto) {
            TLLocalAvatarPhoto avatarPhoto = (TLLocalAvatarPhoto) dialogDescription.getPhoto();
            if (avatarPhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                avatarView.requestTask(new StelsImageTask((TLLocalFileLocation) avatarPhoto.getPreviewLocation()));
            } else {
                avatarView.requestTask(null);
            }
        } else {
            avatarView.requestTask(null);
        }

        changeAvatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogDescription.getPhoto() instanceof TLLocalAvatarPhoto) {
                    requestPhotoChooserWithDelete(0);
                } else {
                    requestPhotoChooser(0);
                }
            }
        });

        titleView.setText(dialogDescription.getTitle());
        if (application.getNotificationSettings().isEnabledForChat(chatId)) {
            enabledView.setImageResource(R.drawable.holo_btn_check_on);
        } else {
            enabledView.setImageResource(R.drawable.holo_btn_check_off);
        }

        int mediaCount = getEngine().getMediaCount(PeerType.PEER_CHAT, chatId);
        if (mediaCount == 0) {
            mediaContainer.setVisibility(View.GONE);
        } else {
            mediaContainer.setVisibility(View.VISIBLE);
            mediaCounter.setText(I18nUtil.getInstance().correctFormatNumber(mediaCount));
        }
        notifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (application.getNotificationSettings().isEnabledForChat(chatId)) {
                    application.getNotificationSettings().disableForChat(chatId);
                    enabledView.setImageResource(R.drawable.holo_btn_check_off);
                } else {
                    application.getNotificationSettings().enableForChat(chatId);
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
                if (application.getNotificationSettings().getChatNotificationSound(chatId) != null) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            Uri.parse(application.getNotificationSettings().getChatNotificationSound(chatId)));
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                }
                startActivityForResult(intent, PICK_NOTIFICATION_SOUND);
            }
        });

        updateNotificationSound();

        int onlineCount = 0;
        for (int i = 0; i < fullChatInfo.getUids().length; i++) {
            if (getUserState(getEngine().getUser(fullChatInfo.getUids()[i]).getStatus()) == 0) {
                onlineCount++;
            }
        }
        if (onlineCount == 0) {
            onlineView.setText(getQuantityString(R.plurals.st_members, fullChatInfo.getUids().length)
                    .replace("{d}", I18nUtil.getInstance().correctFormatNumber(fullChatInfo.getUids().length)));
        } else {
            onlineView.setText(Html.fromHtml(
                    getQuantityString(R.plurals.st_members, fullChatInfo.getUids().length)
                            .replace("{d}", I18nUtil.getInstance().correctFormatNumber(fullChatInfo.getUids().length)) + ", <font color='#006FC8'>" + I18nUtil.getInstance().correctFormatNumber(onlineCount) + " " + getStringSafe(R.string.st_online) + "</font>"));
        }

        membersTitle.setText(getQuantityString(R.plurals.st_members_caps, fullChatInfo.getUids().length)
                .replace("{d}", I18nUtil.getInstance().correctFormatNumber(fullChatInfo.getUids().length) + ""));
        final Context context = getActivity();
        users = new Pair[fullChatInfo.getUids().length];
        for (int i = 0; i < users.length; i++) {
            users[i] = new Pair<Integer, Integer>(fullChatInfo.getUids()[i], fullChatInfo.getInviters()[i]);
        }
        Arrays.sort(users, new Comparator<Pair<Integer, Integer>>() {
            @Override
            public int compare(Pair<Integer, Integer> a, Pair<Integer, Integer> b) {
                return -(getUserOnlineSorkKey(getEngine().getUser(a.first).getStatus())
                        - getUserOnlineSorkKey(getEngine().getUser(b.first).getStatus()));
            }
        });
        listView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return users.length;
            }

            @Override
            public User getItem(int i) {
                return application.getEngine().getUser(users[i].first);
            }

            @Override
            public long getItemId(int i) {
                return getItem(i).getUid();
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @Override
            public View getView(int i, View row, ViewGroup viewGroup) {
                if (row == null) {
                    row = View.inflate(context, R.layout.chat_edit_item, null);
                }
                final User user = getItem(i);

                FastWebImageView imageView = (FastWebImageView) row.findViewById(R.id.avatar);
                imageView.setLoadingDrawable(Placeholders.USER_PLACEHOLDERS[user.getUid() % Placeholders.USER_PLACEHOLDERS.length]);

                if (user.getPhoto() instanceof TLLocalAvatarPhoto) {
                    TLLocalAvatarPhoto avatarPhoto = (TLLocalAvatarPhoto) user.getPhoto();
                    if (avatarPhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                        imageView.requestTask(new StelsImageTask((TLLocalFileLocation) avatarPhoto.getPreviewLocation()));
                    } else {
                        imageView.requestTask(null);
                    }
                } else {
                    imageView.requestTask(null);
                }

                ((TextView) row.findViewById(R.id.name)).setText(user.getFirstName() + " " + user.getLastName());

                int status = getUserState(user.getStatus());
                if (status < 0) {
                    ((TextView) row.findViewById(R.id.status)).setText(R.string.st_offline);
                    ((TextView) row.findViewById(R.id.status)).setTextColor(context.getResources().getColor(R.color.st_grey_text));
                } else if (status == 0) {
                    ((TextView) row.findViewById(R.id.status)).setText(R.string.st_online);
                    ((TextView) row.findViewById(R.id.status)).setTextColor(context.getResources().getColor(R.color.st_blue_bright));
                } else {
                    ((TextView) row.findViewById(R.id.status)).setTextColor(context.getResources().getColor(R.color.st_grey_text));
                    ((TextView) row.findViewById(R.id.status)).setText(
                            TextUtil.formatHumanReadableLastSeen(status, getStringSafe(R.string.st_lang)));
                }

                return row;
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) {
                    return true;
                }
                int uid = users[i - 1].first;
                int inviter = users[i - 1].second;
                final User user = application.getEngine().getUser(uid);
                if (uid == application.getCurrentUid()) {
                    AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).setItems(new CharSequence[]{getStringSafe(R.string.st_edit_dialog_action_leave)}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            leaveChat();
                        }
                    }).create();
                    alertDialog.setCanceledOnTouchOutside(true);
                    alertDialog.show();
                } else {
                    CharSequence[] items;
                    if (fullChatInfo.getAdminId() == application.getCurrentUid() || inviter == application.getCurrentUid()) {
                        items = new CharSequence[]{
                                getStringSafe(R.string.st_edit_dialog_action_view).replace("{0}", user.getFirstName()),
                                getStringSafe(R.string.st_edit_dialog_action_dialog).replace("{0}", user.getFirstName()),
                                getStringSafe(R.string.st_edit_dialog_action_remove).replace("{0}", user.getFirstName()),
                        };
                    } else {
                        items = new CharSequence[]{
                                getStringSafe(R.string.st_edit_dialog_action_view).replace("{0}", user.getFirstName()),
                                getStringSafe(R.string.st_edit_dialog_action_dialog).replace("{0}", user.getFirstName())
                        };
                    }
                    AlertDialog dialog = new AlertDialog.Builder(getActivity()).setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (i == 0) {
                                getRootController().openUser(user.getUid());
                            } else if (i == 1) {
                                getRootController().openDialog(PeerType.PEER_USER, user.getUid());
                            } else {
                                removeUser(user.getUid());
                            }
                        }
                    }).create();
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.show();
                }
                return true;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                int uid = users[i - 1].first;
                final User user = application.getEngine().getUser(uid);
                getRootController().openDialog(PeerType.PEER_USER, user.getUid());
            }
        });
    }

    private void leaveChat() {
        AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_edit_dialog_delete_title)
                .setMessage(R.string.st_edit_dialog_delete_message)
                .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        runUiTask(new AsyncAction() {
                            @Override
                            public void execute() throws AsyncException {
                                int offset = 0;
                                do {
                                    TLAffectedHistory affectedHistory = rpc(new TLRequestMessagesDeleteHistory(new TLInputPeerChat(chatId), offset));
                                    application.getUpdateProcessor().onMessage(new TLLocalAffectedHistory(affectedHistory));
                                    offset = affectedHistory.getOffset();
                                } while (offset != 0);

                                try {
                                    rpcRaw(new TLRequestMessagesDeleteChatUser(chatId, new TLInputUserSelf()));
                                } catch (RpcException e) {
                                    e.printStackTrace();
                                    return;
                                }

                                getEngine().deleteHistory(PeerType.PEER_CHAT, chatId);

                                application.notifyUIUpdate();
                            }

                            @Override
                            public void afterExecute() {
                                Toast.makeText(getActivity(), R.string.st_edit_dialog_left, Toast.LENGTH_SHORT).show();
                                getRootController().popFragment(2);
                            }
                        });
                    }
                })
                .setNegativeButton(R.string.st_no, null).create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private void updateNotificationSound() {
        String title = application.getNotificationSettings().getChatNotificationSoundTitle(chatId);
        if (title == null) {
            notificationSoundTitle.setText(R.string.st_default);
        } else {
            notificationSoundTitle.setText(title);
        }
    }

    private void removeUser(final int uid) {
        runUiTask(new AsyncAction() {
            @Override
            public void execute() throws AsyncException {
                TLAbsStatedMessage message = rpc(new TLRequestMessagesDeleteChatUser(chatId, new TLInputUserContact(uid)));
                TLMessageService service = (TLMessageService) message.getMessage();
                TLMessageActionChatDeleteUser removeUser = (TLMessageActionChatDeleteUser) service.getAction();
                ArrayList<TLAbsMessage> messages = new ArrayList<TLAbsMessage>();
                messages.add(message.getMessage());
                application.getEngine().onNewMessages(messages, message.getUsers(), message.getChats(), new ArrayList<TLDialog>());
                application.getEngine().onChatUserRemoved(chatId, removeUser.getUserId());
                application.getUpdateProcessor().onMessage(new TLLocalRemoveChatUser(message));
            }

            @Override
            public void afterExecute() {
                Toast.makeText(getActivity(), R.string.st_edit_dialog_removed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onPhotoArrived(Uri uri, int width, int height, int requestId) {
        if (cropSupported(uri)) {
            requestCrop(uri, 200, 200, 0);
        } else {
            uploadPhoto(null, uri);
        }
    }

    @Override
    protected void onPhotoArrived(final String fileName, int width, int height, int requestId) {
        if (cropSupported(Uri.fromFile(new File(fileName)))) {
            requestCrop(fileName, 200, 200, 0);
        } else {
            uploadPhoto(fileName, null);
        }
    }

    @Override
    protected void onPhotoCropped(Uri uri, int requestId) {
        uploadPhoto(null, uri);
    }

    @Override
    protected void onPhotoDeleted(int requestId) {
        uploadPhoto(null, null);
    }

    private void uploadPhoto(final String fileName, final Uri content) {
        runUiTask(new AsyncAction() {
            @Override
            public void execute() throws AsyncException {
                InputStream stream = null;
                try {
                    if (fileName != null || content != null) {
                        long fileId = Entropy.generateRandomId();
                        String destFile = getUploadTempFile();
                        if (fileName != null) {
                            Optimizer.optimize(fileName, destFile);
                        } else {
                            Optimizer.optimize(content.toString(), application, destFile);
                        }
                        File file = new File(destFile);
                        int len = (int) file.length();
                        stream = new FileInputStream(file);
                        UploadResult res = application.getUploadController().uploadFile(stream, len, fileId);
                        if (res == null)
                            throw new AsyncException(AsyncException.ExceptionType.CONNECTION_ERROR);

                        TLAbsStatedMessage message = rpc(new TLRequestMessagesEditChatPhoto(chatId,
                                new TLInputChatUploadedPhoto(
                                        new TLInputFile(fileId, res.getPartsCount(), "photo.jpg", res.getHash()),
                                        new TLInputPhotoCropAuto())));
                        TLMessageService service = (TLMessageService) message.getMessage();
                        TLMessageActionChatEditPhoto editPhoto = (TLMessageActionChatEditPhoto) service.getAction();

                        ArrayList<TLAbsMessage> messages = new ArrayList<TLAbsMessage>();
                        messages.add(message.getMessage());
                        application.getEngine().onNewMessages(messages, message.getUsers(), message.getChats(), new ArrayList<TLDialog>());
                        application.getEngine().onChatAvatarChanges(chatId, editPhoto.getPhoto());
                        application.getUpdateProcessor().onMessage(new TLLocalUpdateChatPhoto(message));
                    } else {
                        TLAbsStatedMessage message = rpc(new TLRequestMessagesEditChatPhoto(chatId, new TLInputChatPhotoEmpty()));
                        ArrayList<TLAbsMessage> messages = new ArrayList<TLAbsMessage>();
                        messages.add(message.getMessage());
                        application.getEngine().onNewMessages(messages, message.getUsers(), message.getChats(), new ArrayList<TLDialog>());
                        application.getEngine().onChatAvatarChanges(chatId, null);
                        application.getUpdateProcessor().onMessage(new TLLocalUpdateChatPhoto(message));
                    }
                    application.getChatSource().notifyChatChanged(chatId);
                    application.getDialogSource().getViewSource().invalidateData();
                    application.notifyUIUpdate();
                } catch (RpcException e) {
                    e.printStackTrace();
                    throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
                } catch (FileNotFoundException e) {
                    throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
                } catch (IOException e) {
                    throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void afterExecute() {
                if (fileName != null || content != null) {
                    Toast.makeText(getActivity(), R.string.st_avatar_group_changed, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), R.string.st_avatar_group_removed, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onCreateOptionsMenuChecked(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        if (fullChatInfo != null && !fullChatInfo.isForbidden()) {
            inflater.inflate(R.menu.edit_chat, menu);
        }
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_edit_dialog_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.addMember) {
            pickUser();
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
                @Override
                public void execute() throws AsyncException {
                    TLAbsStatedMessage message = rpc(new TLRequestMessagesAddChatUser(chatId, new TLInputUserContact(user.getUid()), 10));
                    TLMessageService service = (TLMessageService) message.getMessage();
                    TLMessageActionChatAddUser addUser = (TLMessageActionChatAddUser) service.getAction();
                    ArrayList<TLAbsMessage> messages = new ArrayList<TLAbsMessage>();
                    messages.add(message.getMessage());
                    application.getEngine().onNewMessages(messages, message.getUsers(), message.getChats(), new ArrayList<TLDialog>());
                    application.getEngine().onChatUserAdded(chatId, service.getFromId(), addUser.getUserId());
                    application.getUpdateProcessor().onMessage(new TLLocalAddChatUser(message));
                }

                @Override
                public void afterExecute() {
                    Toast.makeText(getActivity(), R.string.st_edit_dialog_added, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_NOTIFICATION_SOUND && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), uri);
                String title = ringtone.getTitle(getActivity());
                application.getNotificationSettings().setChatNotificationSound(chatId, uri.toString(), title);
            } else {
                application.getNotificationSettings().setChatNotificationSound(chatId, null, null);
            }
            updateNotificationSound();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        titleView = null;
        avatarView = null;
        enabledView = null;
        notifications = null;
        notificationsSound = null;
        notificationSoundTitle = null;
        onlineView = null;
        membersTitle = null;
        mediaContainer = null;
        mediaCounter = null;
    }

    @Override
    public void onChatChanged(int chatId, DialogDescription description) {
        if (chatId == this.chatId) {
            fullChatInfo = getEngine().getFullChatInfo(chatId);
            dialogDescription = getEngine().getDescriptionForPeer(PeerType.PEER_CHAT, chatId);
            if (fullChatInfo != null && dialogDescription != null) {
                bindView();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        maxChatSize = application.getKernel().getTechKernel().getSystemConfig().getMaxChatSize();
        application.getChatSource().registerListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        application.getChatSource().unregisterListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("chatId", chatId);
    }
}
