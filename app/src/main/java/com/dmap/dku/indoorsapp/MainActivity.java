package com.dmap.dku.indoorsapp;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;

import com.customlbs.coordinates.GeoCoordinate;
import com.customlbs.library.IndoorsException;
import com.customlbs.library.IndoorsFactory;
import com.customlbs.surface.library.IndoorsSurfaceFactory;
import com.customlbs.surface.library.IndoorsSurfaceFragment;
import com.customlbs.shared.Coordinate;
import com.customlbs.library.callbacks.RoutingCallback;

import com.customlbs.library.IndoorsLocationListener;
import com.customlbs.library.callbacks.LoadingBuildingStatus;
import com.customlbs.library.model.Building;
import com.customlbs.library.model.Zone;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements IndoorsLocationListener {

    public static final int REQUEST_CODE_LOCATION = 795523136; //Random request code, use your own
    public static final int REQUEST_CODE_PERMISSIONS = 34168; //Random request code, use your own
    private IndoorsSurfaceFragment indoorsSurfaceFragment;
    private Toast progressToast;
    private static int lastProgress = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);

//        Coordinate start = new Coordinate(1234, 1234, 0);
//        Coordinate end = new Coordinate(12345, 12345, 0);
//
//        IndoorsFactory.Builder indoorsBuilder = new IndoorsFactory.Builder();
//        IndoorsSurfaceFactory.Builder surfaceBuilder = new IndoorsSurfaceFactory.Builder();
//
//        indoorsBuilder.setContext(this);
//
//        // TODO: replace this with your API-key
//        indoorsBuilder.setApiKey("91c2793f-993f-454f-8802-96a3fe8cdb3c");
//
//        // TODO: replace 12345 with the id of the building you uploaded to
//        // our cloud using the MMT
//        indoorsBuilder.setBuildingId((long) 795523136);
//
//        surfaceBuilder.setIndoorsBuilder(indoorsBuilder);
//
//        final IndoorsSurfaceFragment indoorsFragment = surfaceBuilder.build();
//
//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        transaction.add(android.R.id.content, indoorsFragment, "indoors");
//        transaction.commit();
//
//        indoorsFragment.getIndoors().getRouteAToB(start, end, new RoutingCallback() {
//
//            @Override
//            public void onError(IndoorsException arg0) {
//
//            }
//
//            @Override
//            public void setRoute(ArrayList<Coordinate> route) {
//                indoorsFragment.getSurfaceState().setRoutingPath(route);
//                IndoorsFactory.getInstance().enableRouteSnapping(route);
//                indoorsFragment.updateSurface();
//            }
//        });

        requestPermissionsFromUser();

    } // end of onCreate()

    private void requestPermissionsFromUser() {
        /**
         * Since API level 23 we need to request permissions for so called dangerous permissions from the user.
         *
         * You can see a full list of needed permissions in the Manifest File.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionCheckForLocation = ContextCompat.checkSelfPermission(
                    MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);

            if (permissionCheckForLocation != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[] {
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        REQUEST_CODE_PERMISSIONS);
            } else {
                //If permissions were already granted,
                // we can go on to check if Location Services are enabled.
                checkLocationIsEnabled();
            }
        } else {
            //Continue loading Indoors if we don't need user-settable-permissions.
            // In this case we are pre-Marshmallow.
            continueLoading();
        }
    } // end of requestPermissionsFromUser()

    /**
     * The Android system calls us back
     * after the user has granted permissions (or denied them)
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // Since we have requested multiple permissions,
            // we need to check if any were denied
            for (int grant : grantResults) {
                if (grant == PackageManager.PERMISSION_DENIED) {
                    if (shouldShowRequestPermissionRationale(
                            Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        // User has *NOT* allowed us to use ACCESS_COARSE_LOCATION
                        // permission on first try. This is the last chance we get
                        // to ask the user, so we explain why we want this permission
                        Toast.makeText(this,
                                "Location is used for Bluetooth location",
                                Toast.LENGTH_SHORT).show();
                        // Re-ask for permission
                        requestPermissionsFromUser();
                        return;
                    }

                    // The user has finally denied us permissions.
                    Toast.makeText(this,
                            "Cannot continue without permissions.",
                            Toast.LENGTH_SHORT).show();
                    this.finishAffinity();
                    return;
                }
            }
            checkLocationIsEnabled();
        }
    } // end of onRequestPermissionsResult()

    private void checkLocationIsEnabled() {
        // On android Marshmallow we also need to have active Location Services (GPS or Network based)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            boolean isNetworkLocationProviderEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            boolean isGPSLocationProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (!isGPSLocationProviderEnabled && !isNetworkLocationProviderEnabled) {
                // Only if both providers are disabled we need to ask the user to do something
                Toast.makeText(this, "Location is off, enable it in system settings.", Toast.LENGTH_LONG).show();
                Intent locationInSettingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                this.startActivityForResult(locationInSettingsIntent, REQUEST_CODE_LOCATION);
            } else {
                continueLoading();
            }
        } else {
            continueLoading();
        }
    } // end of checkLocationIsEnabled()

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_LOCATION) {
            // Check if the user has really enabled Location services.
            checkLocationIsEnabled();
        }
    }

    // At this point we can continue to load the Indoo.rs SDK as we did with previous android versions
    private void continueLoading() {
        IndoorsFactory.Builder indoorsBuilder = initializeIndoorsLibrary();
        indoorsSurfaceFragment = initializeIndoorsSurface(indoorsBuilder);
        setSurfaceFragment(indoorsSurfaceFragment);
    }

    private IndoorsFactory.Builder initializeIndoorsLibrary() {
        /**
         * This will initialize the builder for the Indoo.rs object
         */
        IndoorsFactory.Builder indoorsBuilder = new IndoorsFactory.Builder();
        indoorsBuilder.setContext(this);
        /**
         * TODO: replace this with your API-key
         * This is your API key as set on https://api.indoo.rs
         */
        indoorsBuilder.setApiKey("91c2793f-993f-454f-8802-96a3fe8cdb3c");
        /**
         * TODO: replace 12345 with the id of the building you uploaded to our cloud using the MMT
         * This is the ID of the Building as shown in the desktop Measurement Tool (MMT)
         */
        indoorsBuilder.setBuildingId((long) 795523136);
        // callback for indoo.rs-events
        indoorsBuilder.setUserInteractionListener(this);
        return indoorsBuilder;
    }

    private IndoorsSurfaceFragment initializeIndoorsSurface(IndoorsFactory.Builder indoorsBuilder) {
        /**
         * This will initialize the UI from Indoo.rs which is called IndoorsSurface.
         * The implementation is the IndoorsSurfaceFragment
         *
         * If you use your own map view implementation you don't need the Surface.
         * https://indoors.readme.io/docs/localisation-without-ui
         *
         */
        IndoorsSurfaceFactory.Builder surfaceBuilder = new IndoorsSurfaceFactory.Builder();
        surfaceBuilder.setIndoorsBuilder(indoorsBuilder);
        return surfaceBuilder.build();
    }

    private void setSurfaceFragment(final IndoorsSurfaceFragment indoorsFragment) {
        /**
         * This will add the IndoorsSurfaceFragment to the current layout
         */
        // http://stackoverflow.com/questions/33264031/calling-dialogfragments-show-from-within-onrequestpermissionsresult-causes/34204394#34204394
        // http://stackoverflow.com/questions/17184653/commitallowingstateloss-in-fragment-activities
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.add(android.R.id.content, indoorsFragment, "indoors");
                transaction.commit();
            }
        });
    }

    @Override
    public void positionUpdated(Coordinate userPosition, int accuracy) {
        /**
         * Is called each time the Indoors Library calculated a new position for the user
         * If Lat/Lon/Rotation of your building are set correctly you can calculate a
         * GeoCoordinate for your users current location in the building.
         */
        GeoCoordinate geoCoordinate = indoorsSurfaceFragment.getCurrentUserGpsPosition();

        if (geoCoordinate != null) {
            Toast.makeText(
                    this,
                    "User is located at " + geoCoordinate.getLatitude() + ","
                            + geoCoordinate.getLongitude(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void loadingBuilding(LoadingBuildingStatus loadingBuildingStatus) {
        // indoo.rs is still downloading or parsing the requested building
        // Inform the User of Progress
        showDownloadProgressToUser(loadingBuildingStatus.getProgress());
    }

    @Override
    public void buildingLoaded(Building building) {
        // Fake a 100% progress to your UI when you receive info that the download is finished.
        showDownloadProgressToUser(100);
        // indoo.rs SDK successfully loaded the building you requested and
        // calculates a position now
        Toast.makeText(
                this,
                "Building is located at " + building.getLatOrigin() / 1E6 + ","
                        + building.getLonOrigin() / 1E6, Toast.LENGTH_SHORT).show();
    }

    private void showDownloadProgressToUser(int progress) {
        if (progress % 10 == 0) { // Avoid showing too many values.
            if (progress > lastProgress) {
                lastProgress = progress; // Avoid showing same value multiple times.

                if (progressToast != null) {
                    progressToast.cancel();
                }

                progressToast = Toast.makeText(this, "Building downloading : "+progress+"%", Toast.LENGTH_SHORT);
                progressToast.show();
            }
        }
    }

    @Override
    public void onError(IndoorsException indoorsException) {
        Toast.makeText(this, indoorsException.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void changedFloor(int floorLevel, String name) {
        // user changed the floor
    }

    @Override
    public void leftBuilding(Building building) {
        // Deprecated
    }

    @Override
    public void buildingReleased(Building building) {
        // Another building was loaded, you can release any resources related to linked building
    }

    @Override
    public void orientationUpdated(float orientation) {
        // user changed the direction he's heading to
    }

    @Override
    public void enteredZones(List<Zone> zones) {
        // user entered one or more zones
    }

    @Override
    public void buildingLoadingCanceled() {
        // Loading of building was cancelled
    }
}
