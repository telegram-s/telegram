package org.telegram.android.preview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import org.telegram.android.util.CustomBufferedInputStream;

import java.io.*;

/**
 * Created by ex3ndr on 05.02.14.
 */
public class ImageStorage {
    private File folder;

    public ImageStorage(Context context, String name) {
        folder = new File(context.getFilesDir(), name);
        folder.mkdirs();
    }

    private String getFileName(String key) {
        return folder.getAbsolutePath() + "/" + key;
    }

    public void saveFile(String key, byte[] file) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(getFileName(key));
        outputStream.write(file);
        outputStream.close();
    }

    public void saveFile(String key, Bitmap bitmap) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(getFileName(key));
        bitmap.compress(Bitmap.CompressFormat.JPEG, 87, outputStream);
        outputStream.close();
    }

    public Bitmap tryLoadFile(String key, BitmapFactory.Options o) {
        String fileName = getFileName(key);
        if (!new File(fileName).exists()) {
            return null;
        }
        InputStream inputStream = null;
        try {
            inputStream = new CustomBufferedInputStream(new FileInputStream(fileName));
            return BitmapFactory.decodeStream(inputStream, null, o);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
