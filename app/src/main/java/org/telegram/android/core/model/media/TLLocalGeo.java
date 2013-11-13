package org.telegram.android.core.model.media;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 17.10.13
 * Time: 22:03
 */
public class TLLocalGeo extends TLObject {

    public static final int CLASS_ID = 0x224a5495;

    private double latitude;
    private double longitude;

    public TLLocalGeo(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public TLLocalGeo() {

    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeDouble(latitude, stream);
        writeDouble(longitude, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        latitude = readDouble(stream);
        longitude = readDouble(stream);
    }
}
