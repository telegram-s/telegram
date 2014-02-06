package org.telegram.android.ui;

import org.telegram.android.R;

/**
 * Author: Korshakov Stepan
 * Created: 30.07.13 2:04
 */
public class Placeholders {

    public static int getUserBgColor(int index) {
        return USER_BG_COLOR[Math.abs(index) % USER_BG_COLOR.length];
    }

    public static int getGroupBgColor(int index) {
        return GROUP_BG_COLOR[Math.abs(index) % GROUP_BG_COLOR.length];
    }

    public static int getUserTitleColor(int index) {
        return USER_PLACEHOLDERS_COLOR[Math.abs(index) % USER_PLACEHOLDERS_COLOR.length];
    }

    public static int getGroupTitleColor(int index) {
        return GROUP_PLACEHOLDERS_COLOR[Math.abs(index) % GROUP_PLACEHOLDERS_COLOR.length];
    }

    public static int getUserPlaceholder(int index) {
        return USER_PLACEHOLDERS[Math.abs(index) % USER_PLACEHOLDERS.length];
    }

    public static int getGroupPlaceholder(int index) {
        return GROUP_PLACEHOLDERS[Math.abs(index) % GROUP_PLACEHOLDERS.length];
    }

    public static int getGroupPlaceHolderIndex(int index) {
        return Math.abs(index) % GROUP_PLACEHOLDERS.length;
    }

    public static int getUserPlaceHolderIndex(int index) {
        return Math.abs(index) % USER_PLACEHOLDERS.length;
    }

    public static final int[] USER_BG_COLOR = new int[]
            {
                    0xff4495d6, 0xff4BB4D7, 0xff6CCB2F, 0xffD8914B, 0xffDB629E, 0xffAB6BCB, 0xffD8764B, 0xffF0C61D
            };

    public static final int[] GROUP_BG_COLOR = new int[]
            {
                    0xff4495d6, 0xff4BB4D7, 0xff6CCB2F, 0xffD8914B, 0xffDB629E, 0xffAB6BCB, 0xffD8764B, 0xffF0C61D
            };

    public static final int[] USER_PLACEHOLDERS_COLOR = new int[]
            {
                    0xff0f94ed, 0xff00a1c4, 0xff41a903, 0xffe09602, 0xfffc4380, 0xff8f3bf7, 0xffee4928, 0xffeb7002
            };
    public static final int[] USER_PLACEHOLDERS = new int[]{R.drawable.st_user_placeholder_blue, R.drawable.st_user_placeholder_cyan,
            R.drawable.st_user_placeholder_green, R.drawable.st_user_placeholder_orange, R.drawable.st_user_placeholder_pink,
            R.drawable.st_user_placeholder_purple, R.drawable.st_user_placeholder_red, R.drawable.st_user_placeholder_yellow};


    public static final int[] GROUP_PLACEHOLDERS_COLOR = new int[]
            {
                    0xff0f94ed, 0xff00a1c4, 0xff41a903, 0xffe09602, 0xfffc4380, 0xff8f3bf7, 0xffee4928, 0xffeb7002
            };

    public static final int[] GROUP_PLACEHOLDERS = new int[]{R.drawable.st_group_placeholder_blue, R.drawable.st_group_placeholder_cyan,
            R.drawable.st_group_placeholder_green, R.drawable.st_group_placeholder_orange, R.drawable.st_group_placeholder_pink,
            R.drawable.st_group_placeholder_purple, R.drawable.st_group_placeholder_red, R.drawable.st_group_placeholder_yellow};

}
