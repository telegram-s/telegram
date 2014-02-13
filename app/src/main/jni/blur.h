#include <stdio.h>
#include <android/log.h>
#include <android/bitmap.h>
#include "log.h"

#define  MIN(a,b) (a < b ? a : b)
#define  MAX(a,b) (a > b ? a : b)
#define  ABS(a) (a > 0 ? a : -a)

static void fastBlur3(AndroidBitmapInfo* info, void* pixels) {
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