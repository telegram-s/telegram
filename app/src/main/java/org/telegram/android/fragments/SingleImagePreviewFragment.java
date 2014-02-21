package org.telegram.android.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.telegram.android.R;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.core.model.media.TLLocalFileLocation;

/**
 * Author: Korshakov Stepan
 * Created: 12.09.13 1:49
 */
public class SingleImagePreviewFragment extends TelegramFragment {
    private TLLocalFileLocation location;

    public SingleImagePreviewFragment(TLLocalFileLocation location) {
        this.location = location;
    }

    public SingleImagePreviewFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            location = (TLLocalFileLocation) savedInstanceState.getSerializable("location");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            location = (TLLocalFileLocation) savedInstanceState.getSerializable("location");
        }
        View res = inflater.inflate(R.layout.view_image, container, false);
        // TODO: Implement
//        PhotoImageView photoImageView = (PhotoImageView) res.findViewById(R.id.previewImage);
//        photoImageView.setImageTask(new StelsImageTask(location));
//        photoImageView.getPhotoViewAttacher().setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
//            @Override
//            public void onViewTap(View view, float x, float y) {
//                if (getSherlockActivity() != null && getSherlockActivity().getSupportActionBar() != null) {
//                    if (getSherlockActivity().getSupportActionBar().isShowing()) {
//                        getSherlockActivity().getSupportActionBar().hide();
//                    } else {
//                        getSherlockActivity().getSupportActionBar().show();
//                    }
//                }
//            }
//        });
        return res;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("location", location);
    }
}
