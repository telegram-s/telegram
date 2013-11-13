package org.telegram.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import org.telegram.android.screens.ScreenLogicType;
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

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
        setOpenAnimation();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        super.startActivityForResult(intent, requestCode, options);
        setOpenAnimation();
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        setOpenAnimation();
    }

    @Override
    public void finish() {
        super.finish();
        if (application.getScreenLogicType() == ScreenLogicType.MULTIPLE_CUSTOM) {
            overridePendingTransition(R.anim.activity_close_enter, R.anim.activity_close_exit);
        }
    }

    private void setOpenAnimation() {
        if (application.getScreenLogicType() == ScreenLogicType.MULTIPLE_CUSTOM) {
            overridePendingTransition(R.anim.activity_open_enter, R.anim.activity_open_exit);
        }
    }

    public void onBack(View view) {
        onBackPressed();
    }
}
