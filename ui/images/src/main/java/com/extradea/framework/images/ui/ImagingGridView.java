package com.extradea.framework.images.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AbsListView;
import android.widget.GridView;
import com.extradea.framework.images.ImageSupport;

/**
 * Author: Korshakov Stepan
 * Created: 24.08.13 6:55
 */
public class ImagingGridView extends GridView {

    ImageSupport imageSupport;

    public ImagingGridView(Context context) {
        super(context);
        init();
    }

    public ImagingGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ImagingGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }


    private void init() {
        if (getContext() instanceof ImageSupport) {
            imageSupport = (ImageSupport) getContext();
        } else if (getContext().getApplicationContext() instanceof ImageSupport) {
            imageSupport = (ImageSupport) getContext().getApplicationContext();
        }

        if (imageSupport != null) {
            setOnScrollListener(new OnScrollListener() {

                boolean isScrolling = false;

                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {
                    if (i == SCROLL_STATE_IDLE) {
                        imageSupport.getImageController().doResume();
                        isScrolling = false;
                    } else {
                        imageSupport.getImageController().doPause();
                        isScrolling = true;
                    }
                }

                @Override
                public void onScroll(AbsListView absListView, int i, int i1, int i2) {
                    if (isScrolling) {
                        imageSupport.getImageController().doPause();
                    }
                }
            });
        }
    }
}
