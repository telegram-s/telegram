package org.telegram.android.core.video.buffer;

import android.media.*;
import android.util.Log;
import org.telegram.android.core.video.VideoChunks;
import org.telegram.android.core.video.mux.MuxerFactory;
import org.telegram.android.log.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by ex3ndr on 18.02.14.
 */
public class BufferTranscode {
    private static final String TAG = "BufferTranscode";

    public static boolean transcode(String sourceFile, String destFile) throws IOException {
        long tStart = System.currentTimeMillis();
        long start = System.currentTimeMillis();
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(sourceFile);
        int trackIndex = selectTrack(extractor);
        if (trackIndex < 0) {
            throw new RuntimeException("No video track found in " + sourceFile);
        }
        extractor.selectTrack(trackIndex);

        VideoChunks chunks = extractVideo(extractor, trackIndex);
        Logger.d(TAG, "Extracted in " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        VideoChunks res = editVideo(chunks);
        Logger.d(TAG, "Edited in " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        res.saveToFile(new File(destFile + ".h264"));
        MuxerFactory.createMuxer().muxVideo(sourceFile, destFile + ".h264", destFile);
        Logger.d(TAG, "Muxed in " + (System.currentTimeMillis() - start) + " ms");

        Logger.d(TAG, "Completed in " + (System.currentTimeMillis() - tStart) + " ms");
        return true;
    }

    private static VideoChunks editVideo(VideoChunks inputData) {
        VideoChunks outputData = new VideoChunks();
        MediaCodec decoder = null;
        MediaCodec encoder = null;

        try {

            int numCodecs = MediaCodecList.getCodecCount();
            MediaCodecInfo codecInfo = null;
            for (int i = 0; i < numCodecs && codecInfo == null; i++) {
                MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
                if (!info.isEncoder()) {
                    continue;
                }
                String[] types = info.getSupportedTypes();
                boolean found = false;
                for (int j = 0; j < types.length && !found; j++) {
                    if (types[j].equals("video/avc"))
                        found = true;
                }
                if (!found)
                    continue;
                codecInfo = info;
            }

            int colorFormat = 0;
            MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType("video/avc");
            for (int i = 0; i < capabilities.colorFormats.length && colorFormat == 0; i++) {
                int format = capabilities.colorFormats[i];
                switch (format) {
                    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
                    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
                    case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                        colorFormat = format;
                        break;
                    default:
                        Log.d(TAG, "Skipping unsupported color format " + format);
                        break;
                }
            }
            Log.d(TAG, "Using color format " + colorFormat);

            MediaFormat inputFormat = inputData.getMediaFormat();

            // Create an encoder format that matches the input format.  (Might be able to just
            // re-use the format used to generate the video, since we want it to be the same.)
            MediaFormat outputFormat = MediaFormat.createVideoFormat("video/avc",
                    inputFormat.getInteger(MediaFormat.KEY_WIDTH),
                    inputFormat.getInteger(MediaFormat.KEY_HEIGHT));
            outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1000000);
            outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
            outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 15);

            encoder = MediaCodec.createEncoderByType("video/avc");
            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();

            decoder = MediaCodec.createDecoderByType("video/avc");
            decoder.configure(inputFormat, null, null, 0);
            decoder.start();

            editVideoData(inputData, decoder, outputData, encoder);
        } finally {
            Log.d(TAG, "shutting down encoder, decoder");
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
            if (decoder != null) {
                decoder.stop();
                decoder.release();
            }
        }

        return outputData;
    }

    private static void editVideoData(VideoChunks inputData, MediaCodec decoder, VideoChunks outputData, MediaCodec encoder) {
        final int TIMEOUT_USEC = 10000;
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
        ByteBuffer[] decoderOutputBuffers = decoder.getOutputBuffers();

        ByteBuffer[] encoderInputBuffers = encoder.getInputBuffers();
        ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int inputChunk = 0;
        int outputCount = 0;

        boolean outputDone = false;
        boolean inputDone = false;
        boolean decoderDone = false;
        while (!outputDone) {
            // Log.d(TAG, "edit loop");

            // Feed more data to the decoder.
            if (!inputDone) {
                int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufIndex >= 0) {
                    if (inputChunk == inputData.getNumChunks()) {
                        // End of stream -- send empty frame with EOS flag set.
                        decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                        // Log.d(TAG, "sent input EOS (with zero-length frame)");
                    } else {
                        // Copy a chunk of input to the decoder.  The first chunk should have
                        // the BUFFER_FLAG_CODEC_CONFIG flag set.
                        ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                        inputBuf.clear();
                        inputData.getChunkData(inputChunk, inputBuf);
                        int flags = inputData.getChunkFlags(inputChunk);
                        long time = inputData.getChunkTime(inputChunk);
                        decoder.queueInputBuffer(inputBufIndex, 0, inputData.getChunkSize(inputChunk),
                                time, flags);

                        // Log.d(TAG, "submitted frame " + inputChunk + " to dec, size=" + inputBuf.position() + " flags=" + flags);
                        inputChunk++;
                    }
                } else {
                    // Log.d(TAG, "input buffer not available");
                }
            }

            // Assume output is available.  Loop until both assumptions are false.
            boolean decoderOutputAvailable = !decoderDone;
            boolean encoderOutputAvailable = true;
            while (decoderOutputAvailable || encoderOutputAvailable) {
                // Start by draining any pending output from the encoder.  It's important to
                // do this before we try to stuff any more data in.
                int encoderStatus = encoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    // Log.d(TAG, "no output from encoder available");
                    encoderOutputAvailable = false;
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    encoderOutputBuffers = encoder.getOutputBuffers();
                    // Log.d(TAG, "encoder output buffers changed");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = encoder.getOutputFormat();
                    // Log.d(TAG, "encoder output format changed: " + newFormat);
                    outputData.setMediaFormat(newFormat);
                } else if (encoderStatus < 0) {
                    // fail("unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                } else { // encoderStatus >= 0
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        // fail("encoderOutputBuffer " + encoderStatus + " was null");
                    }

                    // Write the data to the output "file".
                    if (info.size != 0) {
                        encodedData.position(info.offset);
                        encodedData.limit(info.offset + info.size);

                        outputData.addChunk(encodedData, info.size, info.flags, info.presentationTimeUs);
                        outputCount++;
                    }
                    // Log.d(TAG, "encoder output " + info.size + " bytes");
                    outputDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    encoder.releaseOutputBuffer(encoderStatus, false);
                }
                if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // Continue attempts to drain output.
                    continue;
                }

                // Encoder is drained, check to see if we've got a new frame of output from
                // the decoder.  (The output is going to a Surface, rather than a ByteBuffer,
                // but we still get information through BufferInfo.)
                if (!decoderDone) {
                    int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                    if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // no output available yet
                        Log.d(TAG, "no output from decoder available");
                        decoderOutputAvailable = false;
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        decoderOutputBuffers = decoder.getOutputBuffers();
                        Log.d(TAG, "decoder output buffers changed");
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // expected before first buffer of data
                        MediaFormat newFormat = decoder.getOutputFormat();
                        Log.d(TAG, "decoder output format changed: " + newFormat);
                    } else if (decoderStatus < 0) {
                        // fail("unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus);
                    } else { // decoderStatus >= 0
                        // Log.d(TAG, "surface decoder given buffer " + decoderStatus + " (size=" + info.size + ")");
                        // The ByteBuffers are null references, but we still get a nonzero
                        // size for the decoded data.
                        boolean doRender = (info.size != 0);

                        // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                        // to SurfaceTexture to convert to a texture.  The API doesn't
                        // guarantee that the texture will be available before the call
                        // returns, so we need to wait for the onFrameAvailable callback to
                        // fire.  If we don't wait, we risk rendering from the previous frame.
                        decoder.releaseOutputBuffer(decoderStatus, false);
                        if (doRender) {
                            // This waits for the image and renders it after it arrives.
                            // Log.d(TAG, "awaiting frame");
                            // outputSurface.awaitNewImage();
                            // outputSurface.drawImage();

                            // Send it to the encoder.
                            // inputSurface.setPresentationTime(info.presentationTimeUs * 1000);
                            // Log.d(TAG, "swapBuffers");
                            // inputSurface.swapBuffers();
                            int index = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                        }
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            // forward decoder EOS to encoder
                            //if (VERBOSE) Log.d(TAG, "signaling input EOS");
                            //if (WORK_AROUND_BUGS) {
                            // Bail early, possibly dropping a frame.
                            // return;
                            //} else {
                            encoder.signalEndOfInputStream();
                            //}
                        }
                    }
                }
            }
        }

//        if (inputChunk != outputCount) {
//            throw new RuntimeException("frame lost: " + inputChunk + " in, " +
//                    outputCount + " out");
//        }
    }

    private static VideoChunks extractVideo(MediaExtractor extractor, int track) {
        VideoChunks dest = new VideoChunks();
        dest.setMediaFormat(extractor.getTrackFormat(track));
        ByteBuffer inputBuffer = ByteBuffer.allocate(5 * 1024 * 1024);
        int size = 0;
        while ((size = extractor.readSampleData(inputBuffer, 0)) >= 0) {
            long presentationTimeUs = extractor.getSampleTime();
            int flags = extractor.getSampleFlags();
            dest.addChunk(inputBuffer, size, flags, presentationTimeUs);
            inputBuffer.clear();
            extractor.advance();

        }
        return dest;
    }

    /**
     * Selects the video track, if any.
     *
     * @return the track index, or -1 if no video track is found.
     */
    private static int selectTrack(MediaExtractor extractor) {
        // Select the first video track we find, ignore the rest.
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                return i;
            }
        }

        return -1;
    }

    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns a color format that is supported by the codec and by this test code.  If no
     * match is found, this throws a test failure -- the set of formats known to the test
     * should be expanded for new platforms.
     */
    private static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        throw new RuntimeException("Unknown color format");
    }

}
