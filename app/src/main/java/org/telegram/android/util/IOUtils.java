package org.telegram.android.util;

import java.io.*;

/**
 * Created by ex3ndr on 18.11.13.
 */
public class IOUtils {
    public static void delete(File src) {
        if (!src.delete()) {
            src.deleteOnExit();
        }
    }

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        copy(in, dst);
        in.close();
    }

    public static void copy(InputStream in, File dst) throws IOException {
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.close();
    }

    public static byte[] readAll(String fileName) throws IOException {
        byte[] res;
        InputStream in = new FileInputStream(fileName);
        res = readAll(in);
        in.close();
        return res;
    }

    public static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = in.read(buffer)) >= 0) {
                os.write(buffer, 0, len);
            }
        } catch (java.io.IOException e) {
        }
        return os.toByteArray();
    }
}
