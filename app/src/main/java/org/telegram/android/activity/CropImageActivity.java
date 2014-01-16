package org.telegram.android.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.edmodo.cropper.CropImageView;
import org.telegram.android.R;
import org.telegram.android.StelsActivity;
import org.telegram.android.media.Optimizer;

import java.io.IOException;

/**
 * Created by ex3ndr on 10.01.14.
 */
public class CropImageActivity extends StelsActivity {

    public static Intent cropIntent(String fileName, int ratioX, int ratioY, String destFileName, Context context) {
        Intent res = new Intent();
        res.setClass(context, CropImageActivity.class);
        res.putExtra(EXTRA_FILENAME, fileName);
        res.putExtra(EXTRA_OUT_FILENAME, destFileName);
        res.putExtra(EXTRA_RATIO_X, ratioX);
        res.putExtra(EXTRA_RATIO_Y, ratioY);
        return res;
    }

    public static Intent cropUriIntent(Uri fileName, int ratioX, int ratioY, String destFileName, Context context) {
        Intent res = new Intent();
        res.setClass(context, CropImageActivity.class);
        res.putExtra(EXTRA_STREAM, fileName.toString());
        res.putExtra(EXTRA_OUT_FILENAME, destFileName);
        res.putExtra(EXTRA_RATIO_X, ratioX);
        res.putExtra(EXTRA_RATIO_Y, ratioY);
        return res;
    }

    private static final String EXTRA_FILENAME = "fileName";
    private static final String EXTRA_STREAM = "stream";
    private static final String EXTRA_OUT_FILENAME = "outFileName";
    private static final String EXTRA_RATIO_X = "ratioX";
    private static final String EXTRA_RATIO_Y = "ratioY";
    private static final String EXTRA_MIN_W = "minW";
    private static final String EXTRA_MIN_H = "minH";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setTitle("Crop photo");
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayUseLogoEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_image);

        final CropImageView imageView = (CropImageView) findViewById(R.id.cropImage);

        if (getIntent().getExtras().containsKey(EXTRA_FILENAME)) {
            try {
                String fileName = getIntent().getExtras().getString(EXTRA_FILENAME);
                imageView.setImageBitmap(Optimizer.optimize(fileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                String uri = getIntent().getExtras().getString(EXTRA_STREAM);
                imageView.setImageBitmap(Optimizer.optimize(uri, this));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        imageView.setAspectRatio(
                getIntent().getIntExtra(EXTRA_RATIO_X, 1),
                getIntent().getIntExtra(EXTRA_RATIO_Y, 1));
        imageView.setFixedAspectRatio(true);

        findViewById(R.id.doCropImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap res = imageView.getCroppedImage();
                int minW = getIntent().getIntExtra(EXTRA_MIN_W, 160);
                int minH = getIntent().getIntExtra(EXTRA_MIN_H, 160);
                res = Optimizer.scaleForMinimumSize(res, minW, minH);
                try {
                    Optimizer.save(res, getIntent().getStringExtra(EXTRA_OUT_FILENAME));
                    setResult(RESULT_OK);
                    finish();
                } catch (IOException e) {
                    Toast.makeText(CropImageActivity.this, "Unable to save photo", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}