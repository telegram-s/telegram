package org.telegram.android.media;

/**
 * Author: Korshakov Stepan
 * Created: 10.08.13 21:46
 */
public interface DownloadListener {
    public void onStateChanged(String key, DownloadState state, int percent);
}
