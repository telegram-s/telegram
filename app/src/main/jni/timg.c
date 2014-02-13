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
#include "libjpeg/jerror.h"
#include "jmemsource.h"
#include "pq.h"
#include "blur.h"
#include "aes.h"
#include "decode.h"

#define  MIN(a,b) (a < b ? a : b)
#define  MAX(a,b) (a > b ? a : b)
#define  ABS(a) (a > 0 ? a : -a)
#define  R(c) ((c & 0x000000ff) >> 0)
#define  G(c) ((c & 0x0000ff00) >> 8)
#define  B(c) ((c & 0x00ff0000) >> 16)
#define  A(c) ((c & 0xff000000) >> 24)
#define  ARGB(a,r,g,b) (a << 24 | (b << 16) | (g << 8) | r)
#define CLAMP(a) (a < 0 ? 0 : (a > 255 ? 255 : a))

JNIEXPORT jboolean Java_org_telegram_android_util_ImageNativeUtils_nativeMergeBitmapAlpha(
        JNIEnv* env,
        jclass clazz,
        jobject source,
        jobject alpha)
{
    AndroidBitmapInfo sinfo, ainfo;
    int ret;
    void* sourceP;
    void* alphaP;
    int xx, yy;

    if ((ret = AndroidBitmap_getInfo(env, source, &sinfo)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return JNI_FALSE;
    }

    if (sinfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888 !");
        return JNI_FALSE;
    }

    if ((ret = AndroidBitmap_lockPixels(env, source, &sourceP)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }

    if ((ret = AndroidBitmap_getInfo(env, alpha, &ainfo)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        AndroidBitmap_unlockPixels(env, source);
        return JNI_FALSE;
    }

    if (ainfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888 !");
        AndroidBitmap_unlockPixels(env, source);
        return JNI_FALSE;
    }

    if ((ret = AndroidBitmap_lockPixels(env, alpha, &alphaP)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        AndroidBitmap_unlockPixels(env, source);
        return JNI_FALSE;
    }

    uint32_t* sline = sourceP;
    uint32_t* aline = alphaP;

    for(yy = 0; yy < sinfo.height; yy++){
        sline = (uint32_t*) sourceP;
        aline = (uint32_t*) alphaP;

        for(xx =0; xx < sinfo.width; xx++) {
            sline[xx] = ARGB(R(aline[xx]), R(sline[xx]), G(sline[xx]), B(sline[xx]));
        }
        sourceP = (char*)sourceP + sinfo.stride;
        alphaP = (char*)alphaP + ainfo.stride;
    }


    AndroidBitmap_unlockPixels(env, source);
    AndroidBitmap_unlockPixels(env, alpha);

    return JNI_TRUE;
}

JNIEXPORT jboolean Java_org_telegram_android_util_ImageNativeUtils_nativeFastBlur(
        JNIEnv* env,
        jclass clazz,
        jobject bitmap)
{
    AndroidBitmapInfo  info;
    int ret;
    void* pixels;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return JNI_FALSE;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888 !");
        return JNI_FALSE;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return JNI_FALSE;
    }

    fastBlur3(&info, pixels);

    AndroidBitmap_unlockPixels(env, bitmap);

    return JNI_TRUE;
}

//void Java_org_telegram_android_util_ImageNativeUtils_nativeLoadEmoji(
//        JNIEnv* env,
//        jclass clazz,
//        jstring colorPath,
//        jstring alphaPath) {
//    char * cPath =  (*env)->GetStringUTFChars( env, colorPath , NULL );
//    char * aPath =  (*env)->GetStringUTFChars( env, alphaPath , NULL );
//    AndroidBitmapInfo  info;
//    struct jpeg_error_mgr jerr;
//    struct jpeg_decompress_struct cinfo;
//    struct jpeg_decompress_struct ainfo;
//    FILE *cFile, *aFile;
//    JSAMPARRAY cBuffer, aBuffer;
//    int strideC, strideA, ret, rowIndex, i;
//    void *cPixels, *aPixels;
//    uint32_t *cData, *aData;
//
//    cinfo.err = jpeg_std_error(&jerr);
//    ainfo.err = jpeg_std_error(&jerr);
//
//    jpeg_create_decompress(&cinfo);
//    jpeg_create_decompress(&ainfo);
//
//    if ((cFile = fopen(cPath, "rb")) == NULL) {
//        LOGE("Unable to open file");
//        return;
//    }
//
//    if ((aFile = fopen(aPath, "rb")) == NULL) {
//        LOGE("Unable to open file");
//        return;
//    }
//
//    jpeg_stdio_src(&cinfo, cFile);
//    jpeg_stdio_src(&ainfo, aFile);
//
//    (void) jpeg_read_header(&cinfo, TRUE);
//    (void) jpeg_read_header(&ainfo, TRUE);
//
//    (void) jpeg_start_decompress(&cinfo);
//    (void) jpeg_start_decompress(&ainfo);
//
//    strideC = cinfo.output_width * cinfo.output_components;
//    strideA = ainfo.output_width * ainfo.output_components;
//
//    cBuffer = (*cinfo.mem->alloc_sarray)
//        ((j_common_ptr) &cinfo, JPOOL_IMAGE, strideC, 1);
//    aBuffer = (*ainfo.mem->alloc_sarray)
//        ((j_common_ptr) &ainfo, JPOOL_IMAGE, strideA, 1);
//
//    jclass java_bitmap_class = (jclass)(*env)->FindClass(env, "android/graphics/Bitmap");
//    jmethodID mid = (*env)->GetStaticMethodID(env, java_bitmap_class, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
//
//    jclass bcfg_class = (*env)->FindClass(env, "android/graphics/Bitmap$Config");
//    jobject java_bitmap_config = (*env)->CallStaticObjectMethod(env, bcfg_class, (*env)->GetStaticMethodID(env, bcfg_class, "valueOf", "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;"), (*env)->NewStringUTF(env, "ARGB_8888"));
//
//    jobject* res =(jobject*) malloc(16 * sizeof(jobject));
//    for(i = 0; i < 16; i++) {
//        res[i] = (*env)->CallStaticObjectMethod(env, java_bitmap_class, mid, 8 * 54, 8 * 54, java_bitmap_config);
//    }
//
//    cData = (uint32_t*)cPixels;
//    aData = (unsigned char*)aPixels;
//    while (cinfo.output_scanline < cinfo.output_height) {
//        (void) jpeg_read_scanlines(&cinfo, cBuffer, 1);
//        (void) jpeg_read_scanlines(&ainfo, aBuffer, 1);
//    }
//
//    (void) jpeg_finish_decompress(&cinfo);
//    (void) jpeg_finish_decompress(&ainfo);
//}