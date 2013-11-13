package org.telegram.android.critical;

import android.content.Context;
import org.telegram.android.log.Logger;

import java.io.*;
import java.util.Random;
import java.util.zip.CRC32;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Author: Korshakov Stepan
 * Created: 23.08.13 1:46
 */
public class SafeFileWriter {
    private final String TAG;
    private Random random = new Random();
    private Context context;
    private String fileName;

    public SafeFileWriter(Context context, String fileName) {
        this.context = context;
        this.fileName = fileName;
        TAG = "SafeFileWriter#" + hashCode();
    }

    private File getFile() {
        File res = new File(context.getFilesDir().getPath() + "/" + fileName);
        return res;
    }

    private File getTempFile() {
        return new File(context.getFilesDir().getPath() + "/random_" + random.nextLong() + ".tmp");
    }

    public synchronized void saveData(byte[] data) {
        File file = getTempFile();
        if (file.exists()) {
            file.delete();
        }

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file);
            writeInt(data.length, os);
            writeByteArray(data, os);
            CRC32 crc32 = new CRC32();
            crc32.update(data);
            writeLong(crc32.getValue(), os);
            os.flush();
            os.getFD().sync();
            os.close();
            os = null;
            boolean res = file.renameTo(getFile());
            String s = res+"";
        } catch (FileNotFoundException e) {
            Logger.t(TAG, e);
        } catch (IOException e) {
            Logger.t(TAG, e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Logger.t(TAG, e);
                }
            }
        }
    }

    public synchronized byte[] loadData() {
        File file = getFile();
        if (!file.exists())
            return null;

        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            int len = readInt(is);
            byte[] res = readBytes(len, is);
            CRC32 crc32 = new CRC32();
            crc32.update(res);
            long crc = readLong(is);
            if (crc32.getValue() != crc) {
                return null;
            }
            return res;
        } catch (FileNotFoundException e) {
            Logger.t(TAG, e);
        } catch (IOException e) {
            Logger.t(TAG, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Logger.t(TAG, e);
                }
            }
        }
        return null;
    }
}