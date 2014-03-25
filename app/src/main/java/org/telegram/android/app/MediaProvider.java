package org.telegram.android.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import org.telegram.android.TelegramApplication;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by ex3ndr on 22.01.14.
 */
public class MediaProvider extends ContentProvider {
    private TelegramApplication application;

    @Override
    public boolean onCreate() {
        application = (TelegramApplication) getContext().getApplicationContext();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings2, String s2) {
        return new MatrixCursor(new String[0], 0);
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!application.isLoggedIn()) {
            throw new IllegalArgumentException();
        }
        String fileName = application.getDownloadManager().getFileName(uri.getPath());
        File file = new File(fileName);
        if (!file.exists()) {
            throw new IllegalArgumentException();
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
