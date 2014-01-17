package org.telegram.android;

import android.os.Bundle;
import android.view.View;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import org.telegram.android.config.UserSettings;
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

    public void setBarBg() {
        switch (application.getSettingsKernel().getUserSettings().getBarColor()) {
            default:
            case UserSettings.BAR_COLOR_DEFAULT:
                getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.st_bar_bg));
                break;
            case UserSettings.BAR_COLOR_CYAN:
                getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.st_bar_bg_cyan));
                break;
            case UserSettings.BAR_COLOR_PURPLE:
                getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.st_bar_bg_purple));
                break;
            case UserSettings.BAR_COLOR_GREEN:
                getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.st_bar_bg_green));
                break;
            case UserSettings.BAR_COLOR_RED:
                getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.st_bar_bg_red));
                break;
            case UserSettings.BAR_COLOR_WA:
                getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.st_bar_bg_wa));
                break;
            case UserSettings.BAR_COLOR_MAGENTA:
                getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.st_bar_bg_magenta));
                break;
            case UserSettings.BAR_COLOR_LINE:
                getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.st_bar_bg_line));
                break;
        }
        invalidateOptionsMenu();
    }

    public void onBack(View view) {
        onBackPressed();
    }
}
