package org.telegram.android.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.v4.text.BidiFormatter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.viewpagerindicator.UnderlinePageIndicator;
import org.telegram.android.R;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.login.ActivationController;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 17.09.13
 * Time: 16:29
 */
public class TourFragment extends TelegramFragment {
    private View indicatorContainer;
    private View indicator0;
    private View indicator1;
    private View indicator2;
    private View indicator3;
    private View indicator4;
    private View indicator5;
    private View[] indicators;
    private View openAppButton;
    private ViewPager pager;
    // private View dynamicBg;
    // private View bottomView;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateOrientation();
    }

    private void updateOrientation() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

            int topHeight = getResources().getDisplayMetrics().heightPixels - getPx(150) - getPx(150);

            if (topHeight > getPx(180)) {
                topHeight = (getResources().getDisplayMetrics().heightPixels - getPx(150)) / 2;
            }

            // bottomView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getPx(220), Gravity.BOTTOM));
            pager.setPadding(0, topHeight, 0, getPx(150));

            indicatorContainer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, topHeight, Gravity.TOP));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getPx(64));
            params.topMargin = -getPx(4);
            // params.bottomMargin = params.topMargin = getPx(4);
            // params.leftMargin = params.rightMargin = getPx(8);
            openAppButton.setLayoutParams(params);
        } else {
            int topHeight = getResources().getDisplayMetrics().heightPixels - getPx(60) - getPx(150);

            pager.setPadding(0, topHeight, 0, getPx(80));
            indicatorContainer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, topHeight, Gravity.TOP));

//            if (getResources().getDisplayMetrics().heightPixels <= getPx(320)) {
//                bottomView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getPx(100), Gravity.BOTTOM));
//            } else {
//                bottomView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getPx(150), Gravity.BOTTOM));
//            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getPx(48));
            params.topMargin = -getPx(4);
            //params.bottomMargin = params.topMargin = getPx(4);
            //params.leftMargin = params.rightMargin = getPx(8);
            openAppButton.setLayoutParams(params);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View res = wrap(inflater).inflate(R.layout.tour_container, container, false);
        final UnderlinePageIndicator pageIndicator = (UnderlinePageIndicator) res.findViewById(R.id.pageIndicator);
        pageIndicator.setFades(false);
        pageIndicator.setSelectedColor(0xffbfbfbf);

        openAppButton = res.findViewById(R.id.openAppButton);
        openAppButton.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (application.getKernel().getActivationController() == null) {
                    application.getKernel().setActivationController(new ActivationController(application));
                }
                if (application.getKernel().getActivationController().getCurrentState() == ActivationController.STATE_PHONE_CONFIRM) {
                    try {
                        final Phonenumber.PhoneNumber numberUtil = PhoneNumberUtil.getInstance().parse(
                                "+" + application.getKernel().getActivationController().getAutoPhone(), "us");

                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setMessage(
                                getStringSafe(R.string.st_auth_confirm_phone)
                                        .replace("\\n", "\n")
                                        .replace("{0}", BidiFormatter.getInstance().unicodeWrap(PhoneNumberUtil.getInstance().format(numberUtil, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))));
                        builder.setPositiveButton(R.string.st_yes, secure(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (application.getKernel().getActivationController() != null) {
                                    if (application.getKernel().getActivationController().getCurrentState() == ActivationController.STATE_PHONE_CONFIRM) {
                                        application.getKernel().getActivationController().doConfirmPhone();
                                    }
                                }
                                getRootController().openApp();
                            }
                        })).setNegativeButton(R.string.st_edit, secure(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (application.getKernel().getActivationController() != null) {
                                    if (application.getKernel().getActivationController().getCurrentState() == ActivationController.STATE_PHONE_CONFIRM) {
                                        application.getKernel().getActivationController().doEditPhone();
                                    }
                                }
                                getRootController().openApp();
                            }
                        }));
                        AlertDialog dialog = builder.create();
                        dialog.setCanceledOnTouchOutside(true);
                        dialog.show();
                    } catch (NumberParseException e) {
                        application.getKernel().getActivationController().doEditPhone();
                        getRootController().openApp();
                    }
                } else {
                    getRootController().openApp();
                }
            }
        }));
        // dynamicBg = res.findViewById(R.id.dynamicBg);
        // bottomView = dynamicBg;
        indicator0 = res.findViewById(R.id.intro0);
        indicator1 = res.findViewById(R.id.intro1);
        indicator2 = res.findViewById(R.id.intro2);
        indicator3 = res.findViewById(R.id.intro3);
        indicator4 = res.findViewById(R.id.intro4);
        indicator5 = res.findViewById(R.id.intro5);

        indicatorContainer = res.findViewById(R.id.iconContainer);

        indicator0.setVisibility(View.VISIBLE);
        indicator1.setVisibility(View.GONE);
        indicator2.setVisibility(View.GONE);
        indicator3.setVisibility(View.GONE);
        indicator4.setVisibility(View.GONE);
        indicator5.setVisibility(View.GONE);

        if (application.isRTL()) {
            indicators = new View[]{indicator5, indicator4, indicator3, indicator2, indicator1, indicator0};
        } else {
            indicators = new View[]{indicator0, indicator1, indicator2, indicator3, indicator4, indicator5};
        }

        pager = (ViewPager) res.findViewById(R.id.tourText);
        final String[] messages;
        if (application.isRTL()) {
            messages = new String[]
                    {
                            getStringSafe(R.string.st_tour_page6),
                            getStringSafe(R.string.st_tour_page5),
                            getStringSafe(R.string.st_tour_page4),
                            getStringSafe(R.string.st_tour_page3),
                            getStringSafe(R.string.st_tour_page2),
                            getStringSafe(R.string.st_tour_page1),
                    };
        } else {
            messages = new String[]
                    {
                            getStringSafe(R.string.st_tour_page1),
                            getStringSafe(R.string.st_tour_page2),
                            getStringSafe(R.string.st_tour_page3),
                            getStringSafe(R.string.st_tour_page4),
                            getStringSafe(R.string.st_tour_page5),
                            getStringSafe(R.string.st_tour_page6)
                    };
        }

        final String[] titles;
        if (application.isRTL()) {
            titles = new String[]
                    {
                            getStringSafe(R.string.st_tour_page6_title),
                            getStringSafe(R.string.st_tour_page5_title),
                            getStringSafe(R.string.st_tour_page4_title),
                            getStringSafe(R.string.st_tour_page3_title),
                            getStringSafe(R.string.st_tour_page2_title),
                            getStringSafe(R.string.st_tour_page1_title),
                    };
        } else {
            titles = new String[]
                    {
                            getStringSafe(R.string.st_tour_page1_title),
                            getStringSafe(R.string.st_tour_page2_title),
                            getStringSafe(R.string.st_tour_page3_title),
                            getStringSafe(R.string.st_tour_page4_title),
                            getStringSafe(R.string.st_tour_page5_title),
                            getStringSafe(R.string.st_tour_page6_title)
                    };
        }
        final int[] colors;
        if (application.isRTL()) {
            colors = new int[]
                    {
                            0x2f92e8, 0x5dc326, 0xfac800, 0xf99117, 0xf75b2f, 0xbfbfbf
                    };
        } else {
            colors = new int[]
                    {
                            0xbfbfbf, 0xf75b2f, 0xf99117, 0xfac800, 0x5dc326, 0x2f92e8
                    };
        }
        pager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return 6;
            }

            @Override
            public boolean isViewFromObject(View view, Object o) {
                return o == view;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                View res = getLayoutInflater(null).inflate(R.layout.tour_item, container, false);
                ((TextView) res.findViewById(R.id.title)).setText(titles[position]);
                ((TextView) res.findViewById(R.id.message)).setText(html(messages[position]));
                container.addView(res);
                return res;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }
        });
        pageIndicator.setViewPager(pager);
        pageIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private int interpolateColors(int a, int b, float v) {
                int aRed = (a & 0xFF0000) >> 16;
                int aGreen = (a & 0xFF00) >> 8;
                int aBlue = a & 0xFF;

                int bRed = (b & 0xFF0000) >> 16;
                int bGreen = (b & 0xFF00) >> 8;
                int bBlue = b & 0xFF;

                int cRed = (int) (aRed * v + bRed * (1 - v));
                int cGreen = (int) (aGreen * v + bGreen * (1 - v));
                int cBlue = (int) (aBlue * v + bBlue * (1 - v));

                return 0xFF000000 | (cRed << 16) | (cGreen << 8) | cBlue;
            }

            private Drawable interpolateDrawables(Drawable a, Drawable b, float v) {
                a.setAlpha((int) (255 * v));
                b.setAlpha((int) (255 * (1 - v)));
                return new LayerDrawable(new Drawable[]{a, b});
            }

            @Override
            public void onPageScrolled(int i, float v, int i2) {
                if (i2 > 0) {
                    int color = interpolateColors(colors[i], colors[i + 1], 1 - v);
                    pageIndicator.setSelectedColor(color);
                    //dynamicBg.setBackgroundDrawable(interpolateDrawables(backgrounds[i], backgrounds[i + 1], 1 - v));
                } else {
//                    if (i > 0) {
//                        dynamicBg.setBackgroundDrawable(new ColorDrawable(colors[viewPager.getCurrentItem()] | 0xFF000000));
//                    } else {
//                        dynamicBg.setBackgroundDrawable(new ColorDrawable(0xffbfbfbf));
//                    }
                    //dynamicBg.setBackgroundDrawable(backgrounds[pager.getCurrentItem()]);
                }
            }

            @Override
            public void onPageSelected(int index) {
//                if (index == 0) {
//                    hideView(dynamicBg);
//                } else {
//                    showView(dynamicBg);
//                }
                for (int i = 0; i < indicators.length; i++) {
                    if (i == index) {
                        showView(indicators[i]);
                    } else {
                        goneView(indicators[i]);
                    }
                }

                sendEvent("page_view", index + "");
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        if (application.isRTL()) {
            pager.setCurrentItem(5);
        } else {
            pager.setCurrentItem(0);
        }

        // dynamicBg.setBackgroundDrawable(backgrounds[pager.getCurrentItem()]);

        updateOrientation();

        return res;
    }
}
