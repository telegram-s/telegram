package com.extradea.framework.images.tasks;

/**
 * Created by ex3ndr on 17.01.14.
 */
public class ScaleTask extends ImageTask {

    private String srcKey;
    private int w;
    private int h;

    public ScaleTask(ImageTask task, int w, int h) {
        task.setPutInMemoryCache(false);
        setPutInDiskCache(false);
        setRequiredTasks(new ImageTask[]{task});
        this.srcKey = task.getKey();
        this.w = w;
        this.h = h;
    }

    public String getSrcKey() {
        return srcKey;
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }

    @Override
    public boolean skipDiskCacheCheck() {
        return true;
    }

    @Override
    public String getKeyImpl() {
        return "Scale(" + w + "," + h + "):" + srcKey;
    }
}
