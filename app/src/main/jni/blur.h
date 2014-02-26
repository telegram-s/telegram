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

#define SIZE_FULL 8100

static inline uint32_t get_colors (const uint8_t *p) {
  return (p[0] >> 4) + ((p[1] & 0xF0) << 6) + ((p[2] & 0xF0) << 16);
}

static inline uint64_t get_colors_64 (const uint8_t *p) {
  return p[0] + (p[1] << 16) + ((uint64_t)p[2] << 32);
}

static void fastBlur7 (AndroidBitmapInfo *info, const int w, const int h, void *pixels) {
  uint8_t *pix = (uint8_t *)pixels;
  const int stride = info->stride;
  const int radius = 7;
  const int r1 = radius + 1;
  const int div = radius * 2 + 1;

  if (radius > 15 || div >= w || div >= h || w * h > SIZE_FULL) {
    return;
  }

  static uint32_t rgb[SIZE_FULL];

  int x, y, i;

  int yw = 0;
  const int we = w - r1;
  for (y = 0; y < h; y++) {
    uint32_t cur = get_colors (&pix[yw]);
    uint32_t rgballsum = -radius * cur;
    uint32_t rgbsum = cur * ((r1 * (r1 + 1)) >> 1);

    for (i = 1; i <= radius; i++) {
      uint32_t cur = get_colors (&pix[yw + i * 4]);
      rgbsum += cur * (r1 - i);
      rgballsum += cur;
    }

    x = 0;

#define update(start, middle, end)                         \
      rgb[y * w + x] = ((rgbsum + 0x03B0F03B) >> 6) & 0x00F03C0F; \
                                                           \
      rgballsum += get_colors (&pix[yw + (start) * 4]) -   \
               2 * get_colors (&pix[yw + (middle) * 4]) +  \
                   get_colors (&pix[yw + (end) * 4]);      \
      rgbsum += rgballsum;                                 \
      x++;                                                 \

    while (x < r1) {
      update (0, x, x + r1);
    }
    while (x < we) {
      update (x - r1, x, x + r1);
    }
    while (x < w) {
      update (x - r1, x, w - 1);
    }
#undef update

    yw += stride;
  }

  const int he = h - r1;
  for (x = 0; x < w; x++) {
    uint32_t rgballsum = -radius * rgb[x];
    uint32_t rgbsum = rgb[x] * ((r1 * (r1 + 1)) >> 1);
    for (i = 1; i <= radius; i++) {
      rgbsum += rgb[i * w + x] * (r1 - i);
      rgballsum += rgb[i * w + x];
    }

    y = 0;
    int yi = x * 4;

#define update(start, middle, end)         \
      pix[yi] = rgbsum >> 2;               \
      pix[yi + 1] = rgbsum >> 12;          \
      pix[yi + 2] = rgbsum >> 22;          \
                                           \
      rgballsum += rgb[x + (start) * w] -  \
               2 * rgb[x + (middle) * w] + \
                   rgb[x + (end) * w];     \
      rgbsum += rgballsum;                 \
      y++;                                 \
      yi += stride;

    while (y < r1) {
      update (0, y, y + r1);
    }
    while (y < he) {
      update (y - r1, y, y + r1);
    }
    while (y < h) {
      update (y - r1, y, h - 1);
    }
#undef update
  }
}


static void fastBlur7_2 (AndroidBitmapInfo *info,const int w, const int h, void *pixels) {
  uint8_t *pix = (uint8_t *)pixels;
  const int stride = info->stride;
  const int radius = 7;
  const int r1 = radius + 1;
  const int div = radius * 2 + 1;

  if (radius > 15 || div >= w || div >= h || w * h > SIZE_FULL) {
    return;
  }

  static uint64_t rgb[SIZE_FULL];

  int x, y, i;

  int yw = 0;
  const int we = w - r1;
  for (y = 0; y < h; y++) {
    uint64_t cur = get_colors_64 (&pix[yw]);
    uint64_t rgballsum = -radius * cur;
    uint64_t rgbsum = cur * ((r1 * (r1 + 1)) >> 1);

    for (i = 1; i <= radius; i++) {
      uint64_t cur = get_colors_64 (&pix[yw + i * 4]);
      rgbsum += cur * (r1 - i);
      rgballsum += cur;
    }

    x = 0;

#define update(start, middle, end)                         \
      rgb[y * w + x] = (rgbsum >> 6) & 0x00FF00FF00FF00FFLL; \
                                                           \
      rgballsum += get_colors_64 (&pix[yw + (start) * 4]) -   \
               2 * get_colors_64 (&pix[yw + (middle) * 4]) +  \
                   get_colors_64 (&pix[yw + (end) * 4]);      \
      rgbsum += rgballsum;                                 \
      x++;                                                 \

    while (x < r1) {
      update (0, x, x + r1);
    }
    while (x < we) {
      update (x - r1, x, x + r1);
    }
    while (x < w) {
      update (x - r1, x, w - 1);
    }
#undef update

    yw += stride;
  }

  const int he = h - r1;
  for (x = 0; x < w; x++) {
    uint64_t rgballsum = -radius * rgb[x];
    uint64_t rgbsum = rgb[x] * ((r1 * (r1 + 1)) >> 1);
    for (i = 1; i <= radius; i++) {
      rgbsum += rgb[i * w + x] * (r1 - i);
      rgballsum += rgb[i * w + x];
    }

    y = 0;
    int yi = x * 4;

#define update(start, middle, end)         \
      int64_t res = rgbsum >> 6;           \
      pix[yi] = res & 0xFF;                       \
      pix[yi + 1] = (res >> 16) & 0xFF;             \
      pix[yi + 2] = (res >> 32) & 0xFF;             \
                                           \
      rgballsum += rgb[x + (start) * w] -  \
               2 * rgb[x + (middle) * w] + \
                   rgb[x + (end) * w];     \
      rgbsum += rgballsum;                 \
      y++;                                 \
      yi += stride;

    while (y < r1) {
      update (0, y, y + r1);
    }
    while (y < he) {
      update (y - r1, y, y + r1);
    }
    while (y < h) {
      update (y - r1, y, h - 1);
    }
#undef update
  }
}
