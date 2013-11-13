package org.telegram.android.core.background;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.ApiUtils;
import org.telegram.android.core.model.*;
import org.telegram.android.log.Logger;
import org.telegram.api.TLInputEncryptedChat;
import org.telegram.api.TLInputPeerChat;
import org.telegram.api.TLInputPeerContact;
import org.telegram.api.TLInputPeerForeign;
import org.telegram.api.engine.RpcException;
import org.telegram.api.messages.TLAffectedHistory;
import org.telegram.api.requests.*;
import org.telegram.tl.TLIntVector;

import java.io.IOException;
import java.util.List;

/**
 * Author: Korshakov Stepan
 * Created: 29.07.13 23:53
 */
public class FastActions {
    private static final String TAG = "FastActions";
    private Thread actionsThread;
    private Handler handler;

    private StelsApplication application;

    private int lastTypingTime;

    public FastActions(StelsApplication _application) {
        this.application = _application;
        actionsThread = new Thread() {
            @Override
            public void run() {
                setPriority(Thread.MIN_PRIORITY);
                Looper.prepare();
                handler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        Logger.d(TAG, "actionProcessing");
                        // TODO: correct back-off
                        if (application.isLoggedIn()) {
                            if (application.getApi() == null) {
                                return;
                            }
                            if (msg.what == 0) {
                                if (application.isAppActive()) {
                                    application.getApi().doRpcCall(new TLRequestAccountUpdateStatus(false), null);
                                    handler.sendEmptyMessageDelayed(0, 60 * 1000);
                                } else {
                                    application.getApi().doRpcCall(new TLRequestAccountUpdateStatus(true), null);
                                }
                            } else if (msg.what == 1) {
                                int peerType = msg.arg1;
                                int peerId = msg.arg2;
                                if (peerType == PeerType.PEER_USER) {
                                    application.getApi().doRpcCall(new TLRequestMessagesSetTyping(new TLInputPeerContact(peerId), true), null);
                                } else if (peerType == PeerType.PEER_CHAT) {
                                    application.getApi().doRpcCall(new TLRequestMessagesSetTyping(new TLInputPeerChat(peerId), true), null);
                                } else if (peerType == PeerType.PEER_USER_ENCRYPTED) {
                                    EncryptedChat chat = application.getEngine().getEncryptedChat(peerId);
                                    if (chat == null) {
                                        return;
                                    }

                                    application.getApi().doRpcCall(new TLRequestMessagesSetEncryptedTyping(new TLInputEncryptedChat(chat.getId(), chat.getAccessHash()), true), null);
                                }

                            } else if (msg.what == 2) {
                                // TODO: implement cyclic updates
                                int peerType = msg.arg1;
                                int peerId = msg.arg2;
                                DialogDescription description = application.getEngine().getDescriptionForPeer(peerType, peerId);
                                if (description == null) {
                                    return;
                                }
                                if (peerType == PeerType.PEER_USER) {
                                    try {
                                        int mid = description.getLastLocalViewedMessage();
                                        User user = application.getEngine().getUser(description.getPeerId());
                                        TLInputPeerForeign peer = new TLInputPeerForeign(user.getUid(), user.getAccessHash());
                                        TLAffectedHistory history = application.getApi().doRpcCall(new TLRequestMessagesReadHistory(peer, mid, 0));
                                        application.getEngine().onMaxRemoteViewed(peerType, peerId, mid);
                                        application.getUpdateProcessor().onMessage(history);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else if (peerType == PeerType.PEER_CHAT) {
                                    try {
                                        int mid = description.getLastLocalViewedMessage();
                                        TLAffectedHistory history = application.getApi().doRpcCall(new TLRequestMessagesReadHistory(new TLInputPeerChat(peerId), mid, 0));
                                        application.getEngine().onMaxRemoteViewed(peerType, peerId, mid);
                                        application.getUpdateProcessor().onMessage(history);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else if (peerType == PeerType.PEER_USER_ENCRYPTED) {
                                    EncryptedChat chat = application.getEngine().getEncryptedChat(peerId);
                                    if (chat == null) {
                                        return;
                                    }
                                    ChatMessage[] messages = application.getEngine().findUnreadedSelfDestructMessages(peerType, peerId);
                                    for (ChatMessage message : messages) {
                                        message.setMessageDieTime((int) (System.currentTimeMillis() / 1000 + message.getMessageTimeout()));
                                        application.getSelfDestructProcessor().performSelfDestruct(message.getDatabaseId(), message.getMessageDieTime());
                                        application.getEngine().getMessagesDao().update(message);
                                        application.onSourceUpdateMessage(message);
                                        application.notifyUIUpdate();
                                    }
                                    try {
                                        int mid = description.getLastLocalViewedMessage();
                                        application.getApi().doRpcCall(new TLRequestMessagesReadEncryptedHistory(new TLInputEncryptedChat(peerId, chat.getAccessHash()), mid));
                                        application.getEngine().onMaxRemoteViewed(peerType, peerId, mid);
                                    } catch (RpcException e) {
                                        Logger.t(TAG, e);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else if (msg.what == 3) {
                                ChatMessage[] messages = application.getEngine().getUnsyncedDeletedMessages();
                                if (messages.length > 0) {
                                    TLIntVector mids = new TLIntVector();
                                    for (ChatMessage message : messages) {
                                        mids.add(message.getMid());
                                    }
                                    try {
                                        TLIntVector res = application.getApi().doRpcCall(new TLRequestMessagesDeleteMessages(mids));
                                        application.getEngine().onDeletedOnServer(res.toArray(new Integer[0]));
                                    } catch (RpcException e) {
                                        Logger.t(TAG, e);
                                    } catch (IOException e) {
                                        Logger.t(TAG, e);
                                        handler.sendEmptyMessageDelayed(3, 10 * 1000);
                                    }
                                }

                                messages = application.getEngine().getUnsyncedRestoredMessages();
                                if (messages.length > 0) {
                                    TLIntVector mids = new TLIntVector();
                                    for (ChatMessage message : messages) {
                                        mids.add(message.getMid());
                                    }
                                    try {
                                        TLIntVector res = application.getApi().doRpcCall(new TLRequestMessagesRestoreMessages(mids));
                                        application.getEngine().onRestoredOnServer(res.toArray(new Integer[0]));
                                    } catch (RpcException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        handler.sendEmptyMessageDelayed(3, 10 * 1000);
                                    }
                                }

                                application.notifyUIUpdate();
                            }
                        }
                    }
                };
                Looper.loop();
            }
        };
        actionsThread.setName("FastActionsThread#" + hashCode());
        actionsThread.start();
        while (handler == null) {
            Thread.yield();
        }
    }

    public void onAppGoesForeground() {
        handler.removeMessages(0);
        handler.sendEmptyMessage(0);
    }

    public void onAppGoesBackground() {
        handler.removeMessages(0);
        handler.sendEmptyMessage(0);
    }

    public void resetTypingDelay() {
        lastTypingTime = 0;
    }

    public void onTyping(final int peerType, final int peerId) {
        if (peerType == PeerType.PEER_USER) {
            User usr = application.getEngine().getUser(peerId);
            if (usr != null) {
                if (ApiUtils.getUserState(usr.getStatus()) != 0) {
                    return;
                }
            }
        }
        if (application.getEngine().getCurrentTime() - lastTypingTime > 5) {
            lastTypingTime = application.getEngine().getCurrentTime();
            handler.removeMessages(1);
            handler.sendMessage(handler.obtainMessage(1, peerType, peerId));
        }
    }

    public void readHistory(int peerType, int peerId) {
        DialogDescription description = application.getEngine().getDescriptionForPeer(peerType, peerId);
        if (description == null)
            return;
        if (description.getLastLocalViewedMessage() <= description.getLastRemoteViewedMessage()) {
            return;
        }
        handler.sendMessage(handler.obtainMessage(2, peerType, peerId));
    }

    public void checkHistory() {
        List<DialogDescription> descriptionList = application.getEngine().getUnreadedRemotelyDescriptions();
        for (DialogDescription description : descriptionList) {
            handler.sendMessage(handler.obtainMessage(2, description.getPeerType(), description.getPeerId()));
        }
    }

    public void checkForDeletions() {
        handler.removeMessages(3);
        handler.sendEmptyMessage(3);
    }
}