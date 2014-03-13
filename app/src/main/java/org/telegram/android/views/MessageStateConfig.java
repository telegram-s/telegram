package org.telegram.android.views;

import android.graphics.drawable.Drawable;

/**
 * Created by ex3ndr on 13.03.14.
 */
public class MessageStateConfig {
    private int radius;
    private int radiusHour;
    private int radiusMinute;
    private int clockColor;
    private int normalOutColor;
    private int normalInColor;
    private int errorColor;
    private Drawable check;
    private Drawable halfcheck;
    private Drawable error;

    public MessageStateConfig(int radius, int radiusHour, int radiusMinute, int clockColor, int normalOutColor, int normalInColor, int errorColor, Drawable check, Drawable halfcheck, Drawable error) {
        this.radius = radius;
        this.radiusHour = radiusHour;
        this.radiusMinute = radiusMinute;
        this.clockColor = clockColor;
        this.normalOutColor = normalOutColor;
        this.normalInColor = normalInColor;
        this.errorColor = errorColor;
        this.check = check;
        this.halfcheck = halfcheck;
        this.error = error;
    }

    public int getNormalInColor() {
        return normalInColor;
    }

    public int getNormalOutColor() {
        return normalOutColor;
    }

    public int getErrorColor() {
        return errorColor;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void setRadiusHour(int radiusHour) {
        this.radiusHour = radiusHour;
    }

    public void setRadiusMinute(int radiusMinute) {
        this.radiusMinute = radiusMinute;
    }

    public void setClockColor(int clockColor) {
        this.clockColor = clockColor;
    }

    public void setCheck(Drawable check) {
        this.check = check;
    }

    public void setHalfcheck(Drawable halfcheck) {
        this.halfcheck = halfcheck;
    }

    public void setError(Drawable error) {
        this.error = error;
    }

    public int getRadius() {
        return radius;
    }

    public int getRadiusHour() {
        return radiusHour;
    }

    public int getRadiusMinute() {
        return radiusMinute;
    }

    public int getClockColor() {
        return clockColor;
    }

    public Drawable getCheck() {
        return check;
    }

    public Drawable getHalfcheck() {
        return halfcheck;
    }

    public Drawable getError() {
        return error;
    }
}
