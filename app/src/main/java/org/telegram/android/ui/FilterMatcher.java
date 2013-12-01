package org.telegram.android.ui;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

/**
 * Author: Korshakov Stepan
 * Created: 01.09.13 23:32
 */
public class FilterMatcher {
    private String query;
    private String queryLo;
    private String queryLoTranslit;

    public FilterMatcher(String query) {
        this.query = query;
        this.queryLo = query.toLowerCase();
        this.queryLoTranslit = Translit.translitText(queryLo);
    }

    public String getQuery() {
        return query;
    }

    public boolean isMatched(String src) {
        if (src.length() == 0)
            return true;

        if (query == null)
            return true;

        if (src.toLowerCase().contains(queryLo)) {
            return true;
        }

        if (src.toLowerCase().contains(queryLoTranslit)) {
            return true;
        }

        return false;
    }

    public void highlight(Context context, SpannableString string) {
        if (queryLo.length() == 0)
            return;
        String src = string.toString().toLowerCase();
        int offset = 0;
        while (offset < src.length()) {
            int index = src.indexOf(queryLo, offset);
            int len = queryLo.length();
            if (index < 0) {
                if (queryLoTranslit.length() > 0) {
                    index = src.indexOf(queryLoTranslit, offset);
                    len = queryLoTranslit.length();
                }
                if (index < 0) {
                    return;
                }
            }
            string.setSpan(new ForegroundColorSpan(0xff1274C9), index, index + len, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            offset += len;
        }
    }
}
