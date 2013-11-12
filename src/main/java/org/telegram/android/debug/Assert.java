package org.telegram.android.debug;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 08.10.13
 * Time: 15:55
 */
public class Assert {
    public static final boolean ENABLED = false;

    public static void assertEquals(Object o1, Object o2) {
        if (o1 == null && o2 == null) {
            return;
        }

        if (o1 == null || o2 == null) {
            if (o1 == null) {
                throw new AssertionError("o1 is null, but o2 is not");
            } else {
                throw new AssertionError("o2 is null, but o1 is not");
            }
        }

        if (!o1.equals(o2)) {
            throw new AssertionError("o2 is not equals o1");
        }
    }
}
