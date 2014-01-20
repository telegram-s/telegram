package org.telegram.android.fragments;

import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.actionbarsherlock.view.Menu;
import com.extradea.framework.images.ui.FastWebImageView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import org.telegram.android.base.TelegramMapFragment;
import org.telegram.android.R;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.ui.Placeholders;
import org.telegram.android.ui.TextUtil;

/**
 * Author: Korshakov Stepan
 * Created: 15.08.13 19:32
 */
public class ViewLocationFragment extends TelegramMapFragment {
    private int uid;
    private double latitude;
    private double longitude;

    private FastWebImageView avatarImage;
    private TextView userNameView;
    private TextView locationView;

    public ViewLocationFragment(int uid, double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.uid = uid;
    }

    public ViewLocationFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            latitude = savedInstanceState.getDouble("latitude");
            longitude = savedInstanceState.getDouble("longitude");
            uid = savedInstanceState.getInt("uid");
        }
        View res = createMapView(R.layout.map_view, inflater, container, savedInstanceState);
        avatarImage = (FastWebImageView) res.findViewById(R.id.avatar);
        userNameView = (TextView) res.findViewById(R.id.name);
        locationView = (TextView) res.findViewById(R.id.location);
        res.findViewById(R.id.bottomPanel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRootController().openUser(uid);
            }
        });
        User user = getEngine().getUser(uid);
        userNameView.setText(user.getDisplayName());
        locationView.setText(R.string.st_location_unavailable);
        avatarImage.setLoadingDrawable(Placeholders.getUserPlaceholder(uid));
        if (user.getPhoto() instanceof TLLocalAvatarPhoto) {
            TLLocalAvatarPhoto avatarPhoto = (TLLocalAvatarPhoto) user.getPhoto();
            if (avatarPhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                avatarImage.requestTask(new StelsImageTask((TLLocalFileLocation) avatarPhoto.getPreviewLocation()));
            } else {
                avatarImage.requestTask(null);
            }
        } else {
            avatarImage.requestTask(null);
        }
        return res;
    }

    @Override
    protected void setUpMap() {

        // Hack zoom controls
        View zoomControls = getMapView().findViewById(0x1);
        if (zoomControls != null && zoomControls.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            // ZoomControl is inside of RelativeLayout
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) zoomControls.getLayoutParams();

            // Align it to - parent top|left
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

            // Update margins, set to 10dp
            int marginBottom = getPx(100);
            int marginRight = getPx(12);
            params.setMargins(params.leftMargin, params.topMargin, marginRight, marginBottom);
        }

        getMap().addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.st_map_pin)));
        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 17));

        getMap().setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                updateDistance(location.getLatitude(), location.getLongitude());
            }
        });

        Location myLoc = getMap().getMyLocation();
        if (myLoc != null) {
            updateDistance(myLoc.getLatitude(), myLoc.getLongitude());
        }
    }

    private void updateDistance(double lat, double lon) {
        float[] res = new float[1];
        Location.distanceBetween(latitude, longitude, lat, lon, res);
        if (locationView != null) {
            locationView.setText(TextUtil.formatDistance(res[0]));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_location_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble("longitude", longitude);
        outState.putDouble("latitude", latitude);
        outState.putInt("uid", uid);
    }
}
