package org.telegram.android.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import org.telegram.android.R;

/**
 * Author: Korshakov Stepan
 * Created: 15.08.13 17:49
 */
public abstract class TelegramMapFragment extends TelegramFragment {
    private SharedPreferences preferences;
    private MapView mMapView;
    private GoogleMap mMap;
    private boolean isEnabled = false;

    public GoogleMap getMap() {
        return mMap;
    }

    public MapView getMapView() {
        return mMapView;
    }

    @Override
    public abstract View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

    protected View createMapView(int layout, LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(layout, container, false);
        preferences = getActivity().getSharedPreferences("org.telegram.android.maps", Context.MODE_PRIVATE);
        try {
            MapsInitializer.initialize(getActivity());
        } catch (GooglePlayServicesNotAvailableException e) {
            // TODO handle this situation
        }

        mMapView = (MapView) inflatedView.findViewById(R.id.map);

        try {
            MapsInitializer.initialize(application);
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }

        Bundle mapBundle = null;
        if (savedInstanceState != null) {
            mapBundle = savedInstanceState.getBundle("mapView");
        }

        mMapView.post(new Runnable() {
            @Override
            public void run() {
                mMapView.requestTransparentRegion(mMapView);
            }
        });
        mMapView.onCreate(mapBundle);

        final FrameLayout frame = new FrameLayout(getActivity());
        frame.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        frame.setBackgroundColor(Color.WHITE);
        frame.postDelayed(new Runnable() {
            @Override
            public void run() {
                AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
                fadeOut.setInterpolator(new AccelerateDecelerateInterpolator());
                fadeOut.setDuration(150);
                fadeOut.setFillEnabled(false);
                frame.startAnimation(fadeOut);
                frame.setVisibility(View.GONE);
            }
        }, 200);
        mMapView.addView(frame);

        setUpMapIfNeeded(inflatedView);

        return inflatedView;
    }

    private void setUpMapIfNeeded(View inflatedView) {
        mMap = ((MapView) inflatedView.findViewById(R.id.map)).getMap();
        if (mMap != null) {
            isEnabled = true;
            getMap().setMyLocationEnabled(true);
            getMap().setMapType(preferences.getInt("view_type", GoogleMap.MAP_TYPE_NORMAL));
            getMap().getUiSettings().setCompassEnabled(false);
            getMap().getUiSettings().setMyLocationButtonEnabled(false);
            setUpMap();
        } else {
            isEnabled = false;
            Toast.makeText(getActivity(), R.string.st_map_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    protected void switchViewType(int viewType) {
        preferences.edit().putInt("view_type", viewType).commit();
        if (getMap() != null) {
            getMap().setMapType(viewType);
        } else {
            Toast.makeText(getActivity(), R.string.st_map_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    protected void setUpMap() {

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.map_menu, menu);
        menu.findItem(R.id.hybridMap).setTitle(highlightMenuText(R.string.st_map_hybrid));
        menu.findItem(R.id.generalMap).setTitle(highlightMenuText(R.string.st_map_normal));
        menu.findItem(R.id.satelliteMap).setTitle(highlightMenuText(R.string.st_map_satellite));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.hybridMap) {
            switchViewType(GoogleMap.MAP_TYPE_HYBRID);
            return true;
        } else if (item.getItemId() == R.id.generalMap) {
            switchViewType(GoogleMap.MAP_TYPE_NORMAL);
            return true;
        } else if (item.getItemId() == R.id.satelliteMap) {
            switchViewType(GoogleMap.MAP_TYPE_SATELLITE);
            return true;
        } else if (item.getItemId() == R.id.moveToMyLocation) {
            if (getMap() != null) {
                Location myLoc = getMap().getMyLocation();
                if (myLoc == null) {
                    Toast.makeText(getActivity(), R.string.st_map_location_unavailable, Toast.LENGTH_SHORT).show();
                } else {
                    getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(myLoc.getLatitude(), myLoc.getLongitude()), 17));
                }
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMapView != null && isEnabled) {
            mMapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMapView != null && isEnabled) {
            mMapView.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMapView != null && isEnabled) {
            mMapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mMapView != null && isEnabled) {
            mMapView.onLowMemory();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapView != null) {
            Bundle bundle = new Bundle();
            mMapView.onSaveInstanceState(bundle);
            outState.putBundle("mapView", bundle);
        }
    }
}
