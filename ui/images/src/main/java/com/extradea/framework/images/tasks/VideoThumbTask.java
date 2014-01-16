package com.extradea.framework.images.tasks;

/**
 * Author: Korshakov Stepan
 * Created: 15.08.13 14:25
 */
public class VideoThumbTask extends ImageTask {
    private String fileName;

    public VideoThumbTask(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    protected String getKeyImpl() {
        return "video-thumb:" + fileName;
    }
}
