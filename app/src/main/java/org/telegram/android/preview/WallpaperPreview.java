package org.telegram.android.preview;

import android.content.Context;
import android.util.AttributeSet;
import org.telegram.android.core.model.media.TLAbsLocalFileLocation;

/**
 * Created by ex3ndr on 21.02.14.
 */
public class WallpaperPreview extends BaseView<WallpaperLoader> {

    private TLAbsLocalFileLocation fileLocation;

    public WallpaperPreview(Context context) {
        super(context);
    }

    public WallpaperPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WallpaperPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected WallpaperLoader bindLoader() {
        return application.getUiKernel().getWallpaperLoader();
    }

    public void requestPreview(TLAbsLocalFileLocation localFileLocation) {
        fileLocation = localFileLocation;
        requestBind();
    }

    @Override
    protected void bind() {
        if (fileLocation == null) {
            getLoader().cancelRequest(this);
        } else {
            getLoader().requestPreview(fileLocation, this);
        }
    }
}
