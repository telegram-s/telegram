package com.extradea.framework.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.extradea.framework.images.utils.ImageUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: Korshakov Stepan
 * Created: 04.07.13 19:03
 */
public class BitmapDecoder {
    private ImageController controller;
    private Semaphore semaphore;

    public BitmapDecoder(ImageController controller) {
        this.controller = controller;
        semaphore = new Semaphore(Runtime.getRuntime().availableProcessors());
    }

    public Bitmap executeGuarded(Callable<Bitmap> runnable) {
        controller.waitPaused();

        boolean isAcquired = false;
        try {
            semaphore.acquire();
            isAcquired = true;
            return runnable.call();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (isAcquired) {
                semaphore.release();
            }
        }

        return null;
    }

    public Bitmap decodeBitmap(byte[] data) {
        controller.waitPaused();

        boolean isAcquired = false;
        try {
            semaphore.acquire();
            isAcquired = true;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inTempStorage = ImageUtils.getTempStorage();
            return BitmapFactory.decodeByteArray(data, 0, data.length, o);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (isAcquired) {
                semaphore.release();
            }
        }
    }
}
