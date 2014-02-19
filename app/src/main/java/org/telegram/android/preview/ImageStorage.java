package org.telegram.android.preview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import org.telegram.android.media.BitmapDecoderEx;
import org.telegram.android.media.Optimizer;
import org.telegram.android.util.CustomBufferedInputStream;
import org.telegram.tl.TLBytes;

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

    public void saveFile(String key, TLBytes file) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(getFileName(key));
        outputStream.write(file.getData(), file.getOffset(), file.getLength());
        outputStream.close();
    }

    public void saveFile(String key, byte[] file) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(getFileName(key));
        outputStream.write(file);
        outputStream.close();
    }

    public void saveFile(String key, Bitmap bitmap) throws IOException {
        BitmapDecoderEx.saveBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), getFileName(key));
    }

    public void saveFile(String key, Bitmap bitmap, int w, int h) throws IOException {
        BitmapDecoderEx.saveBitmap(bitmap, w, h, getFileName(key));
    }

    public Optimizer.BitmapInfo tryLoadFile(String key, Bitmap reuse) {
        String fileName = getFileName(key);
        if (!new File(fileName).exists()) {
            return null;
        }
        try {
            return Optimizer.loadTo(fileName, reuse);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Bitmap tryLoadFile(String key) {
        String fileName = getFileName(key);
        if (!new File(fileName).exists()) {
            return null;
        }

        try {
            return Optimizer.load(fileName);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
}
