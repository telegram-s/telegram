package org.telegram.android.preview;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by ex3ndr on 19.02.14.
 */
public class PreviewConfig {
    // DANGER! Changing this settings may break generated preview cache!

    private static final int WALLPAPER_S_W_DP = 100;
    private static final int WALLPAPER_S_H_DP = 100;

    private static final int MIN_PREVIEW_W_DP = 100;
    private static final int MIN_PREVIEW_H_DP = 40;

    private static final int MAX_PREVIEW_W_DP = 240;
    private static final int MAX_PREVIEW_H_DP = 300;

    private static final int MAX_MEDIA_PREVIEW_DP = 120;
    private static final int MIN_MEDIA_PREVIEW_MARGIN_DP = 2;

    private static final int MAX_PREVIEW_BITMAP_W_DP = MAX_PREVIEW_W_DP;
    private static final int MAX_PREVIEW_BITMAP_H_DP = MAX_PREVIEW_H_DP;

    private static int WALL_S_MAX_W_DP = 100;
    private static int WALL_S_MAX_H_DP = 100;

    private static final int MAP_W_DP = 160;
    private static final int MAP_H_DP = 160;

    private static final int ROUND_RADIUS_DP = 3;

    public static int WALLPAPER_S_W = WALLPAPER_S_W_DP;
    public static int WALLPAPER_S_H = WALLPAPER_S_H_DP;

    public static int MIN_PREVIEW_W = MIN_PREVIEW_W_DP;
    public static int MIN_PREVIEW_H = MIN_PREVIEW_H_DP;

    public static int MAX_PREVIEW_W = MAX_PREVIEW_W_DP;
    public static int MAX_PREVIEW_H = MAX_PREVIEW_H_DP;

    public static int MAX_PREVIEW_BITMAP_W = MAX_PREVIEW_BITMAP_W_DP;
    public static int MAX_PREVIEW_BITMAP_H = MAX_PREVIEW_BITMAP_H_DP;

    public static int MAP_W = MAP_W_DP;
    public static int MAP_H = MAP_H_DP;

    public static final int FAST_MAX_W = 90;
    public static final int FAST_MAX_H = 90;

    // Maximum expected download image size
    public static int WALL_D_MAX_W = 320;
    public static int WALL_D_MAX_H = 320;

    public static int WALL_S_MAX_W = WALL_S_MAX_W_DP;
    public static int WALL_S_MAX_H = WALL_S_MAX_H_DP;

    public static int WALL_MAX_W = 600;
    public static int WALL_MAX_H = 200;

    public static int ROUND_RADIUS = 2;

    public static final int MEDIA_ROW_COUNT = 3;
    public static int MEDIA_PREVIEW = MAX_MEDIA_PREVIEW_DP;
    public static int MEDIA_SPACING = MIN_MEDIA_PREVIEW_MARGIN_DP;

    public static void init(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float density = metrics.density;
        MIN_PREVIEW_W = (int) (MIN_PREVIEW_W_DP * density);
        MIN_PREVIEW_H = (int) (MIN_PREVIEW_H_DP * density);
        MAX_PREVIEW_W = (int) (MAX_PREVIEW_W_DP * density);
        MAX_PREVIEW_H = (int) (MAX_PREVIEW_H_DP * density);

        MAP_W = (int) (MAP_W_DP * density);
        MAP_H = (int) (MAP_H_DP * density);

        MAX_PREVIEW_BITMAP_W = (int) (MAX_PREVIEW_BITMAP_W_DP * density);
        MAX_PREVIEW_BITMAP_H = (int) (MAX_PREVIEW_BITMAP_H_DP * density);

        ROUND_RADIUS = (int) (ROUND_RADIUS_DP * density);

        WALLPAPER_S_W = (int) (WALLPAPER_S_W_DP * density);
        WALLPAPER_S_H = (int) (WALLPAPER_S_H_DP * density);

        WALL_S_MAX_W = (int) (WALL_S_MAX_W_DP * density);
        WALL_S_MAX_H = (int) (WALL_S_MAX_H_DP * density);

        WALL_MAX_W = Math.min(metrics.widthPixels, metrics.heightPixels);
        WALL_MAX_H = Math.max(metrics.widthPixels, metrics.heightPixels);

        int side = Math.min(metrics.widthPixels, metrics.heightPixels);

        int margin = (int) (MIN_MEDIA_PREVIEW_MARGIN_DP * metrics.density);
        int cellWidth = ((side - (MEDIA_ROW_COUNT - 1) * margin) / MEDIA_ROW_COUNT);

        if (cellWidth >= (MAX_MEDIA_PREVIEW_DP * metrics.density)) {
            MEDIA_PREVIEW = (int) (MAX_MEDIA_PREVIEW_DP * metrics.density);
            MEDIA_SPACING = (int) (MIN_MEDIA_PREVIEW_MARGIN_DP * metrics.density);
        } else {
            MEDIA_PREVIEW = cellWidth;
            MEDIA_SPACING = margin;
        }
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
