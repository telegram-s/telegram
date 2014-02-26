package org.telegram.android.preview.media;

/**
* Created by ex3ndr on 22.02.14.
*/
public class MediaGeoTask extends BaseTask {
    private double latitude;
    private double longitude;

    public MediaGeoTask(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public String getKey() {
        return "geo:" + latitude + "," + longitude;
    }
}
