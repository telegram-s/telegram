package org.telegram.android.kernel.compat.v6;

import org.telegram.android.kernel.compat.CompatContextPersistence;

/**
 * Created by ex3ndr on 10.12.13.
 */
public class CompatVersionHolder extends CompatContextPersistence {
    protected int prevVersionInstalled;
    protected int currentVersionInstalled;
    protected boolean wasUpgraded;

    public int getPrevVersionInstalled() {
        return prevVersionInstalled;
    }

    public int getCurrentVersionInstalled() {
        return currentVersionInstalled;
    }

    public boolean isWasUpgraded() {
        return wasUpgraded;
    }
}
