package org.telegram.android.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import org.telegram.android.R;
import org.telegram.android.base.TelegramActivity;
import org.telegram.android.fragments.ImagePreviewFragment;

/**
 * Author: Korshakov Stepan
 * Created: 22.08.13 16:41
 */
public class ViewImagesActivity extends TelegramActivity {
    public static Intent createIntent(int mid, int peerType, int peerId, Context context) {
        Intent res = new Intent();
        res.setClass(context, ViewImagesActivity.class);
        res.putExtra("mid", mid);
        res.putExtra("peerType", peerType);
        res.putExtra("peerId", peerId);
        return res;
    }

    private int mid;
    private int peerType;
    private int peerId;

    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);

        if (!application.getKernelsLoader().isLoaded()) {
            finish();
            return;
        }

        setContentView(R.layout.images_container);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.st_photo_panel)));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        mid = getIntent().getIntExtra("mid", 0);
        peerType = getIntent().getIntExtra("peerType", 0);
        peerId = getIntent().getIntExtra("peerId", 0);


        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.imagesContainer, new ImagePreviewFragment(mid, peerType, peerId))
                    .commit();
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
}