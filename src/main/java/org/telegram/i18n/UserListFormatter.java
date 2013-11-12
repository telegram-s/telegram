package org.telegram.i18n;

import android.content.Context;
import org.telegram.android.R;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 19.10.13
 * Time: 19:24
 */
public class UserListFormatter {

    public static Context context;

    static void init(Context context) {
        UserListFormatter.context = context;
    }

    public static String formatNamesAndMore(String lang, String[] values, int moreItems) {
        if (values.length == 1 && moreItems == 0) {
            return values[0];
        }
        if (lang.equals("ar")) {
            // Arabic language. Examples:
            // 1,2
            // 1,2,3
            // 1,2,5 more
            String res = values[0];
            for (int i = 1; i < values.length; i++) {
                res = context.getString(R.string.lang_common_and_short)
                        .replace("{content1}", res)
                        .replace("{content2}", values[i]);
            }

            if (moreItems != 0) {
                res = context.getString(R.string.lang_common_and_short)
                        .replace("{content1}", res)
                        .replace("{content2}",
                                I18nUtil.getInstance().getPluralFormatted(R.plurals.lang_more, moreItems));
            }
            return res;
        } else {
            // Common language. Examples:
            // 1 and 2
            // 1, 2 and 3
            // 1, 2 and 5 more

            String res = values[0];
            for (int i = 1; i < values.length; i++) {
                if (i == values.length - 1 && moreItems == 0) {
                    res = context.getString(R.string.lang_common_and)
                            .replace("{content1}", res)
                            .replace("{content2}", values[i]);
                } else {
                    res = context.getString(R.string.lang_common_and_short)
                            .replace("{content1}", res)
                            .replace("{content2}", values[i]);
                }
            }

            if (moreItems != 0) {
                res = context.getString(R.string.lang_common_and)
                        .replace("{content1}", res)
                        .replace("{content2}",
                                I18nUtil.getInstance().getPluralFormatted(R.plurals.lang_more, moreItems));
            }

            return res;
        }
    }
}
