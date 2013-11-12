package com.extradea.framework.persistence;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 17.09.13
 * Time: 17:29
 * To change this template use File | Settings | File Templates.
 */
public class WeakObjectInputStream extends ObjectInputStream {

    public WeakObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass resultClassDescriptor = super.readClassDescriptor(); // initially streams descriptor
        Class localClass; // the class in the local JVM that this descriptor represents.
        try {
            localClass = Class.forName(resultClassDescriptor.getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return resultClassDescriptor;
        }
        ObjectStreamClass localClassDescriptor = ObjectStreamClass.lookup(localClass);
        if (localClassDescriptor != null) { // only if class implements serializable
            final long localSUID = localClassDescriptor.getSerialVersionUID();
            final long streamSUID = resultClassDescriptor.getSerialVersionUID();
            if (streamSUID != localSUID) { // check for serialVersionUID mismatch.
                final StringBuffer s = new StringBuffer("Overriding serialized class version mismatch: ");
                s.append("local serialVersionUID = ").append(localSUID);
                s.append(" stream serialVersionUID = ").append(streamSUID);
                System.err.println(s);
                resultClassDescriptor = localClassDescriptor; // Use local class descriptor for deserialization
            }
        }
        return resultClassDescriptor;
    }
}
