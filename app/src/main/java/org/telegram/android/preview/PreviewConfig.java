package org.telegram.android.preview;

import android.content.Context;

/**
 * Created by ex3ndr on 19.02.14.
 */
public class PreviewConfig {
    // DANGER! Changing this settings may break generated preview cache!

    private static final int MIN_PREVIEW_W_DP = 100;
    private static final int MIN_PREVIEW_H_DP = 40;

    private static final int MAX_PREVIEW_W_DP = 260;
    private static final int MAX_PREVIEW_H_DP = 400;

    private static final int MAX_PREVIEW_BITMAP_W_DP = MAX_PREVIEW_W_DP;
    private static final int MAX_PREVIEW_BITMAP_H_DP = MAX_PREVIEW_H_DP;

    private static final int MAP_W_DP = 160;
    private static final int MAP_H_DP = 160;

    private static final int ROUND_RADIUS_DP = 3;

    public static int MIN_PREVIEW_W = MIN_PREVIEW_W_DP;
    public static int MIN_PREVIEW_H = MIN_PREVIEW_H_DP;

    public static int MAX_PREVIEW_W = MAX_PREVIEW_W_DP;
    public static int MAX_PREVIEW_H = MAX_PREVIEW_H_DP;

    public static int MAX_PREVIEW_BITMAP_W = MAX_PREVIEW_BITMAP_W_DP;
    public static int MAX_PREVIEW_BITMAP_H = MAX_PREVIEW_BITMAP_H_DP;

    public static int MAP_W = MAP_W_DP;
    public static int MAP_H = MAP_H_DP;

    public static int ROUND_RADIUS = 2;

    public static void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        MIN_PREVIEW_W = (int) (MIN_PREVIEW_W_DP * density);
        MIN_PREVIEW_H = (int) (MIN_PREVIEW_H_DP * density);
        MAX_PREVIEW_W = (int) (MAX_PREVIEW_W_DP * density);
        MAX_PREVIEW_H = (int) (MAX_PREVIEW_H_DP * density);

        MAP_W = (int) (MAP_W_DP * density);
        MAP_H = (int) (MAP_H_DP * density);

        MAX_PREVIEW_BITMAP_W = (int) (MAX_PREVIEW_BITMAP_W_DP * density);
        MAX_PREVIEW_BITMAP_H = (int) (MAX_PREVIEW_BITMAP_H_DP * density);

        ROUND_RADIUS = (int) (ROUND_RADIUS_DP * density);
    }

    public static int[] getSizes(int w, int h) {
        float ratio = Math.min(MAX_PREVIEW_W / (float) w, MAX_PREVIEW_H / (float) h);

        int destW = (int) (w * ratio);
        int destH = (int) (h * ratio);

        int paddingH = 0;
        int paddingV = 0;

        if (destW < MIN_PREVIEW_W) {
            paddingH = (MIN_PREVIEW_W - destW) / 2;
        }

        if (destH < MIN_PREVIEW_H) {
            paddingV = (MIN_PREVIEW_H - destH) / 2;
        }

        return new int[]{destW, destH, paddingH, paddingV};
    }
}
