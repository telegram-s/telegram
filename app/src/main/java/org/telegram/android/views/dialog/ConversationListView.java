package org.telegram.android.views.dialog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.AbsListView;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import com.extradea.framework.images.ui.ImagingListView;
import com.google.android.gms.internal.ca;
import com.google.android.gms.internal.da;
import org.telegram.android.R;
import org.telegram.android.ui.FontController;
import org.telegram.android.ui.TextUtil;

/**
 * Created by ex3ndr on 15.11.13.
 */
public class ConversationListView extends ImagingListView {

    private static final int DELTA = 26;


    private String visibleDate = null;

    private int timeDivMeasure;
    private int currentVisibleItem = -1;


    private TextPaint timeDivPaint;
    private Drawable serviceDrawable;
    private Rect servicePadding;

    private int offset;

    public ConversationListView(Context context) {
        super(context);
        init();
    }

    public ConversationListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ConversationListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setOnScrollListener(new ScrollListener());
        serviceDrawable = getResources().getDrawable(R.drawable.st_bubble_service);
        servicePadding = new Rect();
        serviceDrawable.getPadding(servicePadding);

        timeDivPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        timeDivPaint.setTextSize(getSp(15));
        timeDivPaint.setColor(0xffFFFFFF);
        timeDivPaint.setTypeface(FontController.loadTypeface(getContext(), "regular"));
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (visibleDate != null) {

            int drawOffset = offset;

            serviceDrawable.setBounds(
                    getWidth() / 2 - timeDivMeasure / 2 - servicePadding.left,
                    getPx(44 - 8) - serviceDrawable.getIntrinsicHeight() + drawOffset,
                    getWidth() / 2 + timeDivMeasure / 2 + servicePadding.right,
                    getPx(44 - 8) + drawOffset);
            serviceDrawable.draw(canvas);
            canvas.drawText(visibleDate, getWidth() / 2 - timeDivMeasure / 2, getPx(44 - 17) + drawOffset, timeDivPaint);
        }
    }

    protected int getPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    protected int getSp(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }

    private class ScrollListener implements OnScrollListener {

        @Override
        public void onScrollStateChanged(AbsListView absListView, int i) {

        }

        @Override
        public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            ListAdapter adapter = getAdapter();
            if (adapter instanceof HeaderViewListAdapter) {
                adapter = ((HeaderViewListAdapter) adapter).getWrappedAdapter();
            }
            if (adapter instanceof ConversationAdapter) {
                int realFirstVisibleItem = firstVisibleItem - getHeaderViewsCount();
                if (realFirstVisibleItem >= 0 && realFirstVisibleItem < adapter.getCount()) {
                    int date = ((ConversationAdapter) adapter).getItemDate(realFirstVisibleItem);
                    boolean isSameDays = true;
                    if (realFirstVisibleItem > 0) {
                        int prevDate = ((ConversationAdapter) adapter).getItemDate(realFirstVisibleItem + 2);
                        isSameDays = TextUtil.areSameDays(prevDate, date);
                    }


                    if (isSameDays) {
                        offset = 0;
                    } else {
                        View view = getChildAt(firstVisibleItem - realFirstVisibleItem);
                        if (view != null) {
                            offset = Math.min(view.getTop() - getPx(DELTA), 0);
                        }
                    }

                    visibleDate = TextUtil.formatDateLong(date);
                    timeDivMeasure = (int) timeDivPaint.measureText(visibleDate);
                }
            }
        }
    }
}
