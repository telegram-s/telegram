package org.telegram.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.widget.Button;
import android.widget.RelativeLayout;
import com.actionbarsherlock.view.Menu;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;
import org.telegram.android.base.TelegramMapFragment;
import org.telegram.android.R;
import org.telegram.android.core.model.media.TLLocalGeo;

import java.util.List;

/**
 * Author: Korshakov Stepan
 * Created: 14.08.13 18:07
 */
public class PickLocationFragment extends TelegramMapFragment {
    private Marker currentMarker;
    private double latitude;
    private double longitude;
    private Button sendButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            latitude = savedInstanceState.getDouble("latitude");
            longitude = savedInstanceState.getDouble("longitude");
        }
        setResult(Activity.RESULT_CANCELED, null);
        View res = createMapView(R.layout.map_pick, inflater, container, savedInstanceState);
        sendButton = (Button) res.findViewById(R.id.pickLocation);
        sendButton.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(Activity.RESULT_OK, new TLLocalGeo(latitude, longitude));
                getActivity().onBackPressed();
            }
        }));

        if (latitude != 0 && longitude != 0) {
            currentMarker = getMap().addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.st_map_pin)));
            sendButton.setEnabled(true);
        } else {
            sendButton.setEnabled(false);
        }
        return res;
    }

    @Override
    protected void setUpMap() {
        getMap().getUiSettings().setMyLocationButtonEnabled(false);
        getMap().getUiSettings().setZoomControlsEnabled(true);
        // Hack zoom controls
        View zoomControls = getMapView().findViewById(0x1);
        if (zoomControls != null && zoomControls.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            // ZoomControl is inside of RelativeLayout
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) zoomControls.getLayoutParams();

            // Align it to - parent top|left
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

            // Update margins, set to 10dp
            int marginBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, getResources().getDisplayMetrics());
            int marginRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
            params.setMargins(params.leftMargin, params.topMargin, marginRight, marginBottom);
        }

        getMap().setMyLocationEnabled(true);
        getMap().setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (currentMarker != null) {
                    currentMarker.remove();
                } else {
                    sendButton.setEnabled(true);
                }
                latitude = latLng.latitude;
                longitude = latLng.longitude;
                currentMarker = getMap().addMarker(new MarkerOptions()
                        .position(latLng)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.st_map_pin)));
            }
        });

        // My location
        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);

        Location l = null;
        for (int i = providers.size() - 1; i >= 0; i--) {
            l = lm.getLastKnownLocation(providers.get(i));
            if (l != null) break;
        }

        if (l != null) {
            getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(l.getLatitude(), l.getLongitude()), 17));
            getMap().setOnMyLocationChangeListener(null);
            if (latitude == 0 || longitude == 0) {
                latitude = l.getLatitude();
                longitude = l.getLongitude();
            }
        } else {
            getMap().setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(Location location) {
                    getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 17));
                    getMap().setOnMyLocationChangeListener(null);
                    if (currentMarker == null) {
                        sendButton.setEnabled(true);
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        currentMarker = getMap().addMarker(new MarkerOptions()
                                .position(new LatLng(latitude, longitude))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.st_map_pin)));
                    }
                }
            });
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_location_pick_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble("latitude", latitude);
        outState.putDouble("longitude", longitude);
    }
}
