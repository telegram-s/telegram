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
#include "log.h"
#include "libjpeg/jpeglib.h"

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
  LOGI("Fast bluring image w=%d, h=%d, s=%d", info->width, info->height, info->stride);

  uint32_t* pix = (uint32_t*)pixels;
  int w = info->width;
  int h = info->height;
  const int radius = 3;
  const int div = radius * 2 + 1;

  uint32_t* rgb = (uint32_t *)malloc(w * h * sizeof(uint32_t));

  uint32_t rgbsum;
  int x, y, i;
  int* vmin = (int *)malloc(MAX(w, h) * sizeof(int));

  uint32_t stack[div];
  int stackpointer, stackstart;
  uint32_t rgboutsum, rgbinsum;

  for (x = 0; x < w - radius - 1; x++) {
    vmin[x] = x + radius + 1;
  }
  for (x = w - radius - 1; x < w; x++) {
    vmin[x] = w - 1;
  }

  int yw = 0;
  for (y = 0; y < h; y++) {
    rgbinsum = rgboutsum = rgbsum = 0;
    for (i = -radius; i <= radius; i++) {
      uint32_t p = pix[yw + MIN(w - 1, MAX(i, 0))];
      stack[i + radius] = ((p & 0x0000FC) >> 2) +
                           (p & 0x00FC00) +
                          ((p & 0xFC0000) << 2);

      rgbsum += stack[i + radius] * (radius + 1 - ABS(i));

      if (i > 0) {
        rgbinsum += stack[i + radius];
      } else {
        rgboutsum += stack[i + radius];
      }
    }
    stackstart = 0;
    stackpointer = radius + 1;

    for (x = 0; x < w; x++) {
      // rgb[yw + x] = (rgbsum >> 4) & 0x03F0FC3F;
      rgb[yw + x] = ((rgbsum + 0x00B02C0B) >> 4) & 0x03F0FC3F;

      rgbsum -= rgboutsum;

      rgboutsum -= stack[stackstart];

      uint32_t p = pix[yw + vmin[x]];

      stack[stackstart] = ((p & 0x0000FC) >> 2) +
                           (p & 0x00FC00) +
                          ((p & 0xFC0000) << 2);

      rgbinsum += stack[stackstart];

      rgbsum += rgbinsum;

      rgboutsum += stack[stackpointer];

      rgbinsum -= stack[stackpointer];

      stackstart++;
      if (stackstart == div) {
        stackstart = 0;
      }

      stackpointer++;
      if (stackpointer == div) {
        stackpointer = 0;
      }
    }
    yw += w;
  }

  for (y = 0; y < h - radius - 1; y++) {
    vmin[y] = (y + radius + 1) * w;
  }
  for (y = h - radius - 1; y < h; y++) {
    vmin[y] = (h - 1) * w;
  }

  for (x = 0; x < w; x++) {
    rgbinsum = rgboutsum = rgbsum = 0;
    for (i = -radius; i <= radius; i++) {
      stack[i + radius] = rgb[MIN(h - 1, MAX(0, i)) * w + x];

      rgbsum += stack[i + radius] * (radius + 1 - ABS(i));

      if (i > 0) {
        rgbinsum += stack[i + radius];
      } else {
        rgboutsum += stack[i + radius];
      }
    }
    stackstart = 0;
    stackpointer = radius + 1;

    int yi = x;
    for (y = 0; y < h; y++) {
      pix[yi] = (pix[yi] & 0xFF000000) + ((rgbsum & 0x3FC00000) >> 6) + ((rgbsum & 0x000FF000) >> 4) + ((rgbsum & 0x000003FC) >> 2);

      rgbsum -= rgboutsum;
      rgboutsum -= stack[stackstart];

      stack[stackstart] = rgb[x + vmin[y]];

      rgbinsum += stack[stackstart];

      rgbsum += rgbinsum;

      rgboutsum += stack[stackpointer];

      rgbinsum -= stack[stackpointer];

      stackstart++;
      if (stackstart == div) {
        stackstart = 0;
      }

      stackpointer++;
      if (stackpointer == div) {
        stackpointer = 0;
      }

      yi += w;
    }
  }

  free (rgb);
  free (vmin);
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
            red = rgb_clamp((int)(red * brightnessValue + (1-brightnessValue)*255));
            green = rgb_clamp((int)(green * brightnessValue+ (1-brightnessValue)*255));
            blue = rgb_clamp((int)(blue * brightnessValue+ (1-brightnessValue)*255));

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
        brightness(&info,pixels, 0.8f);

        AndroidBitmap_unlockPixels(env, bitmap);
}
void Java_org_telegram_android_media_BitmapDecoderEx_nativeDecodeBitmap(
                                                             JNIEnv* env,
                                                             jobject thiz,
                                                             jstring fileName,
                                                             jobject bitmap)
{
    char * path =  (*env)->GetStringUTFChars( env, fileName , NULL ) ;
    AndroidBitmapInfo  info;
    struct jpeg_error_mgr jerr;
    struct jpeg_decompress_struct cinfo;
    FILE * infile;		/* source file */
    JSAMPARRAY buffer;		/* Output row buffer */
    int row_stride;		/* physical row width in output buffer */
    int ret;
    int rowIndex;
    int i;
    void* pixels;
    
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }
    
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }
    
    LOGI("Loading image from path %s", path);
    
    if ((infile = fopen(path, "rb")) == NULL) {
        LOGE("Unable to open file");
        return;
    }
    
    cinfo.err = jpeg_std_error(&jerr);
    
    jpeg_create_decompress(&cinfo);
    
    jpeg_stdio_src(&cinfo, infile);
    (void) jpeg_read_header(&cinfo, TRUE);
    
    (void) jpeg_start_decompress(&cinfo);
    
    row_stride = cinfo.output_width * cinfo.output_components;
    
    buffer = (*cinfo.mem->alloc_sarray)
    ((j_common_ptr) &cinfo, JPOOL_IMAGE, row_stride, 1);
    
    rowIndex = 0;
    uint32_t* line = (uint32_t*)pixels;
    while (cinfo.output_scanline < cinfo.output_height) {
        (void) jpeg_read_scanlines(&cinfo, buffer, 1);
        
        if (rowIndex++ < info.height) {
            for( i = 0; i < MIN(info.width, cinfo.output_width); i++) {
                line[i] = RGBA(255, buffer[0][i*3], buffer[0][i*3+1], buffer[0][i*3 + 2]);
            }
            line = (char*)line + (info.stride);
        }
    }
    
    (void) jpeg_finish_decompress(&cinfo);
    
    AndroidBitmap_unlockPixels(env, bitmap);
}

void Java_org_telegram_android_media_BitmapDecoderEx_nativeDecodeArray(
                                                             JNIEnv* env,
                                                             jobject thiz,
                                                             jbyteArray array,
                                                             jobject bitmap)
{
    AndroidBitmapInfo  info;
    struct jpeg_error_mgr jerr;
    struct jpeg_decompress_struct cinfo;
    JSAMPARRAY buffer;		/* Output row buffer */
    int row_stride;		/* physical row width in output buffer */
    int ret;
    int rowIndex;
    int i;
    void* pixels;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }

    jbyte *b = (jbyte *)(*env)->GetByteArrayElements(env, array, NULL);
    jsize len = (*env)->GetArrayLength(env, array);

    cinfo.err = jpeg_std_error(&jerr);

    jpeg_create_decompress(&cinfo);

    jpeg_mem_src(&cinfo, b, len);
    // jpeg_stdio_src(&cinfo, infile);

    (void) jpeg_read_header(&cinfo, TRUE);

    (void) jpeg_start_decompress(&cinfo);

    row_stride = cinfo.output_width * cinfo.output_components;

    buffer = (*cinfo.mem->alloc_sarray)
    ((j_common_ptr) &cinfo, JPOOL_IMAGE, row_stride, 1);

    rowIndex = 0;
    uint32_t* line = (uint32_t*)pixels;
    while (cinfo.output_scanline < cinfo.output_height) {
        (void) jpeg_read_scanlines(&cinfo, buffer, 1);

        if (rowIndex++ < info.height) {
            for( i = 0; i < MIN(info.width, cinfo.output_width); i++) {
                line[i] = RGBA(255, buffer[0][i*3], buffer[0][i*3+1], buffer[0][i*3 + 2]);
            }
            line = (char*)line + (info.stride);
        }
    }

    (void) jpeg_finish_decompress(&cinfo);

    AndroidBitmap_unlockPixels(env, bitmap);

    (*env)->ReleaseByteArrayElements(env, array, b, 0 );
}