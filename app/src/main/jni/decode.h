#include <jni.h>
#include <stdio.h>
#include <setjmp.h>

#include "libjpeg/jerror.h"

#define  R(c) ((c & 0x000000ff) >> 0)
#define  G(c) ((c & 0x0000ff00) >> 8)
#define  B(c) ((c & 0x00ff0000) >> 16)
#define  A(c) ((c & 0xff000000) >> 24)
#define  ARGB(a,r,g,b) (a << 24 | (b << 16) | (g << 8) | r)

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
                                                             jobject bitmap)
{
    char * path =  (*env)->GetStringUTFChars( env, fileName , NULL );
    AndroidBitmapInfo  info;
    struct my_error_mgr jerr;
    struct jpeg_decompress_struct cinfo;
    FILE * infile;
    JSAMPARRAY buffer;
    int row_stride, ret, rowIndex, i;
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
                line[i] = ARGB(buffer[0][i], R(line[i]), G(line[i]), B(line[i]));
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
        throwIOException(env,"Unable to open JPEG");
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

    (void) jpeg_start_decompress(&cinfo);

    row_stride = cinfo.output_width * cinfo.output_components;

    buffer = (*cinfo.mem->alloc_sarray)
    ((j_common_ptr) &cinfo, JPOOL_IMAGE, row_stride, 1);

    rowIndex = 0;
    sRowIndex = 0;
    uint32_t* line = (uint32_t*)pixels;
    while (cinfo.output_scanline < cinfo.output_height) {
        (void) jpeg_read_scanlines(&cinfo, buffer, 1);

        if (sRowIndex++ % 2 == 0)
        {
            if (rowIndex++ < info.height) {
                for( i = 0; i < MIN(info.width, cinfo.output_width/2); i++) {
                    ind = i * 2;
                    line[i] = ARGB(255, buffer[0][ind*3], buffer[0][ind*3+1], buffer[0][ind*3 + 2]);
                }
                line = (char*)line + (info.stride);
            }
        }
    }

    (void) jpeg_finish_decompress(&cinfo);

    jpeg_destroy_decompress(&cinfo);
    AndroidBitmap_unlockPixels(env, bitmap);
    fclose(infile);
}