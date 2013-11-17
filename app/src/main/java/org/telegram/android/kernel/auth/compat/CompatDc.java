package org.telegram.android.kernel.auth.compat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ex3ndr on 17.11.13.
 */
public class CompatDc extends CompatContextPersistence implements Serializable {
    public static final int DEFAULT_DC = 1;

    private int nearestDC = DEFAULT_DC;

    private HashMap<Integer, ArrayList<CompatDcConfig>> configuration = new HashMap<Integer, ArrayList<CompatDcConfig>>();

    public HashMap<Integer, ArrayList<CompatDcConfig>> getConfiguration() {
        return configuration;
    }

    public int getNearestDC() {
        return nearestDC;
    }
}
