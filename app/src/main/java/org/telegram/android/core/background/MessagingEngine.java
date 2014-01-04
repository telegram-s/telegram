package org.telegram.android.core.background;

import org.telegram.android.StelsApplication;
import org.telegram.android.core.engines.ModelEngine;

/**
 * Created by ex3ndr on 04.01.14.
 */
public class MessagingEngine {
    private ModelEngine engine;
    private StelsApplication application;

    public MessagingEngine(StelsApplication application, ModelEngine engine) {
        this.engine = engine;
    }
}
