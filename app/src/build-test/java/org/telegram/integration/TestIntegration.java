package org.telegram.integration;

import org.telegram.android.StelsActivity;
import net.hockeyapp.android.UpdateManager;

/**
 * Created by ex3ndr on 22.11.13.
 */
public class TestIntegration {
    public static void initActivity(StelsActivity activity) {
        UpdateManager.register(this, "446434345de6d88bda2ff7c2d77b255c");
    }
}