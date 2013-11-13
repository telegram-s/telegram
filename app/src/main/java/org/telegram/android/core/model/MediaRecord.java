package org.telegram.android.core.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.Serializable;

/**
 * Author: Korshakov Stepan
 * Created: 07.08.13 16:37
 */
public class MediaRecord implements Serializable {
    @DatabaseField(generatedId = true, version = false)
    private int _id;

    @DatabaseField(index = true, version = false)
    private int mid;

    @DatabaseField(index = true, version = false)
    private int peerType;

    @DatabaseField(index = true, version = false)
    private int peerId;

    @DatabaseField(version = false)
    private int date;

    @DatabaseField(version = false)
    private int senderId;

    @DatabaseField(version = false)
    private int mediaType;

    @DatabaseField(version = false, persisterClass = TlDataType.class)
    private TLObject preview;

    public int getDatabaseId() {
        return _id;
    }

    public void setDatabaseId(int _id) {
        this._id = _id;
    }

    public int getMid() {
        return mid;
    }

    public void setMid(int mid) {
        this.mid = mid;
    }

    public int getPeerType() {
        return peerType;
    }

    public void setPeerType(int peerType) {
        this.peerType = peerType;
    }

    public int getPeerId() {
        return peerId;
    }

    public void setPeerId(int peerId) {
        this.peerId = peerId;
    }

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getMediaType() {
        return mediaType;
    }

    public void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }

    public TLObject getPreview() {
        return preview;
    }

    public void setPreview(TLObject preview) {
        this.preview = preview;
    }
}