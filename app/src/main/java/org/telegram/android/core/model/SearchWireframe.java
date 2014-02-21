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
    private CharSequence title;
    private TLObject photo;

    public SearchWireframe(int peerId, int peerType, CharSequence title, TLObject photo) {
        this.peerId = peerId;
        this.peerType = peerType;
        this.title = title;
        this.photo = photo;
    }

    public int getPeerId() {
        return peerId;
    }

    public int getPeerType() {
        return peerType;
    }

    public CharSequence getTitle() {
        return title;
    }

    public TLObject getPhoto() {
        return photo;
    }
}