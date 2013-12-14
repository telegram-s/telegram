package org.telegram.android.core.model.update;

import org.telegram.android.core.model.ChatMessage;
import org.telegram.api.messages.TLAbsStatedMessage;

/**
 * Created by ex3ndr on 14.12.13.
 */
public class TLLocalMessageSentPhoto extends TLLocalUpdate {
    private TLAbsStatedMessage absStatedMessage;

    private ChatMessage message;

    private byte[] fastPreview;
    private int fastPreviewW;
    private int fastPreviewH;

    public TLLocalMessageSentPhoto(TLAbsStatedMessage absStatedMessage, ChatMessage message, byte[] fastPreview, int fastPreviewW, int fastPreviewH) {
        this.absStatedMessage = absStatedMessage;
        this.message = message;
        this.fastPreview = fastPreview;
        this.fastPreviewW = fastPreviewW;
        this.fastPreviewH = fastPreviewH;
    }

    public TLAbsStatedMessage getAbsStatedMessage() {
        return absStatedMessage;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public byte[] getFastPreview() {
        return fastPreview;
    }

    public int getFastPreviewW() {
        return fastPreviewW;
    }

    public int getFastPreviewH() {
        return fastPreviewH;
    }
}
