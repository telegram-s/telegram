#include <jni.h>
#include <stdio.h>
#include "aes/aes.h"

JNIEXPORT void Java_org_telegram_android_util_NativeAES_nativeAesDecrypt(
                                                             JNIEnv* env,
                                                             jobject thiz,
                                                             jbyteArray _source,
                                                             jbyteArray _dest,
                                                             jint len,
                                                             jbyteArray _iv,
                                                             jbyteArray _key) {
    unsigned char *source = (unsigned char *)(*env)->GetByteArrayElements(env, _source, NULL);
    unsigned char *dest = (unsigned char *)(*env)->GetByteArrayElements(env, _dest, NULL);
    unsigned char *key = (unsigned char *)(*env)->GetByteArrayElements(env, _key, NULL);
    unsigned char *iv = (unsigned char *)(*env)->GetByteArrayElements(env, _iv, NULL);

    AES_KEY akey;
    AES_set_decrypt_key(key, (*env)->GetArrayLength(env, _key) * 8, &akey);
    AES_ige_encrypt(source, dest, len, &akey, iv, AES_DECRYPT);

    (*env)->ReleaseByteArrayElements(env, _source, _source, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, _dest, _dest, 0);
    (*env)->ReleaseByteArrayElements(env, _key, key, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, _iv, iv, JNI_ABORT);
}

JNIEXPORT void Java_org_telegram_android_util_NativeAES_nativeAesDecryptStreaming(
                                                             JNIEnv* env,
                                                             jobject thiz,
                                                             jbyteArray _source,
                                                             jbyteArray _dest,
                                                             jint len,
                                                             jbyteArray _iv,
                                                             jbyteArray _key) {
    unsigned char *source = (unsigned char *)(*env)->GetByteArrayElements(env, _source, NULL);
    unsigned char *dest = (unsigned char *)(*env)->GetByteArrayElements(env, _dest, NULL);
    unsigned char *key = (unsigned char *)(*env)->GetByteArrayElements(env, _key, NULL);
    unsigned char *iv = (unsigned char *)(*env)->GetByteArrayElements(env, _iv, NULL);

    AES_KEY akey;
    AES_set_decrypt_key(key, (*env)->GetArrayLength(env, _key) * 8, &akey);
    AES_ige_encrypt(source, dest, len, &akey, iv, AES_DECRYPT);

    (*env)->ReleaseByteArrayElements(env, _source, _source, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, _dest, _dest, 0);
    (*env)->ReleaseByteArrayElements(env, _key, key, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, _iv, iv, 0);
}

JNIEXPORT void Java_org_telegram_android_util_NativeAES_nativeAesEncrypt(
                                                             JNIEnv* env,
                                                             jobject thiz,
                                                             jbyteArray _source,
                                                             jbyteArray _dest,
                                                             jint len,
                                                             jbyteArray _iv,
                                                             jbyteArray _key) {

    unsigned char *source = (unsigned char *)(*env)->GetByteArrayElements(env, _source, NULL);
    unsigned char *dest = (unsigned char *)(*env)->GetByteArrayElements(env, _dest, NULL);
    unsigned char *key = (unsigned char *)(*env)->GetByteArrayElements(env, _key, NULL);
    unsigned char *iv = (unsigned char *)(*env)->GetByteArrayElements(env, _iv, NULL);

    AES_KEY akey;
    AES_set_encrypt_key(key, (*env)->GetArrayLength(env, _key) * 8, &akey);
    AES_ige_encrypt(source, dest, len, &akey, iv, AES_ENCRYPT);

    (*env)->ReleaseByteArrayElements(env, _source, _source, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, _dest, _dest, 0);
    (*env)->ReleaseByteArrayElements(env, _key, key, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, _iv, iv, JNI_ABORT);
}

JNIEXPORT void Java_org_telegram_android_util_NativeAES_nativeAesEncryptStreaming(
                                                             JNIEnv* env,
                                                             jobject thiz,
                                                             jbyteArray _source,
                                                             jbyteArray _dest,
                                                             jint len,
                                                             jbyteArray _iv,
                                                             jbyteArray _key) {

    unsigned char *source = (unsigned char *)(*env)->GetByteArrayElements(env, _source, NULL);
    unsigned char *dest = (unsigned char *)(*env)->GetByteArrayElements(env, _dest, NULL);
    unsigned char *key = (unsigned char *)(*env)->GetByteArrayElements(env, _key, NULL);
    unsigned char *iv = (unsigned char *)(*env)->GetByteArrayElements(env, _iv, NULL);

    AES_KEY akey;
    AES_set_encrypt_key(key, (*env)->GetArrayLength(env, _key) * 8, &akey);
    AES_ige_encrypt(source, dest, len, &akey, iv, AES_ENCRYPT);

    (*env)->ReleaseByteArrayElements(env, _source, _source, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, _dest, _dest, 0);
    (*env)->ReleaseByteArrayElements(env, _key, key, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, _iv, iv, 0);
}