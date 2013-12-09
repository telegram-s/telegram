package org.telegram.android.kernel.compat.v6;

import org.telegram.android.kernel.compat.CompatContextPersistence;

import java.util.HashSet;

/**
 * Created by ex3ndr on 10.12.13.
 */
public class CompatDownload extends CompatContextPersistence {
    private HashSet<String> downloadedVideos = new HashSet<String>();

    public HashSet<String> getDownloadedVideos() {
        return downloadedVideos;
    }
}
