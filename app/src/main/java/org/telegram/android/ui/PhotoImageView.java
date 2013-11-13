package org.telegram.android.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import com.extradea.framework.images.ui.WebImageView;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Author: Korshakov Stepan
 * Created: 12.09.13 2:41
 */
public class PhotoImageView extends WebImageView {

    private PhotoViewAttacher photoViewAttacher;

    public PhotoImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public PhotoImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PhotoImageView(Context context) {
        super(context);
        init();
    }

    private void init() {
        photoViewAttacher = new PhotoViewAttacher(this);
    }

    public PhotoViewAttacher getPhotoViewAttacher() {
        return photoViewAttacher;
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        if (bm != null) {
            try {
                photoViewAttacher.update();
            } catch (IllegalStateException e) {

            }
        }
    }
}
