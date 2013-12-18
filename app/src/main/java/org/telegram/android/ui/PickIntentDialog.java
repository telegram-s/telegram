package org.telegram.android.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.*;
import com.nineoldandroids.animation.ObjectAnimator;
import org.telegram.android.R;
import org.telegram.android.log.Logger;

/**
 * Created by ex3ndr on 19.12.13.
 */
public class PickIntentDialog extends Dialog {
    public PickIntentDialog(Context context) {
        super(context, R.style.PickDialog_Theme);
        init();
    }

    private void init() {

        WindowManager manager = (WindowManager) getContext().getSystemService(Activity.WINDOW_SERVICE);
        int width, height;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            width = manager.getDefaultDisplay().getWidth();
            height = manager.getDefaultDisplay().getHeight();
        } else {
            Point point = new Point();
            manager.getDefaultDisplay().getSize(point);
            width = point.x;
            height = point.y;
        }

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.width = width;
        lp.height = height;
        getWindow().setAttributes(lp);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new ScrollView.LayoutParams(
                width,
                ViewGroup.LayoutParams.WRAP_CONTENT));

//        View view = new View(getContext());
//        LinearLayout.LayoutParams paddingParams = new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                400);
//        view.setLayoutParams(paddingParams);
//        view.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                cancel();
//                return false;
//            }
//        });
//
//        view.setClickable(true);
//        layout.addView(view);

        for (int i = 0; i < 10; i++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setBackgroundColor(Color.WHITE);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            for (int j = 0; j < 3; j++) {
                ImageView imageView = new ImageView(getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200, 1);
                imageView.setLayoutParams(params);
                imageView.setImageResource(R.drawable.app_icon);
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(getContext(), "Item clicked", Toast.LENGTH_SHORT).show();
                    }
                });
                row.addView(imageView);
            }
            layout.addView(row);
        }

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                width,
                ViewGroup.LayoutParams.FILL_PARENT));
        scrollView.addView(layout);
//        ObjectAnimator animator = ObjectAnimator.ofFloat(rootView, "alpha", 0, 1)
//                .setDuration(300);
//        animator.setInterpolator(new AccelerateDecelerateInterpolator());
//        animator.start();

        TextView headerView = new TextView(getContext());
        headerView.setBackgroundColor(Color.WHITE);
        headerView.setText("Sample header");
        headerView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 100));

        RootView rootView = new RootView(getContext(), headerView, scrollView);
        rootView.setLayoutParams(new ViewGroup.LayoutParams(
                width,
                ViewGroup.LayoutParams.FILL_PARENT));

        setContentView(rootView);
    }


    class RootView extends LinearLayout {
        private View header;
        private ScrollView content;

        private boolean isDragging = false;

        private int touchY;
        private int touchScrollStart;
        private int partialOffset;

        private int touchSlop;

        public RootView(Context context, View header, ScrollView content) {
            super(context);
            this.header = header;
            this.content = content;
            this.partialOffset = 300;
            this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

            setOrientation(VERTICAL);
            addView(header);
            addView(content);
            scrollTo(0, -(getHeight() - partialOffset));

            post(new Runnable() {
                @Override
                public void run() {
                    scrollTo(0, -(getHeight() - partialOffset));
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (ev.getAction() == MotionEvent.ACTION_MOVE && isDragging) {
                return true;
            }

            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                touchY = (int) ev.getY();
                touchScrollStart = getScrollY();
            } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
                if (getScrollY() == 0) {
                    if (content.getScrollY() < 10) {
                        if (ev.getY() - touchY > touchSlop) {
                            isDragging = true;
                        }
                    }
                } else {
                    if (Math.abs(ev.getY() - touchY) > touchSlop) {
                        isDragging = true;
                    }
                }
            } else if (ev.getAction() == MotionEvent.ACTION_UP) {
                isDragging = false;
            }

            return isDragging;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touchY = (int) event.getY();
                touchScrollStart = getScrollY();
                if (touchY < -getScrollY()) {
                    cancel();
                    return true;
                }
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                int scrollY = (int) (touchY - event.getY() + touchScrollStart);
                if (scrollY > 0) {
                    scrollTo(0, 0);
                } else if (scrollY < -(getHeight() - partialOffset)) {
                    scrollTo(0, -(getHeight() - partialOffset));
                } else {
                    scrollTo(0, scrollY);
                }
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                int paddingH = getHeight() - partialOffset;
                if (getScrollY() > -paddingH / 2) {
                    scrollTo(0, 0);
                } else {
                    scrollTo(0, -(getHeight() - partialOffset));
                }
                isDragging = false;
                return true;
            }
            return super.onTouchEvent(event);
        }
    }
}
