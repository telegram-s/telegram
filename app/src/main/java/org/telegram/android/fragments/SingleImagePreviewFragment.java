package org.telegram.android.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.telegram.android.R;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.preview.AvatarLoader;
import org.telegram.android.preview.ImageHolder;
import org.telegram.android.preview.ImageReceiver;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Author: Korshakov Stepan
 * Created: 12.09.13 1:49
 */
public class SingleImagePreviewFragment extends TelegramFragment implements ImageReceiver {
    private TLLocalFileLocation location;
    private ImageHolder mediaHolder;
    private PhotoView imageView;

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
        View res = wrap(inflater).inflate(R.layout.view_image, container, false);
        imageView = (PhotoView) res.findViewById(R.id.previewImage);

        application.getUiKernel().getAvatarLoader().requestAvatar(location, AvatarLoader.TYPE_FULL, this);
        imageView.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float x, float y) {
                if (getSherlockActivity() != null && getSherlockActivity().getSupportActionBar() != null) {
                    if (getSherlockActivity().getSupportActionBar().isShowing()) {
                        getSherlockActivity().getSupportActionBar().hide();
                    } else {
                        getSherlockActivity().getSupportActionBar().show();
                    }
                }
            }
        });
        return res;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("location", location);
    }

    @Override
    public void onImageReceived(ImageHolder mediaHolder, boolean intermediate) {
        if (imageView != null) {
            imageView.setImageBitmap(mediaHolder.getBitmap());
            this.mediaHolder = mediaHolder;
        } else {
            mediaHolder.release();
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        application.getUiKernel().getAvatarLoader().cancelRequest(this);
        if (mediaHolder != null) {
            mediaHolder.release();
            mediaHolder = null;
        }
        imageView = null;
    }
}
