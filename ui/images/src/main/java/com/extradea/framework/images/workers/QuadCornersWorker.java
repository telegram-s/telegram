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

import android.graphics.*;
import com.extradea.framework.images.ImageController;
import com.extradea.framework.images.tasks.ImageTask;
import com.extradea.framework.images.tasks.RoundedQuadImageTask;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 19.07.12
 * Time: 6:50
 */
public class QuadCornersWorker implements ImageWorker {
    private static final int SIDE = 100;
    private static final int DELTA = 4;

    @Override
    public String toString() {
        return "[QuadCornersWorker]";
    }

    private void drawBoundRect(Canvas canvas, Paint paint, int width, int height) {

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST));

        final int color = 0x00000000;
        final Rect rect = new Rect(0, 0, width, height);
        final RectF rectF = new RectF(rect);
        final float roundPx = 8;

        paint.setAntiAlias(true);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    }

    private void addCornerToCanvas(Canvas canvas, Paint paint, int width, int height, int r) {
        //final int color = 0xff424242;
        final int color = 0xffffffff;
        final Rect rect = new Rect(0, 0, width, height);
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, r, r, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    }

    public Bitmap getRounded1Bitmap(Bitmap bitmap, int r) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        addCornerToCanvas(canvas, paint, SIDE, SIDE, r);

        canvas.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), new Rect(0, 0, SIDE, SIDE), paint);

        return output;
    }

    public Bitmap getRounded2Bitmap(Bitmap bitmap1, Bitmap bitmap2, int r) {
        Bitmap output = Bitmap.createBitmap(SIDE,
                SIDE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        addCornerToCanvas(canvas, paint, SIDE, SIDE, r);

        int width = (SIDE - DELTA) / 2;
        int height = SIDE;
        //int height = (SIDE - DELTA) / 2;

        Rect src1 = new Rect((bitmap1.getWidth() - width) / 2, (bitmap1.getHeight() - height) / 2,
                (bitmap1.getWidth() + width) / 2, (bitmap1.getHeight() + height) / 2);

        Rect src2 = new Rect((bitmap2.getWidth() - width) / 2, (bitmap2.getHeight() - height) / 2,
                (bitmap2.getWidth() + width) / 2, (bitmap2.getHeight() + height) / 2);

        canvas.drawBitmap(bitmap1, src1, new Rect(0, 0, width, height), paint);
        canvas.drawBitmap(bitmap2, src2, new Rect(width + DELTA, 0, width + DELTA + width, height), paint);

        return output;
    }

    public Bitmap getRounded3Bitmap(Bitmap bitmap1, Bitmap bitmap2, Bitmap bitmap3, int r) {
        Bitmap output = Bitmap.createBitmap(SIDE,
                SIDE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        addCornerToCanvas(canvas, paint, SIDE, SIDE, r);

        int width = (SIDE - DELTA) / 2;
        int height = (SIDE - DELTA) / 2;

        Rect src1 = new Rect(0, 0, bitmap1.getWidth(), bitmap1.getHeight());
        canvas.drawBitmap(bitmap1, src1, new Rect(0, 0, width, height), paint);

        Rect src2 = new Rect((bitmap2.getWidth() - width) / 2, 0, (bitmap2.getWidth() + width) / 2, bitmap2.getHeight());
        canvas.drawBitmap(bitmap2, src2, new Rect(width + DELTA, 0, width + DELTA + width, SIDE), paint);

        Rect src3 = new Rect(0, 0, bitmap3.getWidth(), bitmap3.getHeight());
        canvas.drawBitmap(bitmap3, src3, new Rect(0, height + DELTA, width, height + DELTA + height), paint);

        return output;
    }

    public Bitmap getRounded4Bitmap(Bitmap bitmap1, Bitmap bitmap2, Bitmap bitmap3, Bitmap bitmap4, int r) {
        Bitmap output = Bitmap.createBitmap(SIDE,
                SIDE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        addCornerToCanvas(canvas, paint, SIDE, SIDE, r);

        int width = (SIDE - DELTA) / 2;
        int height = (SIDE - DELTA) / 2;

        Rect src1 = new Rect(0, 0, bitmap1.getWidth(), bitmap1.getHeight());
        canvas.drawBitmap(bitmap1, src1, new Rect(0, 0, width, height), paint);

        Rect src2 = new Rect(0, 0, bitmap2.getWidth(), bitmap2.getHeight());
        canvas.drawBitmap(bitmap2, src2, new Rect(width + DELTA, 0, width + DELTA + width, height), paint);

        Rect src3 = new Rect(0, 0, bitmap3.getWidth(), bitmap3.getHeight());
        canvas.drawBitmap(bitmap3, src3, new Rect(0, height + DELTA, width, height + DELTA + height), paint);

        if (bitmap4 != null) {
            Rect src4 = new Rect(0, 0, bitmap4.getWidth(), bitmap4.getHeight());
            canvas.drawBitmap(bitmap4, src4, new Rect(width + DELTA, height + DELTA, width + DELTA + width, height + DELTA + height), paint);
        }

        return output;
    }

    public QuadCornersWorker() {
    }

    @Override
    public boolean acceptTask(ImageTask task, ImageController controller) {
        return task instanceof RoundedQuadImageTask;
    }

    @Override
    public int processTask(ImageTask task, ImageController controller) {
        RoundedQuadImageTask quadImageTask = (RoundedQuadImageTask) task;
        Bitmap[] images = new Bitmap[quadImageTask.getUrls().length];
        for (int i = 0; i < images.length; i++) {
            images[i] = quadImageTask.getRequiredTasks()[i].getResult();
        }

        int r = quadImageTask.getRadius();

        if (images.length == 1) {
            task.setResult(getRounded1Bitmap(images[0], r));
        }

        if (images.length == 2) {
            task.setResult(getRounded2Bitmap(images[0], images[1], r));
        }

        if (images.length == 3) {
            task.setResult(getRounded3Bitmap(images[0], images[1], images[2], r));
        }

        if (images.length >= 4) {
            task.setResult(getRounded4Bitmap(images[0], images[1], images[2], images[3], r));
        }
        //task.setResult(getRoundedCornerBitmap(task.getRequiredTasks()[0].getResult()));
        // task.setResult(getRoundedCornerBitmap(task.getRequiredTasks()[0].getResult()));
        return RESULT_OK;
    }

    @Override
    public boolean isPausable() {
        return true;
    }
}
