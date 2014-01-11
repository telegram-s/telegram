package org.telegram.android.activity;

import android.app.Activity;
import android.os.Bundle;
import com.actionbarsherlock.view.Window;
import com.edmodo.cropper.CropImageView;
import org.telegram.android.StelsActivity;
import org.telegram.android.media.Optimizer;

/**
 * Created by ex3ndr on 10.01.14.
 */
public class CropImageActivity extends StelsActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CropImageView imageView = new CropImageView(this);
    }
}