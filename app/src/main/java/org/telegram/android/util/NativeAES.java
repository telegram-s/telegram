package org.telegram.android.util;

import org.telegram.android.log.Logger;
import org.telegram.mtproto.secure.CryptoUtils;
import org.telegram.mtproto.secure.aes.AESImplementation;
import org.telegram.mtproto.secure.aes.DefaultAESImplementation;

import java.io.*;
import java.util.Arrays;

/**
 * Created by ex3ndr on 12.02.14.
 */
public class NativeAES implements AESImplementation {

    private static final String TAG = "NativeAES";

    static {
        System.loadLibrary("timg");
    }

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
            int realCount = count;
            if (realCount < 4 * 1024) {
                realCount += (16 - (realCount % 16) % 16);
            }
            nativeAesEncryptStreaming(buffer, outBuffer, realCount, workIv, key);
            outputStream.write(outBuffer, 0, realCount);
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
            int realCount = count;
            if (realCount < 4 * 1024) {
                realCount += (16 - (realCount % 16) % 16);
            }
            nativeAesDecryptStreaming(buffer, outBuffer, realCount, workIv, key);

            outputStream.write(outBuffer, 0, realCount);
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
