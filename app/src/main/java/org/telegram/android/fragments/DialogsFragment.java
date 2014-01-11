package org.telegram.android.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.*;
import com.actionbarsherlock.view.MenuItem;
import com.extradea.framework.images.ui.FastWebImageView;
import com.nineoldandroids.animation.ObjectAnimator;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;
import org.telegram.android.config.DebugSettings;
import org.telegram.android.core.DialogSource;
import org.telegram.android.core.DialogSourceState;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.media.*;
import org.telegram.android.core.model.update.TLLocalAffectedHistory;
import org.telegram.android.core.wireframes.DialogWireframe;
import org.telegram.android.log.Logger;
import org.telegram.android.cursors.ViewSource;
import org.telegram.android.media.Optimizer;
import org.telegram.android.cursors.ViewSourceListener;
import org.telegram.android.cursors.ViewSourceState;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.ui.FilterMatcher;
import org.telegram.android.ui.Placeholders;
import org.telegram.android.ui.TextUtil;
import org.telegram.android.views.DialogView;
import org.telegram.api.TLAbsInputPeer;
import org.telegram.api.TLInputPeerChat;
import org.telegram.api.TLInputPeerForeign;
import org.telegram.api.TLInputUserSelf;
import org.telegram.api.engine.RpcException;
import org.telegram.api.engine.TimeoutException;
import org.telegram.api.messages.TLAffectedHistory;
import org.telegram.api.requests.TLRequestMessagesDeleteChatUser;
import org.telegram.api.requests.TLRequestMessagesDeleteHistory;
import org.telegram.api.requests.TLRequestMessagesDiscardEncryption;
import org.telegram.i18n.I18nUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 19:35
 */
public class DialogsFragment extends StelsFragment implements ViewSourceListener {

    private static final String TAG = "DialogsFragment";

    public static final String ACTION_FORWARD = "action_forward";
    public static final String ACTION_FORWARD_MULTIPLE = "action_forward_multiple";
    public static final String ACTION_SEND_VIDEO = "action_send_video";
    public static final String ACTION_SEND_IMAGE = "action_send_image";
    public static final String ACTION_SEND_IMAGES = "action_send_images";
    public static final String ACTION_SEND_TEXT = "action_send_text";
    public static final String ACTION_SEND_CONTACT = "action_send_contact";
    public static final String ACTION_SEND_DOC = "action_send_doc";
    public static final String ACTION_SEND_DOCS = "action_send_docs";

    public static DialogsFragment buildSendImages(String[] uris) {
        DialogsFragment res = new DialogsFragment();
        res.action = ACTION_SEND_IMAGES;
        res.actionUris = uris;
        res.setSaveInStack(false);
        return res;
    }

    public static DialogsFragment buildSendDocs(String[] uris) {
        DialogsFragment res = new DialogsFragment();
        res.action = ACTION_SEND_DOCS;
        res.actionUris = uris;
        res.setSaveInStack(false);
        return res;
    }

    public static DialogsFragment buildSendContact(int uid) {
        DialogsFragment res = new DialogsFragment();
        res.action = ACTION_SEND_CONTACT;
        res.actionUid = uid;
        res.setSaveInStack(false);
        return res;
    }

    public static DialogsFragment buildSendImage(String uri) {
        DialogsFragment res = new DialogsFragment();
        res.action = ACTION_SEND_IMAGE;
        res.actionUri = uri;
        res.setSaveInStack(false);
        return res;
    }

    public static DialogsFragment buildSendDoc(String uri) {
        DialogsFragment res = new DialogsFragment();
        res.action = ACTION_SEND_DOC;
        res.actionUri = uri;
        res.setSaveInStack(false);
        return res;
    }

    public static DialogsFragment buildSendVideo(String uri) {
        DialogsFragment res = new DialogsFragment();
        res.action = ACTION_SEND_VIDEO;
        res.actionUri = uri;
        res.setSaveInStack(false);
        return res;
    }

    public static DialogsFragment buildForwardMessage(int mid) {
        DialogsFragment res = new DialogsFragment();
        res.action = ACTION_FORWARD;
        res.actionMid = mid;
        res.setSaveInStack(false);
        return res;
    }

    public static DialogsFragment buildForwardMessages(Integer[] mids) {
        DialogsFragment res = new DialogsFragment();
        res.action = ACTION_FORWARD_MULTIPLE;
        res.actionMids = mids;
        Arrays.sort(res.actionMids);
        res.setSaveInStack(false);
        return res;
    }

    public static DialogsFragment buildSendMessage(String text) {
        DialogsFragment res = new DialogsFragment();
        res.action = ACTION_SEND_TEXT;
        res.actionText = text;
        res.setSaveInStack(false);
        return res;
    }


    private String action;
    private String actionUri;
    private String actionText;
    private String[] actionUris;
    private int actionMid;
    private Integer[] actionMids;
    private int actionUid;

    private ArrayList<DialogWireframe> workingSet;
    private BaseAdapter dialogAdapter;
    private View mainContainer;
    private ListView listView;
    private View loading;
    private View empty;
    private View loadMore;
    private TextView dialogCounter;
    private Button tryAgain;

    private FilterMatcher matcher;
    private View searchContainer;
    private View searchHint;
    private View searchEmpty;
    private ListView searchListView;
    private SearchWireframe[] searchWireframes = new SearchWireframe[0];
    private BaseAdapter searchResultAdapter;

    private boolean isInSearchMode = false;
    private int paddingTop = 0;
    private int selectedIndex = -1;
    private int selectedTop = 0;

    public DialogsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            action = savedInstanceState.getString("action");
            actionUri = savedInstanceState.getString("actionUri");
            actionMid = savedInstanceState.getInt("actionMid", 0);
            actionUid = savedInstanceState.getInt("actionUid", 0);
            paddingTop = savedInstanceState.getInt("paddingTop", 0);
            selectedTop = savedInstanceState.getInt("selectedTop", 0);
            selectedIndex = savedInstanceState.getInt("selectedIndex", -1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        long start = System.currentTimeMillis();
        if (savedInstanceState != null) {
            action = savedInstanceState.getString("action");
            actionUri = savedInstanceState.getString("actionUri");
            actionMid = savedInstanceState.getInt("actionMid", 0);
            actionUid = savedInstanceState.getInt("actionUid", 0);
            paddingTop = savedInstanceState.getInt("paddingTop", 0);
            selectedTop = savedInstanceState.getInt("selectedTop", 0);
            selectedIndex = savedInstanceState.getInt("selectedIndex", -1);
        }
        View res = inflater.inflate(R.layout.dialogs_list, container, false);

        if (workingSet == null) {
            DialogSource source = application.getDialogSource();
            ViewSource<DialogWireframe, DialogDescription> viewSource = source.getViewSource();
            workingSet = viewSource.getCurrentWorkingSet();
        }

        dialogAdapter = new BaseAdapter() {

            @Override
            public int getCount() {
                return workingSet.size();
            }

            @Override
            public DialogWireframe getItem(int i) {
                return workingSet.get(i);
            }

            @Override
            public long getItemId(int i) {
                return getItem(i).getDatabaseId();
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                DialogWireframe description = getItem(i);
                if (view == null) {
                    view = new DialogView(getActivity());
                }

                DialogView dialogView = (DialogView) view;

                dialogView.setDescription(description, description.getPreparedLayout());
                application.getDialogSource().getViewSource().onItemsShown(i);

                return view;
            }
        };

        View bottomPadding = View.inflate(getActivity(), R.layout.dialogs_bottom, null);

        loadMore = bottomPadding.findViewById(R.id.loadingMore);
        dialogCounter = (TextView) bottomPadding.findViewById(R.id.dialogsCount);
        tryAgain = (Button) bottomPadding.findViewById(R.id.tryAgain);
        tryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getDialogSource().requestLoadMore(dialogAdapter.getCount());
            }
        });

        mainContainer = res.findViewById(R.id.mainContainer);
        listView = (ListView) res.findViewById(R.id.dialogsList);

//        TransitionDrawable drawable = new TransitionDrawable(new Drawable[]{
//                new ColorDrawable(getResources().getColor(R.color.st_selector)),
//                new ColorDrawable(getResources().getColor(R.color.st_selector_end))});
//        listView.setSelector(drawable);

        listView.addFooterView(bottomPadding, null, false);
        listView.setAdapter(new HeaderViewListAdapter(
                new ArrayList<ListView.FixedViewInfo>(0),
                new ArrayList<ListView.FixedViewInfo>(1), dialogAdapter));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Object item = adapterView.getItemAtPosition(i);
                if (item instanceof DialogWireframe) {
                    DialogWireframe description = (DialogWireframe) item;
                    onItemClicked(description.getPeerType(), description.getPeerId(), description.getDialogTitle());
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                final DialogWireframe description = (DialogWireframe) adapterView.getItemAtPosition(i);
                AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_dialogs_delete_header)
                        .setMessage(description.getPeerType() == PeerType.PEER_CHAT ? R.string.st_dialogs_delete_group : R.string.st_dialogs_delete_history)
                        .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                runUiTask(new AsyncAction() {
                                    @Override
                                    public void execute() throws AsyncException {
                                        if (description.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                                            try {
                                                rpcRaw(new TLRequestMessagesDiscardEncryption(description.getPeerId()));
                                            } catch (RpcException e) {
                                                e.printStackTrace();
//                                                if ("ENCRYPTION_ALREADY_DECLINED".equals(e.getErrorTag())) {
//                                                    e.printStackTrace();
//                                                } else {
//                                                    throw new AsyncException(e);
//                                                }
                                            }
                                            getEngine().getSecretEngine().deleteEncryptedChat(description.getPeerId());
                                            getEngine().deleteHistory(description.getPeerType(), description.getPeerId());
                                        } else {

                                            TLAbsInputPeer peer;
                                            if (description.getPeerType() == PeerType.PEER_CHAT) {
                                                peer = new TLInputPeerChat(description.getPeerId());
                                            } else {
                                                User user = application.getEngine().getUser(description.getPeerId());
                                                peer = new TLInputPeerForeign(user.getUid(), user.getAccessHash());
                                            }

                                            int offset = 0;
                                            do {
                                                TLAffectedHistory affectedHistory = rpc(new TLRequestMessagesDeleteHistory(peer, offset));
                                                application.getUpdateProcessor().onMessage(new TLLocalAffectedHistory(affectedHistory));
                                                offset = affectedHistory.getOffset();
                                            } while (offset != 0);

                                            if (description.getPeerType() == PeerType.PEER_CHAT) {
                                                try {
                                                    rpcRaw(new TLRequestMessagesDeleteChatUser(description.getPeerId(), new TLInputUserSelf()));
                                                } catch (RpcException e) {
                                                    e.printStackTrace();
                                                }
                                            }

                                            getEngine().deleteHistory(description.getPeerType(), description.getPeerId());
                                        }
                                    }

                                    @Override
                                    public void afterExecute() {
                                        application.notifyUIUpdate();
                                    }
                                });
                            }
                        }).setNegativeButton(R.string.st_no, null).create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
                return true;
            }
        });

        res.findViewById(R.id.writeToFriend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRootController().openContacts();
            }
        });

        searchListView = (ListView) res.findViewById(R.id.dialogsSearch);
        searchContainer = res.findViewById(R.id.searchContainer);
        searchEmpty = res.findViewById(R.id.emptySearch);
        searchHint = res.findViewById(R.id.searchHint);
        searchContainer.setVisibility(View.GONE);
        isInSearchMode = false;
        final Context context = getActivity();
        searchResultAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return searchWireframes.length;
            }

            @Override
            public SearchWireframe getItem(int i) {
                return searchWireframes[i];
            }

            @Override
            public long getItemId(int i) {
                return searchWireframes[i].getPeerId() * 2 + searchWireframes[i].getPeerType();
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                if (view == null) {
                    view = View.inflate(context, R.layout.item_search_item, null);
                }
                SearchWireframe wireframe = getItem(i);

                if (matcher != null) {
                    SpannableString string = new SpannableString(wireframe.getTitle());
                    matcher.highlight(context, string);
                    ((TextView) view.findViewById(R.id.name)).setText(string);
                } else {
                    ((TextView) view.findViewById(R.id.name)).setText(wireframe.getTitle());
                }

                FastWebImageView avatarView = (FastWebImageView) view.findViewById(R.id.avatar);

                if (wireframe.getPeerType() == PeerType.PEER_USER) {
                    avatarView.setLoadingDrawable(Placeholders.getUserPlaceholder(wireframe.getPeerId()));
                } else {
                    avatarView.setLoadingDrawable(Placeholders.getGroupPlaceholder(wireframe.getPeerId()));
                }

                if (wireframe.getPhoto() instanceof TLLocalAvatarPhoto) {
                    TLLocalAvatarPhoto localAvatarPhoto = (TLLocalAvatarPhoto) wireframe.getPhoto();
                    if (localAvatarPhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                        avatarView.requestTask(new StelsImageTask((TLLocalFileLocation) localAvatarPhoto.getPreviewLocation()));
                    } else {
                        avatarView.requestTask(null);
                    }
                } else {
                    avatarView.requestTask(null);
                }

                if (wireframe.getPeerType() == PeerType.PEER_USER) {
                    int status = getUserState(wireframe.getStatus());
                    if (status < 0) {
                        ((TextView) view.findViewById(R.id.status)).setText(R.string.st_offline);
                        ((TextView) view.findViewById(R.id.status)).setTextColor(context.getResources().getColor(R.color.st_grey_text));
                    } else if (status == 0) {
                        ((TextView) view.findViewById(R.id.status)).setText(R.string.st_online);
                        ((TextView) view.findViewById(R.id.status)).setTextColor(context.getResources().getColor(R.color.st_blue_bright));
                    } else {
                        ((TextView) view.findViewById(R.id.status)).setTextColor(context.getResources().getColor(R.color.st_grey_text));
                        ((TextView) view.findViewById(R.id.status)).setText(
                                TextUtil.formatHumanReadableLastSeen(status, getStringSafe(R.string.st_lang)));
                    }
                } else {
                    ((TextView) view.findViewById(R.id.status)).setText(
                            getQuantityString(R.plurals.st_members, wireframe.getMembers()).replace("{d}", "" + I18nUtil.getInstance().correctFormatNumber(wireframe.getMembers())));
                    ((TextView) view.findViewById(R.id.status)).setTextColor(context.getResources().getColor(R.color.st_grey_text));
                }

                return view;
            }
        };
        searchListView.setAdapter(searchResultAdapter);
        searchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                SearchWireframe wireframe = (SearchWireframe) adapterView.getItemAtPosition(i);
                onItemClicked(wireframe.getPeerType(), wireframe.getPeerId(), wireframe.getTitle());
            }
        });

        loading = res.findViewById(R.id.loading);
        empty = res.findViewById(R.id.empty);
        empty.setVisibility(View.GONE);
        loading.setVisibility(View.GONE);
        updateSearchMode();

        if (paddingTop != 0) {
            listView.setPadding(0, paddingTop, 0, 0);
        }
        if (selectedIndex >= 0) {
            listView.setSelectionFromTop(selectedIndex, selectedTop);
        }

        application.getDialogSource().getViewSource().onConnected();

        // dialogAdapter.notifyDataSetChanged();
        onDataStateChanged();

        Logger.d(TAG, "Loading in " + (System.currentTimeMillis() - start) + " ms");

        return res;
    }

    private void onItemClicked(final int peerType, final int peerId, final String title) {
        if (ACTION_SEND_CONTACT.equals(action)) {
            if (peerType == PeerType.PEER_USER_ENCRYPTED) {
                Toast.makeText(getActivity(), R.string.st_dialogs_share_to_secret, Toast.LENGTH_SHORT).show();
            } else {
                AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_dialogs_confirm_header)
                        .setMessage(getStringSafe(R.string.st_dialogs_confirm_contact).replace("{0}", title))
                        .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                application.getEngine().shareContact(peerType, peerId, actionUid);
                                application.getSyncKernel().getBackgroundSync().resetTypingDelay();
                                application.notifyUIUpdate();
                                action = null;
                                getRootController().openDialog(peerType, peerId);
                            }
                        }).setNegativeButton(R.string.st_no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        }).create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }
        } else if (ACTION_SEND_TEXT.equals(action)) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_dialogs_confirm_header)
                    .setMessage(getStringSafe(R.string.st_dialogs_confirm_send).replace("{0}", title))
                    .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            application.getEngine().sendMessage(peerType, peerId, actionText);
                            application.getSyncKernel().getBackgroundSync().resetTypingDelay();
                            application.notifyUIUpdate();
                            action = null;
                            getRootController().openDialog(peerType, peerId);
                        }
                    })
                    .setNegativeButton(R.string.st_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    }).create();
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();
        } else if (ACTION_FORWARD_MULTIPLE.equals(action)) {
            if (peerType == PeerType.PEER_USER_ENCRYPTED) {
                Toast.makeText(getActivity(), R.string.st_dialogs_forward_to_secret, Toast.LENGTH_SHORT).show();
            } else {
                AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_dialogs_confirm_header)
                        .setMessage(getStringSafe(R.string.st_dialogs_confirm_forward_multiple).replace("{0}", title))
                        .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                for (Integer mid : actionMids) {
                                    application.getEngine().forwardMessage(peerType, peerId, mid);
                                }
                                application.getSyncKernel().getBackgroundSync().resetTypingDelay();
                                application.notifyUIUpdate();
                                action = null;
                                getRootController().openDialog(peerType, peerId);
                            }
                        }).setNegativeButton(R.string.st_no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        }).create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }
        } else if (ACTION_FORWARD.equals(action)) {
            if (peerType == PeerType.PEER_USER_ENCRYPTED) {
                Toast.makeText(getActivity(), R.string.st_dialogs_forward_to_secret, Toast.LENGTH_SHORT).show();
            } else {
                AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_dialogs_confirm_header)
                        .setMessage(getStringSafe(R.string.st_dialogs_confirm_forward).replace("{0}", title))
                        .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                application.getEngine().forwardMessage(peerType, peerId, actionMid);
                                application.getSyncKernel().getBackgroundSync().resetTypingDelay();
                                application.notifyUIUpdate();
                                action = null;
                                getRootController().openDialog(peerType, peerId);
                            }
                        }).setNegativeButton(R.string.st_no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        }).create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }
        } else if (ACTION_SEND_IMAGE.equals(action) || ACTION_SEND_VIDEO.equals(action) || ACTION_SEND_DOC.equals(action)) {
            int textId;
            if (ACTION_SEND_IMAGE.equals(action)) {
                textId = R.string.st_dialogs_confirm_image;
            } else if (ACTION_SEND_VIDEO.equals(action)) {
                textId = R.string.st_dialogs_confirm_video;
            } else {
                textId = R.string.st_dialogs_confirm_doc;
            }
            AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_dialogs_confirm_header)
                    .setMessage(getStringSafe(textId).replace("{0}", title))
                    .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (ACTION_SEND_IMAGE.equals(action)) {
                                runUiTask(new AsyncAction() {
                                    @Override
                                    public void execute() throws AsyncException {
                                        Point size;
                                        try {
                                            size = Optimizer.getSize(Uri.parse(actionUri), getActivity());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            Toast.makeText(getActivity(), R.string.st_error_file_open, Toast.LENGTH_SHORT).show();
                                            getRootController().doBack();
                                            return;
                                        }
                                        application.getEngine().sendPhoto(peerType, peerId, size.x, size.y, Uri.parse(actionUri));
                                        application.notifyUIUpdate();
                                    }

                                    @Override
                                    public void afterExecute() {
                                        action = null;
                                        getRootController().openDialog(peerType, peerId);
                                    }
                                });
                            } else if (ACTION_SEND_VIDEO.equals(action)) {
                                runUiTask(new AsyncAction() {
                                    @Override
                                    public void execute() throws AsyncException {
                                        String fileName = getRealPathFromURI(Uri.parse(actionUri));
                                        Bitmap res = ThumbnailUtils.createVideoThumbnail(fileName, MediaStore.Video.Thumbnails.MINI_KIND);
                                        int w = 0;
                                        int h = 0;
                                        if (res != null) {
                                            w = res.getWidth();
                                            h = res.getHeight();
                                        }
                                        application.getEngine().sendVideo(peerType, peerId, new TLUploadingVideo(fileName, w, h));
                                        application.notifyUIUpdate();

                                    }

                                    @Override
                                    public void afterExecute() {
                                        action = null;
                                        getRootController().doBack();
                                        getRootController().openDialog(peerType, peerId);
                                    }
                                });

                            } else {
                                runUiTask(new AsyncAction() {
                                    @Override
                                    public void execute() throws AsyncException {
                                        String fileName = getRealPathFromURI(Uri.parse(actionUri));
                                        application.getEngine().sendDocument(peerType, peerId, new TLUploadingDocument(fileName));
                                    }

                                    @Override
                                    public void afterExecute() {
                                        action = null;
                                        getRootController().doBack();
                                        getRootController().openDialog(peerType, peerId);
                                    }
                                });
                            }
                        }
                    })
                    .setNegativeButton(R.string.st_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    }).create();
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();
        } else if (ACTION_SEND_IMAGES.equals(action) || ACTION_SEND_DOCS.equals(action)) {
            int textId;
            if (ACTION_SEND_IMAGES.equals(action)) {
                textId = R.string.st_dialogs_confirm_images;
            } else {
                textId = R.string.st_dialogs_confirm_docs;
            }
            AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_dialogs_confirm_header)
                    .setMessage(getStringSafe(textId).replace("{0}", title))
                    .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            runUiTask(new AsyncAction() {
                                @Override
                                public void execute() throws AsyncException {
                                    if (ACTION_SEND_IMAGES.equals(action)) {
                                        for (String uri : actionUris) {

                                            Point size;
                                            try {
                                                size = Optimizer.getSize(Uri.parse(uri), getActivity());
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                                Toast.makeText(getActivity(), R.string.st_error_file_open, Toast.LENGTH_SHORT).show();
                                                getRootController().doBack();
                                                return;
                                            }
                                            application.getEngine().sendPhoto(peerType, peerId, size.x, size.y, Uri.parse(uri));
                                        }
                                    } else {
                                        for (String uri : actionUris) {
                                            String fileName = getRealPathFromURI(Uri.parse(uri));
                                            application.getEngine().sendDocument(peerType, peerId, new TLUploadingDocument(fileName));
                                        }
                                    }
                                    application.notifyUIUpdate();
                                }

                                @Override
                                public void afterExecute() {
                                    action = null;
                                    getRootController().openDialog(peerType, peerId);
                                }
                            });
                        }
                    })
                    .setNegativeButton(R.string.st_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    }).create();
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();
        } else {
            getRootController().openDialog(peerType, peerId);
        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        if ("file".equals(contentUri.getScheme())) {
            return contentUri.getPath();
        } else {
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = getActivity().getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        application.getUiKernel().onDialogsResume();
        application.getDialogSource().getViewSource().addListener(this);
        application.getNotifications().hideAllNotifications();

        applyFreshData();
        onDataStateChanged();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            switch (application.getTechKernel().getDebugSettings().getDialogListLayerType()) {
                default:
                case DebugSettings.LAYER_NONE:
                    listView.setLayerType(View.LAYER_TYPE_NONE, null);
                    break;
                case DebugSettings.LAYER_HARDWARE:
                    listView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    break;
                case DebugSettings.LAYER_SOFTWARE:
                    listView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    break;
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu, menu);
        if (action != null) {
            getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_dialogs_pick_title));
            getSherlockActivity().getSupportActionBar().setSubtitle(null);
            getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSherlockActivity().getSupportActionBar().setHomeButtonEnabled(true);

            menu.findItem(R.id.contactsMenu).setVisible(false);
            menu.findItem(R.id.settingsMenu).setVisible(false);
            menu.findItem(R.id.newSecretMenu).setVisible(false);
            menu.findItem(R.id.newGroupMenu).setVisible(false);
            menu.findItem(R.id.writeToContact).setVisible(false);
        } else {
            menu.findItem(R.id.contactsMenu).setTitle(highlightMenuText(R.string.st_dialogs_menu_contacts));
            menu.findItem(R.id.settingsMenu).setTitle(highlightMenuText(R.string.st_dialogs_menu_settings));
            menu.findItem(R.id.newSecretMenu).setTitle(highlightMenuText(R.string.st_dialogs_menu_secret));
            menu.findItem(R.id.newGroupMenu).setTitle(highlightMenuText(R.string.st_dialogs_menu_group));
            menu.findItem(R.id.writeToContact).setTitle(R.string.st_dialogs_menu_write_contact);

            getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSherlockActivity().getSupportActionBar().setHomeButtonEnabled(false);
            getSherlockActivity().getSupportActionBar().setTitle(highlightTitleDisabledText(R.string.st_app_name));
            getSherlockActivity().getSupportActionBar().setSubtitle(null);
        }

        MenuItem searchItem = menu.findItem(R.id.searchMenu);
        com.actionbarsherlock.widget.SearchView searchView = (com.actionbarsherlock.widget.SearchView) searchItem.getActionView();
        //searchView.setQueryHint(getStringSafe(R.string.st_dialogs_search_hint));
        searchView.setQueryHint("");

        ((ImageView) searchView.findViewById(R.id.abs__search_button)).setImageResource(R.drawable.st_bar_logo);

//            AutoCompleteTextView searchText = (AutoCompleteTextView) searchView.findViewById(R.id.abs__search_src_text);
//            searchText.setHintTextColor(0xccB8B8B8);
//            searchText.setTextColor(0xff010101);

        searchView.setOnSuggestionListener(new com.actionbarsherlock.widget.SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                return false;
            }
        });

        searchView.setOnQueryTextListener(new com.actionbarsherlock.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                doSearch(newText.trim());
                return true;
            }
        });

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                isInSearchMode = true;
                updateSearchMode();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                isInSearchMode = false;
                updateSearchMode();
                return true;
            }
        });
        isInSearchMode = false;
        updateSearchMode();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.contactsMenu || item.getItemId() == R.id.writeToContact) {
            getRootController().openContacts();
            return true;
        }
        if (item.getItemId() == R.id.settingsMenu) {
            getRootController().openSettings();
            return true;
        }
        if (item.getItemId() == R.id.newGroupMenu) {
            getRootController().openCreateNewChat();
            return true;
        }
        if (item.getItemId() == R.id.newSecretMenu) {
            pickUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onFragmentResult(int resultCode, final Object data) {
        if (data instanceof Integer) {
            runUiTask(new AsyncAction() {
                private int chatId;

                @Override
                public void execute() throws AsyncException {
                    try {
                        chatId = application.getEncryptionController().requestEncryption((Integer) data);
                    } catch (RpcException e) {
                        e.printStackTrace();
                        throw new AsyncException(e);
                    } catch (TimeoutException e1) {
                        e1.printStackTrace();
                        throw new AsyncException(AsyncException.ExceptionType.CONNECTION_ERROR);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
                    }
                }

                @Override
                public void afterExecute() {
                    getRootController().openDialog(PeerType.PEER_USER_ENCRYPTED, chatId);
                }
            });
        }
    }

    private void doSearch(String query) {
        if (!isInSearchMode)
            return;

        if (query == null || query.length() == 0) {
            matcher = null;
        } else {
            matcher = new FilterMatcher(query);
        }

        if (matcher == null) {
            searchWireframes = new SearchWireframe[0];
            searchResultAdapter.notifyDataSetChanged();
            showView(searchHint);
            goneView(searchEmpty);
            goneView(searchListView);
        } else {
            searchWireframes = application.getSearchKernel().doSearch(matcher);
            searchResultAdapter.notifyDataSetChanged();
            goneView(searchHint);
            if (searchWireframes.length == 0) {
                showView(searchEmpty);
                goneView(searchListView);
            } else {
                goneView(searchEmpty);
                showView(searchListView);
            }
        }
    }

    private void updateSearchMode() {
        if (searchContainer != null) {
            if (isInSearchMode) {
                showView(searchContainer);
                searchHint.setVisibility(View.VISIBLE);
                searchEmpty.setVisibility(View.GONE);
                searchListView.setVisibility(View.GONE);
            } else {
                goneView(searchContainer);
            }
        }
    }

    private void onDataStateChanged() {
        if (dialogAdapter.getCount() == 0) {
            if ((application.getDialogSource().getViewSource().getState() == ViewSourceState.COMPLETED ||
                    application.getDialogSource().getViewSource().getState() == ViewSourceState.SYNCED)) {
                hideView(loading);
                showView(empty);
            } else {
                showView(loading);
                empty.setVisibility(View.GONE);
            }
            tryAgain.setVisibility(View.GONE);
            listView.setVisibility(View.GONE);
        } else {
            goneView(loading);
            goneView(empty);
            showView(listView);

            if (application.getDialogSource().getViewSource().getState() == ViewSourceState.IN_PROGRESS
                    ||
                    application.getDialogSource().getViewSource().getState() != ViewSourceState.COMPLETED) {
                loadMore.setVisibility(View.VISIBLE);
                dialogCounter.setVisibility(View.GONE);
                tryAgain.setVisibility(View.GONE);
            } else {
                loadMore.setVisibility(View.GONE);
                if (application.getDialogSource().getState() == DialogSourceState.LOAD_MORE_ERROR) {
                    tryAgain.setVisibility(View.VISIBLE);
                    dialogCounter.setVisibility(View.GONE);
                } else {
                    tryAgain.setVisibility(View.GONE);
                    if (application.getDialogSource().isCompleted()) {
                        dialogCounter.setVisibility(View.VISIBLE);
                        dialogCounter.setText(
                                getQuantityString(R.plurals.st_dialogs, dialogAdapter.getCount())
                                        .replace("{d}", I18nUtil.getInstance().correctFormatNumber(dialogAdapter.getCount())));
                    } else {
                        dialogCounter.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    private void applyFreshData() {

        final HashMap<Long, Integer> offsets = new HashMap<Long, Integer>();

        long[] ids = new long[listView.getChildCount()];

        for (int i = 0; i < listView.getChildCount(); i++) {
            long id = listView.getItemIdAtPosition(
                    listView.getFirstVisiblePosition() + i);
            ids[i] = id;
            View view = listView.getChildAt(i);
            offsets.put(id, view.getTop());
        }

        workingSet = application.getDialogSource().getViewSource().getCurrentWorkingSet();
        dialogAdapter.notifyDataSetChanged();

        boolean changed = false;

        long[] idsNew = new long[listView.getChildCount()];

        if (idsNew.length != ids.length) {
            changed = true;
            for (int i = 0; i < listView.getChildCount(); i++) {
                long id = listView.getItemIdAtPosition(
                        listView.getFirstVisiblePosition() + i);
                idsNew[i] = id;
                //Log.d("LISTS_SCROLL", "founded2: " + id);
            }
        } else {
            for (int i = 0; i < listView.getChildCount(); i++) {
                long id = listView.getItemIdAtPosition(
                        listView.getFirstVisiblePosition() + i);
                idsNew[i] = id;
                if (ids[i] != id) {
                    changed = true;
                }
                //Log.d("LISTS_SCROLL", "founded2: " + id);
            }
        }

        if (changed) {
            final HashSet<Long> changedIds = new HashSet<Long>();
            for (int i = 0; i < Math.min(ids.length, idsNew.length); i++) {
                if (ids[i] != idsNew[i]) {
                    changedIds.add(ids[i]);
                    changedIds.add(idsNew[i]);
                }
            }

            if (ids.length < idsNew.length) {
                for (int i = ids.length; i < idsNew.length; i++) {
                    changedIds.add(idsNew[i]);
                }
            }

            if (idsNew.length < ids.length) {
                for (int i = idsNew.length; i < ids.length; i++) {
                    changedIds.add(ids[i]);
                }
            }

            listView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (listView == null)
                        return true;

                    listView.getViewTreeObserver().removeOnPreDrawListener(this);

                    for (int i = 0; i < listView.getChildCount(); i++) {
                        View view = listView.getChildAt(i);
                        long id = listView.getItemIdAtPosition(
                                listView.getFirstVisiblePosition() + i);
                        if (changedIds.contains(id) && offsets.containsKey(id)) {
                            int oldTop;
                            if (offsets.containsKey(id)) {
                                oldTop = offsets.get(id);
                            } else {
                                oldTop = -view.getHeight();
                            }
                            int newTop = view.getTop();
                            if (oldTop != newTop) {
                                ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationY", oldTop - newTop, 0)
                                        .setDuration(300);
                                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                                animator.start();
                            }
                        }
                    }

                    offsets.clear();

                    return true;
                }
            });
        }
    }

    @Override
    public void onSourceStateChanged() {
        onDataStateChanged();
    }

    @Override
    public void onSourceDataChanged() {
        applyFreshData();
        onDataStateChanged();
    }

    @Override
    public void onEmojiUpdated(boolean completed) {
        listView.post(new Runnable() {
            @Override
            public void run() {
                if (listView != null) {
                    for (int i = 0; i < listView.getChildCount(); i++) {
                        View v = listView.getChildAt(i);
                        if (v != null) {
                            v.invalidate();
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        application.getDialogSource().getViewSource().removeListener(this);
        application.getUiKernel().onDialogPaused();

        saveListPosition();
    }

    private void saveListPosition() {
        if (listView != null) {
            int index = listView.getFirstVisiblePosition();
            View v = listView.getChildAt(0);
            if (v != null) {
                selectedTop = v.getTop();
                selectedIndex = index;
            } else {
                selectedTop = 0;
                selectedIndex = -1;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("action", action);
        outState.putString("actionUri", actionUri);
        outState.putInt("actionMid", actionMid);
        outState.putInt("actionUid", actionUid);
        outState.putInt("paddingTop", paddingTop);

        saveListPosition();
        outState.putInt("selectedIndex", selectedIndex);
        outState.putInt("selectedTop", selectedTop);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listView = null;
        dialogAdapter = null;
        loading = null;
        loadMore = null;
        dialogCounter = null;
        tryAgain = null;
        searchContainer = null;
        searchListView = null;
    }
}