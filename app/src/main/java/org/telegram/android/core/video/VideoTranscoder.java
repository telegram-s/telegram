package org.telegram.android.core.video;

import android.os.Build;
import org.telegram.android.core.video.buffer.BufferTranscode;
import org.telegram.android.core.video.gl.GLTranscoder;

import java.io.IOException;

/**
 * Created by ex3ndr on 15.02.14.
 */
public class VideoTranscoder {

    private static final String TAG = "VideoTranscoder";

    public static boolean transcodeVideo(String source, String dest) throws IOException {
        if (Build.VERSION.SDK_INT >= 18) {
            return GLTranscoder.transcode(source, dest);
        }

        return false;
    }
}
