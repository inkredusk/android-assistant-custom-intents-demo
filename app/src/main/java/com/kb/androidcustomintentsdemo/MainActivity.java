package com.kb.androidcustomintentsdemo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.AvoidType;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        if(mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.i(TAG, "inside on map ready");
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i(TAG, "inside on new intent");
        if (intent != null) {
            Uri data = intent.getData();
            String action = intent.getAction();
            Log.i(TAG, "Action ==>"+action);
            Log.i(TAG, "data=>" +data.toString());

            switch(action) {
                case Intent.ACTION_VIEW:
                    handleDeepLink(action, data);
                break;
                default:
                    // Do nothing
                    // Show default view
                break;
            }

        }
    }

    /**
     * Function to handle deep links
     * @param action
     * @param data
     */
    private void handleDeepLink(String action, Uri data) {
        Log.i(TAG, "inside handle deep link");
        String originLocationName = data.getQueryParameter("origin");
        String destinationLocationName = data.getQueryParameter("dest");

        Log.i(TAG, "origin location ==>"+originLocationName);
        Log.i(TAG, "destination location ==>"+destinationLocationName);

        LatLng source = getLatitudeLongitude(originLocationName).get(0);
        LatLng destination = getLatitudeLongitude(destinationLocationName).get(0);

        if(source != null && destination != null) {
            // Get fastest route
            getFastestRoute(source, destination);
        }
    }

    /**
     * Function to get fastest route
     * @param source
     * @param destination
     */
    private void getFastestRoute(LatLng source, LatLng destination) {
        Log.i(TAG, "inside fastest route");
        try {
            GoogleDirection.withServerKey(AppConstants.GOOGLE_MAP_API_KEY)
                    .from(source)
                    .to(destination)
                    .avoid(AvoidType.TOLLS)
                    .avoid(AvoidType.FERRIES)
                    .avoid(AvoidType.HIGHWAYS)
                    .execute(new DirectionCallback() {
                        @Override public void onDirectionSuccess(@Nullable Direction direction) {
                            Log.i(TAG, "Direction success");
                            if (direction.isOK()) {
                                Route route = direction.getRouteList().get(0);
                                googleMap.addMarker(new MarkerOptions().position(source));
                                googleMap.addMarker(new MarkerOptions().position(destination));
                                ArrayList<LatLng> arrDirectionList =
                                        route.getLegList().get(0).getDirectionPoint();
                                googleMap.addPolyline(DirectionConverter.createPolyline
                                        (MainActivity.this, arrDirectionList, 4, Color.RED));
                                setCameraWithinCoordinateBounds(route);
                            }
                        }

                        @Override public void onDirectionFailure(@NonNull Throwable t) {
                            Log.i(TAG, "Direction failure");
                        }
                    });
        }catch(Exception ex){
            Log.e(TAG, "error while fetching fastest route", ex);
        }
    }

    /**
     * Function to set camera within bounds
     * @param route
     */
    private void setCameraWithinCoordinateBounds(Route route) {
        Log.i(TAG, "inside setCameraWithinCoordinateBounds");
        LatLng southwest = route.getBound().getSouthwestCoordination().getCoordination();
        LatLng northeast = route.getBound().getNortheastCoordination().getCoordination();
        LatLngBounds latLngBounds = new LatLngBounds(southwest, northeast);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100));
    }

    /**
     * Function to extract latitude and longitude using Geocoder API
     * @param location
     * @return
     */
    private List<LatLng> getLatitudeLongitude(String location) {
        Log.i(TAG, "inside getLatitudeLongitude");
        List<LatLng> arrLatLng = null;
        try {
            Geocoder gc = new Geocoder(this);
            List<Address> addresses = gc.getFromLocationName(location, AppConstants.MAXIMUM_SEARCH_RESULTS);
            arrLatLng = new ArrayList<>(addresses.size());
            for (Address a : addresses) {
                if (a.hasLatitude() && a.hasLongitude()) {
                    arrLatLng.add(new LatLng(a.getLatitude(), a.getLongitude()));
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error occurred when fetching latitude longitude");
        }
        return arrLatLng;
    }
}
