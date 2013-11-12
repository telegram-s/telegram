package org.telegram.android.countries;

/**
 * Author: Korshakov Stepan
 * Created: 31.07.13 6:04
 */
public class CountryRecord {
    private String title;
    private String iso;
    private int callPrefix;
    private boolean disabled;

    public CountryRecord(String title, String iso, int callPrefix, boolean disabled) {
        this.title = title;
        this.iso = iso;
        this.callPrefix = callPrefix;
        this.disabled = disabled;
    }

    public CountryRecord(String title, String iso, int callPrefix) {
        this.title = title;
        this.iso = iso;
        this.callPrefix = callPrefix;
        this.disabled = false;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public String getTitle() {
        return title;
    }

    public String getIso() {
        return iso;
    }

    public int getCallPrefix() {
        return callPrefix;
    }

    @Override
    public String toString() {
        return title;
    }
}
