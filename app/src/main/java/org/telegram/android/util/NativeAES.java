package org.telegram.android.util;

import org.telegram.mtproto.secure.AESImplementation;
import org.telegram.mtproto.secure.DefaultAESImplementation;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;

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
        defaultAESImplementation.AES256IGEEncrypt(sourceFile, destFile, iv, key);
    }

    @Override
    public void AES256IGEDecrypt(String sourceFile, String destFile, byte[] iv, byte[] key) throws IOException {
        defaultAESImplementation.AES256IGEDecrypt(sourceFile, destFile, iv, key);
    }

    private native void nativeAesDecrypt(byte[] src, byte[] dest, int len, byte[] iv, byte[] key);

    private native void nativeAesEncrypt(byte[] src, byte[] dest, int len, byte[] iv, byte[] key);
}
