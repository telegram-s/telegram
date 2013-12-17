package org.telegram.android.media;

import android.content.Context;
import com.extradea.framework.persistence.ContextPersistence;
import org.telegram.android.critical.SafeFileWriter;
import org.telegram.android.kernel.compat.CompatObjectInputStream;
import org.telegram.android.kernel.compat.Compats;
import org.telegram.android.kernel.compat.v6.CompatDownload;

import java.io.*;
import java.util.HashSet;

/**
 * Author: Korshakov Stepan
 * Created: 11.08.13 2:51
 */
public class DownloadPersistence {

    private HashSet<String> downloadedVideos = new HashSet<String>();

    private SafeFileWriter fileWriter;

    public DownloadPersistence(Context context) {
        fileWriter = new SafeFileWriter(context, "org.telegram.android.DownloadPersistence.bin");

        byte[] data = fileWriter.loadData();
        if (data != null) {
            try {
                DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(data));
                int count = dataInputStream.readInt();
                for (int i = 0; i < count; i++) {
                    downloadedVideos.add(dataInputStream.readUTF());
                }
                dataInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        File obsoleteFile = new File(context.getFilesDir().getAbsolutePath(), "org.telegram.android.media.DownloadPersistence.sav");
        if (obsoleteFile.exists()) {
            try {
                FileInputStream inputStream = new FileInputStream(obsoleteFile);
                ObjectInputStream dataInputStream = new CompatObjectInputStream(inputStream, Compats.DOWNLOADER);
                CompatDownload download = (CompatDownload) dataInputStream.readObject();
                for (String s : download.getDownloadedVideos()) {
                    downloadedVideos.add(s);
                }
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            obsoleteFile.delete();
        }
    }

    private void saveState() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            dataOutputStream.writeInt(downloadedVideos.size());
            for (String s : downloadedVideos) {
                dataOutputStream.writeUTF(s);
            }
            dataOutputStream.close();
            fileWriter.saveData(outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isDownloaded(String key) {
        return downloadedVideos.contains(key);
    }

    public synchronized void markDownloaded(String key) {
        downloadedVideos.add(key);
        saveState();
    }

    public synchronized void markUnloaded(String key) {
        downloadedVideos.remove(key);
        saveState();
    }

}
