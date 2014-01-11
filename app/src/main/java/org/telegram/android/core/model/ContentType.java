package org.telegram.android.core.model;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 18:25
 */
public class ContentType {
    public static final int MESSAGE_TEXT = 0;
    public static final int MESSAGE_PHOTO = 1;
    public static final int MESSAGE_VIDEO = 2;
    public static final int MESSAGE_SYSTEM = 4;
    public static final int MESSAGE_GEO = 5;
    public static final int MESSAGE_CONTACT = 6;
    public static final int MESSAGE_UNKNOWN = 7;
    public static final int MESSAGE_DOCUMENT = 8;
    public static final int MESSAGE_AUDIO = 9;
    public static final int MESSAGE_DOC_PREVIEW = 10;
    public static final int MESSAGE_DOC_ANIMATED = 11;
    public static final int CONTENT_MASK = 0xFF;
    public static final int MESSAGE_FORWARDED = 1024;
}
