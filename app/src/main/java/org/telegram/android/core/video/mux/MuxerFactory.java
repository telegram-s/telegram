package org.telegram.android.core.video.mux;

/**
 * Created by ex3ndr on 17.02.14.
 */
public final class MuxerFactory {
    public static Muxer createMuxer() {
        return new CompatMuxer();
    }

    MuxerFactory() {
    }
}
