package org.telegram.android.activity;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import com.actionbarsherlock.view.MenuItem;
import org.telegram.android.R;
import org.telegram.android.base.TelegramActivity;
import org.telegram.android.core.model.WebSearchResult;
import org.telegram.android.fragments.WebSearchFragment;
import org.telegram.android.fragments.WebSearchPreviewFragment;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by ex3ndr on 14.03.14.
 */
public class PickWebImageActivity extends TelegramActivity {

    private boolean isStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setBarBg(true);
        getSupportActionBar().setLogo(R.drawable.st_bar_logo);
        getSupportActionBar().setIcon(R.drawable.st_bar_logo);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().show();

        setContentView(R.layout.activity_web_search);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragmentContainer, new WebSearchFragment())
                    .commit();
        }
    }

    public void openPreview(WebSearchResult searchResult) {
        FragmentTransaction transaction;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            transaction = getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit);
        } else {
            transaction = getSupportFragmentManager().beginTransaction();
        }
        transaction
                .addToBackStack(null)
                .replace(R.id.fragmentContainer, new WebSearchPreviewFragment(searchResult))
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setBarBg(!isStarted);
        isStarted = true;
    }
}
