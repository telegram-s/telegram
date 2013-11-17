package org.telegram.android.kernel.compat;

import org.telegram.android.log.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.HashMap;

/**
 * Created by ex3ndr on 17.11.13.
 */
public class CompatObjectInputStream extends ObjectInputStream {
    private HashMap<String, String> classsesMap;

    public CompatObjectInputStream(InputStream input, HashMap<String, String> compat) throws IOException {
        super(input);
        this.classsesMap = compat;
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();
        Logger.d("CompatStream", resultClassDescriptor.getName());
        if (classsesMap.containsKey(resultClassDescriptor.getName())) {
            Class nclass = Class.forName(classsesMap.get(resultClassDescriptor.getName()));
            return ObjectStreamClass.lookup(nclass);
        } else {
            return resultClassDescriptor;
        }
    }
}
