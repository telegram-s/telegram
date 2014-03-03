package org.telegram.android.ui;

import org.telegram.android.R;

/**
 * Author: Korshakov Stepan
 * Created: 30.07.13 2:04
 */
public class Placeholders {

    public static final int GREY = 0xffb1bbc2;

    public static final int BLUE_BG = 0xff8abae7;
    public static final int BLUE = 0xff0f94ed;
    public static final int CYAN_BG = 0xff99d4e6;
    public static final int CYAN = 0xff00a1c4;
    public static final int GREEN_BG = 0xffabd683;
    public static final int GREEN = 0xff41a903;
    public static final int ORANGE_BG = 0xfff5ad6e;
    public static final int ORANGE = 0xffe09602;
    public static final int PINK_BG = 0xfff79db7;
    public static final int PINK = 0xfffc4380;
    public static final int PURPLE_BG = 0xff9096e7;
    public static final int PURPLE = 0xff8f3bf7;
    public static final int RED_BG = 0xffda866f;
    public static final int RED = 0xffee4928;
    public static final int YELLOW_BG = 0xffedcd82;
    public static final int YELLOW = 0xffeb7002;

    private static final int COUNT = 8;
    private static final int[] BG_COLORS = new int[]{BLUE_BG, CYAN_BG, GREEN_BG, ORANGE_BG, PINK_BG, PURPLE_BG, RED_BG, YELLOW_BG};
    private static final int[] COLORS = new int[]{BLUE, CYAN, GREEN, ORANGE, PINK, PURPLE, RED, YELLOW};

    public static int getTitleColor(int peerId) {
        return COLORS[Math.abs(peerId) % COUNT];
    }

    public static int getBgColor(int peerId) {
        return BG_COLORS[Math.abs(peerId) % COUNT];
    }
}
