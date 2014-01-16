package org.telegram.android.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.text.*;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.*;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.extradea.framework.images.ui.FastWebImageView;
import org.telegram.android.MediaReceiverFragment;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;
import org.telegram.android.config.DebugSettings;
import org.telegram.android.core.*;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.local.TLLocalChatParticipant;
import org.telegram.android.core.model.media.*;
import org.telegram.android.core.model.service.*;
import org.telegram.android.core.model.update.TLLocalAffectedHistory;
import org.telegram.android.core.wireframes.MessageWireframe;
import org.telegram.android.ui.source.ViewSourceListener;
import org.telegram.android.ui.source.ViewSourceState;
import org.telegram.android.log.Logger;
import org.telegram.android.media.DownloadManager;
import org.telegram.android.media.DownloadState;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.ui.*;
import org.telegram.android.views.*;
import org.telegram.android.views.dialog.ConversationAdapter;
import org.telegram.android.views.dialog.ConversationListView;
import org.telegram.api.TLAbsInputPeer;
import org.telegram.api.TLInputPeerChat;
import org.telegram.api.TLInputPeerForeign;
import org.telegram.api.TLInputUserSelf;
import org.telegram.api.engine.RpcException;
import org.telegram.api.messages.TLAffectedHistory;
import org.telegram.api.requests.TLRequestMessagesDeleteChatUser;
import org.telegram.api.requests.TLRequestMessagesDeleteHistory;
import org.telegram.api.requests.TLRequestMessagesDiscardEncryption;
import org.telegram.i18n.I18nUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Author: Korshakov Stepan
 * Created: 29.07.13 0:31
 */
public class ConversationFragment extends MediaReceiverFragment implements ViewSourceListener, ChatSourceListener, TypingStates.TypingListener, UserSourceListener, EncryptedChatSourceListener {

    private static final String TAG = "ConversaionFragment";

    private static final int MINIMAL_NEWDIV_COUNT = 10;

    private int peerId;
    private int peerType;

    private ArrayList<MessageWireframe> workingSet;
    private BaseAdapter dialogAdapter;
    private View mainContainer;
    private ConversationListView listView;

    private MessageSource source;

    private View tryAgain;
    private TextView messagesCount;
    private View loadingMore;
    private View loading;
    private View empty;
    private View emptyEncrypted;
    private View deleteButton;
    private TextView waitMessageView;

    private View inputOverlay;

    private EditText editText;
    private ImageButton smileButton;
    private ImageButton sendButton;

    private long viewCreateTime = 0;

    private View contactsPanel;

    private HashSet<Integer> selected = new HashSet<Integer>();

    private ActionMode actionMode;

    private int selectedIndex = -1;
    private int selectedTop = 0;

    private boolean isEnabledInput = false;

    private boolean isFreshUpdate = true;

    private long firstUnreadMessage = 0;
    private int unreadCount = 0;

    private View audioRecordContainer;
    private TextView audioRecordTimer;
    private MediaRecorder audioRecorder;
    private String audioFile;
    private long audioStart;
    private Handler handler = new Handler(Looper.getMainLooper());

    private Runnable updateTimer = new Runnable() {
        @Override
        public void run() {
            updateAudioUi();
            handler.removeCallbacks(this);
            if (audioRecorder != null) {
                handler.postDelayed(this, 1000);
            }
        }
    };

    public ConversationFragment(int peerType, int peerId) {
        this.peerId = peerId;
        this.peerType = peerType;
    }

    public ConversationFragment() {

    }

    public int getPeerId() {
        return peerId;
    }

    public int getPeerType() {
        return peerType;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            peerId = savedInstanceState.getInt("peerId");
            peerType = savedInstanceState.getInt("peerType");
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getSherlockActivity().invalidateOptionsMenu();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            peerId = savedInstanceState.getInt("peerId");
            peerType = savedInstanceState.getInt("peerType");
        }

        viewCreateTime = SystemClock.uptimeMillis();

        View res = inflater.inflate(R.layout.conv_list, container, false);

        if (peerId == 333000 && peerType == PeerType.PEER_USER) {
            ((TextView) res.findViewById(R.id.emptyLabel)).setText(R.string.st_conv_empty_support);
        }

        listView = (ConversationListView) res.findViewById(R.id.listView);

        // ((ImagingListView) listView).setEnabledDynamicPause(true);

        mainContainer = res.findViewById(R.id.mainContainer);

        if (application.getUserSettings().isWallpaperSet()) {
            if (application.getUserSettings().isWallpaperSolid()) {
                ColorDrawable drawable = new ColorDrawable(application.getUserSettings().getCurrentWallpaperSolidColor());
                if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    res.setBackgroundDrawable(drawable);
                } else {
                    res.setBackground(drawable);
                }
            } else {
                Bitmap bitmap = application.getWallpaperHolder().getBitmap();
                if (bitmap != null) {
                    FastBackgroundDrawable drawable = new FastBackgroundDrawable(bitmap);
                    if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        res.setBackgroundDrawable(drawable);
                    } else {
                        res.setBackground(drawable);
                    }
                }
            }
        } else {
            Bitmap bitmap = application.getWallpaperHolder().getBitmap();
            FastBackgroundDrawable drawable = new FastBackgroundDrawable(bitmap);
            mainContainer.setBackgroundDrawable(drawable);
        }
        listView.setCacheColorHint(0);

        contactsPanel = res.findViewById(R.id.contactsPanel);

        audioRecordContainer = res.findViewById(R.id.audioPanel);
        audioRecordContainer.setVisibility(View.GONE);

        audioRecordContainer.findViewById(R.id.sendAudio).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendAudio();
            }
        });

        audioRecordContainer.findViewById(R.id.cancelAudio).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelAudio();
            }
        });

        audioRecordTimer = (TextView) audioRecordContainer.findViewById(R.id.timer);

        loading = res.findViewById(R.id.loading);
        empty = res.findViewById(R.id.empty);
        emptyEncrypted = res.findViewById(R.id.emptyEncrypted);
        empty.setVisibility(View.GONE);
        emptyEncrypted.setVisibility(View.GONE);
        loading.setVisibility(View.GONE);


        inputOverlay = res.findViewById(R.id.panel);

        waitMessageView = (TextView) res.findViewById(R.id.waiting);
        deleteButton = res.findViewById(R.id.delete);

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteDialog();
            }
        });

        View bottomPadding = new View(getActivity());
        bottomPadding.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getPx(8)));

        View topView = View.inflate(getActivity(), R.layout.conv_top, null);
        loadingMore = topView.findViewById(R.id.loadingMore);
        messagesCount = (TextView) topView.findViewById(R.id.messagesCount);
        tryAgain = topView.findViewById(R.id.tryAgain);

        tryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                source.requestLoadMore(dialogAdapter.getCount());
            }
        });

        source = application.getDataSourceKernel().getMessageSource(peerType, peerId);
        source.getMessagesSource().onConnected();
        workingSet = source.getMessagesSource().getCurrentWorkingSet();

        dialogAdapter = new MessagesAdapter();

        editText = (EditText) res.findViewById(R.id.text);

        updateImeConfig();

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (charSequence.toString().trim().length() > 0) {
                    application.getSyncKernel().getBackgroundSync().onTyping(peerType, peerId);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (sendButton != null) {
                    sendButton.setEnabled(editable.toString().trim().length() > 0);
                }
            }
        });
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEND) {
                    doSend();
                    return true;
                }
                if (i == EditorInfo.IME_ACTION_DONE) {
                    doSend();
                    return true;
                }
                if (application.getUserSettings().isSendByEnter()) {
                    if (keyEvent != null && i == EditorInfo.IME_NULL && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                        doSend();
                        return true;
                    }
                }
                return false;
            }
        });
        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keycode, KeyEvent keyEvent) {
                if (application.getUserSettings().isSendByEnter()) {
                    if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keycode == KeyEvent.KEYCODE_ENTER) {
                        doSend();
                        return true;
                    }
                }
                return false;
            }
        });

        smileButton = (ImageButton) res.findViewById(R.id.smileysButton);
        if (getSmileysController().areSmileysVisible()) {
            smileButton.setImageResource(R.drawable.st_conv_panel_kb);
        } else {
            smileButton.setImageResource(R.drawable.st_conv_panel_smiles);
        }

        smileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getSmileysController().areSmileysVisible()) {
                    smileButton.setImageResource(R.drawable.st_conv_panel_smiles);
                } else {
                    smileButton.setImageResource(R.drawable.st_conv_panel_kb);
                }
                getSmileysController().showSmileys(editText);
            }
        });

        sendButton = (ImageButton) res.findViewById(R.id.sendMessage);
        sendButton.setEnabled(false);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSend();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            listView.setLayerType(View.LAYER_TYPE_NONE, null);
        }

        listView.addHeaderView(topView, null, false);
        listView.addFooterView(bottomPadding, null, false);
        listView.setAdapter(dialogAdapter);

        workingSet = source.getMessagesSource().getCurrentWorkingSet();

        updateContactsPanel(true);

        onDataChanged();

        updateOverlayPanel();

        if (peerType == PeerType.PEER_USER_ENCRYPTED) {
            EncryptedChat chat = application.getEngine().getEncryptedChat(peerId);
            User user = application.getEngine().getUser(chat.getUserId());
            if (chat.isOut()) {
                ((TextView) emptyEncrypted.findViewById(R.id.emptyEncryptedTitle))
                        .setText(getStringSafe(R.string.st_conv_enc_invite_out_title).replace("{name}", user.getFirstName()));
            } else {
                ((TextView) emptyEncrypted.findViewById(R.id.emptyEncryptedTitle))
                        .setText(getStringSafe(R.string.st_conv_enc_invite_in_title).replace("{name}", user.getFirstName()));
            }
        }

        return res;
    }

    private void updateImeConfig() {
        if (application.getUserSettings().isSendByEnter()) {
            editText.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES | EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
            editText.setImeOptions(EditorInfo.IME_ACTION_SEND);
        } else {
            editText.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES | EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
            editText.setImeOptions(EditorInfo.IME_ACTION_NONE);
        }
    }

    private void deleteDialog() {
        AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_dialogs_delete_header)
                .setMessage(peerType == PeerType.PEER_CHAT ? R.string.st_dialogs_delete_group : R.string.st_dialogs_delete_history)
                .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        runUiTask(new AsyncAction() {
                            @Override
                            public void execute() throws AsyncException {
                                if (peerType == PeerType.PEER_USER_ENCRYPTED) {
                                    try {
                                        rpcRaw(new TLRequestMessagesDiscardEncryption(peerId));
                                    } catch (RpcException e) {
                                        e.printStackTrace();
//                                        if ("ENCRYPTION_ALREADY_DECLINED".equals(e.getErrorTag())) {
//                                            e.printStackTrace();
//                                        } else {
//                                            throw new AsyncException(e);
//                                        }
                                    }
                                    getEngine().deleteHistory(peerType, peerId);
                                    getEngine().getSecretEngine().deleteEncryptedChat(peerId);
                                } else {
                                    final TLAbsInputPeer peer;
                                    if (peerType == PeerType.PEER_CHAT) {
                                        peer = new TLInputPeerChat(peerId);
                                    } else {
                                        User user = application.getEngine().getUser(peerId);
                                        peer = new TLInputPeerForeign(user.getUid(), user.getAccessHash());
                                    }

                                    int offset = 0;
                                    do {
                                        TLAffectedHistory affectedHistory = rpc(new TLRequestMessagesDeleteHistory(peer, offset));
                                        application.getUpdateProcessor().onMessage(new TLLocalAffectedHistory(affectedHistory));
                                        offset = affectedHistory.getOffset();
                                    } while (offset != 0);

                                    if (peerId == PeerType.PEER_CHAT) {
                                        rpc(new TLRequestMessagesDeleteChatUser(peerId, new TLInputUserSelf()));
                                    }

                                    getEngine().deleteHistory(peerType, peerId);
                                }
                                application.notifyUIUpdate();
                            }

                            @Override
                            public void afterExecute() {
                                getRootController().doBack();
                            }
                        });
                    }
                }).setNegativeButton(R.string.st_no, null).create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private void updateActionMode() {
        if (selected.size() == 1 && actionMode == null) {
            actionMode = getSherlockActivity().startActionMode(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    mode.getMenuInflater().inflate(R.menu.conv_actions, menu);
                    if (peerType == PeerType.PEER_USER_ENCRYPTED) {
                        menu.findItem(R.id.forwardMessages).setVisible(false);
                    }
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    mode.setTitle("Selected: " + selected.size());
                    return true;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    if (item.getItemId() == R.id.forwardMessages) {
                        ArrayList<Integer> forwardMids = new ArrayList<Integer>();
                        for (Integer i : selected) {
                            ChatMessage message = getEngine().getMessageByDbId(i);
                            if (message != null) {
                                if (message.getState() == MessageState.SENT || message.getState() == MessageState.READED) {
                                    forwardMids.add(message.getMid());
                                }
                            }
                        }
                        mode.finish();

                        if (forwardMids.size() == 0) {
                            Toast.makeText(getActivity(), R.string.st_conv_action_nothing_to_forward, Toast.LENGTH_SHORT).show();
                        } else {
                            getRootController().forwardMessages(forwardMids.toArray(new Integer[0]));
                        }
                        return true;
                    } else if (item.getItemId() == R.id.deleteMessages) {
                        for (Integer i : selected) {
                            ChatMessage message = getEngine().getMessageByDbId(i);
                            if (message != null) {
                                if (message.getState() == MessageState.SENT || message.getState() == MessageState.READED) {
                                    getEngine().deleteSentMessage(i);
                                } else {
                                    getEngine().deleteUnsentMessage(i);
                                }
                            }
                        }
                        application.getSyncKernel().getBackgroundSync().resetDeletionsSync();
                        application.notifyUIUpdate();
                        mode.finish();
                        return true;
                    }
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    actionMode = null;
                    selected.clear();
                    if (dialogAdapter != null) {
                        dialogAdapter.notifyDataSetChanged();
                    }
                }
            });
        } else if (selected.size() == 0 && actionMode != null) {
            actionMode.finish();
            actionMode = null;
        } else if (actionMode != null) {
            actionMode.invalidate();
        }
    }

    protected void updateContactsPanel(boolean initial) {
        if (peerType == PeerType.PEER_USER) {
            if (peerId == 333000) {
                goneView(contactsPanel, false);
                contactsPanel.setOnClickListener(null);
            } else {
                int linkType = application.getEngine().getUser(peerId).getLinkType();
                if (linkType == LinkType.FOREIGN || linkType == LinkType.REQUEST) {
                    showView(contactsPanel, !initial);
                    if (linkType == LinkType.REQUEST) {
                        ((TextView) contactsPanel.findViewById(R.id.panelTitle)).setText(R.string.st_conv_add_contact);
                        contactsPanel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                getRootController().addContact(peerId);
                            }
                        });
                    } else {
                        ((TextView) contactsPanel.findViewById(R.id.panelTitle)).setText(R.string.st_conv_share_info);
                        contactsPanel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                application.getEngine().shareContact(peerType, peerId, application.getCurrentUid());
                                application.getSyncKernel().getBackgroundSync().resetTypingDelay();
                                application.notifyUIUpdate();
                            }
                        });
                    }
                } else {
                    goneView(contactsPanel, !initial);
                    contactsPanel.setOnClickListener(null);
                }
            }
        } else {
            goneView(contactsPanel, !initial);
            contactsPanel.setOnClickListener(null);
        }
    }

    protected void doSend() {
        if (editText == null) {
            return;
        }
        String message = editText.getText().toString().trim();
        if (message.length() == 0) {
            return;
        }
        application.getMessageSender().postTextMessage(peerType, peerId, message);
        application.getSyncKernel().getBackgroundSync().resetTypingDelay();
        onSourceDataChanged();
        editText.setText("");
        application.getTextSaver().clearText(peerType, peerId);
    }

    protected Spanned fixedHtml(String src) {
        Spanned html = Html.fromHtml(src);
        Spannable res = new SpannableString(html.toString());
        for (StyleSpan span : html.getSpans(0, res.length(), StyleSpan.class)) {
            if (span.getStyle() == Typeface.BOLD) {
                bold(res, html.getSpanStart(span), html.getSpanEnd(span));
            }
        }
        for (URLSpan span : html.getSpans(0, res.length(), URLSpan.class)) {
            int uid = Integer.parseInt(span.getURL().substring(1));
            userSpan(res, html.getSpanStart(span), html.getSpanEnd(span), uid);
        }
        return res;
    }

    protected void italic(Spannable src, int start, int end) {
        src.setSpan(new StelsTypefaceSpan("regular", getActivity(), false), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

    }

    protected void bold(Spannable src, int start, int end) {
        src.setSpan(new StelsTypefaceSpan("regular", getActivity(), true), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    }

    private void userSpan(Spannable src, int start, int end, final int uid) {
        src.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View view) {
                view.playSoundEffect(SoundEffectConstants.CLICK);
                getRootController().openUser(uid);
            }

            @Override
            public void updateDrawState(TextPaint ds) {

            }
        }, start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    }

    protected void onMessageClick(final MessageWireframe message) {
        if (message.message.getContentType() == ContentType.MESSAGE_CONTACT) {
            final int uid = ((TLLocalContact) message.message.getExtras()).getUserId();
            getRootController().openUser(uid);
//            if (application.getEngine().getUidContact(uid) != null || uid == application.getCurrentUid()) {
//                getRootController().openUser(uid);
//            } else {
//                new AlertDialog.Builder(getActivity())
//                        .setTitle(R.string.st_conv_action_add_header)
//                        .setMessage(R.string.st_conv_action_add)
//                        .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                getRootController().addContact(uid);
//                            }
//                        })
//                        .setNegativeButton(R.string.st_no, null)
//                        .show();
//            }
            return;
        }

        ArrayList<CharSequence> items = new ArrayList<CharSequence>();
        final ArrayList<Runnable> actions = new ArrayList<Runnable>();

        if (message.message.getRawContentType() == ContentType.MESSAGE_TEXT) {
            items.add(getStringSafe(R.string.st_conv_action_copy));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    copyToPastebin(message.message.getMessage());
                    Toast.makeText(getActivity(), R.string.st_conv_copied, Toast.LENGTH_SHORT).show();
                }
            });
            if (isEnabledInput) {
                items.add(getStringSafe(R.string.st_conv_action_quote));
                actions.add(new Runnable() {
                    @Override
                    public void run() {
                        if (!isEnabledInput) {
                            Toast.makeText(getActivity(), R.string.st_conv_chat_closed_title, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        editText.getText().append("\"" + message.message.getMessage() + "\" ");
                        showKeyboard(editText);
                    }
                });
            }
        }

        if (message.message.getState() != MessageState.PENDING && message.message.getState() != MessageState.FAILURE && peerType != PeerType.PEER_USER_ENCRYPTED) {
            if (isEnabledInput) {
                items.add(getStringSafe(R.string.st_conv_action_forward));
                actions.add(new Runnable() {
                    @Override
                    public void run() {
                        if (!isEnabledInput) {
                            Toast.makeText(getActivity(), R.string.st_conv_chat_closed_title, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        getRootController().forwardMessage(message.message.getMid());
                    }
                });
            }
        }

        if (message.message.getState() == MessageState.FAILURE) {
            items.add(getStringSafe(R.string.st_conv_action_delete));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    application.getEngine().deleteUnsentMessage(message.message.getDatabaseId());
                    application.notifyUIUpdate();
                }
            });
        }

        if (message.message.getState() == MessageState.SENT || message.message.getState() == MessageState.READED) {
            items.add(getStringSafe(R.string.st_conv_action_delete));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    application.getEngine().deleteSentMessage(message.message.getDatabaseId());
                    application.getSyncKernel().getBackgroundSync().resetDeletionsSync();
                    application.notifyUIUpdate();
                }
            });
        }

        if (message.message.isForwarded() && message.message.getRawContentType() == ContentType.MESSAGE_TEXT) {
            final User forwarded = getEngine().getUser(message.message.getForwardSenderId());
            items.add(forwarded.getDisplayName());
            actions.add(new Runnable() {
                @Override
                public void run() {
                    getRootController().openUser(forwarded.getUid());
                }
            });
        }

        if (message.message.getRawContentType() == ContentType.MESSAGE_CONTACT) {
            final TLLocalContact contact = (TLLocalContact) message.message.getExtras();
            items.add(contact.getFirstName() + " " + contact.getLastName());
            actions.add(new Runnable() {
                @Override
                public void run() {
                    getRootController().openUser(contact.getUserId());
                }
            });
        }

        if (message.message.getState() == MessageState.FAILURE && (
                message.message.getRawContentType() == ContentType.MESSAGE_TEXT ||
                        message.message.getRawContentType() == ContentType.MESSAGE_GEO ||
                        message.message.isForwarded())) {
            items.add(getStringSafe(R.string.st_conv_action_try_again));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    application.getEngine().tryAgain(message.message.getDatabaseId());
                    application.notifyUIUpdate();
                }
            });
        }

        if (message.message.getState() == MessageState.FAILURE &&
                (message.message.getRawContentType() == ContentType.MESSAGE_VIDEO ||
                        message.message.getRawContentType() == ContentType.MESSAGE_PHOTO ||
                        message.message.getRawContentType() == ContentType.MESSAGE_DOCUMENT) && !(message.message.isForwarded())) {
            items.add(getStringSafe(R.string.st_conv_action_try_again));
            actions.add(new Runnable() {
                @Override
                public void run() {
                    application.getEngine().tryAgainMedia(message.message.getDatabaseId());
                    application.notifyUIUpdate();
                }
            });
        }

        // Add links
        Spannable text = new SpannableString(message.message.getMessage());
        if (Linkify.addLinks(text, Linkify.ALL)) {
            for (final URLSpan span : text.getSpans(0, text.length(), URLSpan.class)) {
                try {
                    final Uri uri = Uri.parse(span.getURL());

                    items.add(span.getURL());

                    actions.add(new Runnable() {
                        @Override
                        public void run() {
                            getActivity().startActivity(new Intent(Intent.ACTION_VIEW).setData(uri));
                        }
                    });
                } catch (Exception e) {
                    // Just ignore
                }
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_conv_action_title).setItems(items.toArray(new CharSequence[items.size()]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i >= 0 && i < actions.size()) {
                    actions.get(i).run();
                }
            }
        }).create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    @Override
    protected void onPhotoArrived(final String fileName, final int width, final int height, int requestId) {
        if (Build.VERSION.SDK_INT >= 18) {
            listView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    application.getEngine().sendPhoto(peerType, peerId, width, height, fileName);
                    application.notifyUIUpdate();
                }
            }, 300);
        } else {
            application.getEngine().sendPhoto(peerType, peerId, width, height, fileName);
            application.notifyUIUpdate();
        }
    }

    @Override
    protected void onPhotoArrived(final Uri uri, final int width, final int height, int requestId) {
        if (Build.VERSION.SDK_INT >= 18) {
            listView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    application.getEngine().sendPhoto(peerType, peerId, width, height, uri);
                    application.notifyUIUpdate();
                }
            }, 300);
        } else {
            application.getEngine().sendPhoto(peerType, peerId, width, height, uri);
            application.notifyUIUpdate();
        }
    }

    @Override
    protected void onVideoArrived(final String fileName, int requestId) {
        runUiTask(new AsyncAction() {
            @Override
            public void execute() throws AsyncException {
                Bitmap res = ThumbnailUtils.createVideoThumbnail(fileName, MediaStore.Video.Thumbnails.MINI_KIND);
                int w = 0;
                int h = 0;
                if (res != null) {
                    w = res.getWidth();
                    h = res.getHeight();
                }
                application.getEngine().sendVideo(peerType, peerId, new TLUploadingVideo(fileName, w, h));
            }

            @Override
            public void afterExecute() {
                application.notifyUIUpdate();
            }
        });
    }

    private void startAudio() {
        if (audioRecorder != null) {
            updateAudioUi();
            return;
        }

        try {
            audioFile = getUploadTempAudioFile();
            audioRecorder = new MediaRecorder();
            audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            audioRecorder.setOutputFile(audioFile);
            audioRecorder.prepare();
            audioRecorder.start();
            audioStart = SystemClock.uptimeMillis();
        } catch (Exception e) {
            audioRecorder = null;
            audioFile = null;
        }
        updateTimer.run();
    }

    private void sendAudio() {
        if (audioRecorder == null) {
            updateAudioUi();
            return;
        }

        audioRecorder.stop();
        audioRecorder = null;

        application.getEngine().sendAudio(peerType, peerId,
                new TLUploadingAudio(audioFile, (int) ((SystemClock.uptimeMillis() - audioStart) / 1000)));

        updateAudioUi();
    }

    private void cancelAudio() {
        if (audioRecorder == null) {
            updateAudioUi();
            return;
        }

        audioRecorder.stop();
        audioRecorder = null;

        updateAudioUi();
    }

    private void updateAudioUi() {
        if (audioRecorder == null) {
            // Hide
            audioRecordContainer.setVisibility(View.GONE);
        } else {
            // Show
            audioRecordContainer.setVisibility(View.VISIBLE);
            audioRecordTimer.setText(TextUtil.formatDuration((int) ((SystemClock.uptimeMillis() - audioStart) / 1000)));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        source.getMessagesSource().addListener(this);
        application.getChatSource().registerListener(this);
        application.getTypingStates().registerListener(this);
        application.getUserSource().registerListener(this);
        application.getEncryptedChatSource().registerListener(this);

        updateImeConfig();

        application.getUiKernel().onOpenedChat(peerType, peerId);

        if (selectedIndex >= 0) {
            listView.setSelectionFromTop(selectedIndex, selectedTop - listView.getPaddingTop());
        }

        listView.post(new Runnable() {
            @Override
            public void run() {
                if (listView != null) {
                    listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL);
                }
            }
        });

//        if (workingSet != source.getMessagesSource().getCurrentWorkingSet()) {
//            onSourceDataChanged();
//        }
        onSourceDataChanged();

        if (getSmileysController().areSmileysVisible()) {
            smileButton.setImageResource(R.drawable.st_conv_panel_kb);
        } else {
            smileButton.setImageResource(R.drawable.st_conv_panel_smiles);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            switch (application.getTechKernel().getDebugSettings().getConversationListLayerType()) {
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

        String text = application.getTextSaver().getText(peerType, peerId);
        if (text != null && editText != null) {
            editText.setText(text);
            showKeyboard(editText);
        }

        getSherlockActivity().invalidateOptionsMenu();
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater
            inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.conv_menu, menu);
        menu.findItem(R.id.attachCamera).setTitle(highlightMenuText(R.string.st_conv_menu_photo));
        menu.findItem(R.id.attachVideo).setTitle(highlightMenuText(R.string.st_conv_menu_video));
        menu.findItem(R.id.attachLocation).setTitle(highlightMenuText(R.string.st_conv_menu_location));

        FastWebImageView imageView = (FastWebImageView) menu.findItem(R.id.userAvatar)
                .getActionView().findViewById(R.id.image);
        View touchLayer = menu.findItem(R.id.userAvatar).getActionView().findViewById(R.id.avatarTouchLayer);
        int padding = 0;//(int) (getPx(1) * (getBarHeight() / ((float) getPx(48))) + 0.5f);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(getBarHeight() - padding, getBarHeight() - padding, Gravity.TOP | Gravity.RIGHT);
        params.bottomMargin = padding;
        imageView.setLayoutParams(params);
        touchLayer.setLayoutParams(params);

        menu.findItem(R.id.userAvatar)
                .getActionView().findViewById(R.id.imageOverlay).setLayoutParams(params);

        if (peerType == PeerType.PEER_USER) {
            if (peerId == 333000) {
                imageView.setLoadingDrawable(R.drawable.st_support_avatar);
                touchLayer.setOnClickListener(null);
            } else {
                imageView.setLoadingDrawable(Placeholders.USER_PLACEHOLDERS[peerId % Placeholders.USER_PLACEHOLDERS.length]);
                User usr = application.getEngine().getUser(peerId);
                if (usr != null) {
                    if (usr.getPhoto() instanceof TLLocalAvatarPhoto) {
                        TLLocalAvatarPhoto localAvatarPhoto = (TLLocalAvatarPhoto) usr.getPhoto();
                        if (localAvatarPhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                            imageView.requestTask(
                                    new StelsImageTask((TLLocalFileLocation) localAvatarPhoto.getPreviewLocation()));
                        } else {
                            imageView.requestTask(null);
                        }
                    } else {
                        imageView.requestTask(null);
                    }
                } else {
                    imageView.requestTask(null);
                }
                touchLayer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getRootController().openUser(peerId);
                    }
                });
            }
        } else if (peerType == PeerType.PEER_CHAT) {
            imageView.setLoadingDrawable(Placeholders.GROUP_PLACEHOLDERS[peerId % Placeholders.GROUP_PLACEHOLDERS.length]);
            Group group = getEngine().getGroupsEngine().getGroup(peerId);
            if (group != null) {
                if (group.getAvatar() instanceof TLLocalAvatarPhoto) {
                    TLLocalAvatarPhoto avatarPhoto = (TLLocalAvatarPhoto) group.getAvatar();
                    if (avatarPhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                        imageView.requestTask(new StelsImageTask((TLLocalFileLocation) avatarPhoto.getPreviewLocation()));
                    } else {
                        imageView.requestTask(null);
                    }
                } else {
                    imageView.requestTask(null);
                }
            } else {
                imageView.requestTask(null);
            }
            touchLayer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getRootController().openChatEdit(peerId);
                }
            });
        } else {
            final EncryptedChat chat = application.getEngine().getEncryptedChat(peerId);
            if (chat.getUserId() == 333000) {
                imageView.setLoadingDrawable(R.drawable.st_support_avatar);
                touchLayer.setOnClickListener(null);
            } else {
                imageView.setLoadingDrawable(Placeholders.USER_PLACEHOLDERS[chat.getUserId() % Placeholders.USER_PLACEHOLDERS.length]);
                User usr = application.getEngine().getUser(chat.getUserId());
                if (usr != null) {
                    if (usr.getPhoto() instanceof TLLocalAvatarPhoto) {
                        TLLocalAvatarPhoto localAvatarPhoto = (TLLocalAvatarPhoto) usr.getPhoto();
                        if (localAvatarPhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                            imageView.requestTask(
                                    new StelsImageTask((TLLocalFileLocation) localAvatarPhoto.getPreviewLocation()));
                        } else {
                            imageView.requestTask(null);
                        }
                    } else {
                        imageView.requestTask(null);
                    }
                } else {
                    imageView.requestTask(null);
                }
                touchLayer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getRootController().openSecretChatInfo(chat.getId());
                    }
                });
            }
        }

        updateHeader();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.attachCamera) {
            if (!isEnabledInput) {
                Toast.makeText(getActivity(), R.string.st_conv_chat_closed_title, Toast.LENGTH_SHORT).show();
                return true;
            }
            requestPhotoChooser(0);
            return true;
        }
        if (item.getItemId() == R.id.attachVideo) {
            if (!isEnabledInput) {
                Toast.makeText(getActivity(), R.string.st_conv_chat_closed_title, Toast.LENGTH_SHORT).show();
                return true;
            }

            requestVideo(2);
            return true;
        }

        if (item.getItemId() == R.id.attachLocation) {
            if (!isEnabledInput) {
                Toast.makeText(getActivity(), R.string.st_conv_chat_closed_title, Toast.LENGTH_SHORT).show();
                return true;
            }
            pickLocation();
            return true;
        }

        if (item.getItemId() == R.id.attachDocument) {
            if (!isEnabledInput) {
                Toast.makeText(getActivity(), R.string.st_conv_chat_closed_title, Toast.LENGTH_SHORT).show();
                return true;
            }

            pickFile();
            return true;
        }

        if (item.getItemId() == R.id.attachAudio) {
            if (!isEnabledInput) {
                Toast.makeText(getActivity(), R.string.st_conv_chat_closed_title, Toast.LENGTH_SHORT).show();
                return true;
            }

            startAudio();
            return true;
        }

        return false;
    }

    @Override
    protected void onFragmentResult(int resultCode, Object data) {
        if (resultCode == Activity.RESULT_OK) {
            if (data instanceof TLLocalGeo) {
                TLLocalGeo point = (TLLocalGeo) data;
                application.getEngine().sendLocation(peerType, peerId, point);
                application.getSyncKernel().getBackgroundSync().resetTypingDelay();
                application.notifyUIUpdate();
                editText.setText("");
            } else if (data instanceof String) {
                application.getEngine().sendDocument(peerType, peerId, new TLUploadingDocument((String) data));
                application.notifyUIUpdate();
            }
        }
    }

    private void hideOverlayPanel() {
        editText.setEnabled(true);
        inputOverlay.setVisibility(View.GONE);
        isEnabledInput = true;
    }

    private void showOverlayPanel() {
        if (getSmileysController() != null) {
            if (getSmileysController().areSmileysVisible()) {
                getSmileysController().hideSmileys();
            }
        }
        editText.setEnabled(false);
        inputOverlay.setVisibility(View.VISIBLE);
        isEnabledInput = false;
    }

    private void updateOverlayPanel() {
        if (peerType == PeerType.PEER_CHAT) {
            Group group = application.getEngine().getGroupsEngine().getGroup(peerId);
            FullChatInfo chatInfo = application.getChatSource().getChatInfo(peerId);
            if ((chatInfo != null && chatInfo.getChatInfo().isForbidden()) || (group != null && group.isForbidden())) {
                showOverlayPanel();
                deleteButton.setVisibility(View.VISIBLE);
                waitMessageView.setVisibility(View.GONE);
            } else {
                hideOverlayPanel();
            }
        } else if (peerType == PeerType.PEER_USER_ENCRYPTED) {
            EncryptedChat chat = application.getEngine().getEncryptedChat(peerId);
            if (chat == null) {
                showOverlayPanel();
                deleteButton.setVisibility(View.VISIBLE);
                waitMessageView.setVisibility(View.GONE);
            } else if (chat.getState() == EncryptedChatState.DISCARDED) {
                showOverlayPanel();
                deleteButton.setVisibility(View.VISIBLE);
                waitMessageView.setVisibility(View.GONE);
            } else if (chat.getState() != EncryptedChatState.NORMAL) {
                if (chat.getState() == EncryptedChatState.REQUESTED) {
                    waitMessageView.setText(R.string.st_conv_enc_exchanging_keys);
                } else {
                    waitMessageView.setText(html(
                            getStringSafe(R.string.st_conv_enc_waiting)
                                    .replace("{name}", application.getEngine().getUser(chat.getUserId()).getFirstName())));
                }
                showOverlayPanel();
                deleteButton.setVisibility(View.GONE);
                waitMessageView.setVisibility(View.VISIBLE);
            } else {
                hideOverlayPanel();
            }
        } else {
            hideOverlayPanel();
        }
    }

    private void updateHeader() {
        updateContactsPanel(false);
        updateOverlayPanel();
        if (peerType == PeerType.PEER_USER) {
            if (peerId == 333000) {
                getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText("Telegram"));
                if (application.getTypingStates().isUserTyping(peerId)) {
                    getSherlockActivity().getSupportActionBar().setSubtitle(highlightSubtitleText(R.string.lang_common_typing));
                } else {
                    getSherlockActivity().getSupportActionBar().setSubtitle(highlightSubtitleText(R.string.st_support_hint));
                }
            } else {
                User user = application.getEngine().getUser(peerId);
                getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(user.getDisplayName()));

                if (application.getTypingStates().isUserTyping(peerId)) {
                    getSherlockActivity().getSupportActionBar().setSubtitle(highlightSubtitleText(R.string.lang_common_typing));
                } else {
                    int status = getUserState(user.getStatus());
                    if (status < 0) {
                        getSherlockActivity().getSupportActionBar().setSubtitle(highlightSubtitleText(R.string.st_offline));
                    } else if (status == 0) {
                        getSherlockActivity().getSupportActionBar().setSubtitle(highlightSubtitleText(R.string.st_online));
                    } else {
                        getSherlockActivity().getSupportActionBar().setSubtitle(
                                highlightSubtitleText(TextUtil.formatHumanReadableLastSeen(status, getStringSafe(R.string.st_lang))));
                    }
                }
            }
        } else if (peerType == PeerType.PEER_USER_ENCRYPTED) {
            final EncryptedChat chat = application.getEngine().getEncryptedChat(peerId);
            if (chat.getUserId() == 333000) {
                getSherlockActivity().getSupportActionBar().setTitle(highlightSecureTitleText("Telegram"));
                if (application.getTypingStates().isEncryptedTyping(chat.getId())) {
                    getSherlockActivity().getSupportActionBar().setSubtitle(highlightSubtitleText(R.string.lang_common_typing));
                } else {
                    getSherlockActivity().getSupportActionBar().setSubtitle(highlightSubtitleText(R.string.st_support_hint));
                }
            } else {
                User user = application.getEngine().getUser(chat.getUserId());
                getSherlockActivity().getSupportActionBar().setTitle(highlightSecureTitleText(user.getDisplayName()));
                if (chat.getState() == EncryptedChatState.DISCARDED) {
                    getSherlockActivity().getSupportActionBar().setSubtitle(highlightSubtitleText(R.string.st_conv_enc_cancelled));
                } else {
                    if (application.getTypingStates().isEncryptedTyping(chat.getId())) {
                        getSherlockActivity().getSupportActionBar().setSubtitle(highlightSubtitleText(R.string.lang_common_typing));
                    } else {
                        int status = getUserState(user.getStatus());
                        if (status < 0) {
                            getSherlockActivity().getSupportActionBar().setSubtitle(highlightSubtitleText(R.string.st_offline));
                        } else if (status == 0) {
                            getSherlockActivity().getSupportActionBar().setSubtitle(highlightSubtitleText(R.string.st_online));
                        } else {
                            getSherlockActivity().getSupportActionBar().setSubtitle(
                                    highlightSubtitleText(TextUtil.formatHumanReadableLastSeen(status, getStringSafe(R.string.st_lang))));
                        }
                    }
                }
            }
        } else {
            Group group = application.getEngine().getGroupsEngine().getGroup(peerId);
            if (group == null)
                return;
            getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(group.getTitle()));
            FullChatInfo chatInfo = application.getChatSource().getChatInfo(peerId);
            if (group.isForbidden() || (chatInfo != null && chatInfo.getChatInfo().isForbidden())) {
                getSherlockActivity().getSupportActionBar().setSubtitle(highlightSubtitleText(R.string.st_conv_removed));
            } else {
                int[] typing = application.getTypingStates().getChatTypes(peerId);
                if (typing.length == 0) {
                    if (chatInfo != null) {
                        int onlineCount = 0;
                        for (TLLocalChatParticipant participant : chatInfo.getChatInfo().getUsers()) {
                            if (getUserState(getEngine().getUser(participant.getUid()).getStatus()) == 0) {
                                onlineCount++;
                            }
                        }
                        if (onlineCount != 0) {
                            String title = "<light>" + getQuantityString(R.plurals.st_members,
                                    group.getUsersCount()).replace("{d}", "" +
                                    I18nUtil.getInstance().correctFormatNumber(group.getUsersCount()));
                            title += ",</light> " + I18nUtil.getInstance().correctFormatNumber(onlineCount) + " " + getStringSafe(R.string.st_online);
                            getSherlockActivity().getSupportActionBar().setSubtitle(highlightSubtitleText(title));
                        } else {
                            getSherlockActivity().getSupportActionBar().setSubtitle(
                                    highlightSubtitleText(
                                            "<light>" +
                                                    getQuantityString(R.plurals.st_members,
                                                            group.getUsersCount())
                                                            .replace("{d}", "" + I18nUtil.getInstance().correctFormatNumber(group.getUsersCount())) + "</light>"));
                        }
                    } else {
                        getSherlockActivity().getSupportActionBar().setSubtitle(
                                highlightSubtitleText(
                                        "<light>" +
                                                getQuantityString(R.plurals.st_members,
                                                        group.getUsersCount())
                                                        .replace("{d}", "" + I18nUtil.getInstance().correctFormatNumber(group.getUsersCount())) + "</light>"));
                    }
                } else {
                    String[] names = new String[typing.length];
                    for (int i = 0; i < names.length; i++) {
                        names[i] = application.getEngine().getUser(typing[i]).getFirstName();
                    }
                    getSherlockActivity().getSupportActionBar().setSubtitle(highlightSubtitleText(TextUtil.formatTyping(names)));
                }
            }
        }

        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
    }

    public void onDataChanged() {
        boolean isLoading = (SystemClock.uptimeMillis() - viewCreateTime < 500);
        if (dialogAdapter.getCount() == 0) {
            if (source.getMessagesSource().getState() == ViewSourceState.COMPLETED) {
                goneView(loading, !isLoading);
                if (peerType == PeerType.PEER_USER_ENCRYPTED) {
                    showView(emptyEncrypted, !isLoading);
                } else {
                    showView(empty, !isLoading);
                }
            } else {
                showView(loading, !isLoading);
                if (peerType == PeerType.PEER_USER_ENCRYPTED) {
                    emptyEncrypted.setVisibility(View.GONE);
                } else {
                    empty.setVisibility(View.GONE);
                }
            }

            messagesCount.setVisibility(View.GONE);
            tryAgain.setVisibility(View.GONE);
            listView.setVisibility(View.GONE);
        } else {
            goneView(loading, !isLoading);
            if (peerType == PeerType.PEER_USER_ENCRYPTED) {
                goneView(emptyEncrypted, !isLoading);
            } else {
                goneView(empty, !isLoading);
            }
            showView(listView, !isLoading);

            if (source.getMessagesSource().getState() == ViewSourceState.IN_PROGRESS) {
                loadingMore.setVisibility(View.VISIBLE);
                messagesCount.setVisibility(View.GONE);
                tryAgain.setVisibility(View.GONE);
            } else {
                loadingMore.setVisibility(View.GONE);
                if (source.getMessagesSource().getState() == ViewSourceState.LOAD_MORE_ERROR) {
                    tryAgain.setVisibility(View.VISIBLE);
                    messagesCount.setVisibility(View.GONE);
                } else {
                    tryAgain.setVisibility(View.GONE);
                    messagesCount.setVisibility(View.GONE);

                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("peerId", peerId);
        outState.putInt("peerType", peerType);

        saveListPosition();
        outState.putInt("selectedIndex", selectedIndex);
        outState.putInt("selectedTop", selectedTop);
    }

    @Override
    public void onPause() {
        super.onPause();
        source.getMessagesSource().removeListener(this);
        application.getChatSource().unregisterListener(this);
        application.getTypingStates().unregisterListener(this);
        application.getUserSource().unregisterListener(this);
        application.getEncryptedChatSource().unregisterListener(this);

        application.getUiKernel().onClosedChat(peerType, peerId);

        if (editText != null && editText.getText().toString().trim().length() > 0) {
            application.getTextSaver().saveText(editText.getText().toString().trim(), peerType, peerId);
        } else {
            application.getTextSaver().clearText(peerType, peerId);
        }

        if (actionMode != null) {
            actionMode.finish();
        }

        hideKeyboard(editText);

        isFreshUpdate = true;

        saveListPosition();

        cancelAudio();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listView = null;
        dialogAdapter = null;
        loadingMore = null;
        tryAgain = null;
        messagesCount = null;
        smileButton = null;

        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        editText = null;

        sendButton = null;
        empty = null;
        loading = null;
    }

    @Override
    public void onChatChanged(int chatId, DialogDescription description) {
        if (peerType == PeerType.PEER_CHAT && peerId == chatId) {
            getSherlockActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public void onChatTypingChanged(int chatId, int[] uids) {
        if (peerType == PeerType.PEER_CHAT && peerId == chatId) {
            getSherlockActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public void onUserTypingChanged(int uid, boolean types) {
        if (peerType == PeerType.PEER_USER && peerId == uid) {
            getSherlockActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public void onEncryptedTypingChanged(int chatId, boolean types) {
        if (peerType == PeerType.PEER_USER_ENCRYPTED && peerId == chatId) {
            getSherlockActivity().invalidateOptionsMenu();
        }
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
    public void onSourceStateChanged() {
        onDataChanged();
    }

    @Override
    public void onSourceDataChanged() {
        long start = SystemClock.uptimeMillis();
        ArrayList<MessageWireframe> nWorkingSet = source.getMessagesSource().getCurrentWorkingSet();
        Logger.d(TAG, "onSourceDataChanged: " + firstUnreadMessage + ", " + isFreshUpdate);
        if (isFreshUpdate) {
            if (nWorkingSet.size() == 0) {
                workingSet = nWorkingSet;
                dialogAdapter.notifyDataSetChanged();
                onDataChanged();
//                if (source.getState() == MessagesSourceState.COMPLETED) {
//                    isFreshUpdate = false;
//                }
                return;
            }

            DialogDescription description = application.getEngine().getDescriptionForPeer(peerType, peerId);
            if (description != null && description.getFirstUnreadMessage() != 0 && nWorkingSet.size() > 0) {
                long unreadMessage = description.getFirstUnreadMessage();
                application.getEngine().getDialogsEngine().clearFirstUnreadMessage(peerType, peerId);
                Logger.d(TAG, "Founded first unread message: " + unreadMessage);
                int index = -1;
                for (int i = 0; i < nWorkingSet.size(); i++) {
                    MessageWireframe message = nWorkingSet.get(i);
                    if (peerType != PeerType.PEER_USER_ENCRYPTED) {
                        if (!message.message.isOut() && unreadMessage == message.message.getMid()) {
                            index = i;
                            break;
                        }
                    } else {
                        if (!message.message.isOut() && unreadMessage == message.message.getRandomId()) {
                            index = i;
                            break;
                        }
                    }
                }
                if (index != -1) {
                    firstUnreadMessage = unreadMessage;
                    unreadCount = 0;
                    for (int i = 0; i <= index; i++) {
                        ChatMessage message = nWorkingSet.get(i).message;
                        if (!message.isOut()) {
                            unreadCount++;
                        }
                    }

                    Logger.d(TAG, "Scrolling to first unread message: " + index + ", count: " + unreadCount);

                    if (unreadCount < MINIMAL_NEWDIV_COUNT) {
                        Logger.d(TAG, "Scrolling to first unread message: " + index + ", count: " + unreadCount);
                        firstUnreadMessage = 0;
                        unreadCount = 0;
                    } else {
                        workingSet = nWorkingSet;
                        dialogAdapter.notifyDataSetChanged();
                        onDataChanged();

                        final int finalIndex = index;
                        listView.setSelectionFromTop(workingSet.size() - finalIndex, getPx(64));
                        listView.post(new Runnable() {
                            @Override
                            public void run() {
                                listView.setSelectionFromTop(workingSet.size() - finalIndex, getPx(64));
                            }
                        });
                        isFreshUpdate = false;
                        return;
                    }
                } else {
                    Logger.d(TAG, "Unable to find unread message inf working set");
                }
            }
        }

        View v = listView.getChildAt(listView.getChildCount() - 2);
        int index = listView.getLastVisiblePosition() - 1;
        long id = listView.getItemIdAtPosition(index);
        int count = dialogAdapter.getCount();
        int top = ((v == null) ? 0 : v.getTop()) - listView.getPaddingTop();

        workingSet = nWorkingSet;
        dialogAdapter.notifyDataSetChanged();

        int delta = Math.abs(dialogAdapter.getCount() - count);

        if (delta != 0) {
            int newIndex = index + (dialogAdapter.getCount() - count) + 1;

            for (int i = index - delta; i < index + delta + 1; i++) {
                if (i < 0)
                    continue;
                if (i >= dialogAdapter.getCount())
                    break;

                if (listView.getItemIdAtPosition(i) == id) {
                    newIndex = i;
                    break;
                }
            }

            listView.setSelectionFromTop(newIndex, top);
        }

        onDataChanged();

        Logger.d(TAG, "onSourceDataChanged time: " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    @Override
    public void onEncryptedChatChanged(int chatId, EncryptedChat chat) {
        if (peerType == PeerType.PEER_USER_ENCRYPTED && chatId == peerId) {
            getSherlockActivity().invalidateOptionsMenu();
        }
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
    public void onUsersChanged(User[] users) {

        if (peerType == PeerType.PEER_USER) {
            for (User u : users) {
                if (u.getUid() == peerId) {
                    getSherlockActivity().invalidateOptionsMenu();
                    return;
                }
            }
        } else if (peerType == PeerType.PEER_USER_ENCRYPTED) {
            EncryptedChat encryptedChat = application.getEngine().getEncryptedChat(peerId);
            if (encryptedChat != null) {
                for (User u : users) {
                    if (u.getUid() == encryptedChat.getUserId()) {
                        getSherlockActivity().invalidateOptionsMenu();
                        return;
                    }
                }
            }
        }
    }

    private class MessagesAdapter extends BaseAdapter implements ConversationAdapter {

        @Override
        public int getCount() {
            return workingSet.size();
        }

        @Override
        public MessageWireframe getItem(int i) {
            return workingSet.get(getCount() - i - 1);
        }

        @Override
        public long getItemId(int i) {
            return getItem(i).message.getDatabaseId();
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            MessageWireframe message = getItem(i);
            if (view == null) {
                view = newView(application, message, viewGroup);
            }

            boolean showTime = i == 0;
            if (i > 0) {
                MessageWireframe prev = getItem(i - 1);
                showTime = !TextUtil.areSameDays(prev.message.getDate(), message.message.getDate());
            }

            bindView(view, application, message, showTime);

            source.getMessagesSource().onItemsShown(getCount() - i - 1);

            return view;
        }

        @Override
        public int getItemViewType(int position) {
            MessageWireframe msg = getItem(position);

            if (msg.message.getRawContentType() == ContentType.MESSAGE_PHOTO ||
                    msg.message.getRawContentType() == ContentType.MESSAGE_VIDEO ||
                    msg.message.getRawContentType() == ContentType.MESSAGE_GEO ||
                    msg.message.getRawContentType() == ContentType.MESSAGE_UNKNOWN ||
                    msg.message.getRawContentType() == ContentType.MESSAGE_DOC_PREVIEW ||
                    msg.message.getRawContentType() == ContentType.MESSAGE_DOC_ANIMATED) {
                return 1;
            }

            if (msg.message.getRawContentType() == ContentType.MESSAGE_AUDIO) {
                return 2;
            }

            if (msg.message.getRawContentType() == ContentType.MESSAGE_DOCUMENT) {
                return 3;
            }

            if (msg.message.getRawContentType() == ContentType.MESSAGE_SYSTEM) {
                return 4;
            }

            if (msg.message.getRawContentType() == ContentType.MESSAGE_CONTACT) {
                return 5;
            }

            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 6;
        }


        public View newView(Context context, MessageWireframe object, ViewGroup parent) {
            if (object.message.getRawContentType() == ContentType.MESSAGE_SYSTEM) {
                return View.inflate(context, R.layout.conv_item_system, null);
            }

            if (object.message.getRawContentType() == ContentType.MESSAGE_DOCUMENT) {
                return new MessageDocumentView(context);
            }

            if (object.message.getRawContentType() == ContentType.MESSAGE_AUDIO) {
                return new MessageAudioView(context);
            }

            if (object.message.getRawContentType() == ContentType.MESSAGE_PHOTO ||
                    object.message.getRawContentType() == ContentType.MESSAGE_VIDEO ||
                    object.message.getRawContentType() == ContentType.MESSAGE_GEO ||
                    object.message.getRawContentType() == ContentType.MESSAGE_UNKNOWN ||
                    object.message.getRawContentType() == ContentType.MESSAGE_DOC_PREVIEW ||
                    object.message.getRawContentType() == ContentType.MESSAGE_DOC_ANIMATED) {
                return new MessageMediaView(context);
            } else if (object.message.getRawContentType() == ContentType.MESSAGE_CONTACT) {
                return new MessageContactView(context);
            } else {
                return new MessageView(context);
            }
        }

        public void bindView(View view, Context context, final MessageWireframe object, boolean showTime) {
            if (object.message.getRawContentType() == ContentType.MESSAGE_SYSTEM) {
                TextView timeView = (TextView) view.findViewById(R.id.time);
                if (showTime) {
                    timeView.setText(TextUtil.formatDateLong(object.message.getDate()));
                    timeView.setVisibility(View.VISIBLE);
                } else {
                    timeView.setVisibility(View.GONE);
                }

                boolean showDiv = false;
                if (!object.message.isOut() && object.message.getMid() == firstUnreadMessage) {
                    showDiv = true;
                }

                TextView unreadView = (TextView) view.findViewById(R.id.unreadDivider);

                if (showDiv) {
                    unreadView.setText(I18nUtil.getInstance().getPluralFormatted(R.plurals.st_new_messages, unreadCount));
                    unreadView.setVisibility(View.VISIBLE);
                } else {
                    unreadView.setVisibility(View.GONE);
                }

                TextView messageView = (TextView) view.findViewById(R.id.message);
                messageView.setMovementMethod(LinkMovementMethod.getInstance());
                FastWebImageView imageView = (FastWebImageView) view.findViewById(R.id.image);

                TLAbsLocalAction action = (TLAbsLocalAction) object.message.getExtras();
                User sender = getEngine().getUser(object.message.getSenderId());
                boolean byMyself = sender.getUid() == application.getCurrentUid();
                String senderName = byMyself ? getStringSafe(R.string.st_message_by_you) : sender.getDisplayName();
                String senderHtml = "<b><a href='#" + sender.getUid() + "'>" + unicodeWrap(TextUtils.htmlEncode(senderName)) + "</a></b>";
                if (action instanceof TLLocalActionUserEditPhoto) {
                    final TLLocalActionUserEditPhoto chatEditPhoto = (TLLocalActionUserEditPhoto) action;
                    messageView.setText(fixedHtml(getStringSafe(R.string.st_message_user_photo_change).replace("{0}", senderHtml)));
                    imageView.setVisibility(View.VISIBLE);
                    if (chatEditPhoto.getPhoto().getPreviewLocation() instanceof TLLocalFileLocation) {
                        imageView.requestTask(new StelsImageTask((TLLocalFileLocation) chatEditPhoto.getPhoto().getPreviewLocation()));
                    } else {
                        imageView.requestTask(null);
                    }
                    if (chatEditPhoto.getPhoto().getFullLocation() instanceof TLLocalFileLocation) {
                        imageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                getRootController().openImage((TLLocalFileLocation) chatEditPhoto.getPhoto().getFullLocation());
                            }
                        });
                    } else {
                        imageView.setOnClickListener(null);
                    }
                } else if (action instanceof TLLocalActionChatEditPhoto) {
                    final TLLocalActionChatEditPhoto chatEditPhoto = (TLLocalActionChatEditPhoto) action;
                    messageView.setText(fixedHtml(getStringSafe(byMyself ? R.string.st_message_photo_change_you : R.string.st_message_photo_change).replace("{0}", senderHtml)));
                    imageView.setVisibility(View.VISIBLE);
                    if (chatEditPhoto.getPhoto().getPreviewLocation() instanceof TLLocalFileLocation) {
                        imageView.requestTask(new StelsImageTask((TLLocalFileLocation) chatEditPhoto.getPhoto().getPreviewLocation()));
                    } else {
                        imageView.requestTask(null);
                    }
                    if (chatEditPhoto.getPhoto().getFullLocation() instanceof TLLocalFileLocation) {
                        imageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                getRootController().openImage((TLLocalFileLocation) chatEditPhoto.getPhoto().getFullLocation());
                            }
                        });
                    } else {
                        imageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                getRootController().openImage((TLLocalFileLocation) chatEditPhoto.getPhoto().getPreviewLocation());
                            }
                        });
                    }
                } else if (action instanceof TLLocalActionChatCreate) {
                    messageView.setText(fixedHtml(getStringSafe(byMyself ? R.string.st_message_create_group_you : R.string.st_message_create_group).replace("{0}", senderHtml)));
                    imageView.setVisibility(View.GONE);
                } else if (action instanceof TLLocalActionChatAddUser) {
                    TLLocalActionChatAddUser addUser = (TLLocalActionChatAddUser) action;
                    User added = getEngine().getUser(addUser.getUserId());
                    String addedName = added.getUid() == application.getCurrentUid() ? getStringSafe(R.string.st_message_you) : added.getDisplayName();
                    String addedHtml = "<b><a href='#" + added.getUid() + "'>" + unicodeWrap(TextUtils.htmlEncode(addedName)) + "</a></b>";
                    messageView.setText(fixedHtml(getStringSafe(addUser.getUserId() == application.getCurrentUid() ? R.string.st_message_added_user_of_you : (byMyself ? R.string.st_message_added_user_you : R.string.st_message_added_user))
                            .replace("{0}", senderHtml)
                            .replace("{1}", addedHtml)));
                    imageView.setVisibility(View.GONE);
                } else if (action instanceof TLLocalActionChatDeletePhoto) {
                    messageView.setText(fixedHtml(getStringSafe(byMyself ? R.string.st_message_photo_remove_you : R.string.st_message_photo_remove)
                            .replace("{0}", senderHtml)));
                    imageView.setVisibility(View.GONE);
                } else if (action instanceof TLLocalActionChatEditTitle) {
                    TLLocalActionChatEditTitle editTitle = (TLLocalActionChatEditTitle) action;
                    messageView.setText(fixedHtml(getStringSafe(byMyself ? R.string.st_message_name_change_you : R.string.st_message_name_change)
                            .replace("{0}", senderHtml)
                            .replace("{1}", "<b>" + unicodeWrap(TextUtils.htmlEncode(editTitle.getTitle())) + "</b>")));
                    imageView.setVisibility(View.GONE);
                } else if (action instanceof TLLocalActionChatDeleteUser) {
                    TLLocalActionChatDeleteUser deleteUser = (TLLocalActionChatDeleteUser) action;
                    if (object.message.getSenderId() == deleteUser.getUserId()) {
                        messageView.setText(fixedHtml(getStringSafe(byMyself ? R.string.st_message_left_user_you : R.string.st_message_left_user).replace("{0}", senderHtml)));
                    } else {
                        User removed = getEngine().getUser(deleteUser.getUserId());
                        String removedName = removed.getUid() == application.getCurrentUid() ? getStringSafe(R.string.st_message_you) : removed.getDisplayName();
                        String removedHtml = "<b><a href='#" + removed.getUid() + "'>" + unicodeWrap(TextUtils.htmlEncode(removedName)) + "</a></b>";
                        messageView.setText(fixedHtml(getStringSafe(deleteUser.getUserId() == application.getCurrentUid() ? R.string.st_message_kicked_user_of_you : (byMyself ? R.string.st_message_kicked_user_you : R.string.st_message_kicked_user))
                                .replace("{0}", senderHtml)
                                .replace("{1}", removedHtml)));
                    }
                    imageView.setVisibility(View.GONE);
                } else if (action instanceof TLLocalActionEncryptedTtl) {
                    TLLocalActionEncryptedTtl ttl = (TLLocalActionEncryptedTtl) action;
                    if (ttl.getTtlSeconds() > 0) {
                        messageView.setText(fixedHtml(getStringSafe(R.string.st_message_enc_self_destruct)
                                .replace("{0}", senderHtml)
                                .replace("{1}", "<b>" + unicodeWrap(TextUtil.formatHumanReadableDuration(((TLLocalActionEncryptedTtl) action).getTtlSeconds())) + "</b>")));
                    } else {
                        messageView.setText(fixedHtml(getStringSafe(R.string.st_message_enc_self_destruct_off)
                                .replace("{0}", senderHtml)));
                    }

                    imageView.setVisibility(View.GONE);
                } else if (action instanceof TLLocalActionUserRegistered) {
                    messageView.setText(fixedHtml(getStringSafe(R.string.st_message_user_joined_app)
                            .replace("{0}", senderHtml)));
                    imageView.setVisibility(View.GONE);
                } else {
                    messageView.setText(R.string.st_message_service);
                    imageView.setVisibility(View.GONE);
                }
            } else {
                bindChatView((BaseMsgView) view, context, object, showTime);
            }
        }

        public void bindChatView(final BaseMsgView view, final Context context, final MessageWireframe object, boolean showTime) {
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (selected.contains(object.message.getDatabaseId())) {
                        selected.remove(object.message.getDatabaseId());
                    } else {
                        selected.add(object.message.getDatabaseId());
                    }
                    ((BaseMsgView) v).setChecked(selected.contains(object.message.getDatabaseId()));
                    updateActionMode();
                    return true;
                }
            });

            view.setChecked(selected.contains(object.message.getDatabaseId()));

            if (object.message.getRawContentType() == ContentType.MESSAGE_DOCUMENT) {
                MessageDocumentView documentView = (MessageDocumentView) view;

                boolean showDiv = false;
                if (!object.message.isOut() && object.message.getMid() == firstUnreadMessage) {
                    showDiv = true;
                }
                documentView.bind(object, showTime, showDiv, unreadCount);

                documentView.setOnBubbleClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (selected.size() > 0) {
                            if (selected.contains(object.message.getDatabaseId())) {
                                selected.remove(object.message.getDatabaseId());
                            } else {
                                selected.add(object.message.getDatabaseId());
                            }
                            ((BaseMsgView) view).setChecked(selected.contains(object.message.getDatabaseId()));
                            updateActionMode();
                        } else {
                            if (object.message.getExtras() instanceof TLUploadingDocument) {
                                if (object.message.getState() == MessageState.FAILURE) {
                                    onMessageClick(object);
                                } else {
                                    AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_conv_cancel_title).setMessage(R.string.st_conv_cancel_message)
                                            .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    application.getMediaSender().cancelUpload(object.message.getDatabaseId());
                                                    application.notifyUIUpdate();
                                                }
                                            }).setNegativeButton(R.string.st_no, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {

                                                }
                                            }).create();
                                    dialog.setCanceledOnTouchOutside(true);
                                    dialog.show();
                                }
                            } else if (object.message.getExtras() instanceof TLLocalDocument) {
                                final String key = DownloadManager.getDocumentKey((TLLocalDocument) object.message.getExtras());
                                final TLLocalDocument doc = (TLLocalDocument) object.message.getExtras();
                                DownloadState state = application.getDownloadManager().getState(key);
                                if (state == DownloadState.COMPLETED) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);

                                    String mimeType = null;
                                    if (doc.getFileName().indexOf('.') > -1) {
                                        String ext = doc.getFileName().substring(doc.getFileName().lastIndexOf('.') + 1);
                                        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                                    }

                                    if (mimeType != null) {
                                        intent.setDataAndType(Uri.fromFile(new File(application.getDownloadManager().getDocFileName(key))), mimeType);
                                    } else {
                                        intent.setDataAndType(Uri.fromFile(new File(application.getDownloadManager().getDocFileName(key))), "*/*");
                                    }

                                    try {
                                        startActivity(intent);
                                    } catch (android.content.ActivityNotFoundException e) {
                                        if (mimeType != null) {
                                            intent.setDataAndType(Uri.fromFile(new File(application.getDownloadManager().getDocFileName(key))), "*/*");
                                            try {
                                                startActivity(intent);
                                            } catch (android.content.ActivityNotFoundException e2) {
                                                Toast.makeText(getActivity(), "Unable to open file", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Toast.makeText(getActivity(), "Unable to open file", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                } else if (state == DownloadState.PENDING || state == DownloadState.IN_PROGRESS) {
                                    // CANCEL
                                    application.getDownloadManager().abortDownload(key);
                                } else {
                                    TLLocalDocument video = (TLLocalDocument) object.message.getExtras();
                                    // Check
                                    application.getDownloadManager().requestDownload(video);
                                }
                            }
                        }
                    }
                });
                documentView.setOnAvatarClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getRootController().openUser(object.message.getSenderId());
                    }
                });

            } else if (object.message.getRawContentType() == ContentType.MESSAGE_CONTACT) {
                MessageContactView messageView = (MessageContactView) view;
                boolean showDiv = false;
                if (!object.message.isOut() && object.message.getMid() == firstUnreadMessage) {
                    showDiv = true;
                }
                messageView.bind(object, showTime, showDiv, unreadCount);

                messageView.setOnBubbleClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (selected.size() > 0) {
                            if (selected.contains(object.message.getDatabaseId())) {
                                selected.remove(object.message.getDatabaseId());
                            } else {
                                selected.add(object.message.getDatabaseId());
                            }
                            ((BaseMsgView) view).setChecked(selected.contains(object.message.getDatabaseId()));
                            updateActionMode();
                        } else {
                            onMessageClick(object);
                        }
                    }
                });
                messageView.setOnAvatarClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getRootController().openUser(object.message.getSenderId());
                    }
                });
                messageView.setOnAddContactClick(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getRootController().addContact(((TLLocalContact) object.message.getExtras()).getUserId());
                    }
                });
            } else if (object.message.getRawContentType() == ContentType.MESSAGE_TEXT) {
                MessageView messageView = (MessageView) view;
                boolean showDiv = false;
                if (!object.message.isOut() && object.message.getMid() == firstUnreadMessage) {
                    showDiv = true;
                }
                long start = SystemClock.uptimeMillis();
                boolean res = messageView.bind(object, showTime, showDiv, unreadCount);
                Logger.d(TAG, "Bind #" + object.message.getContentType() + " in " + (SystemClock.uptimeMillis() - start) + " ms \t" + res);

                messageView.setOnBubbleClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (selected.size() > 0) {
                            if (selected.contains(object.message.getDatabaseId())) {
                                selected.remove(object.message.getDatabaseId());
                            } else {
                                selected.add(object.message.getDatabaseId());
                            }
                            ((BaseMsgView) view).setChecked(selected.contains(object.message.getDatabaseId()));
                            updateActionMode();
                        } else {
                            onMessageClick(object);
                        }
                    }
                });
                messageView.setOnAvatarClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getRootController().openUser(object.message.getSenderId());
                    }
                });
            } else if (object.message.getRawContentType() == ContentType.MESSAGE_PHOTO
                    || object.message.getRawContentType() == ContentType.MESSAGE_VIDEO
                    || object.message.getRawContentType() == ContentType.MESSAGE_GEO
                    || object.message.getRawContentType() == ContentType.MESSAGE_UNKNOWN
                    || object.message.getRawContentType() == ContentType.MESSAGE_DOC_PREVIEW
                    || object.message.getRawContentType() == ContentType.MESSAGE_DOC_ANIMATED) {
                MessageMediaView messageView = (MessageMediaView) view;
                boolean showDiv = false;
                if (!object.message.isOut() && object.message.getMid() == firstUnreadMessage) {
                    showDiv = true;
                }
                messageView.bind(object, showTime, showDiv, unreadCount);
                messageView.setOnAvatarClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getRootController().openUser(object.message.getSenderId());
                    }
                });

                if (object.message.getExtras() instanceof TLUploadingPhoto || object.message.getExtras() instanceof TLUploadingVideo || object.message.getExtras() instanceof TLUploadingDocument) {
                    messageView.setOnBubbleClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (selected.size() > 0) {
                                if (selected.contains(object.message.getDatabaseId())) {
                                    selected.remove(object.message.getDatabaseId());
                                } else {
                                    selected.add(object.message.getDatabaseId());
                                }
                                ((BaseMsgView) view).setChecked(selected.contains(object.message.getDatabaseId()));
                                updateActionMode();
                            } else {
                                if (object.message.getState() == MessageState.FAILURE) {
                                    onMessageClick(object);
                                } else {
                                    AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_conv_cancel_title).setMessage(R.string.st_conv_cancel_message)
                                            .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    application.getMediaSender().cancelUpload(object.message.getDatabaseId());
                                                    application.notifyUIUpdate();
                                                }
                                            }).setNegativeButton(R.string.st_no, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {

                                                }
                                            }).create();
                                    dialog.setCanceledOnTouchOutside(true);
                                    dialog.show();
                                }
                            }
                        }
                    });
                } else if (object.message.getExtras() instanceof TLLocalPhoto) {

                    TLLocalPhoto photo = (TLLocalPhoto) object.message.getExtras();
                    if (!(photo.getFullLocation() instanceof TLLocalFileEmpty)) {
                        final String key = DownloadManager.getPhotoKey(photo);
                        DownloadState state = application.getDownloadManager().getState(key);
                        if (state == DownloadState.FAILURE || state == DownloadState.NONE) {
                            application.getDownloadManager().requestDownload(photo);
                        }

                        messageView.setOnBubbleClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (selected.size() > 0) {
                                    if (selected.contains(object.message.getDatabaseId())) {
                                        selected.remove(object.message.getDatabaseId());
                                    } else {
                                        selected.add(object.message.getDatabaseId());
                                    }
                                    ((BaseMsgView) view).setChecked(selected.contains(object.message.getDatabaseId()));
                                    updateActionMode();
                                } else {
                                    DownloadState state = application.getDownloadManager().getState(key);
                                    if (state == DownloadState.COMPLETED) {
                                        getRootController().openImage(object.message.getMid(), peerType, peerId);
                                    } else if (state == DownloadState.PENDING || state == DownloadState.IN_PROGRESS) {
                                        // CANCEL
                                        application.getDownloadManager().abortDownload(key);
                                    } else {
                                        // Check
                                        application.getDownloadManager().requestDownload((TLLocalPhoto) object.message.getExtras());
                                    }
                                }
                            }
                        });
                    } else {
                        messageView.setOnBubbleClickListener(null);
                    }
                } else if (object.message.getExtras() instanceof TLLocalVideo) {
                    final String key = DownloadManager.getVideoKey((TLLocalVideo) object.message.getExtras());
                    messageView.setOnBubbleClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (selected.size() > 0) {
                                if (selected.contains(object.message.getDatabaseId())) {
                                    selected.remove(object.message.getDatabaseId());
                                } else {
                                    selected.add(object.message.getDatabaseId());
                                }
                                ((BaseMsgView) view).setChecked(selected.contains(object.message.getDatabaseId()));
                                updateActionMode();
                            } else {
                                DownloadState state = application.getDownloadManager().getState(key);
                                if (state == DownloadState.COMPLETED) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(Uri.fromFile(new File(application.getDownloadManager().getVideoFileName(key))), "video/*");
                                    startActivity(intent);
                                } else if (state == DownloadState.PENDING || state == DownloadState.IN_PROGRESS) {
                                    // CANCEL
                                    application.getDownloadManager().abortDownload(key);
                                } else {
                                    TLLocalVideo video = (TLLocalVideo) object.message.getExtras();
                                    // Check
                                    application.getDownloadManager().requestDownload(video);
                                }
                            }
                        }
                    });
                } else if (object.message.getExtras() instanceof TLLocalGeo) {
                    messageView.setOnBubbleClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (selected.size() > 0) {
                                if (selected.contains(object.message.getDatabaseId())) {
                                    selected.remove(object.message.getDatabaseId());
                                } else {
                                    selected.add(object.message.getDatabaseId());
                                }
                                ((BaseMsgView) view).setChecked(selected.contains(object.message.getDatabaseId()));
                                updateActionMode();
                            } else {
                                TLLocalGeo geo = (TLLocalGeo) object.message.getExtras();
                                getRootController().viewLocation(geo.getLatitude(), geo.getLongitude(), object.message.getSenderId());
                            }
                        }
                    });
                } else if (object.message.getExtras() instanceof TLLocalDocument) {
                    final String key = DownloadManager.getDocumentKey((TLLocalDocument) object.message.getExtras());
                    messageView.setOnBubbleClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (selected.size() > 0) {
                                if (selected.contains(object.message.getDatabaseId())) {
                                    selected.remove(object.message.getDatabaseId());
                                } else {
                                    selected.add(object.message.getDatabaseId());
                                }
                                ((BaseMsgView) view).setChecked(selected.contains(object.message.getDatabaseId()));
                                updateActionMode();
                            } else {
                                DownloadState state = application.getDownloadManager().getState(key);
                                if (state == DownloadState.COMPLETED) {
                                    if (((TLLocalDocument) object.message.getExtras()).getMimeType().equals("image/gif")) {
                                        ((MessageMediaView) view).toggleMovie();
                                    } else {
                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                        TLLocalDocument doc = (TLLocalDocument) object.message.getExtras();

                                        String mimeType = null;
                                        if (doc.getFileName().indexOf('.') > -1) {
                                            String ext = doc.getFileName().substring(doc.getFileName().lastIndexOf('.') + 1);
                                            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                                        }

                                        if (mimeType != null) {
                                            intent.setDataAndType(Uri.fromFile(new File(application.getDownloadManager().getDocFileName(key))), mimeType);
                                        } else {
                                            intent.setDataAndType(Uri.fromFile(new File(application.getDownloadManager().getDocFileName(key))), "*/*");
                                        }

                                        try {
                                            startActivity(intent);
                                        } catch (android.content.ActivityNotFoundException e) {
                                            if (mimeType != null) {
                                                intent.setDataAndType(Uri.fromFile(new File(application.getDownloadManager().getDocFileName(key))), "*/*");
                                                try {
                                                    startActivity(intent);
                                                } catch (android.content.ActivityNotFoundException e2) {
                                                    Toast.makeText(getActivity(), "Unable to open file", Toast.LENGTH_SHORT).show();
                                                }
                                            } else {
                                                Toast.makeText(getActivity(), "Unable to open file", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                                } else if (state == DownloadState.PENDING || state == DownloadState.IN_PROGRESS) {
                                    // CANCEL
                                    application.getDownloadManager().abortDownload(key);
                                } else {
                                    TLLocalDocument video = (TLLocalDocument) object.message.getExtras();
                                    // Check
                                    application.getDownloadManager().requestDownload(video);
                                }
                            }
                        }
                    });
                } else {
                    messageView.setOnBubbleClickListener(null);
                }
            } else if (object.message.getRawContentType() == ContentType.MESSAGE_AUDIO) {
                MessageAudioView audioView = (MessageAudioView) view;
                boolean showDiv = false;
                if (!object.message.isOut() && object.message.getMid() == firstUnreadMessage) {
                    showDiv = true;
                }
                audioView.bind(object, showTime, showDiv, unreadCount);

                if (object.message.getExtras() instanceof TLUploadingDocument) {
                    audioView.setOnBubbleClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (selected.size() > 0) {
                                if (selected.contains(object.message.getDatabaseId())) {
                                    selected.remove(object.message.getDatabaseId());
                                } else {
                                    selected.add(object.message.getDatabaseId());
                                }
                                ((BaseMsgView) view).setChecked(selected.contains(object.message.getDatabaseId()));
                                updateActionMode();
                            } else {
                                if (object.message.getState() == MessageState.FAILURE) {
                                    onMessageClick(object);
                                } else {
                                    AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_conv_cancel_title).setMessage(R.string.st_conv_cancel_message)
                                            .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    application.getMediaSender().cancelUpload(object.message.getDatabaseId());
                                                    application.notifyUIUpdate();
                                                }
                                            }).setNegativeButton(R.string.st_no, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {

                                                }
                                            }).create();
                                    dialog.setCanceledOnTouchOutside(true);
                                    dialog.show();
                                }
                            }
                        }
                    });
                } else if (object.message.getExtras() instanceof TLLocalDocument) {
                    final String key = DownloadManager.getDocumentKey((TLLocalDocument) object.message.getExtras());

                    audioView.setOnBubbleClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (selected.size() > 0) {
                                if (selected.contains(object.message.getDatabaseId())) {
                                    selected.remove(object.message.getDatabaseId());
                                } else {
                                    selected.add(object.message.getDatabaseId());
                                }
                                ((BaseMsgView) view).setChecked(selected.contains(object.message.getDatabaseId()));
                                updateActionMode();
                            } else {
                                DownloadState state = application.getDownloadManager().getState(key);
                                if (state == DownloadState.COMPLETED) {
                                    ((MessageAudioView) view).play();
                                } else if (state == DownloadState.PENDING || state == DownloadState.IN_PROGRESS) {
                                    // CANCEL
                                    application.getDownloadManager().abortDownload(key);
                                } else {
                                    TLLocalDocument video = (TLLocalDocument) object.message.getExtras();
                                    // Check
                                    application.getDownloadManager().requestDownload(video);
                                }
                            }
                        }
                    });
                }
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public int getItemDate(int id) {
            return getItem(id).message.getDate();
        }
    }

    @Override
    public boolean isParentFragment(StelsFragment fragment) {
        return false;
    }
}