package org.telegram.i18n;

import android.content.Context;
import org.telegram.android.R;
import org.telegram.android.ui.plurals.PluralResources;

import java.text.NumberFormat;
import java.util.Formatter;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 19.10.13
 * Time: 18:30
 */
public class I18nUtil {
    private static I18nUtil instance;

    public static void init(Context context) {
        if (instance == null) {
            instance = new I18nUtil(context);
        }
    }

    public static I18nUtil getInstance() {
        return instance;
    }

    public static int MONTH_DEFAULT = 0;
    public static int MONTH_SHORT = 1;
    public static int MONTH_LOWERCASE = 2;

    private PluralResources plurals;
    private Context context;
    private String locale;

    private static final int[] MONTHS = new int[]
            {
                    R.string.lang_january,
                    R.string.lang_february,
                    R.string.lang_march,
                    R.string.lang_april,
                    R.string.lang_may,
                    R.string.lang_june,
                    R.string.lang_july,
                    R.string.lang_august,
                    R.string.lang_september,
                    R.string.lang_october,
                    R.string.lang_november,
                    R.string.lang_december
            };

    private static final int[] MONTHS_LOWER = new int[]
            {
                    R.string.lang_january_s,
                    R.string.lang_february_s,
                    R.string.lang_march_s,
                    R.string.lang_april_s,
                    R.string.lang_may_s,
                    R.string.lang_june_s,
                    R.string.lang_july_s,
                    R.string.lang_august_s,
                    R.string.lang_september_s,
                    R.string.lang_october_s,
                    R.string.lang_november_s,
                    R.string.lang_december_s
            };

    private static final int[] MONTHS_SHORT = new int[]
            {
                    R.string.lang_january_short,
                    R.string.lang_february_short,
                    R.string.lang_march_short,
                    R.string.lang_april_short,
                    R.string.lang_may_short,
                    R.string.lang_june_short,
                    R.string.lang_july_short,
                    R.string.lang_august_short,
                    R.string.lang_september_short,
                    R.string.lang_october_short,
                    R.string.lang_november_short,
                    R.string.lang_december_short
            };

    private static final int[] MONTHS_SHORT_LOWER = new int[]
            {
                    R.string.lang_january_short_s,
                    R.string.lang_february_short_s,
                    R.string.lang_march_short_s,
                    R.string.lang_april_short_s,
                    R.string.lang_may_short_s,
                    R.string.lang_june_short_s,
                    R.string.lang_july_short_s,
                    R.string.lang_august_short_s,
                    R.string.lang_september_short_s,
                    R.string.lang_october_short_s,
                    R.string.lang_november_short_s,
                    R.string.lang_december_short_s
            };

    public I18nUtil(Context context) {
        this.context = context;
        this.locale = this.context.getString(R.string.st_lang);
        try {
            this.plurals = new PluralResources(context.getString(R.string.st_lang), context.getResources());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        UserListFormatter.init(context);
    }

    public String getMonthName(int index) {
        return getMonthName(index, MONTH_DEFAULT);
    }

    public String getMonthLowercase(int index) {
        return getMonthName(index, MONTH_DEFAULT | MONTH_LOWERCASE);
    }

    public String getMonthShort(int index) {
        return getMonthName(index, MONTH_SHORT);
    }

    public String getMonthShortLowercase(int index) {
        return getMonthName(index, MONTH_SHORT | MONTH_LOWERCASE);
    }

    public String getMonthName(int index, int type) {
        if ((type & 0x1) == MONTH_DEFAULT) {
            if ((type & MONTH_LOWERCASE) != 0) {
                return context.getResources().getString(MONTHS_LOWER[index]);
            } else {
                return context.getResources().getString(MONTHS[index]);
            }
        } else {
            if ((type & MONTH_LOWERCASE) != 0) {
                return context.getResources().getString(MONTHS_SHORT_LOWER[index]);
            } else {
                return context.getResources().getString(MONTHS_SHORT[index]);
            }
        }
    }

    public String getPlural(int id, int value) {
        if (plurals == null) {
            return context.getResources().getQuantityString(id, value);
        } else {
            return plurals.getQuantityString(id, value);
        }
    }

    public String getPluralFormatted(int id, int value) {
        if (plurals == null) {
            return context.getResources().getQuantityString(id, value).replace("{d}", correctFormatNumber(value));
        } else {
            return plurals.getQuantityString(id, value).replace("{d}", correctFormatNumber(value));
        }
    }

    private String convertToCorrectNumberString(String src) {
        char zeroNumber = '0';
        if (Locale.getDefault().getLanguage().equals("ar")) {
            zeroNumber = '\u0660';
        }
        if (zeroNumber != '0') {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < src.length(); i++) {
                builder.append((char) (zeroNumber + (src.charAt(i) - '0')));
            }
            return builder.toString();
        } else {
            return src;
        }
    }

    public String correctFormatTwoDigit(int number) {
        String d = "" + number;
        if (d.length() == 1) {
            return convertToCorrectNumberString("0" + d);
        }
        return convertToCorrectNumberString(d);
    }

    public String correctFormatNumber(int number) {
        return convertToCorrectNumberString("" + number);
    }

    public String formatMonthShort(int day, int monthId) {
        return context.getString(R.string.lang_month_pattern)
                .replace("{month}", I18nUtil.getInstance().getMonthShort(monthId))
                .replace("{day}", I18nUtil.getInstance().correctFormatNumber(day));
    }

    public String formatMonth(int day, int monthId) {
        return context.getString(R.string.lang_month_pattern)
                .replace("{month}", I18nUtil.getInstance().getMonthName(monthId))
                .replace("{day}", I18nUtil.getInstance().correctFormatNumber(day));
    }

}