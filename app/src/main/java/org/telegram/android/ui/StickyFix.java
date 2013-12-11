package org.telegram.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Created by ex3ndr on 11.12.13.
 */
public class StickyFix extends StickyListHeadersListView {
    public StickyFix(Context context) {
        super(context);
    }

    public StickyFix(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StickyFix(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        if (left == 0 && right == 0) {
            return;
        }
        super.setPadding(left, top, right, bottom);
    }
}
