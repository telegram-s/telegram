package org.telegram.android.ui.pick;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.*;
import com.nineoldandroids.animation.ObjectAnimator;
import org.telegram.android.R;

/**
 * Created by ex3ndr on 19.12.13.
 */
public class PickIntentDialog extends Dialog {

    private static final int ANIMATION_DURATION = 600;

    private boolean isCanceling = false;

    private RootView rootView;
    private PickIntentItem[] items;
    private PickIntentClickListener clickListener;

    public PickIntentDialog(Context context, PickIntentItem[] items, PickIntentClickListener clickListener) {
        super(context, R.style.PickDialog_Theme);
        this.items = items;
        this.clickListener = clickListener;
        init();
    }

    @Override
    public void cancel() {
        if (isCanceling) {
            return;
        }

        isCanceling = true;

        ViewCompat.postOnAnimation(rootView, new Runnable() {
            @Override
            public void run() {
                ObjectAnimator animator = ObjectAnimator.ofFloat(rootView, "translationY", 0, rootView.getHeight())
                        .setDuration(500);
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.start();
                rootView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        PickIntentDialog.this.superCancel();
                    }
                }, 400);
            }
        });
    }

    private void superCancel() {
        super.cancel();
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getContext().getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private void init() {

        WindowManager manager = (WindowManager) getContext().getSystemService(Activity.WINDOW_SERVICE);
        int width, height;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            width = manager.getDefaultDisplay().getWidth();
            height = manager.getDefaultDisplay().getHeight() - getStatusBarHeight();
        } else {
            Point point = new Point();
            manager.getDefaultDisplay().getSize(point);
            width = point.x;
            height = point.y - getStatusBarHeight();
        }

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.width = width;
        lp.height = height;
        getWindow().setAttributes(lp);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 4, 0, 4);
        layout.setLayoutParams(new ScrollView.LayoutParams(
                width,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout row = null;

        for (int i = 0; i < items.length; i++) {
            if (i % 3 == 0) {

                if (row != null) {
                    layout.addView(row);
                }

                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(20, 0, 20, 0);
                row.setLayoutParams(new LinearLayout.LayoutParams(
                        width,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                row.setWeightSum(3);
            }

            View item = View.inflate(getContext(), R.layout.picker_item, null);
            item.setBackgroundResource(R.drawable.st_list_selector);

            ((TextView) item.findViewById(R.id.title)).setText(items[i].getTitle());
            if (items[i].getBitmap() != null) {
                ((ImageView) item.findViewById(R.id.icon)).setImageBitmap(items[i].getBitmap());
            } else if (items[i].getDrawable() != null) {
                ((ImageView) item.findViewById(R.id.icon)).setImageDrawable(items[i].getDrawable());
            } else if (items[i].getResource() == 0) {
                ((ImageView) item.findViewById(R.id.icon)).setImageResource(items[i].getResource());
            } else {
                ((ImageView) item.findViewById(R.id.icon)).setImageDrawable(null);
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 212, 1);
            item.setLayoutParams(params);
            final int finalI = i;
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.onItemClicked(finalI);
                    dismiss();
                }
            });
            row.addView(item);
        }

        if (row != null) {
            layout.addView(row);
        }

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                width,
                ViewGroup.LayoutParams.FILL_PARENT));
        scrollView.setBackgroundColor(Color.WHITE);
        scrollView.addView(layout);

        View headerView = View.inflate(getContext(), R.layout.picker_header, null);
        headerView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        rootView = new RootView(getContext(), headerView, scrollView, items.length);
        FrameLayout container = new FrameLayout(getContext());
        rootView.setLayoutParams(new FrameLayout.LayoutParams(width, height));
        container.addView(rootView);

        ObjectAnimator animator = ObjectAnimator
                .ofFloat(rootView, "translationY", 2000, 2000)
                .setDuration(0);
        animator.start();

        container.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        setContentView(container);

        ViewCompat.postOnAnimation(rootView, new Runnable() {
            @Override
            public void run() {
                ObjectAnimator animator = ObjectAnimator
                        .ofFloat(rootView, "translationY", rootView.getHeight(), 0)
                        .setDuration(500);
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.start();
            }
        });
    }


    class RootView extends LinearLayout {
        private View header;
        private ScrollView content;

        private boolean isDragging = false;
        private boolean isScrolling = false;

        private int touchY;
        private int touchScrollStart;
        private int partialOffset;

        private int touchSlop;
        private Scroller scroller;
        private VelocityTracker tracker;
        private int maxHeight;

        public RootView(Context context, View header, ScrollView content, int count) {
            super(context);
            this.header = header;
            this.content = content;
            if (count > 3) {
                this.partialOffset = 212 * 2 + 4 + 8 + 56 * 2;
            } else {
                this.partialOffset = 212 + 4 + 8 + 56 * 2;
            }
            this.maxHeight = 4 + 8 + 56 * 2 + 212 * ((int) Math.ceil(count / 3.0));
            this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
            this.scroller = new Scroller(context);
            this.tracker = VelocityTracker.obtain();
            this.header.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    int paddingH = getHeight() - partialOffset;
                    if (getScrollY() > -paddingH / 2) {
                        int destScroll = -(getHeight() - partialOffset);
                        scroller.startScroll(0, getScrollY(), 0, destScroll - getScrollY(), ANIMATION_DURATION);
                        isScrolling = true;
                        postInvalidate();
                    } else {
                        scroller.startScroll(0, getScrollY(), 0, getOpenScrollPosition() - getScrollY(), ANIMATION_DURATION);
                        isScrolling = true;
                        postInvalidate();
                    }
                }
            });

            setOrientation(VERTICAL);
            addView(header);
            View div = new View(context);
            div.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (getResources().getDisplayMetrics().density * 2)));
            div.setBackgroundColor(0xffe3e3e3);
            addView(div);
            addView(content);
            scrollTo(0, -(getHeight() - partialOffset));

            post(new Runnable() {
                @Override
                public void run() {
                    scrollTo(0, -(getHeight() - partialOffset));
                }
            });
        }

        private int getOpenScrollPosition() {
            if (maxHeight > getHeight()) {
                return 0;
            }
            return maxHeight - getHeight();
        }

//        private int getCloseScrollPosition() {
//            return -(getHeight() - partialOffset);
//        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (isCanceling) {
                return true;
            }
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                if (ev.getY() < -getScrollY()) {
                    cancel();
                    return true;
                }
            }

            if (ev.getAction() == MotionEvent.ACTION_MOVE && isDragging) {
                return true;
            }

            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                touchY = (int) ev.getY();
                touchScrollStart = getScrollY();
            } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
                if (getScrollY() == getOpenScrollPosition()) {
                    if (content.getScrollY() < 10) {
                        if (ev.getY() - touchY > touchSlop) {
                            isDragging = true;
                            touchY = (int) ev.getY();
                            touchScrollStart = getScrollY();
                        }
                    }
                } else {
                    if (Math.abs(ev.getY() - touchY) > touchSlop) {
                        isDragging = true;
                        touchY = (int) ev.getY();
                        touchScrollStart = getScrollY();
                    }
                }
            }

            return isDragging;
        }

        @Override
        public void computeScroll() {
            if (isCanceling) {
                return;
            }
            if (scroller.computeScrollOffset()) {
                scrollTo(0, scroller.getCurrY());
                postInvalidate();
            }
        }

        private void flingScroll(int velocity, int offset) {
            // Logger.d("PickIntentDialog", "flingScroll " + velocity + " to " + offset);
            scroller.startScroll(0, getScrollY(), 0, offset - getScrollY(), ANIMATION_DURATION);
            invalidate();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (isCanceling) {
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                touchY = (int) event.getY();
//                touchScrollStart = getScrollY();
                if (event.getY() < -getScrollY()) {
                    cancel();
                    return true;
                }
                this.tracker.clear();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                int scrollY = (int) (touchY - event.getY() + touchScrollStart);
                if (scrollY > getOpenScrollPosition()) {
                    scrollTo(0, getOpenScrollPosition());
                    invalidate();
                } else if (scrollY < -(getHeight() - partialOffset)) {
                    scrollTo(0, -(getHeight() - partialOffset));
                    invalidate();
                } else {
                    scrollTo(0, scrollY);
                    invalidate();
                }
                this.tracker.addMovement(event);
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                int paddingH = getHeight() - partialOffset;
                tracker.computeCurrentVelocity(1000);
                if (tracker.getYVelocity() < -1000) {
                    flingScroll((int) tracker.getYVelocity(), getOpenScrollPosition());
                } else if (tracker.getYVelocity() > 1000) {
                    flingScroll((int) tracker.getYVelocity(), -(getHeight() - partialOffset));
                } else {

                    if (getScrollY() == getOpenScrollPosition()) {
                        invalidate();
                    } else {
                        if (getScrollY() > -paddingH / 2) {
                            scroller.startScroll(0, getScrollY(), 0, getOpenScrollPosition() - getScrollY(), ANIMATION_DURATION);
                            invalidate();
                        } else {
                            int destScroll = -(getHeight() - partialOffset);
                            scroller.startScroll(0, getScrollY(), 0, destScroll - getScrollY(), ANIMATION_DURATION);
                            invalidate();
                        }
                    }
                }
                isDragging = false;
                return true;
            }
            return super.onTouchEvent(event);
        }
    }

}
