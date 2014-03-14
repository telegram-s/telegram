package org.telegram.android.util;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.*;

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
        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
        byte[] buffer = buffers.get();
        int len;
        try {
            while ((len = in.read(buffer)) >= 0) {
                Thread.yield();
                os.write(buffer, 0, len);
            }
        } catch (java.io.IOException e) {
        }
        return os.toByteArray();
    }

    public static byte[] downloadFile(String url) throws IOException {
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
        HttpConnectionParams.setSoTimeout(httpParams, 5000);
        DefaultHttpClient client = new DefaultHttpClient(httpParams);
        client.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);

        HttpGet get = new HttpGet(url.replace(" ", "%20"));
        HttpResponse response = client.execute(get);
        if (response.getEntity().getContentLength() == 0) {
            throw new IOException();
        }

        if (response.getStatusLine().getStatusCode() == 404) {
            throw new IOException();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.getEntity().writeTo(outputStream);
        byte[] data = outputStream.toByteArray();
        return data;
    }

}
