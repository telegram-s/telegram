package org.telegram.android.core.video.mux;

import java.io.IOException;

/**
 * Created by ex3ndr on 17.02.14.
 */
public interface Muxer {

    public void muxVideo(String sourceVideo, String videoTrack, String destFile) throws IOException;

    public void writeRawVideo(String videoTrack, String destFile) throws IOException;
}
