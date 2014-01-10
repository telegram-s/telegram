package com.extradea.framework.images;

import com.extradea.framework.images.tasks.ImageTask;

/**
 * Author: Korshakov Stepan
 * Created: 01.07.13 9:20
 */
public interface ImageTaskListener {
    public void onTaskEvent(ImageTask srcTask, int eventId, Object[] args);
}
