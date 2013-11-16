package org.telegram.android.core.background;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.EngineUtils;
import org.telegram.android.core.model.DialogDescription;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.service.TLLocalActionUserRegistered;
import org.telegram.android.log.Logger;
import org.telegram.api.*;
import org.telegram.api.TLAbsMessage;
import org.telegram.api.TLMessage;
import org.telegram.api.engine.RpcException;
import org.telegram.api.messages.*;
import org.telegram.api.requests.TLRequestUpdatesGetDifference;
import org.telegram.api.requests.TLRequestUpdatesGetState;
import org.telegram.api.updates.*;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.util.*;

/**
 * Author: Korshakov Stepan
 * Created: 29.07.13 2:27
 */
public class UpdateProcessor {

    private static final String TAG = "Updater";

    private class PackageIdentity {
        public int seq;
        public int seqEnd;
        public int date;
        public int pts;
        public int qts;
    }

    private static final int CHECK_TIMEOUT = 2000;

    private StelsApplication application;
    private Thread corrector;
    private Handler correctorHandler;

    private boolean isInvalidated;
    private boolean isBrokenSequence = false;

    private UpdateState updateState;

    private HashMap<Integer, TLObject> further = new HashMap<Integer, TLObject>();

    private boolean isDestroyed = false;
    private boolean isStarted = false;

    public UpdateProcessor(StelsApplication _application) {
        this.application = _application;
        this.isInvalidated = false;
        this.updateState = new UpdateState(_application);

        corrector = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                correctorHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        if (isDestroyed) {
                            return;
                        }
                        if (!application.isLoggedIn())
                            return;
                        if (msg.what == 0) {
                            if (!isInvalidated)
                                return;

                            application.getMonitor().waitForConnection();

                            if (!hasState()) {
                                try {
                                    TLState state = application.getApi().doRpcCall(new TLRequestUpdatesGetState());
                                    if (isDestroyed) {
                                        return;
                                    }
                                    if (!application.isLoggedIn())
                                        return;

                                    updateState.setFullState(
                                            state.getPts(),
                                            state.getSeq(),
                                            state.getDate(),
                                            state.getQts());

                                    dumpState();

                                    isInvalidated = false;
                                    onValidated();
                                    return;
                                } catch (IOException e) {
                                    Logger.t(TAG, e);
                                }
                            } else {
                                try {
                                    TLAbsDifference diff = application.getApi().doRpcCall(new TLRequestUpdatesGetDifference(updateState.getPts(), updateState.getDate(), updateState.getQts()));
                                    if (isDestroyed) {
                                        return;
                                    }
                                    if (!application.isLoggedIn())
                                        return;
                                    if (diff instanceof TLDifference) {
                                        TLDifference difference = (TLDifference) diff;
                                        onDifference(difference);
                                        onValidated();
                                    } else if (diff instanceof TLDifferenceSlice) {
                                        TLDifferenceSlice slice = (TLDifferenceSlice) diff;
                                        onSliceDifference(slice);
                                        getHandler().sendEmptyMessage(0);
                                    } else if (diff instanceof TLDifferenceEmpty) {
                                        TLDifferenceEmpty empty = (TLDifferenceEmpty) diff;
                                        updateState.setSeq(empty.getSeq());
                                        updateState.setDate(empty.getDate());
                                        dumpState();
                                        isInvalidated = false;
                                        onValidated();
                                    }
                                    return;
                                } catch (RpcException e) {
                                    Logger.t(TAG, e);
                                    // Temporary fix
                                    updateState.setFullState(0, 0, 0, 0);
                                } catch (IOException e) {
                                    Logger.t(TAG, e);
                                    getHandler().sendEmptyMessageDelayed(0, 1000);
                                }
                            }

                            // TODO: correct back-off
                            getHandler().sendEmptyMessageDelayed(0, 1000);
                        } else if (msg.what == 1) {
                            checkBrokenSequence();
                        }
                    }
                };
                Looper.loop();
            }
        };
        corrector.setName("CorrectorThread#" + corrector.hashCode());
        Logger.d(TAG, "Initied");
    }

    public void runUpdateProcessor() {
        if (isStarted) {
            return;
        }
        isStarted = true;
        corrector.start();
        if (isInvalidated) {
            while (correctorHandler == null) {
                Thread.yield();
            }
            correctorHandler.removeMessages(0);
            correctorHandler.sendEmptyMessage(0);
        }
    }

    private Handler getHandler() {
        while (correctorHandler == null) {
            Thread.yield();
        }
        return correctorHandler;
    }

    public synchronized void invalidateUpdates() {
        if (!isStarted) {
            isInvalidated = true;
        } else {
            if (isInvalidated) {
                Logger.w(TAG, "Trying to invalidate already invialidated: " + updateState.getSeq());
            } else {
                Logger.w(TAG, "Invialidated: " + updateState.getSeq());
                isInvalidated = true;
                getHandler().removeMessages(0);
                getHandler().sendEmptyMessage(0);
            }
        }
    }

    private boolean hasState() {
        return updateState.getPts() > 0;
    }

    private PackageIdentity getPackageIdentity(TLObject object) {
        if (object instanceof TLAbsSentEncryptedMessage) {
            PackageIdentity identity = new PackageIdentity();
            identity.date = ((TLAbsSentEncryptedMessage) object).getDate();
            return identity;
        } else if (object instanceof TLAbsSentMessage) {
            PackageIdentity identity = new PackageIdentity();
            identity.seq = ((TLAbsSentMessage) object).getSeq();
            identity.seqEnd = 0;
            identity.pts = ((TLAbsSentMessage) object).getPts();
            identity.date = ((TLAbsSentMessage) object).getDate();
            return identity;
        } else if (object instanceof TLAffectedHistory) {
            PackageIdentity identity = new PackageIdentity();
            identity.seq = ((TLAffectedHistory) object).getSeq();
            identity.seqEnd = 0;
            identity.pts = ((TLAffectedHistory) object).getPts();
            identity.date = 0;
            return identity;
        } else if (object instanceof TLAbsStatedMessage) {
            PackageIdentity identity = new PackageIdentity();
            identity.seq = ((TLAbsStatedMessage) object).getSeq();
            identity.seqEnd = 0;
            identity.pts = ((TLAbsStatedMessage) object).getPts();
            identity.date = 0;
            return identity;
        } else if (object instanceof TLAbsStatedMessages) {
            PackageIdentity identity = new PackageIdentity();
            identity.seq = ((TLAbsStatedMessages) object).getSeq();
            identity.seqEnd = 0;
            identity.pts = ((TLStatedMessages) object).getPts();
            identity.date = 0;
            return identity;
        } else if (object instanceof TLUpdateShortChatMessage) {
            PackageIdentity identity = new PackageIdentity();
            identity.seq = ((TLUpdateShortChatMessage) object).getSeq();
            identity.seqEnd = 0;
            identity.pts = ((TLUpdateShortChatMessage) object).getPts();
            identity.date = ((TLUpdateShortChatMessage) object).getDate();
            return identity;
        } else if (object instanceof TLUpdateShortMessage) {
            PackageIdentity identity = new PackageIdentity();
            identity.seq = ((TLUpdateShortMessage) object).getSeq();
            identity.seqEnd = 0;
            identity.pts = ((TLUpdateShortMessage) object).getPts();
            identity.date = ((TLUpdateShortMessage) object).getDate();
            return identity;
        } else if (object instanceof TLUpdateShort) {
            PackageIdentity identity = new PackageIdentity();
            identity.seq = 0;
            identity.seqEnd = 0;
            identity.pts = 0;
            identity.date = ((TLUpdateShort) object).getDate();
            if (((TLUpdateShort) object).getUpdate() instanceof TLUpdateNewEncryptedMessage) {
                identity.qts = ((TLUpdateNewEncryptedMessage) (((TLUpdateShort) object).getUpdate())).getQts();
            }
            return identity;
        } else if (object instanceof TLUpdatesTooLong) {
            PackageIdentity identity = new PackageIdentity();
            identity.seq = 0;
            identity.seqEnd = 0;
            identity.pts = 0;
            identity.date = 0;
            return identity;
        } else if (object instanceof TLUpdates) {
            PackageIdentity identity = new PackageIdentity();
            identity.seq = ((TLUpdates) object).getSeq();
            identity.seqEnd = 0;
            identity.pts = 0;
            identity.date = ((TLUpdates) object).getDate();
            identity.qts = 0;
            for (TLAbsUpdate update : (((TLUpdates) object).getUpdates())) {
                if (update instanceof TLUpdateNewEncryptedMessage) {
                    identity.qts = Math.max(identity.qts, ((TLUpdateNewEncryptedMessage) update).getQts());
                }
            }
            return identity;
        } else if (object instanceof TLUpdatesCombined) {
            PackageIdentity identity = new PackageIdentity();
            identity.seq = ((TLUpdatesCombined) object).getSeqStart();
            identity.seqEnd = ((TLUpdatesCombined) object).getSeq();
            identity.pts = 0;
            identity.date = ((TLUpdatesCombined) object).getDate();
            for (TLAbsUpdate update : (((TLUpdatesCombined) object).getUpdates())) {
                if (update instanceof TLUpdateNewEncryptedMessage) {
                    identity.qts = Math.max(identity.qts, ((TLUpdateNewEncryptedMessage) update).getQts());
                }
            }
            return identity;
        }

        return null;
    }

    public synchronized void checkBrokenSequence() {
        if (!isStarted)
            return;
        if (!isBrokenSequence)
            return;
        getHandler().removeMessages(1);
        isBrokenSequence = false;
        Logger.d(TAG, "#### | Sequence dies by timeout");
        invalidateUpdates();
    }

    private void requestSequenceCheck() {
        if (!isStarted)
            return;
        getHandler().removeMessages(1);
        getHandler().sendEmptyMessageDelayed(1, CHECK_TIMEOUT);
    }

    private void cancelSequenceCheck() {
        if (!isStarted)
            return;
        getHandler().removeMessages(1);
    }

    public synchronized boolean accept(PackageIdentity identity) {
        if (identity == null)
            return true;
        if (!hasState()) {
            return true;
        }

        if (identity.seq != 0) {
            if (identity.seq <= updateState.getSeq()) {
                return false;
            } else {
                return true;
            }
        }
        return true;
    }

    public synchronized boolean causeSeqInvalidation(PackageIdentity identity) {
        if (!hasState()) {
            return false;
        }

        if (identity.seq != 0) {
            if (identity.seq != updateState.getSeq() + 1) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean causeInvalidation(TLObject object) {
        if (object instanceof TLUpdateShortChatMessage) {
            TLUpdateShortChatMessage chatMessage = (TLUpdateShortChatMessage) object;
            if (application.getEngine().getUser(chatMessage.getFromId()) == null)
                return true;
            if (application.getEngine().getDescriptionForPeer(PeerType.PEER_CHAT, chatMessage.getChatId()) == null)
                return true;
        } else if (object instanceof TLUpdateShortMessage) {
            TLUpdateShortMessage chatMessage = (TLUpdateShortMessage) object;
            if (application.getEngine().getUser(chatMessage.getFromId()) == null)
                return true;
        }

        return false;
    }

    private synchronized void onValidated() {
        if (further.size() > 0) {
            Integer[] keys = further.keySet().toArray(new Integer[0]);
            int[] keys2 = new int[keys.length];
            for (int i = 0; i < keys2.length; i++) {
                keys2[i] = keys[i];
            }
            Arrays.sort(keys2);
            TLObject[] messages = new TLObject[keys2.length];
            for (int i = 0; i < keys2.length; i++) {
                messages[i] = further.get(keys2[i]);
            }

            further.clear();
            for (TLObject object : messages) {
                onMessage(object);
            }
        }
    }

    public synchronized void onMessage(TLObject object) {
        if (isDestroyed) {
            return;
        }
        if (!isStarted) {
            return;
        }

        PackageIdentity identity = getPackageIdentity(object);
        if (identity != null && identity.seq > 0) {
            Logger.d(TAG, identity.seq + " : Message " + object);
        } else {
            Logger.d(TAG, "#### | Message " + object);
        }

        if (!accept(identity)) {
            return;
        }

        if (isInvalidated) {
            further.put(identity.seq, object);
            getHandler().removeMessages(0);
            getHandler().sendEmptyMessage(0);
            return;
        }

        if (causeSeqInvalidation(identity)) {
            Logger.d(TAG, identity.seq + " | Out of sequence");
            further.put(identity.seq, object);
            if (!isBrokenSequence) {
                isBrokenSequence = true;
                requestSequenceCheck();
            }
            return;
        }

        if (isBrokenSequence && identity.seq != 0) {
            isBrokenSequence = false;
            Logger.d(TAG, identity.seq + " | Sequence fixed");
            cancelSequenceCheck();
        }

        if (causeInvalidation(object)) {
            further.put(identity.seq, object);
            invalidateUpdates();
            return;
        }

        if (object instanceof TLUpdateShortChatMessage) {
            onUpdateShortChatMessage((TLUpdateShortChatMessage) object);
        } else if (object instanceof TLUpdateShortMessage) {
            onUpdateShortMessage((TLUpdateShortMessage) object);
        } else if (object instanceof TLUpdateShort) {
            onUpdateShort((TLUpdateShort) object, identity);
        } else if (object instanceof TLUpdates) {
            onUpdates((TLUpdates) object, identity);
        } else if (object instanceof TLUpdatesCombined) {
            onCombined((TLUpdatesCombined) object, identity);
        } else if (object instanceof TLUpdatesTooLong) {
            invalidateUpdates();
        }

        if (identity != null && identity.seq != 0) {
            if (identity.seqEnd != 0) {
                updateState.setSeq(identity.seqEnd);
            } else {
                updateState.setSeq(identity.seq);
            }

            if (identity.date != 0) {
                updateState.setDate(identity.date);
            }

            if (identity.qts != 0) {
                updateState.setQts(identity.qts);
            }

            if (identity.pts != 0) {
                updateState.setPts(identity.pts);
            }

            dumpState();
        }

        onValidated();

        application.notifyUIUpdate();
    }

    public void onSyncSeqArrived(int seq) {
        if (updateState.getSeq() < seq) {
            Logger.d(TAG, "Push sync causes invalidation");
            invalidateUpdates();
        }
    }

    private void onUpdates(TLUpdates updates, PackageIdentity identity) {
        application.getEngine().onUsers(updates.getUsers());
        for (TLAbsUpdate u : updates.getUpdates()) {
            onUpdate(updates.getDate(), u, updates.getUsers(), updates.getChats(), identity);
        }
    }

    private void onUpdateShort(TLUpdateShort updateShort, PackageIdentity identity) {
        onUpdate(updateShort.getDate(), updateShort.getUpdate(), new ArrayList<TLAbsUser>(), new ArrayList<TLAbsChat>(), identity);
    }

    private void onUpdate(int date, TLAbsUpdate update, List<TLAbsUser> users, List<TLAbsChat> chats, PackageIdentity identity) {
        Logger.d(TAG, "#### | Update: " + update);
        if (update instanceof TLUpdateContactLink) {
//            TLUpdateContactLink link = (TLUpdateContactLink) update;
//            application.getEngine().onUserLinkChanged(EngineUtils.findUser(users, link.getUserId()));
//            application.getUserSource().notifyUserChanged(link.getUserId());
        } else if (update instanceof TLUpdateUserTyping) {
            application.getTypingStates().onUserTyping(((TLUpdateUserTyping) update).getUserId(), date);
            application.getUserSource().notifyUserChanged(((TLUpdateUserTyping) update).getUserId());
        } else if (update instanceof TLUpdateChatUserTyping) {
            TLUpdateChatUserTyping userTyping = (TLUpdateChatUserTyping) update;
            application.getTypingStates().onUserChatTyping(userTyping.getUserId(), userTyping.getChatId(), date);
        } else if (update instanceof TLUpdateUserStatus) {
            TLUpdateUserStatus updateUserStatus = (TLUpdateUserStatus) update;
            Logger.d(TAG, "#### | UserStatus " + updateUserStatus.getStatus());
            application.getEngine().onUserStatus(updateUserStatus.getUserId(), updateUserStatus.getStatus());
            application.getUserSource().notifyUserChanged(updateUserStatus.getUserId());
        } else if (update instanceof TLUpdateUserName) {
            TLUpdateUserName updateUserName = (TLUpdateUserName) update;
            application.getEngine().onUserNameChanges(updateUserName.getUserId(), updateUserName.getFirstName(), updateUserName.getLastName());
            application.getUserSource().notifyUserChanged(updateUserName.getUserId());
        } else if (update instanceof TLUpdateUserPhoto) {
            TLUpdateUserPhoto updateUserPhoto = (TLUpdateUserPhoto) update;
            TLLocalAvatarPhoto photo = (TLLocalAvatarPhoto) EngineUtils.convertAvatarPhoto(updateUserPhoto.getPhoto());
            application.getEngine().onUserAvatarChanges(updateUserPhoto.getUserId(), photo);
//            application.getEngine().onNewInternalServiceMessage(PeerType.PEER_USER, updateUserPhoto.getUserId(),
//                    updateUserPhoto.getUserId(),
//                    updateUserPhoto.getDate(),
//                    new TLLocalActionUserEditPhoto(photo));
            application.getUserSource().notifyUserChanged(updateUserPhoto.getUserId());
        } else if (update instanceof TLUpdateReadMessages) {
            TLUpdateReadMessages readMessages = (TLUpdateReadMessages) update;
            Integer[] ids = readMessages.getMessages().toArray(new Integer[0]);
            Logger.w(TAG, "updateReadMessage: " + Arrays.toString(ids));
            application.getEngine().onMessagesReaded(ids);
            application.notifyUIUpdate();
        } else if (update instanceof TLUpdateChatParticipants) {
            TLUpdateChatParticipants participants = (TLUpdateChatParticipants) update;
            application.getEngine().onChatParticipants(participants.getParticipants());
        } else if (update instanceof TLUpdateDeleteMessages) {
            TLUpdateDeleteMessages deleteMessages = (TLUpdateDeleteMessages) update;
            application.getEngine().onDeletedOnServer(deleteMessages.getMessages().toArray(new Integer[0]));
        } else if (update instanceof TLUpdateRestoreMessages) {
            TLUpdateRestoreMessages restoreMessages = (TLUpdateRestoreMessages) update;
            application.getEngine().onRestoredOnServer(restoreMessages.getMessages().toArray(new Integer[0]));
        } else if (update instanceof TLUpdateContactRegistered) {
            User src = application.getEngine().getUser(((TLUpdateContactRegistered) update).getUserId());
            application.getEngine().onNewInternalServiceMessage(
                    PeerType.PEER_USER,
                    src.getUid(),
                    src.getUid(),
                    ((TLUpdateContactRegistered) update).getDate(),
                    new TLLocalActionUserRegistered());
            application.notifyUIUpdate();
            application.getNotifications().onNewMessageJoined(src.getDisplayName(), src.getUid(), 0, src.getPhoto());
            application.getContactsSource().resetState();
            application.getContactsSource().startSync();
        } else if (update instanceof TLUpdateEncryption) {
            application.getEncryptionController().onUpdateEncryption(((TLUpdateEncryption) update).getChat());
        } else if (update instanceof TLUpdateEncryptedChatTyping) {
            application.getTypingStates().onEncryptedTyping(((TLUpdateEncryptedChatTyping) update).getChatId(), date);
        } else if (update instanceof TLUpdateNewEncryptedMessage) {
            application.getEncryptionController().onEncryptedMessage(((TLUpdateNewEncryptedMessage) update).getMessage());
            identity.qts = Math.max(identity.qts, ((TLUpdateNewEncryptedMessage) update).getQts());
        } else if (update instanceof TLUpdateEncryptedMessagesRead) {
            TLUpdateEncryptedMessagesRead read = (TLUpdateEncryptedMessagesRead) update;
            application.getEngine().onEncryptedReaded(read.getChatId(), read.getDate(), read.getMaxDate());
        } else if (update instanceof TLUpdateNewMessage) {
            onUpdateNewMessage((TLUpdateNewMessage) update, users, chats);
        } else if (update instanceof TLUpdateActivation) {

        } else if (update instanceof TLUpdateNewAuthorization) {
            TLUpdateNewAuthorization authorization = (TLUpdateNewAuthorization) update;
            if (authorization.getLocation().length() > 0) {
                application.getNotifications().onAuthUnrecognized(authorization.getDevice(), authorization.getLocation());
            } else {
                application.getNotifications().onAuthUnrecognized(authorization.getDevice());
            }
        }
    }

    private void onUpdateNewMessage(TLUpdateNewMessage newMessage, List<TLAbsUser> users, List<TLAbsChat> chats) {
        ArrayList<TLAbsMessage> messages = new ArrayList<TLAbsMessage>();
        messages.add(newMessage.getMessage());
        applyMessages(messages, users, chats);
    }

    private void onUpdateShortChatMessage(TLUpdateShortChatMessage message) {
        if (application.getEngine().getDescriptionForPeer(PeerType.PEER_CHAT, message.getChatId()) == null) {
            throw new IllegalStateException();
        }

        boolean isAdded = application.getEngine().onNewShortMessage(PeerType.PEER_CHAT, message.getChatId(), message.getId(),
                message.getDate(), message.getFromId(), message.getMessage());
        if (message.getFromId() != application.getCurrentUid()) {
            if (isAdded) {
                onInMessageArrived(PeerType.PEER_CHAT, message.getChatId(), message.getId());
                DialogDescription description = application.getEngine().getDescriptionForPeer(PeerType.PEER_CHAT, message.getChatId());
                User sender = application.getEngine().getUser(message.getFromId());
                if (description != null && sender != null) {
                    application.getNotifications().onNewChatMessage(
                            sender.getDisplayName(),
                            sender.getUid(),
                            description.getTitle(), message.getMessage(),
                            message.getChatId(), message.getId(),
                            description.getPhoto());
                }
                application.getTypingStates().resetUserTyping(message.getFromId(), message.getChatId());
            }
        }
        application.notifyUIUpdate();
    }

    private void onUpdateShortMessage(TLUpdateShortMessage message) {
        boolean isAdded = application.getEngine().onNewShortMessage(PeerType.PEER_USER, message.getFromId(), message.getId(),
                message.getDate(), message.getFromId(), message.getMessage());

        if (message.getFromId() != application.getCurrentUid()) {
            if (isAdded) {
                onInMessageArrived(PeerType.PEER_USER, message.getFromId(), message.getId());
                User sender = application.getEngine().getUser(message.getFromId());
                if (sender != null) {
                    application.getNotifications().onNewMessage(sender.getFirstName() + " " + sender.getLastName(),
                            message.getMessage(),
                            message.getFromId(), message.getId(),
                            sender.getPhoto());
                }
                application.getTypingStates().resetUserTyping(message.getFromId());
            }
        }
        application.notifyUIUpdate();
    }


    private void onCombined(TLUpdatesCombined combined, PackageIdentity identity) {
        ArrayList<TLAbsMessage> messages = new ArrayList<TLAbsMessage>();
        ArrayList<TLAbsUpdate> another = new ArrayList<TLAbsUpdate>();

        for (TLAbsUpdate update : combined.getUpdates()) {
            if (update instanceof TLUpdateNewMessage) {
                messages.add(((TLUpdateNewMessage) update).getMessage());
            } else {
                another.add(update);
            }
        }

        applyMessages(messages, combined.getUsers(), combined.getChats());

        for (TLAbsUpdate update : another) {
            onUpdate(combined.getDate(), update, combined.getUsers(), combined.getChats(), identity);
        }

        application.notifyUIUpdate();
    }

    private synchronized void onDifference(TLDifference difference) {
        applyMessages(difference.getNewMessages(),
                difference.getUsers(),
                difference.getChats());

        List<TLAbsEncryptedMessage> messages = difference.getNewEncryptedMessages();
        for (TLAbsEncryptedMessage encryptedMessage : messages) {
            application.getEncryptionController().onEncryptedMessage(encryptedMessage);
        }

        for (TLAbsUpdate update : difference.getOtherUpdates()) {
            onUpdate(difference.getState().getDate(), update,
                    difference.getUsers(), difference.getChats(), null);
        }
        application.notifyUIUpdate();

        updateState.setFullState(
                difference.getState().getPts(),
                difference.getState().getSeq(),
                difference.getState().getDate(),
                difference.getState().getQts());

        dumpState();

        isInvalidated = false;
        onValidated();
    }

    private synchronized void onSliceDifference(TLDifferenceSlice slice) {

        applyMessages(slice.getNewMessages(), slice.getUsers(), slice.getChats());

        List<TLAbsEncryptedMessage> messages = slice.getNewEncryptedMessages();
        for (TLAbsEncryptedMessage encryptedMessage : messages) {
            application.getEncryptionController().onEncryptedMessage(encryptedMessage);
        }

        for (TLAbsUpdate update : slice.getOtherUpdates()) {
            onUpdate(slice.getIntermediateState().getDate(), update,
                    slice.getUsers(), slice.getChats(), null);
        }
        application.notifyUIUpdate();

        updateState.setFullState(slice.getIntermediateState().getPts(),
                slice.getIntermediateState().getSeq(),
                slice.getIntermediateState().getDate(),
                slice.getIntermediateState().getQts());

        dumpState();
    }

    private void applyMessages(List<TLAbsMessage> messages, List<TLAbsUser> users, List<TLAbsChat> chats) {
        HashSet<Integer> newUnreaded = application.getEngine().onNewMessages(messages, users, chats, new ArrayList<TLDialog>());

        for (TLAbsMessage newMessage : messages) {
            if (newMessage instanceof TLMessage) {
                TLMessage msg = (TLMessage) newMessage;
                if (!newUnreaded.contains(msg.getId())) {
                    return;
                }
                if (!msg.getOut() && msg.getUnread()) {
                    if (msg.getToId() instanceof TLPeerChat) {
                        TLPeerChat chat = (TLPeerChat) msg.getToId();
                        onInMessageArrived(PeerType.PEER_CHAT, chat.getChatId(), msg.getId());
                        application.getTypingStates().resetUserTyping(msg.getFromId(), chat.getChatId());
                    } else {
                        onInMessageArrived(PeerType.PEER_USER, msg.getFromId(), msg.getId());
                        application.getTypingStates().resetUserTyping(msg.getFromId());
                    }

                    notifyAboutMessage(msg);
                }
            } else if (newMessage instanceof TLMessageForwarded) {
                TLMessageForwarded msg = (TLMessageForwarded) newMessage;
                if (!newUnreaded.contains(msg.getId())) {
                    return;
                }
                if (!msg.getOut() && msg.getUnread()) {
                    if (msg.getToId() instanceof TLPeerChat) {
                        TLPeerChat chat = (TLPeerChat) msg.getToId();
                        onInMessageArrived(PeerType.PEER_CHAT, chat.getChatId(), msg.getId());
                        application.getTypingStates().resetUserTyping(msg.getFromId(), chat.getChatId());
                    } else {
                        onInMessageArrived(PeerType.PEER_USER, msg.getFromId(), msg.getId());
                        application.getTypingStates().resetUserTyping(msg.getFromId());
                    }

                    notifyAboutMessage(msg);
                }
            } else if (newMessage instanceof TLMessageService) {
                TLMessageService service = (TLMessageService) newMessage;
                if (newUnreaded.contains(service.getId())) {
                    if (!service.getOut() && service.getUnread()) {
                        if (service.getToId() instanceof TLPeerChat) {
                            TLPeerChat chat = (TLPeerChat) service.getToId();
                            onInMessageArrived(PeerType.PEER_CHAT, chat.getChatId(), service.getId());
                        } else {
                            onInMessageArrived(PeerType.PEER_USER, service.getFromId(), service.getId());
                        }
                    }
                }
                if (service.getAction() instanceof TLMessageActionChatEditPhoto) {
                    int chatId = ((TLPeerChat) service.getToId()).getChatId();
                    application.getEngine().onChatAvatarChanges(chatId,
                            ((TLMessageActionChatEditPhoto) service.getAction()).getPhoto());
                    application.getChatSource().notifyChatChanged(chatId);
                } else if (service.getAction() instanceof TLMessageActionChatDeletePhoto) {
                    int chatId = ((TLPeerChat) service.getToId()).getChatId();
                    application.getEngine().onChatAvatarChanges(chatId, null);
                    application.getChatSource().notifyChatChanged(chatId);
                } else if (service.getAction() instanceof TLMessageActionChatEditTitle) {
                    int chatId = ((TLPeerChat) service.getToId()).getChatId();
                    application.getEngine().onChatTitleChanges(chatId,
                            ((TLMessageActionChatEditTitle) service.getAction()).getTitle());
                    application.getChatSource().notifyChatChanged(chatId);
                } else if (service.getAction() instanceof TLMessageActionChatAddUser) {
                    int chatId = ((TLPeerChat) service.getToId()).getChatId();
                    application.getEngine().onChatUserAdded(chatId, service.getFromId(),
                            ((TLMessageActionChatAddUser) service.getAction()).getUserId());
                    application.getChatSource().notifyChatChanged(chatId);
                } else if (service.getAction() instanceof TLMessageActionChatDeleteUser) {
                    TLMessageActionChatDeleteUser actionChatDeleteUser = (TLMessageActionChatDeleteUser) service.getAction();
                    int chatId = ((TLPeerChat) service.getToId()).getChatId();
                    application.getEngine().onChatUserRemoved(chatId, actionChatDeleteUser.getUserId());
                    application.getChatSource().notifyChatChanged(chatId);
                }
            }
        }
    }

    private void notifyAboutMessage(TLMessageForwarded msg) {
        if (msg.getMedia() instanceof TLMessageMediaEmpty) {
            // Forwarded message!111
            User sender = application.getEngine().getUser(msg.getFromId());
            if (msg.getToId() instanceof TLPeerUser) {
                application.getNotifications().onNewMessage(
                        sender.getFirstName() + " " + sender.getLastName(),
                        msg.getMessage(),
                        msg.getFromId(), msg.getId(),
                        sender.getPhoto());
            } else {
                TLPeerChat chat = (TLPeerChat) msg.getToId();
                DialogDescription description = application.getEngine().getDescriptionForPeer(PeerType.PEER_CHAT, chat.getChatId());
                if (description != null) {
                    application.getNotifications().onNewChatMessage(
                            sender.getDisplayName(),
                            sender.getUid(),
                            description.getTitle(),
                            msg.getMessage(),
                            chat.getChatId(), msg.getId(),
                            sender.getPhoto());
                }
            }
        } else if (msg.getMedia() instanceof TLMessageMediaContact) {
            User sender = application.getEngine().getUser(msg.getFromId());
            if (msg.getToId() instanceof TLPeerUser) {
                application.getNotifications().onNewMessageContact(
                        sender.getFirstName() + " " + sender.getLastName(),
                        msg.getFromId(), msg.getId(),
                        sender.getPhoto());
            } else {
                TLPeerChat chat = (TLPeerChat) msg.getToId();
                DialogDescription description = application.getEngine().getDescriptionForPeer(PeerType.PEER_CHAT, chat.getChatId());
                if (description != null) {
                    application.getNotifications().onNewChatMessageContact(
                            sender.getDisplayName(),
                            sender.getUid(),
                            description.getTitle(),
                            chat.getChatId(), msg.getId(),
                            sender.getPhoto());
                }
            }
        } else if (msg.getMedia() instanceof TLMessageMediaGeo) {
            User sender = application.getEngine().getUser(msg.getFromId());
            if (msg.getToId() instanceof TLPeerUser) {
                application.getNotifications().onNewMessageGeo(
                        sender.getDisplayName(),
                        msg.getFromId(), msg.getId(),
                        sender.getPhoto());
            } else {
                TLPeerChat chat = (TLPeerChat) msg.getToId();
                DialogDescription description = application.getEngine().getDescriptionForPeer(PeerType.PEER_CHAT, chat.getChatId());
                if (description != null) {
                    application.getNotifications().onNewChatMessageGeo(
                            sender.getDisplayName(),
                            sender.getUid(),
                            description.getTitle(),
                            chat.getChatId(), msg.getId(),
                            sender.getPhoto());
                }
            }
        } else if (msg.getMedia() instanceof TLMessageMediaPhoto) {
            User sender = application.getEngine().getUser(msg.getFromId());
            if (msg.getToId() instanceof TLPeerUser) {
                application.getNotifications().onNewMessagePhoto(
                        sender.getDisplayName(),
                        msg.getFromId(), msg.getId(),
                        sender.getPhoto());
            } else {
                TLPeerChat chat = (TLPeerChat) msg.getToId();
                DialogDescription description = application.getEngine().getDescriptionForPeer(PeerType.PEER_CHAT, chat.getChatId());
                if (description != null) {
                    application.getNotifications().onNewChatMessagePhoto(
                            sender.getDisplayName(),
                            sender.getUid(),
                            description.getTitle(),
                            chat.getChatId(), msg.getId(),
                            sender.getPhoto());
                }
            }
        } else if (msg.getMedia() instanceof TLMessageMediaVideo) {
            User sender = application.getEngine().getUser(msg.getFromId());
            if (msg.getToId() instanceof TLPeerUser) {
                application.getNotifications().onNewMessageVideo(
                        sender.getDisplayName(),
                        msg.getFromId(), msg.getId(),
                        sender.getPhoto());
            } else {
                TLPeerChat chat = (TLPeerChat) msg.getToId();
                DialogDescription description = application.getEngine().getDescriptionForPeer(PeerType.PEER_CHAT, chat.getChatId());
                if (description != null) {
                    application.getNotifications().onNewChatMessageVideo(
                            sender.getDisplayName(),
                            sender.getUid(),
                            description.getTitle(),
                            chat.getChatId(), msg.getId(),
                            sender.getPhoto());
                }
            }
        }
    }

    private void notifyAboutMessage(TLMessage msg) {
        if (msg.getMedia() instanceof TLMessageMediaEmpty) {
            User sender = application.getEngine().getUser(msg.getFromId());
            if (msg.getToId() instanceof TLPeerUser) {
                application.getNotifications().onNewMessage(
                        sender.getFirstName() + " " + sender.getLastName(),
                        msg.getMessage(),
                        msg.getFromId(), msg.getId(),
                        sender.getPhoto());
            } else {
                TLPeerChat chat = (TLPeerChat) msg.getToId();
                DialogDescription description = application.getEngine().getDescriptionForPeer(PeerType.PEER_CHAT, chat.getChatId());
                if (description != null) {
                    application.getNotifications().onNewChatMessage(
                            sender.getDisplayName(),
                            sender.getUid(),
                            description.getTitle(),
                            msg.getMessage(),
                            chat.getChatId(), msg.getId(),
                            sender.getPhoto());
                }
            }
        } else if (msg.getMedia() instanceof TLMessageMediaContact) {
            User sender = application.getEngine().getUser(msg.getFromId());
            if (msg.getToId() instanceof TLPeerUser) {
                application.getNotifications().onNewMessageContact(
                        sender.getFirstName() + " " + sender.getLastName(),
                        msg.getFromId(), msg.getId(),
                        sender.getPhoto());
            } else {
                TLPeerChat chat = (TLPeerChat) msg.getToId();
                DialogDescription description = application.getEngine().getDescriptionForPeer(PeerType.PEER_CHAT, chat.getChatId());
                if (description != null) {
                    application.getNotifications().onNewChatMessageContact(
                            sender.getDisplayName(),
                            sender.getUid(),
                            description.getTitle(),
                            chat.getChatId(), msg.getId(),
                            sender.getPhoto());
                }
            }
        } else if (msg.getMedia() instanceof TLMessageMediaGeo) {
            User sender = application.getEngine().getUser(msg.getFromId());
            if (msg.getToId() instanceof TLPeerUser) {
                application.getNotifications().onNewMessageGeo(
                        sender.getDisplayName(),
                        msg.getFromId(), msg.getId(),
                        sender.getPhoto());
            } else {
                TLPeerChat chat = (TLPeerChat) msg.getToId();
                DialogDescription description = application.getEngine().getDescriptionForPeer(PeerType.PEER_CHAT, chat.getChatId());
                if (description != null) {
                    application.getNotifications().onNewChatMessageGeo(
                            sender.getDisplayName(),
                            sender.getUid(),
                            description.getTitle(),
                            chat.getChatId(), msg.getId(),
                            sender.getPhoto());
                }
            }
        } else if (msg.getMedia() instanceof TLMessageMediaPhoto) {
            User sender = application.getEngine().getUser(msg.getFromId());
            if (msg.getToId() instanceof TLPeerUser) {
                application.getNotifications().onNewMessagePhoto(
                        sender.getDisplayName(),
                        msg.getFromId(), msg.getId(),
                        sender.getPhoto());
            } else {
                TLPeerChat chat = (TLPeerChat) msg.getToId();
                DialogDescription description = application.getEngine().getDescriptionForPeer(PeerType.PEER_CHAT, chat.getChatId());
                if (description != null) {
                    application.getNotifications().onNewChatMessagePhoto(
                            sender.getDisplayName(),
                            sender.getUid(),
                            description.getTitle(),
                            chat.getChatId(), msg.getId(),
                            sender.getPhoto());
                }
            }
        } else if (msg.getMedia() instanceof TLMessageMediaVideo) {
            User sender = application.getEngine().getUser(msg.getFromId());
            if (msg.getToId() instanceof TLPeerUser) {
                application.getNotifications().onNewMessageVideo(
                        sender.getDisplayName(),
                        msg.getFromId(), msg.getId(),
                        sender.getPhoto());
            } else {
                TLPeerChat chat = (TLPeerChat) msg.getToId();
                DialogDescription description = application.getEngine().getDescriptionForPeer(PeerType.PEER_CHAT, chat.getChatId());
                if (description != null) {
                    application.getNotifications().onNewChatMessageVideo(
                            sender.getDisplayName(),
                            sender.getUid(),
                            description.getTitle(),
                            chat.getChatId(), msg.getId(),
                            sender.getPhoto());
                }
            }
        }
    }

    private void onInMessageArrived(int peerType, int peerId, int mid) {
        if (application.getUiKernel().getOpenedChatPeerType() == peerType && application.getUiKernel().getOpenedChatPeerId() == peerId) {
            int maxMid = application.getEngine().getMaxMsgInId(peerType, peerId);
            application.getEngine().onMaxLocalViewed(peerType, peerId, Math.max(maxMid, mid));
            application.getActions().readHistory(peerType, peerId);
        } else {
            application.getEngine().onNewUnreadMessageId(peerType, peerId, mid);
        }
    }

    public void dumpState() {
        Logger.d(TAG, "Current state: " + updateState.getPts() + ", " + updateState.getSeq() + ", " + updateState.getQts() + ", " + updateState.getDate());
    }

    public synchronized void destroy() {
        if (isDestroyed) {
            return;
        }
        isDestroyed = true;
        correctorHandler.removeMessages(0);
        correctorHandler.removeMessages(1);
        corrector.interrupt();
        try {
            corrector.join(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        further.clear();
    }

    public synchronized void clearData() {
        updateState.resetState();
    }
}
