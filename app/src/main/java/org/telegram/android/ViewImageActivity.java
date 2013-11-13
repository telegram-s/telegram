package org.telegram.android;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.fragments.SingleImagePreviewFragment;

/**
 * Author: Korshakov Stepan
 * Created: 12.09.13 1:52
 */
public class ViewImageActivity extends StelsActivity {

    public static Intent createIntent(TLLocalFileLocation location, Context context) {
        Intent res = new Intent();
        res.setClass(context, ViewImageActivity.class);
        res.putExtra("location", location);
        return res;
    }

    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.images_container);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.st_photo_panel)));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setTitle(R.string.st_image_title);

        if (savedInstanceState == null) {
            TLLocalFileLocation location = (TLLocalFileLocation) getIntent().getSerializableExtra("location");
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.imagesContainer, new SingleImagePreviewFragment(location))
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}