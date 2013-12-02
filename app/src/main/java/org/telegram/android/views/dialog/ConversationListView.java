package org.telegram.android.views.dialog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import org.telegram.android.log.Logger;
import org.telegram.android.ui.FontController;
import org.telegram.android.ui.TextUtil;

/**
 * Created by ex3ndr on 15.11.13.
 */
public class ConversationListView extends ImagingListView {

    private static final int DELTA = 26;

    private static final int ACTIVATE_DELTA = 50;
    private static final long UI_TIMEOUT = 900;


    private String visibleDate = null;
    private int timeDivMeasure;

    private String visibleDateNext = null;
    private int timeDivMeasureNext;


    private TextPaint timeDivPaint;
    private Drawable serviceDrawable;
    private Rect servicePadding;

    private int offset;

    private boolean isTimeVisible = false;
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                isTimeVisible = false;
                scrollDistance = 0;
                invalidate();
            } else if (msg.what == 1) {
                isTimeVisible = true;
                invalidate();
            }
        }
    };

    private int scrollDistance;

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

    private void drawTime(Canvas canvas, int drawOffset, float alpha, boolean first) {
        int w = first ? timeDivMeasure : timeDivMeasureNext;
        serviceDrawable.setAlpha((int) (alpha * 255));
        timeDivPaint.setAlpha((int) (alpha * 255));
        serviceDrawable.setBounds(
                getWidth() / 2 - w / 2 - servicePadding.left,
                getPx(44 - 8) - serviceDrawable.getIntrinsicHeight() + drawOffset,
                getWidth() / 2 + w / 2 + servicePadding.right,
                getPx(44 - 8) + drawOffset);
        serviceDrawable.draw(canvas);
        canvas.drawText(first ? visibleDate : visibleDateNext, getWidth() / 2 - w / 2, getPx(44 - 17) + drawOffset, timeDivPaint);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (isTimeVisible) {
            if (visibleDate != null) {

                int drawOffset = offset;

                if (offset == 0) {
                    drawTime(canvas, drawOffset, 1.0f, true);
                } else {
                    float ratio = Math.max(0.0f, Math.abs(offset / (float) getPx(DELTA)));
                    drawTime(canvas, drawOffset + getPx(DELTA), ratio, false);
                    drawTime(canvas, drawOffset, 1.0f - ratio, true);
                }
            }
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
            if (i == SCROLL_STATE_FLING || i == SCROLL_STATE_TOUCH_SCROLL) {
                handler.removeMessages(0);
            }

            if (i == SCROLL_STATE_IDLE) {
                handler.removeMessages(0);
                handler.sendEmptyMessageDelayed(0, UI_TIMEOUT);
            }
        }

        int lastVisibleItem = -1;
        int lastTop = 0;

        @Override
        public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (lastVisibleItem == -1 || lastVisibleItem != firstVisibleItem) {
                lastVisibleItem = firstVisibleItem;
                lastTop = 0;
                View view = getChildAt(0 + getHeaderViewsCount());
                if (view != null) {
                    lastTop = view.getTop();
                }
            } else {
                View view = getChildAt(0 + getHeaderViewsCount());
                if (view != null) {
                    int topDelta = Math.abs(view.getTop() - lastTop);
                    lastTop = view.getTop();
                    scrollDistance += topDelta;
                    if (scrollDistance > getPx(ACTIVATE_DELTA) && !isTimeVisible) {
                        isTimeVisible = true;
                        handler.removeMessages(0);
                    }
                }
            }


            // handler.removeMessages(0);

            ListAdapter adapter = getAdapter();
            if (adapter instanceof HeaderViewListAdapter) {
                adapter = ((HeaderViewListAdapter) adapter).getWrappedAdapter();
            }
            if (adapter instanceof ConversationAdapter) {
                int realFirstVisibleItem = firstVisibleItem - getHeaderViewsCount();
                if (realFirstVisibleItem >= 0 && realFirstVisibleItem < adapter.getCount()) {
                    int date = ((ConversationAdapter) adapter).getItemDate(realFirstVisibleItem);
                    int prevDate = date;
                    boolean isSameDays = true;
                    if (realFirstVisibleItem > 0) {
                        prevDate = ((ConversationAdapter) adapter).getItemDate(realFirstVisibleItem + 1);
                        isSameDays = TextUtil.areSameDays(prevDate, date);
                    }


                    if (isSameDays) {
                        offset = 0;
                    } else {
                        View view = getChildAt(firstVisibleItem - realFirstVisibleItem);
                        if (view != null) {
                            offset = Math.min(view.getTop() - getPx(DELTA), 0);
                        }
                        visibleDateNext = TextUtil.formatDateLong(prevDate);
                        timeDivMeasureNext = (int) timeDivPaint.measureText(visibleDateNext);
                    }

                    visibleDate = TextUtil.formatDateLong(date);
                    timeDivMeasure = (int) timeDivPaint.measureText(visibleDate);
                }
            }
        }
    }
}
