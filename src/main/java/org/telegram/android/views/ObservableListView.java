package org.telegram.android.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.extradea.framework.images.ui.ImagingListView;
import org.telegram.android.log.Logger;

/**
 * Author: Korshakov Stepan
 * Created: 12.08.13 13:48
 */
public class ObservableListView extends ImagingListView {

    private static final String TAG = "ObservableList";

    private int oldHeight = -1;

    public ObservableListView(Context context) {
        super(context);
    }

    public ObservableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ObservableListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View v = getChildAt(getChildCount() - 1);
        if (v != null && oldHeight > 0 && changed && ((b - t) < oldHeight)) {
            int bottom = oldHeight - v.getTop();
            int visiblePosition = getLastVisiblePosition();

            Logger.d(TAG, "onLayout1: " + +b + ", " + bottom + ", " + visiblePosition);

            super.onLayout(changed, l, t, r, b);

            Logger.d(TAG, "onLayout2: " + b);

            final int pos = visiblePosition;
            final int offset = (b - t) - bottom;
            post(new Runnable() {
                @Override
                public void run() {
                    setSelectionFromTop(pos, offset - getPaddingTop());
                }
            });
        } else {
            super.onLayout(changed, l, t, r, b);
        }
        oldHeight = (b - t);
    }
}
