package com.extradea.framework.images.tasks;

/**
 * Author: Korshakov Stepan
 * Created: 15.08.13 14:25
 */
public class VideoThumbTask extends ImageTask {
    private String fileName;
    private int kind;

    public VideoThumbTask(String fileName, int kind) {
        this.fileName = fileName;
        this.kind = kind;
    }

    public String getFileName() {
        return fileName;
    }

    public int getKind() {
        return kind;
    }

    @Override
    protected String getKeyImpl() {
        return "video-thumb:" + getMaxWidth() + ":" + getMaxHeight() + ":" + isFillRect() + ":" + kind + ":" + fileName;
    }
}
