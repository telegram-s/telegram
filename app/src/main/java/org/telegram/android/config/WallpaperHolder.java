package org.telegram.android.config;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import org.telegram.android.R;
import org.telegram.android.TelegramApplication;

import java.io.FileNotFoundException;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 18.09.13
 * Time: 18:35
 */
public class WallpaperHolder {
    private Bitmap wallpaper;
    private TelegramApplication application;

    public WallpaperHolder(TelegramApplication application) {
        this.application = application;
    }

    public Bitmap getBitmap() {
        if (wallpaper != null) {
            return wallpaper;
        }

        if (application.getUserSettings().isWallpaperSet()) {
            try {
                Bitmap w = BitmapFactory.decodeStream(application.openFileInput("current_wallpaper.jpg"));
                wallpaper = w;
                return wallpaper;
            } catch (FileNotFoundException e) {
                return null;
            }
        } else {
            try {
                Bitmap w = ((BitmapDrawable) application.getResources().getDrawable(R.drawable.st_chat_bg_default)).getBitmap();
                wallpaper = w;
                return wallpaper;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public void dropCache() {
        wallpaper = null;
    }
}
