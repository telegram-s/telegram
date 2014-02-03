/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <jni.h>
#include <time.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <android/log.h>
#include <android/bitmap.h>

#define  LOG_TAG "libtimg"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  MIN(a,b) (a < b ? a : b)
#define  MAX(a,b) (a > b ? a : b)
#define  ABS(a) (a > 0 ? a : -a)
#define  R(c) ((c & 0x000000ff) >> 0)
#define  G(c) ((c & 0x0000ff00) >> 8)
#define  B(c) ((c & 0x00ff0000) >> 16)
#define  A(c) ((c & 0xff000000) >> 24)
#define  RGBA(a,r,g,b) (a << 24 | (b << 16) | (g << 8) | r)

#define  SIZE 90
#define  SIZE_FULL 8100

static int rgb_clamp(int value) {
  if(value > 255) {
    return 255;
  }
  if(value < 0) {
    return 0;
  }
  return value;
}

static void fastBlur(AndroidBitmapInfo* info, void* pixels) {
    LOGI("Faslt bluring image w=%d, h=%d, s=%d", info->width, info->height, info->stride);

    uint32_t* pix =(uint32_t*)pixels;
    int w = info->width;
    int h = info->height;
    const int radius = 3;
    const int div = radius * 2 + 1;
    int wm = w - 1;
    int hm = h - 1;
    int wh = w * h;


    int* r = malloc(SIZE_FULL * sizeof(int));
    int* g = malloc(SIZE_FULL * sizeof(int));
    int* b = malloc(SIZE_FULL * sizeof(int));

    int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
    int* vmin = malloc(SIZE_FULL * sizeof(uint32_t));

    int divsum = (div + 1) >> 1;
    divsum *= divsum;
    uint32_t* dv = malloc(256 * divsum * sizeof(uint32_t));
    for (i = 0; i < 256 * divsum; i++) {
        dv[i] = (i / divsum);
    }

    yw = yi = 0;

    int stack[div][radius];
    int stackpointer;
    int stackstart;
    int* sir;
    int rbs;
    int r1 = radius + 1;
    int routsum, goutsum, boutsum;
    int rinsum, ginsum, binsum;

    for (y = 0; y < h; y++) {
        rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
        for (i = -radius; i <= radius; i++) {
            p = pix[yi + MIN(wm, MAX(i, 0))];
            sir = stack[i + radius];
            sir[0] = R(p);
            sir[1] = G(p);
            sir[2] = B(p);

            rbs = r1 - ABS(i);
            rsum += sir[0] * rbs;
            gsum += sir[1] * rbs;
            bsum += sir[2] * rbs;

            if (i > 0) {
                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];
            } else {
                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];
            }
        }
        stackpointer = radius;

        for (x = 0; x < w; x++) {
            r[yi] = dv[rsum];
            g[yi] = dv[gsum];
            b[yi] = dv[bsum];

            rsum -= routsum;
            gsum -= goutsum;
            bsum -= boutsum;

            stackstart = stackpointer - radius;
            if (stackstart < 0)
            {
                stackstart += div;
            }

            sir = stack[stackstart];

            routsum -= sir[0];
            goutsum -= sir[1];
            boutsum -= sir[2];

            if (y == 0) {
                vmin[x] = MIN(x + radius + 1, wm);
            }
            p = pix[yw + vmin[x]];

            sir[0] = R(p);
            sir[1] = G(p);
            sir[2] = B(p);

            rinsum += sir[0];
            ginsum += sir[1];
            binsum += sir[2];

            rsum += rinsum;
            gsum += ginsum;
            bsum += binsum;

            stackpointer = stackpointer + 1;
            if (stackpointer == div) {
                stackpointer = 0;
            }
            sir = stack[stackpointer];

            routsum += sir[0];
            goutsum += sir[1];
            boutsum += sir[2];

            rinsum -= sir[0];
            ginsum -= sir[1];
            binsum -= sir[2];

            yi++;
        }
        yw += w;
    }

    for (x = 0; x < w; x++) {
        rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
        yp = -radius * w;
        for (i = -radius; i <= radius; i++) {
            yi = MAX(0, yp) + x;

            sir = stack[i + radius];

            sir[0] = r[yi];
            sir[1] = g[yi];
            sir[2] = b[yi];

            rbs = r1 - ABS(i);

            rsum += r[yi] * rbs;
            gsum += g[yi] * rbs;
            bsum += b[yi] * rbs;

            if (i > 0) {
                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];
            } else {
                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];
            }

            if (i < hm) {
                yp += w;
            }
        }

        yi = x;
        stackpointer = radius;
        for (y = 0; y < h; y++) {
            pix[yi] = RGBA(A(pix[yi]), dv[rsum], dv[gsum], dv[bsum]);

            rsum -= routsum;
            gsum -= goutsum;
            bsum -= boutsum;

            stackstart = stackpointer - radius;
                        if (stackstart < 0)
                        {
                            stackstart += div;
                        }
            sir = stack[stackstart];

            routsum -= sir[0];
            goutsum -= sir[1];
            boutsum -= sir[2];

            if (x == 0) {
                vmin[y] = MIN(y + r1, hm) * w;
            }
            p = x + vmin[y];

            sir[0] = r[p];
            sir[1] = g[p];
            sir[2] = b[p];

            // LOGI("Fast blur3");

            rinsum += sir[0];
            ginsum += sir[1];
            binsum += sir[2];

            rsum += rinsum;
            gsum += ginsum;
            bsum += binsum;

            stackpointer = stackpointer + 1;
            if (stackpointer == div) {
                stackpointer = 0;
            }
            sir = stack[stackpointer];

            // LOGI("Fast blur4");

            routsum += sir[0];
            goutsum += sir[1];
            boutsum += sir[2];

            rinsum -= sir[0];
            ginsum -= sir[1];
            binsum -= sir[2];

            yi += w;
        }
    }
}

static void brightness(AndroidBitmapInfo* info, void* pixels, float brightnessValue){
	int xx, yy, red, green, blue;
	uint32_t* line;

	for(yy = 0; yy < info->height; yy++){
		line = (uint32_t*)pixels;
		for(xx =0; xx < info->width; xx++){
			//extract the RGB values from the pixel
			red = R(line[xx]);
			green = G(line[xx]);
			blue = B(line[xx]);

            //manipulate each value
            red = rgb_clamp((int)(red * brightnessValue));
            green = rgb_clamp((int)(green * brightnessValue));
            blue = rgb_clamp((int)(blue * brightnessValue));

            // set the new pixel back in
            line[xx] = RGBA(A(line[xx]), red, green, blue);
		}
		pixels = (char*)pixels + info->stride;
	}
}

void Java_org_telegram_android_media_OptimizedBlur_nativeFastBlur(
        JNIEnv* env,
        jobject thiz,
        jobject bitmap)
{
    AndroidBitmapInfo  info;
        int ret;
        void* pixels;

        if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
                LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
                return;
        }

        if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
            LOGE("Bitmap format is not RGBA_8888 !");
            return;
        }

        if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
            LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        }

        fastBlur(&info,pixels);
        brightness(&info,pixels, 1.5f);

        AndroidBitmap_unlockPixels(env, bitmap);
}