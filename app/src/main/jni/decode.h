#include <jni.h>
#include <stdio.h>
#include <setjmp.h>

#include "libjpeg/jerror.h"

#define  R(c) ((c & 0x000000ff) >> 0)
#define  G(c) ((c & 0x0000ff00) >> 8)
#define  B(c) ((c & 0x00ff0000) >> 16)
#define  A(c) ((c & 0xff000000) >> 24)
#define  ARGB(a,r,g,b) ((a << 24) | (b << 16) | (g << 8) | r)
#define CLAMP(a) (a < 0 ? 0 : (a > 255 ? 255 : a))

struct my_error_mgr {
  struct jpeg_error_mgr pub;	/* "public" fields */

  jmp_buf setjmp_buffer;	/* for return to caller */
};

typedef struct my_error_mgr * my_error_ptr;

METHODDEF(void)
my_error_exit (j_common_ptr cinfo)
{
  /* cinfo->err really points to a my_error_mgr struct, so coerce pointer */
  my_error_ptr myerr = (my_error_ptr) cinfo->err;

  /* Always display the message. */
  /* We could postpone this until after returning, if we chose. */
  (*cinfo->err->output_message) (cinfo);

  /* Return control to the setjmp point */
  longjmp(myerr->setjmp_buffer, 1);
}

jint throwIOException(JNIEnv *env, char *message ) {
    jclass exClass;
    char *className = "java/io/IOException" ;

    exClass = (*env)->FindClass( env, className );
    if ( exClass == NULL ) {
        // return throwNoClassDefError( env, className );
        return 0;
	}

    return (*env)->ThrowNew( env, exClass, message );
}

jint throwUnsupported(JNIEnv *env, char *message ) {
    jclass exClass;
    char *className = "java/lang/UnsupportedOperationException" ;

    exClass = (*env)->FindClass( env, className );
    if ( exClass == NULL ) {
        // return throwNoClassDefError( env, className );
        return 0;
	}

    return (*env)->ThrowNew( env, exClass, message );
}

JNIEXPORT void Java_org_telegram_android_media_BitmapDecoderEx_nativeDecodeBitmapBlend(
                                                             JNIEnv* env,
                                                             jclass clazz,
                                                             jstring fileName,
                                                             jobject bitmap,
                                                             jboolean scale)
{
    char * path =  (*env)->GetStringUTFChars( env, fileName , NULL );
    AndroidBitmapInfo  info;
    struct my_error_mgr jerr;
    struct jpeg_decompress_struct cinfo;
    FILE * infile;
    JSAMPARRAY buffer;
    int row_stride, ret, rowIndex, i, a, r, g, b, rw;
    void* pixels;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        throwUnsupported(env, "AndroidBitmap_getInfo() failed");
        return;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        throwUnsupported(env, "Bitmap format is not RGBA_8888!");
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        throwUnsupported(env, "AndroidBitmap_lockPixels() failed");
        return;
    }

    if ((infile = fopen(path, "rb")) == NULL) {
        AndroidBitmap_unlockPixels(env, bitmap);
        (*env)->ReleaseStringUTFChars(env, fileName, path);
        throwIOException(env,"Unable to open JPEG");
        return;
    }
    (*env)->ReleaseStringUTFChars(env, fileName, path);

    cinfo.err = jpeg_std_error(&jerr.pub);

    jerr.pub.error_exit = my_error_exit;
    /* Establish the setjmp return context for my_error_exit to use. */
    if (setjmp(jerr.setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error.
        * We need to clean up the JPEG object, close the input file, and return.
        */

        jpeg_destroy_decompress(&cinfo);
        AndroidBitmap_unlockPixels(env, bitmap);
        fclose(infile);

        throwIOException(env,"Unable to open JPEG");
        return;
    }

    jpeg_create_decompress(&cinfo);

    jpeg_stdio_src(&cinfo, infile);
    (void) jpeg_read_header(&cinfo, TRUE);

    (void) jpeg_start_decompress(&cinfo);

    row_stride = cinfo.output_width * cinfo.output_components;

    buffer = (*cinfo.mem->alloc_sarray)
    ((j_common_ptr) &cinfo, JPOOL_IMAGE, row_stride, 1);

    rowIndex = 0;
    uint32_t* line = (uint32_t*)pixels;
    rw = cinfo.output_width;
    if (scale == JNI_TRUE)
    {
        rw = rw >> 1;
    }
    while (cinfo.output_scanline < cinfo.output_height) {
        if (scale) {
            (void) jpeg_read_scanlines(&cinfo, buffer, 1);
        }
        (void) jpeg_read_scanlines(&cinfo, buffer, 1);

        if (rowIndex++ < info.height) {
            if (scale == JNI_TRUE) {
                for(i = 0; i < MIN(info.width, rw); i++) {
                    a = buffer[0][i * 2];
                    r = (R(line[i]) * a) >> 8;
                    g = (G(line[i]) * a) >> 8;
                    b = (B(line[i]) * a) >> 8;
                    line[i] = ARGB(a, r, g, b);
                }
            }
            else{
                for(i = 0; i < MIN(info.width, rw); i++) {
                    a = buffer[0][i];
                    r = (R(line[i]) * a) >> 8;
                    g = (G(line[i]) * a) >> 8;
                    b = (B(line[i]) * a) >> 8;
                    line[i] = ARGB(a, r, g, b);
                }
            }
            line = (char*)line + (info.stride);
        }
    }

    (void) jpeg_finish_decompress(&cinfo);

    jpeg_destroy_decompress(&cinfo);
    AndroidBitmap_unlockPixels(env, bitmap);
    fclose(infile);
}

JNIEXPORT void Java_org_telegram_android_media_BitmapDecoderEx_nativeDecodeBitmap(
                                                             JNIEnv* env,
                                                             jclass clazz,
                                                             jstring fileName,
                                                             jobject bitmap)
{
    char * path =  (*env)->GetStringUTFChars( env, fileName , NULL );
    AndroidBitmapInfo  info;
    struct my_error_mgr jerr;
    struct jpeg_decompress_struct cinfo;
    FILE * infile;		/* source file */
    JSAMPARRAY buffer;		/* Output row buffer */
    int row_stride;		/* physical row width in output buffer */
    int ret;
    int rowIndex;
    int i;
    void* pixels;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        throwUnsupported(env, "AndroidBitmap_getInfo() failed");
        return;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        throwUnsupported(env, "Bitmap format is not RGBA_8888!");
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        throwUnsupported(env, "AndroidBitmap_lockPixels() failed");
        return;
    }

    if ((infile = fopen(path, "rb")) == NULL) {
        AndroidBitmap_unlockPixels(env, bitmap);
        (*env)->ReleaseStringUTFChars(env, fileName, path);
        throwIOException(env,"Unable to open JPEG");
        return;
    }
    (*env)->ReleaseStringUTFChars(env, fileName, path);

    cinfo.err = jpeg_std_error(&jerr.pub);

    jerr.pub.error_exit = my_error_exit;
    /* Establish the setjmp return context for my_error_exit to use. */
    if (setjmp(jerr.setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error.
         * We need to clean up the JPEG object, close the input file, and return.
         */
        jpeg_destroy_decompress(&cinfo);
        AndroidBitmap_unlockPixels(env, bitmap);
        fclose(infile);

        throwIOException(env,"Unable to open JPEG");
        return;
    }

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
                line[i] = ARGB(255, buffer[0][i*3], buffer[0][i*3+1], buffer[0][i*3 + 2]);
            }
            line = (char*)line + (info.stride);
        }
    }

    (void) jpeg_finish_decompress(&cinfo);

    jpeg_destroy_decompress(&cinfo);
    AndroidBitmap_unlockPixels(env, bitmap);
    fclose(infile);
}

JNIEXPORT void Java_org_telegram_android_media_BitmapDecoderEx_nativeDecodeArray(
                                                             JNIEnv* env,
                                                             jclass clazz,
                                                             jbyteArray array,
                                                             jobject bitmap)
{
    AndroidBitmapInfo  info;
    struct my_error_mgr jerr;
    struct jpeg_decompress_struct cinfo;
    JSAMPARRAY buffer;		/* Output row buffer */
    int row_stride;		/* physical row width in output buffer */
    int ret;
    int rowIndex;
    int i;
    void* pixels;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        throwUnsupported(env, "AndroidBitmap_getInfo() failed");
        return;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        throwUnsupported(env, "Bitmap format is not RGBA_8888!");
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        throwUnsupported(env, "AndroidBitmap_lockPixels() failed");
        return;
    }

    jbyte *b = (jbyte *)(*env)->GetByteArrayElements(env, array, NULL);
    jsize len = (*env)->GetArrayLength(env, array);

    cinfo.err = jpeg_std_error(&jerr.pub);

    jerr.pub.error_exit = my_error_exit;
    /* Establish the setjmp return context for my_error_exit to use. */
    if (setjmp(jerr.setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error.
        * We need to clean up the JPEG object, close the input file, and return.
        */
        jpeg_destroy_decompress(&cinfo);
        AndroidBitmap_unlockPixels(env, bitmap);
        (*env)->ReleaseByteArrayElements(env, array, b, JNI_ABORT);
        throwIOException(env,"Unable to open JPEG: internal error");
        return;
    }

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
                line[i] = ARGB(255, buffer[0][i*3], buffer[0][i*3+1], buffer[0][i*3 + 2]);
            }
            line = (char*)line + (info.stride);
        }
    }

    (void) jpeg_finish_decompress(&cinfo);

    jpeg_destroy_decompress(&cinfo);
    AndroidBitmap_unlockPixels(env, bitmap);
    (*env)->ReleaseByteArrayElements(env, array, b, JNI_ABORT);
}

JNIEXPORT void Java_org_telegram_android_media_BitmapDecoderEx_nativeDecodeBitmapScaled(
                                                             JNIEnv* env,
                                                             jclass clazz,
                                                             jstring fileName,
                                                             jobject bitmap,
                                                             jint scale)
{
    char * path =  (*env)->GetStringUTFChars( env, fileName , NULL );
    AndroidBitmapInfo  info;
    struct my_error_mgr jerr;
    struct jpeg_decompress_struct cinfo;
    FILE * infile;		/* source file */
    JSAMPARRAY buffer;		/* Output row buffer */
    int row_stride;		/* physical row width in output buffer */
    int ret;
    int rowIndex;
    int sRowIndex;
    int i;
    int ind;
    void* pixels;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        (*env)->ReleaseStringUTFChars(env, fileName, path);
        throwUnsupported(env, "AndroidBitmap_getInfo() failed");
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        (*env)->ReleaseStringUTFChars(env, fileName, path);
        throwUnsupported(env, "AndroidBitmap_lockPixels() failed ! error=%d");
        return;
    }

    if ((infile = fopen(path, "rb")) == NULL) {
        (*env)->ReleaseStringUTFChars(env, fileName, path);
        AndroidBitmap_unlockPixels(env, bitmap);
        throwIOException(env,"Unable to open JPEG");
        return;
    }
    (*env)->ReleaseStringUTFChars(env, fileName, path);

    cinfo.err = jpeg_std_error(&jerr.pub);

    jerr.pub.error_exit = my_error_exit;
    /* Establish the setjmp return context for my_error_exit to use. */
    if (setjmp(jerr.setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error.
        * We need to clean up the JPEG object, close the input file, and return.
        */
        jpeg_destroy_decompress(&cinfo);
        AndroidBitmap_unlockPixels(env, bitmap);
        fclose(infile);
        throwIOException(env,"Unable to open JPEG");
        return;
    }

    jpeg_create_decompress(&cinfo);

    jpeg_stdio_src(&cinfo, infile);
    (void) jpeg_read_header(&cinfo, TRUE);

    cinfo.scale_denom = scale;
    cinfo.scale_num = 1;

    (void) jpeg_start_decompress(&cinfo);

    LOGD("Scaled size %dx%d",cinfo.output_width, cinfo.output_height);

    row_stride = cinfo.output_width * cinfo.output_components;

    buffer = (*cinfo.mem->alloc_sarray)
    ((j_common_ptr) &cinfo, JPOOL_IMAGE, row_stride, 1);

    rowIndex = 0;
    sRowIndex = 0;
    uint32_t* line = (uint32_t*)pixels;
    while (cinfo.output_scanline < cinfo.output_height) {
        (void) jpeg_read_scanlines(&cinfo, buffer, 1);

        if (rowIndex++ < info.height) {
            for( i = 0; i < MIN(info.width, cinfo.output_width); i++) {
                line[i] = ARGB(255, buffer[0][i*3], buffer[0][i*3+1], buffer[0][i*3 + 2]);
            }
            line = (char*)line + (info.stride);
        }
    }

    (void) jpeg_finish_decompress(&cinfo);

    jpeg_destroy_decompress(&cinfo);
    AndroidBitmap_unlockPixels(env, bitmap);
    fclose(infile);
}

JNIEXPORT void Java_org_telegram_android_media_BitmapDecoderEx_nativeSaveBitmap(
                                                             JNIEnv* env,
                                                             jclass clazz,
                                                             jobject bitmap,
                                                             jint w, jint h,
                                                             jstring fileName)
{
    char * path =  (*env)->GetStringUTFChars( env, fileName , NULL );
    AndroidBitmapInfo  info;
    struct my_error_mgr jerr;
    struct jpeg_compress_struct cinfo;
    FILE * outfile;		/* source file */
    JSAMPARRAY buffer;		/* Output row buffer */
    int row_stride;		/* physical row width in output buffer */
    int ret;
    int rowIndex;
    int sRowIndex;
    int i;
    int ind;
    void* pixels;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        (*env)->ReleaseStringUTFChars(env, fileName, path);
        throwUnsupported(env, "AndroidBitmap_getInfo() failed");
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        (*env)->ReleaseStringUTFChars(env, fileName, path);
        throwUnsupported(env, "AndroidBitmap_lockPixels() failed ! error=%d");
        return;
    }

    if ((outfile = fopen(path, "wb")) == NULL) {
        (*env)->ReleaseStringUTFChars(env, fileName, path);
        AndroidBitmap_unlockPixels(env, bitmap);
        throwIOException(env,"Unable to open JPEG");
        return;
    }
    (*env)->ReleaseStringUTFChars(env, fileName, path);

    cinfo.err = jpeg_std_error(&jerr.pub);

    jerr.pub.error_exit = my_error_exit;
    /* Establish the setjmp return context for my_error_exit to use. */
    if (setjmp(jerr.setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error.
        * We need to clean up the JPEG object, close the input file, and return.
        */
        LOGE("Jpeg error1");
        jpeg_destroy_compress(&cinfo);
        LOGE("Jpeg error2");
        AndroidBitmap_unlockPixels(env, bitmap);
        LOGE("Jpeg error3");
        fclose(outfile);
        LOGE("Jpeg error4");
        throwIOException(env,"Unable to save JPEG");
        return;
    }

    jpeg_create_compress(&cinfo);

    jpeg_stdio_dest(&cinfo, outfile);

    cinfo.image_width = w; 	/* image width and height, in pixels */
    cinfo.image_height = h;
    cinfo.input_components = 3;		/* # of color components per pixel */
    cinfo.in_color_space = JCS_RGB; 	/* colorspace of input image */

    jpeg_set_defaults(&cinfo);

    jpeg_set_quality(&cinfo, 87, TRUE /* limit to baseline-JPEG values */);

    jpeg_start_compress(&cinfo, TRUE);

    row_stride = info.stride;	/* JSAMPLEs per row in image_buffer */
    buffer = (*cinfo.mem->alloc_sarray)
                 ((j_common_ptr) &cinfo, JPOOL_IMAGE, row_stride, 1);
    // Writing
    uint32_t* line;
    while (cinfo.next_scanline < cinfo.image_height) {
        line = (uint32_t*)(pixels + (cinfo.next_scanline * row_stride));
        for(i = 0; i < w; i++) {
             buffer[0][i * 3 + 0] = R(line[i]);
             buffer[0][i * 3 + 1] = G(line[i]);
             buffer[0][i * 3 + 2] = B(line[i]);
        }
       // buffer[0] = (void*)(pixels + (cinfo.next_scanline * row_stride));
        (void) jpeg_write_scanlines(&cinfo, buffer, 1);
    }

    jpeg_finish_compress(&cinfo);
    jpeg_destroy_compress(&cinfo);

    AndroidBitmap_unlockPixels(env, bitmap);

    fclose(outfile);
}