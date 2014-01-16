package org.telegram.android;

import android.os.Bundle;
import android.view.View;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import org.telegram.android.ui.FontController;

/**
 * Author: Korshakov Stepan
 * Created: 24.07.13 17:11
 */
public class StelsActivity extends SherlockFragmentActivity {

    protected StelsApplication application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FontController.initInflater(this);
        super.onCreate(savedInstanceState);
        application = (StelsApplication) getApplicationContext();
    }

    public void onBack(View view) {
        onBackPressed();
    }
}
