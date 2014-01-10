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

package com.extradea.framework.images.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.extradea.framework.images.utils.Crypt;
import com.extradea.framework.images.utils.ImageUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Callable;

/**
 * User: Stepan (aka Ex3NDR) Korshakov
 * Date: 13.09.11
 * Time: 15:42
 */
public class ContextImagePersistence extends ImagePersistence {

    private Context context;

    private Object sync = new Object();

    public ContextImagePersistence(Context context) {
        this.context = context;
    }

    private String idToPath(String id) {
        return context.getFilesDir().getAbsolutePath() + "//image_" + Crypt.md5Hash(id);
        // return "/mnt/sdcard/test/" + id + ".png";
    }

    @Override
    public Date creationDate(String id) {
        synchronized (sync) {
            File file = new File(idToPath(id));
            if (file.exists()) {
                return new Date(file.lastModified());
            } else {
                return null;
            }
        }
    }

    @Override
    public void remove(String id) {
        synchronized (sync) {
            new File(idToPath(id)).delete();
        }
    }

    @Override
    public Bitmap loadImage(final String id) {
        return loadImage(idToPath(id), new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inTempStorage = ImageUtils.getTempStorage();
                return BitmapFactory.decodeFile(idToPath(id), o);
            }
        });
    }

    private Bitmap loadImage(String fileName, Callable<Bitmap> execute) {
        synchronized (sync) {
            File file = new File(fileName);
            if (file.exists()) {
                try {
                    return execute.call();
                } catch (Exception e) {
                    e.printStackTrace();
                    file.delete();
                    return null;
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                    file.delete();
                    return null;
                } finally {
                }
            } else {
                return null;
            }
        }
    }

    @Override
    public Bitmap loadImageOptimized(final String id) {
        return loadImage(idToPath(id), new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception {
                return ImageUtils.getOptimalBitmap(idToPath(id));
            }
        });
    }

    @Override
    public Bitmap loadImageFill(final String id, final int maxWidth, final int maxHeight) {
        return loadImage(idToPath(id), new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception {
                return ImageUtils.getOptimalBitmapFill(idToPath(id), maxWidth, maxHeight);
            }
        });
    }

    @Override
    public Bitmap loadImageOptimized(final String id, final int maxWidth, final int maxHeight) {
        return loadImage(idToPath(id), new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception {
                return ImageUtils.getOptimalBitmap(idToPath(id), maxWidth, maxHeight);
            }
        });
    }

    @Override
    public void saveImage(Bitmap image, String id) {
        synchronized (sync) {
            FileOutputStream stream = null;
            try {
                stream = new FileOutputStream(idToPath(id));
                image.compress(Bitmap.CompressFormat.JPEG, 87, stream);
            } catch (Exception e) {
                e.printStackTrace();
                //Log.e("ContextImagePersistence", e);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        //Log.e("ContextImagePersistence", e);
                    }
                }
            }
        }
    }

    @Override
    public void saveImage(byte[] image, String id) {
        synchronized (sync) {
            FileOutputStream stream = null;
            try {
                stream = new FileOutputStream(idToPath(id));
                stream.write(image);
            } catch (Exception e) {
                e.printStackTrace();
                //Log.e("ContextImagePersistence", e);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        //Log.e("ContextImagePersistence", e);
                    }
                }
            }
        }
    }

    @Override
    public void clearPersistence() {
        File[] files = new File(context.getFilesDir().getAbsolutePath()).listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().startsWith("image_");
            }
        });

        for (File file : files) {
            file.delete();
        }
    }
}
