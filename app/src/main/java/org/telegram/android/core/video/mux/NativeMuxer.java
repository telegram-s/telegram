package org.telegram.android.core.video.mux;

import android.media.MediaCodec;
import android.media.MediaMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by ex3ndr on 18.02.14.
 */
public class NativeMuxer implements Muxer {
    @Override
    public void muxVideo(String sourceVideo, String videoTrack, String destFile) throws IOException {
        // TODO: Use original source

//        MediaMuxer mediaMuxer = new MediaMuxer(fileName, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
//        int track = mediaMuxer.addTrack(chunks.getMediaFormat());
//        mediaMuxer.start();
//        ByteBuffer buffer = ByteBuffer.allocate(5 * 1024 * 1024);
//        for (int i = 0; i < chunks.getNumChunks(); i++) {
//            chunks.getChunkData(i, buffer);
//            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//            bufferInfo.flags = chunks.getChunkFlags(i);
//            bufferInfo.presentationTimeUs = chunks.getChunkTime(i);
//            bufferInfo.size = chunks.getChunkSize(i);
//            bufferInfo.offset = 0;
//            mediaMuxer.writeSampleData(track, buffer, bufferInfo);
//        }
//        mediaMuxer.stop();
//        mediaMuxer.release();

        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void writeRawVideo(String videoTrack, String destFile) throws IOException {
        muxVideo(null, videoTrack, destFile);
    }
}
