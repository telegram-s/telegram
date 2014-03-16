package org.telegram.android.util;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by ex3ndr on 18.11.13.
 */
public class IOUtils {
    private static ThreadLocal<byte[]> buffers = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[4 * 1024];
        }
    };

    public static void delete(File src) {
        if (src.exists()) {
            if (!src.delete()) {
                src.deleteOnExit();
            }
        }
    }

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = buffers.get();
        int len;
        while ((len = in.read(buf)) > 0) {
            Thread.yield();
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static void copy(InputStream in, File dst) throws IOException {
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = buffers.get();
        int len;
        while ((len = in.read(buf)) > 0) {
            Thread.yield();
            out.write(buf, 0, len);
        }
        out.close();
    }

    public static void writeAll(String fileName, byte[] data) throws IOException {
        OutputStream stream = new FileOutputStream(fileName);
        stream.write(data);
        stream.close();
    }

    public static byte[] readAll(String fileName) throws IOException {
        byte[] res;
        InputStream in = new FileInputStream(fileName);
        res = readAll(in);
        in.close();
        return res;
    }

    public static byte[] readAll(InputStream in) throws IOException {
        return readAll(in, null);
    }

    public static byte[] readAll(InputStream in, ProgressListener listener) throws IOException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(in);
        ByteArrayOutputStream os = new ByteArrayOutputStream(4096);
        byte[] buffer = buffers.get();
        int len;
        int readed = 0;
        try {
            while ((len = bufferedInputStream.read(buffer)) >= 0) {
                Thread.yield();
                os.write(buffer, 0, len);
                readed += len;
                if (listener != null) {
                    listener.onProgress(readed);
                }
            }
        } catch (java.io.IOException e) {
        }
        return os.toByteArray();
    }

    public static byte[] downloadFile(String url) throws IOException {
        return downloadFile(url, null);
    }

    public static byte[] downloadFile(String url, ProgressListener listener) throws IOException {
        URL urlSpec = new URL(url);
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) urlSpec.openConnection();
            urlConnection.setConnectTimeout(15000);
            urlConnection.setReadTimeout(15000);
            InputStream in = urlConnection.getInputStream();
            return IOUtils.readAll(in, listener);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    public static interface ProgressListener {
        public void onProgress(int bytes);
    }

}
