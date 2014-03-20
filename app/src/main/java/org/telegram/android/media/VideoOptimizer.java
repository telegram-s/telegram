package org.telegram.android.media;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.provider.MediaStore;
import org.telegram.android.reflection.CrashHandler;

import java.io.IOException;

/**
 * Created by ex3ndr on 10.02.14.
 */
public class VideoOptimizer {
    public static VideoMetadata getVideoSize(String fileName) throws Exception {
        long timeInmillisec;
        int width;
        int height;
        Bitmap img;

        if (Build.VERSION.SDK_INT >= 10) {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(fileName);
                timeInmillisec = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                img = retriever.getFrameAtTime(0);
                width = img.getWidth();
                height = img.getHeight();
            } catch (Exception e) {
                // CrashHandler.logHandledException(e);
                throw e;
            }
        } else {
            img = ThumbnailUtils.createVideoThumbnail(fileName,
                    MediaStore.Images.Thumbnails.MINI_KIND);

            MediaPlayer mp = new MediaPlayer();
            final Object locker = new Object();
            final int[] sizes = new int[2];
            try {
                mp.setDataSource(fileName);
                mp.prepare();
                mp.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                    @Override
                    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                        synchronized (locker) {
                            sizes[0] = width;
                            sizes[1] = height;
                            locker.notify();
                        }
                    }
                });

                synchronized (locker) {
                    if (sizes[0] == 0 || sizes[1] == 1) {
                        locker.wait(5000);
                    }
                }

                if (sizes[0] == 0 || sizes[1] == 1) {
                    throw new IOException();
                }

                timeInmillisec = mp.getDuration() * 1000L;
                width = sizes[0];
                height = sizes[1];
            } catch (Exception e) {
                CrashHandler.logHandledException(e);
                throw e;
            }
        }

        return new VideoMetadata(timeInmillisec, width, height, img);
    }

    public static class VideoMetadata {
        private long duration;
        private int width;
        private int height;
        private Bitmap img;

        private VideoMetadata(long duration, int width, int height, Bitmap img) {
            this.duration = duration;
            this.width = width;
            this.height = height;
            this.img = img;
        }

        public long getDuration() {
            return duration;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public Bitmap getImg() {
            return img;
        }
    }

}
