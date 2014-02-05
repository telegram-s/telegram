package org.telegram.android.preview;

import android.content.Context;

import java.io.File;

/**
 * Created by ex3ndr on 05.02.14.
 */
public class ImageStorage {
    private File folder;

    public ImageStorage(Context context, String name) {
        folder = new File(context.getFilesDir(), name);
    }

    public void saveFile(String key, String variant) {

    }
}
