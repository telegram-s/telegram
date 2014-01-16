/*
 * Copyright (c) 2013 Extradea LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.extradea.framework.images.workers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.extradea.framework.images.ImageController;
import com.extradea.framework.images.TaskEvent;
import com.extradea.framework.images.tasks.ImageDownloadTask;
import com.extradea.framework.images.tasks.ImageTask;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 19.07.12
 * Time: 6:46
 */
public class DownloadWorker implements ImageWorker {

    public static interface ProgressListener {
        void transferred(long transferedBytes);
    }

    static class CountingOutputStream extends ByteArrayOutputStream {

        private final ProgressListener listener;
        private long transferred;

        CountingOutputStream(final ProgressListener listener) {
            this.listener = listener;
            this.transferred = 0;
        }

        @Override
        public void write(final byte[] b, final int off, final int len) {
            super.write(b, off, len);
            this.transferred += len;
            this.listener.transferred(this.transferred);
        }

        @Override
        public void write(final int b) {
            super.write(b);
            this.transferred++;
            this.listener.transferred(this.transferred);
        }
    }

    @Override
    public String toString() {
        return "[ImageDownloadWorker]";
    }

    private DefaultHttpClient client;

    public DownloadWorker() {
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
        HttpConnectionParams.setSoTimeout(httpParams, 5000);
        client = new DefaultHttpClient(httpParams);
        client.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
    }

    private void notifyDownloading(int downloaded, int totalSize, ImageTask task, ImageController controller) {
        controller.notifyTaskState(task, TaskEvent.EVENT_DOWNLOADING, new Object[]{downloaded, totalSize});
    }

    private byte[] downloadImage(String url, final ImageTask task, final ImageController controller) {
        try {
            HttpGet get = new HttpGet(url.replace(" ", "%20"));
            HttpResponse response = client.execute(get);
            if (response.getEntity().getContentLength() == 0)
                return null;

            if (response.getStatusLine().getStatusCode() == 404)
                return null;


            final int totalCount = (int) response.getEntity().getContentLength();

            CountingOutputStream output = new CountingOutputStream(new ProgressListener() {
                @Override
                public void transferred(long transferedBytes) {
                    notifyDownloading((int) transferedBytes,
                            totalCount, task, controller);
                }
            });
            try {
                response.getEntity().writeTo(output);
            } finally {

            }

            notifyDownloading((int) response.getEntity().getContentLength(),
                    (int) response.getEntity().getContentLength(), task, controller);

            return output.toByteArray();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            //Log.e(TAG, e);
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            //Log.e(TAG, e);
            return null;
        }
    }

    @Override
    public boolean acceptTask(ImageTask task, ImageController controller) {
        return task instanceof ImageDownloadTask;
    }

    @Override
    public int processTask(ImageTask task, ImageController controller) {
        try {
            byte[] res = downloadImage(((ImageDownloadTask) task).getUrl(), task, controller);
            Bitmap img = controller.getBitmapDecoder().decodeBitmap(res);
            task.setResult(img);
            task.setBinaryResult(res);
            return RESULT_OK;
        } catch (Exception e) {
            //Log.e(TAG, e);
            e.printStackTrace();
            return RESULT_FAILURE;
        }
    }

    @Override
    public boolean isPausable() {
        return true;
    }
}
