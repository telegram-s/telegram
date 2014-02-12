package org.telegram.android.util;

import org.telegram.mtproto.secure.AESFastEngine;
import org.telegram.mtproto.secure.AESImplementation;
import org.telegram.mtproto.secure.DefaultAESImplementation;
import org.telegram.mtproto.secure.KeyParameter;

import java.io.*;
import java.lang.reflect.Array;
import java.util.Arrays;

import static org.telegram.mtproto.secure.CryptoUtils.substring;

/**
 * Created by ex3ndr on 12.02.14.
 */
public class NativeAES implements AESImplementation {

    private DefaultAESImplementation defaultAESImplementation = new DefaultAESImplementation();

    @Override
    public void AES256IGEDecrypt(byte[] src, byte[] dest, int len, byte[] iv, byte[] key) {
        nativeAesDecrypt(src, dest, len, iv, key);
    }

    @Override
    public void AES256IGEEncrypt(byte[] src, byte[] dest, int len, byte[] iv, byte[] key) {
        nativeAesEncrypt(src, dest, len, iv, key);
    }

    @Override
    public void AES256IGEEncrypt(String sourceFile, String destFile, byte[] iv, byte[] key) throws IOException {

        File src = new File(sourceFile);
        File dest = new File(destFile);

        byte[] workIv = new byte[iv.length];

        // 2.2 doesn't have methods for array copy
        for (int i = 0; i < workIv.length; i++) {
            workIv[i] = iv[i];
        }

        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(src));
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(dest));
        byte[] buffer = new byte[4 * 1024];
        byte[] outBuffer = new byte[4 * 1024];

        int count;
        while ((count = inputStream.read(buffer)) > 0) {
            nativeAesEncryptStreaming(buffer, outBuffer, buffer.length, workIv, key);
            outputStream.write(outBuffer, 0, count);
        }
        outputStream.flush();
        outputStream.close();
        inputStream.close();
    }

    @Override
    public void AES256IGEDecrypt(String sourceFile, String destFile, byte[] iv, byte[] key) throws IOException {
        File src = new File(sourceFile);
        File dest = new File(destFile);

        byte[] workIv = new byte[iv.length];

        // 2.2 doesn't have methods for array copy
        for (int i = 0; i < workIv.length; i++) {
            workIv[i] = iv[i];
        }

        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(src));
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(dest));
        byte[] buffer = new byte[4 * 1024];
        byte[] outBuffer = new byte[4 * 1024];

        int count;
        while ((count = inputStream.read(buffer)) > 0) {
            nativeAesDecryptStreaming(buffer, outBuffer, buffer.length, workIv, key);
            outputStream.write(outBuffer, 0, count);
        }
        outputStream.flush();
        outputStream.close();
        inputStream.close();
    }

    private native void nativeAesDecrypt(byte[] src, byte[] dest, int len, byte[] iv, byte[] key);

    private native void nativeAesDecryptStreaming(byte[] src, byte[] dest, int len, byte[] iv, byte[] key);

    private native void nativeAesEncrypt(byte[] src, byte[] dest, int len, byte[] iv, byte[] key);

    private native void nativeAesEncryptStreaming(byte[] src, byte[] dest, int len, byte[] iv, byte[] key);
}
