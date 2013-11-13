package org.telegram.android.auth.compat;

import org.telegram.android.auth.AuthCredentials;

import java.io.*;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 17.09.13
 * Time: 22:25
 */
public class OldAuthCompatStream extends ObjectInputStream {
    private HashMap<String, String> classsesMap;

    public OldAuthCompatStream(InputStream input, HashMap<String, String> compat) throws IOException {
        super(input);
        this.classsesMap = compat;
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();
        if (classsesMap.containsKey(resultClassDescriptor.getName())) {
            Class nclass = Class.forName(classsesMap.get(resultClassDescriptor.getName()));
            return ObjectStreamClass.lookup(nclass);
        } else {
            return resultClassDescriptor;
        }
    }
}
