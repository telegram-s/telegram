package org.telegram.android.views.dialog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.AbsListView;
import com.extradea.framework.images.ui.ImagingListView;

/**
 * Created by ex3ndr on 15.11.13.
 */
public class ConversationListView extends ImagingListView {
    public ConversationListView(Context context) {
        super(context);
    }

    public ConversationListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ConversationListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void init() {
        setOnScrollListener(new ScrollListener());
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        Rect rect = new Rect();
        rect.set(0, getPaddingTop(), getWidth(), getPaddingTop() + 80);
        Paint selectorPaint = new Paint();
        selectorPaint.setColor(0x66000000);
        canvas.drawRect(rect, selectorPaint);
    }

    private class ScrollListener implements OnScrollListener {

        @Override
        public void onScrollStateChanged(AbsListView absListView, int i) {

        }

        @Override
        public void onScroll(AbsListView absListView, int i, int i2, int i3) {

        }
    }
}
