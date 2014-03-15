package org.telegram.android.base;

import android.os.Bundle;
import android.view.View;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.ActionBarSherlockCompat;
import com.actionbarsherlock.internal.ActionBarSherlockNative;
import org.telegram.android.R;
import org.telegram.android.TelegramApplication;
import org.telegram.android.config.UserSettings;
import org.telegram.android.ui.FontController;

import java.lang.reflect.Field;

/**
 * Author: Korshakov Stepan
 * Created: 24.07.13 17:11
 */
public class TelegramActivity extends SherlockFragmentActivity {

    protected TelegramApplication application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FontController.initInflater(this);
        super.onCreate(savedInstanceState);
        application = (TelegramApplication) getApplicationContext();
    }

    public void setBarBg() {
        setBarBg(true);
    }

    public void setBarBg(boolean fromStart) {
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
        if (!fromStart) {
            invalidateOptionsMenu();
        }
    }

    public void fixBackButton() {
        if (android.os.Build.VERSION.SDK_INT == 19) {
            //workaround for back button dissapear
            try {
                Class firstClass = getSupportActionBar().getClass();
                Class aClass = firstClass;
                Field field = aClass.getDeclaredField("mActionBar");
                field.setAccessible(true);
                android.app.ActionBar bar = (android.app.ActionBar) field.get(getSupportActionBar());

                field = bar.getClass().getDeclaredField("mActionView");
                field.setAccessible(true);
                View v = (View) field.get(bar);
                aClass = v.getClass();

                field = aClass.getDeclaredField("mHomeLayout");
                field.setAccessible(true);
                v = (View) field.get(v);
                v.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void onBack(View view) {
        onBackPressed();
    }
}
