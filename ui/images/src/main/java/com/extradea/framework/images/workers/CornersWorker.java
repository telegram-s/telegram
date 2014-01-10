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
import com.extradea.framework.images.tasks.RoundedImageTask;
import com.extradea.framework.images.utils.ImageUtils;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 19.07.12
 * Time: 6:44
 */
public class CornersWorker implements ImageWorker {
    @Override
    public String toString() {
        return "[CornersWorker]";
    }

    public Bitmap getRoundedCornerBitmap(Bitmap bitmap, int r) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = r;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public CornersWorker() {
    }

    @Override
    public boolean acceptTask(ImageTask task, ImageController controller) {
        return task instanceof RoundedImageTask;
    }

    @Override
    public int processTask(ImageTask task, ImageController controller) {
        Bitmap src = task.getRequiredTasks()[0].getResult();
        if (task.hasSizeLimitation() && task.isFillRect()) {
            src = ImageUtils.scale(src, task.getMaxWidth(), task.getMaxHeight());
        }
        task.setResult(getRoundedCornerBitmap(src, ((RoundedImageTask) task).getRadius()));
        return RESULT_OK;
    }

    @Override
    public boolean isPausable() {
        return true;
    }
}
