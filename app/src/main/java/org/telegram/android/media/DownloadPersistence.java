package org.telegram.android.media;

import android.content.Context;
import com.extradea.framework.persistence.ContextPersistence;

import java.util.HashSet;

/**
 * Author: Korshakov Stepan
 * Created: 11.08.13 2:51
 */
public class DownloadPersistence extends ContextPersistence {
    static final long serialVersionUID = 4L;

    private HashSet<String> downloadedVideos = new HashSet<String>();

    public DownloadPersistence(Context context) {
        super(context, true);
    }

    public boolean isDownloaded(String key) {
        return downloadedVideos.contains(key);
    }

    public synchronized void markDownloaded(String key) {
        downloadedVideos.add(key);
        trySave();
    }

    public synchronized void markUnloaded(String key) {
        downloadedVideos.remove(key);
        trySave();
    }

}
