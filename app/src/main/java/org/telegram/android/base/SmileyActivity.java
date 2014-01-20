package org.telegram.android.base;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import org.telegram.android.R;
import org.telegram.android.fragments.interfaces.SmileysController;
import org.telegram.android.ui.EmojiProcessor;
import org.telegram.android.ui.Smileys;
import org.telegram.android.ui.SmileysView;
import com.viewpagerindicator.UnderlinePageIndicator;

/**
 * Author: Korshakov Stepan
 * Created: 03.09.13 18:13
 */
public class SmileyActivity extends TelegramActivity implements SmileysController, ViewTreeObserver.OnGlobalLayoutListener {

    private View smileysView;

    private WindowManager windowManager;

    private DisplayMetrics metrics;

    private EditText input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        metrics = getResources().getDisplayMetrics();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= 16) {
            getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
        } else {
            getWindow().getDecorView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
        }
        hideSmileysPopup();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(this);
    }


    public void doAddSmiley(String text) {
        int selectionEnd = input.getSelectionEnd();
        if (selectionEnd < 0) {
            selectionEnd = input.getText().length();
        }

        if (input.getText().toString().trim().length() == 0) {
            if (application.isRTL()) {
                text = "\u200F" + text;
            } else {
                text = "\u200E" + text;
            }
        }
        CharSequence appendString = application.getEmojiProcessor().processEmojiMutable(text,
                EmojiProcessor.CONFIGURATION_BUBBLES);
        input.getText().insert(selectionEnd, appendString);
    }

    public void doDeleteText() {
        input.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
    }

    @Override
    public boolean areSmileysVisible() {
        return smileysView != null;
    }

    private void updateSmileysTabs(View smileysView, int page) {
        if (page == 0) {
            ((ImageButton) smileysView.findViewById(R.id.smileyTab1))
                    .setImageResource(R.drawable.msg_smile_section1_active);
        } else {
            ((ImageButton) smileysView.findViewById(R.id.smileyTab1))
                    .setImageResource(R.drawable.msg_smile_section1_normal);
        }
        if (page == 1) {
            ((ImageButton) smileysView.findViewById(R.id.smileyTab2))
                    .setImageResource(R.drawable.msg_smile_section2_active);
        } else {
            ((ImageButton) smileysView.findViewById(R.id.smileyTab2))
                    .setImageResource(R.drawable.msg_smile_section2_normal);
        }
        if (page == 2) {
            ((ImageButton) smileysView.findViewById(R.id.smileyTab3))
                    .setImageResource(R.drawable.msg_smile_section3_active);
        } else {
            ((ImageButton) smileysView.findViewById(R.id.smileyTab3))
                    .setImageResource(R.drawable.msg_smile_section3_normal);
        }
        if (page == 3) {
            ((ImageButton) smileysView.findViewById(R.id.smileyTab4))
                    .setImageResource(R.drawable.msg_smile_section5_active);
        } else {
            ((ImageButton) smileysView.findViewById(R.id.smileyTab4))
                    .setImageResource(R.drawable.msg_smile_section5_normal);
        }
        if (page == 4) {
            ((ImageButton) smileysView.findViewById(R.id.smileyTab5))
                    .setImageResource(R.drawable.msg_smile_section4_active);
        } else {
            ((ImageButton) smileysView.findViewById(R.id.smileyTab5))
                    .setImageResource(R.drawable.msg_smile_section4_normal);
        }
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public int getActionBarHeight() {
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(com.actionbarsherlock.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }

        return 0;
    }

    private boolean isSmilesVisible = false;

    @Override
    public void showSmileys(EditText dest) {
        input = dest;
        View root = findViewById(R.id.fragmentContainer);
        int heightDiff = root.getRootView().getHeight() - root.getHeight() - getStatusBarHeight() - getActionBarHeight();
        int heightDp = (int) (heightDiff / getResources().getDisplayMetrics().density);
        if (heightDp > 100) {
            if (smileysView == null) {
                showSmileysPopup(heightDp);
            } else {
                hideSmileysPopup();
            }
        } else {
            isSmilesVisible = true;
            dest.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(dest, InputMethodManager.SHOW_IMPLICIT);
        }
    }


    private void toggleFullscreen(boolean fullscreen) {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        if (fullscreen) {
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        } else {
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        getWindow().setAttributes(attrs);
    }


    @Override
    public void hideSmileys() {
        hideSmileysPopup();
    }

    @Override
    public void onGlobalLayout() {
        View root = findViewById(R.id.fragmentContainer);
        int heightDiff = root.getRootView().getHeight() - root.getHeight() - getStatusBarHeight() - getActionBarHeight();
        int heightDp = (int) (heightDiff / getResources().getDisplayMetrics().density);
        if (heightDp > 100) {
            if (isSmilesVisible) {
                isSmilesVisible = false;
                showSmileysPopup(heightDp);
            }
            // toggleFullscreen(true);
            // getSupportActionBar().hide();
        } else {
            hideSmileysPopup();
            // toggleFullscreen(false);
            // getSupportActionBar().show();
        }
    }

    protected int getPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
    }

    protected int getDp(float px) {
        return (int) (px / getResources().getDisplayMetrics().density);
    }

    private void showSmileysPopup(final int keyboardHeight) {
        if (smileysView != null) {
            return;
        }

        int maxWidth = getDp(getWindow().getDecorView().getWidth());

        final int countInRow = Math.min((int) (Math.floor(maxWidth / 48.0f)), 12);

        smileysView = View.inflate(this, R.layout.overlay_smileys_new, null);

        smileysView.findViewById(R.id.backspaceButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doDeleteText();
            }
        });

        final ViewPager pager = (ViewPager) smileysView.findViewById(R.id.smileysPages);
        pager.setAdapter(new PagerAdapter() {

            private long[] getSmileys(int pos) {
                switch (pos) {
                    default:
                    case 0:
                        return application.getUiKernel().getLastEmoji().getLastSmileys();
                    case 1:
                        return Smileys.STANDART;
                    case 2:
                        return Smileys.NATURE;
                    case 3:
                        return Smileys.TRANSPORT;
                    case 4:
                        return Smileys.UNSORTED;
                }
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                long[] smileys = getSmileys(position);
                if (smileys.length > 0) {
                    View res = getLayoutInflater().inflate(R.layout.overlay_smileys_container, container, false);


                    SmileysView mainSmileys = new SmileysView(SmileyActivity.this, application.getEmojiProcessor(),
                            smileys,
                            countInRow, getPx(48), getPx(10));
                    mainSmileys.setOnSmileClickedListener(new SmileysView.OnSmileClickedListener() {
                        @Override
                        public void onSmileClicked(long smileId) {
                            String smile = null;
                            char a = (char) (smileId & 0xFFFFFFFF);
                            char b = (char) ((smileId >> 16) & 0xFFFFFFFF);
                            char c = (char) ((smileId >> 32) & 0xFFFFFFFF);
                            char d = (char) ((smileId >> 48) & 0xFFFFFFFF);
                            if (c != 0 && d != 0) {
                                smile = "" + d + c + b + a;
                            } else if (b != 0) {
                                smile = b + "" + a;
                            } else {
                                smile = "" + a;
                            }

                            doAddSmiley(smile);
                        }
                    });

                    ScrollView.LayoutParams layoutParams = new ScrollView.LayoutParams(getPx(48) * countInRow,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                    mainSmileys.setLayoutParams(layoutParams);
                    ((ScrollView) res.findViewById(R.id.scroller)).addView(mainSmileys);

                    container.addView(res);
                    return res;
                } else {
                    View res = getLayoutInflater().inflate(R.layout.msg_smileys_empty, container, false);
                    container.addView(res);
                    return res;
                }
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }

            @Override
            public int getCount() {
                return 5;
            }

            @Override
            public boolean isViewFromObject(View view, Object o) {
                return view.equals(o);
            }
        });

        ((UnderlinePageIndicator) smileysView.findViewById(R.id.pageIndicator)).setViewPager(pager);
        ((UnderlinePageIndicator) smileysView.findViewById(R.id.pageIndicator)).setFades(false);
        ((UnderlinePageIndicator) smileysView.findViewById(R.id.pageIndicator)).setSelectedColor(0xffffffff);

        ((UnderlinePageIndicator) smileysView.findViewById(R.id.pageIndicator)).setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(int i) {
                updateSmileysTabs(smileysView, i);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

        if (application.getUiKernel().getLastEmoji().getLastSmileys().length == 0) {
            pager.setCurrentItem(1);
        }

        smileysView.findViewById(R.id.smileyTab1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pager.setCurrentItem(0);
            }
        });

        updateSmileysTabs(smileysView, pager.getCurrentItem());

        smileysView.findViewById(R.id.smileyTab2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pager.setCurrentItem(1);
            }
        });
        smileysView.findViewById(R.id.smileyTab3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pager.setCurrentItem(2);
            }
        });
        smileysView.findViewById(R.id.smileyTab4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pager.setCurrentItem(3);
            }
        });
        smileysView.findViewById(R.id.smileyTab5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pager.setCurrentItem(4);
            }
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                getPx(keyboardHeight),
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        windowManager.addView(smileysView, params);
    }

    private void hideSmileysPopup() {
        if (smileysView != null) {
            windowManager.removeView(smileysView);
            smileysView = null;
        }
    }
}
