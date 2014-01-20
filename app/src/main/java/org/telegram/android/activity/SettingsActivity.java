package org.telegram.android.activity;

import android.os.Bundle;
import org.telegram.android.R;
import org.telegram.android.base.TelegramActivity;
import org.telegram.android.fragments.SettingsFragment;

/**
 * Created by ex3ndr on 11.01.14.
 */
public class SettingsActivity extends TelegramActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setBarBg();

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragmentContainer, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        setBarBg();
    }
}