package org.telegram.android.core.engines;

import org.telegram.android.core.model.FullChatInfo;
import org.telegram.android.core.model.local.TLLocalChatParticipant;
import org.telegram.android.core.model.local.TLLocalFullChatInfo;
import org.telegram.api.TLAbsChatParticipants;
import org.telegram.api.TLChatParticipant;
import org.telegram.api.TLChatParticipants;

/**
 * Created by ex3ndr on 15.01.14.
 */
public class FullGroupEngine {

    private FullGroupDatabase fullGroupDatabase;

    public FullGroupEngine(ModelEngine modelEngine) {
        this.fullGroupDatabase = new FullGroupDatabase(modelEngine);
    }

    public synchronized FullChatInfo loadFullChatInfo(int chatId) {
        return fullGroupDatabase.loadFullChatInfo(chatId);
    }

    public synchronized void onChatUserAdded(int chatId, int uid, int inviter, int date) {
        FullChatInfo fullChatInfo = loadFullChatInfo(chatId);
        if (fullChatInfo == null) {
            return;
        }

        boolean contains = false;
        for (TLLocalChatParticipant participant : fullChatInfo.getChatInfo().getUsers()) {
            if (participant.getUid() == uid) {
                contains = true;
                break;
            }
        }
        if (contains) {
            return;
        }

        fullChatInfo.getChatInfo().getUsers().add(new TLLocalChatParticipant(uid, inviter, date));
        fullGroupDatabase.updateOrCreate(fullChatInfo);
    }

    public synchronized void onChatUserRemoved(int chatId, int uid) {
        FullChatInfo fullChatInfo = loadFullChatInfo(chatId);
        if (fullChatInfo == null) {
            return;
        }

        boolean updated = false;
        for (TLLocalChatParticipant participant : fullChatInfo.getChatInfo().getUsers().toArray(new TLLocalChatParticipant[0])) {
            if (participant.getUid() == uid) {
                fullChatInfo.getChatInfo().getUsers().remove(participant);
                updated = true;
                break;
            }
        }
        if (!updated) {
            return;
        }

        fullGroupDatabase.updateOrCreate(fullChatInfo);
    }

    public synchronized void onChatParticipants(TLAbsChatParticipants participants) {
        FullChatInfo fullChatInfo = loadFullChatInfo(participants.getChatId());
        if (fullChatInfo == null) {
            return;
        }

        fullChatInfo.getChatInfo().getUsers().clear();
        if (participants instanceof TLChatParticipants) {
            TLChatParticipants chatParticipants = (TLChatParticipants) participants;
            fullChatInfo.getChatInfo().setForbidden(false);
            fullChatInfo.getChatInfo().setAdminId(chatParticipants.getAdminId());

            fullChatInfo.getChatInfo().getUsers().clear();
            for (TLChatParticipant participant : chatParticipants.getParticipants()) {
                fullChatInfo.getChatInfo().getUsers().add(new TLLocalChatParticipant(participant.getUserId(), participant.getInviterId(), participant.getDate()));
            }
        } else {
            fullChatInfo.getChatInfo().setForbidden(true);
            fullChatInfo.getChatInfo().setAdminId(0);
            fullChatInfo.getChatInfo().getUsers().clear();
        }

        fullGroupDatabase.updateOrCreate(fullChatInfo);
    }

    public synchronized void onFullChat(org.telegram.api.TLChatFull fullChat) {
        FullChatInfo fullChatInfo = loadFullChatInfo(fullChat.getId());
        if (fullChatInfo == null) {
            TLLocalFullChatInfo localFullChatInfo = new TLLocalFullChatInfo();

            if (fullChat.getParticipants() instanceof TLChatParticipants) {
                TLChatParticipants chatParticipants = (TLChatParticipants) fullChat.getParticipants();
                localFullChatInfo.setForbidden(false);
                localFullChatInfo.setAdminId(chatParticipants.getAdminId());

                for (TLChatParticipant participant : chatParticipants.getParticipants()) {
                    localFullChatInfo.getUsers().add(new TLLocalChatParticipant(participant.getUserId(), participant.getInviterId(), participant.getDate()));
                }
            } else {
                localFullChatInfo.setForbidden(true);
                localFullChatInfo.setAdminId(0);
            }

            fullGroupDatabase.updateOrCreate(new FullChatInfo(fullChat.getId(), localFullChatInfo));
        } else {
            fullChatInfo.getChatInfo().getUsers().clear();
            if (fullChat.getParticipants() instanceof TLChatParticipants) {
                TLChatParticipants chatParticipants = (TLChatParticipants) fullChat.getParticipants();
                fullChatInfo.getChatInfo().setForbidden(false);
                fullChatInfo.getChatInfo().setAdminId(chatParticipants.getAdminId());

                fullChatInfo.getChatInfo().getUsers().clear();
                for (TLChatParticipant participant : chatParticipants.getParticipants()) {
                    fullChatInfo.getChatInfo().getUsers().add(new TLLocalChatParticipant(participant.getUserId(), participant.getInviterId(), participant.getDate()));
                }
            } else {
                fullChatInfo.getChatInfo().setForbidden(true);
                fullChatInfo.getChatInfo().setAdminId(0);
                fullChatInfo.getChatInfo().getUsers().clear();
            }

            fullGroupDatabase.updateOrCreate(fullChatInfo);
        }
    }

    public synchronized void delete(int chatId) {
        fullGroupDatabase.delete(chatId);
    }
}
