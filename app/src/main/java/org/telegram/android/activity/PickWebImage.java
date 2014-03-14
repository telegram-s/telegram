package org.telegram.android.activity;

import android.os.Bundle;
import org.telegram.android.R;
import org.telegram.android.base.TelegramActivity;
import org.telegram.android.fragments.WebSearchFragment;

/**
 * Created by ex3ndr on 14.03.14.
 */
public class PickWebImage extends TelegramActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().show();

        setContentView(R.layout.activity_web_search);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragmentContainer, new WebSearchFragment())
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setBarBg();
    }
}
