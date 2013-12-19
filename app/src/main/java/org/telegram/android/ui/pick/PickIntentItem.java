package org.telegram.android.ui.pick;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

/**
* Created by ex3ndr on 19.12.13.
*/
public class PickIntentItem {
    private int resource;
    private Bitmap bitmap;
    private Drawable drawable;

    private String title;
    private String subtitle;

    public PickIntentItem(int resource, String title) {
        this.resource = resource;
        this.title = title;
    }

    public PickIntentItem(Bitmap bitmap, String title) {
        this.bitmap = bitmap;
        this.title = title;
    }

    public PickIntentItem(Drawable drawable, String title) {
        this.drawable = drawable;
        this.title = title;
    }

    public PickIntentItem(int resource, String title, String subtitle) {
        this.resource = resource;
        this.title = title;
        this.subtitle = subtitle;
    }

    public PickIntentItem(Bitmap bitmap, String title, String subtitle) {
        this.bitmap = bitmap;
        this.title = title;
        this.subtitle = subtitle;
    }

    public PickIntentItem(Drawable drawable, String title, String subtitle) {
        this.drawable = drawable;
        this.title = title;
        this.subtitle = subtitle;
    }

    public int getResource() {
        return resource;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }
}
