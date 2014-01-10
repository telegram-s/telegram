/*
 * Copyright (c) 2013 Extradea LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.extradea.framework.images.workers;

import com.extradea.framework.images.ImageController;
import com.extradea.framework.images.tasks.CustomImageTask;
import com.extradea.framework.images.tasks.ImageTask;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 31.07.12
 * Time: 1:48
 */
public class CustomWorker implements ImageWorker {

    @Override
    public boolean acceptTask(ImageTask task, ImageController controller) {
        return task instanceof CustomImageTask;
    }

    @Override
    public int processTask(ImageTask task, ImageController controller) {
        CustomImageTask custom = (CustomImageTask) task;
        try {
            CustomImageTask.CustomImageTaskResult result = custom.process();
            task.setBinaryResult(result.getBinaryResult());
            task.setResult(result.getResult());
            return RESULT_OK;
        } catch (Exception e) {
            e.printStackTrace();
            return RESULT_FAILURE;
        }
    }

    @Override
    public boolean isPausable() {
        return true;
    }
}
