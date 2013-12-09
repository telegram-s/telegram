package org.telegram.android.critical;

import android.content.Context;
import org.telegram.android.log.Logger;
import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 09.11.13
 * Time: 0:29
 */
public class TLPersistence<T extends TLObject> {

    private static final String TAG = "KernelPersistence";

    private Class<T> destClass;
    private TLContext context;
    private SafeFileWriter writer;
    private T obj;

    public TLPersistence(Context context, String fileName, Class<T> destClass, TLContext tlContext) {
        this.destClass = destClass;
        this.context = tlContext;
        this.writer = new SafeFileWriter(context, fileName);

        long start = System.currentTimeMillis();
        byte[] data = writer.loadData();
        Logger.d(TAG, "Loaded state in " + (System.currentTimeMillis() - start) + " ms");
        if (data != null) {
            try {
                ByteArrayInputStream stream = new ByteArrayInputStream(data);
                obj = (T) this.context.deserializeMessage(stream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (obj == null) {
            try {
                obj = destClass.newInstance();
            } catch (Exception e1) {
                throw new RuntimeException("Unable to instantiate default settings");
            }
        }

        afterLoaded();
    }

    protected void afterLoaded() {

    }

    public void write() {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            obj.serialize(stream);
            stream.close();
            writer.saveData(stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public T getObj() {
        return obj;
    }
}
