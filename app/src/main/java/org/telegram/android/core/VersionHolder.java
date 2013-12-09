package org.telegram.android.core;

import android.content.Context;
import com.extradea.framework.persistence.ContextPersistence;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 31.10.13
 * Time: 19:09
 */
public class VersionHolder extends ContextPersistence {

    static final long serialVersionUID = 2587807235563473358L;

    protected int prevVersionInstalled;
    protected int currentVersionInstalled;
    protected boolean wasUpgraded;

    public VersionHolder(Context context) {
        super(context);
    }

    public int getPrevVersionInstalled() {
        return prevVersionInstalled;
    }

    public int getCurrentVersionInstalled() {
        return currentVersionInstalled;
    }

    public boolean isWasUpgraded() {
        return wasUpgraded;
    }

    public void setWasUpgraded(boolean wasUpgraded) {
        this.wasUpgraded = wasUpgraded;
        this.prevVersionInstalled = currentVersionInstalled;
    }

    public void markFinishedUpgrade() {
        this.wasUpgraded = false;
        this.prevVersionInstalled = 0;
        trySave();
    }

    public void saveCurrentVersion(int versionCode) {
        this.currentVersionInstalled = versionCode;
        trySave();
    }
}
