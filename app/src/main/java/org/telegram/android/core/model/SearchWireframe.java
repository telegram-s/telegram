package org.telegram.android.core.model;

import android.text.Spanned;
import org.telegram.android.core.model.local.TLAbsLocalUserStatus;
import org.telegram.tl.TLObject;

/**
 * Author: Korshakov Stepan
 * Created: 24.08.13 22:31
 */
public class SearchWireframe {
    private int peerId;
    private int peerType;
    private String title;
    private Spanned highlightedTitle;
    private TLObject photo;
    private TLAbsLocalUserStatus status;
    private int members;

    public SearchWireframe(int peerId, int peerType, String title, Spanned highlightedTitle, TLObject photo, TLAbsLocalUserStatus status, int members) {
        this.peerId = peerId;
        this.peerType = peerType;
        this.title = title;
        this.photo = photo;
        this.status = status;
        this.members = members;
    }

    public int getPeerId() {
        return peerId;
    }

    public int getPeerType() {
        return peerType;
    }

    public String getTitle() {
        return title;
    }

    public TLObject getPhoto() {
        return photo;
    }

    public TLAbsLocalUserStatus getStatus() {
        return status;
    }

    public int getMembers() {
        return members;
    }
}