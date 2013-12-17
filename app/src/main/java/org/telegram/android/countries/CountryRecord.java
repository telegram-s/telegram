package org.telegram.android.countries;

/**
 * Author: Korshakov Stepan
 * Created: 31.07.13 6:04
 */
public class CountryRecord {
    private String title;
    private String iso;
    private int callPrefix;
    private boolean searchByCode;
    private boolean searchByName;

    public CountryRecord(String title, String iso, int callPrefix, boolean searchByCode,boolean searchByName) {
        this.title = title;
        this.iso = iso;
        this.callPrefix = callPrefix;
        this.searchByCode = searchByCode;
        this.searchByName = searchByName;
    }

    public CountryRecord(String title, String iso, int callPrefix) {
        this.title = title;
        this.iso = iso;
        this.callPrefix = callPrefix;
        this.searchByName = true;
        this.searchByCode = true;
    }

    public boolean isSearchByCode() {
        return searchByCode;
    }

    public boolean isSearchByName() {
        return searchByName;
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
