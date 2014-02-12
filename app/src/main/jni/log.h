#ifndef LOG_INCLUDED
#define LOG_INCLUDED

#include <android/log.h>

//#define  LOGI(...)
// #define  LOGE(...)
// #define  LOGD(...)
#define  LOG_TAG "libtimg"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

#endif