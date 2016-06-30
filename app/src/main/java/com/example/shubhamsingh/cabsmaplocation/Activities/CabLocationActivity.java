package com.example.shubhamsingh.cabsmaplocation.Activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Property;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.Toast;

import com.example.shubhamsingh.cabsmaplocation.Commons.CommonUtils;
import com.example.shubhamsingh.cabsmaplocation.Commons.Constants;
import com.example.shubhamsingh.cabsmaplocation.Interpolators.LatLngInterpolator;
import com.example.shubhamsingh.cabsmaplocation.Listeners.ConsumerListener;
import com.example.shubhamsingh.cabsmaplocation.Listeners.GetRequestListener;
import com.example.shubhamsingh.cabsmaplocation.Objects.AnimationObject;
import com.example.shubhamsingh.cabsmaplocation.Objects.CabLocationObject;
import com.example.shubhamsingh.cabsmaplocation.Objects.SimpleLatLngObject;
import com.example.shubhamsingh.cabsmaplocation.Objects.SnapToRoadsObject;
import com.example.shubhamsingh.cabsmaplocation.Objects.SnappedPointObject;
import com.example.shubhamsingh.cabsmaplocation.Objects.StartRideObject;
import com.example.shubhamsingh.cabsmaplocation.R;
import com.example.shubhamsingh.cabsmaplocation.Services.RegistrationIntentService;
import com.example.shubhamsingh.cabsmaplocation.Tasks.GetRequest;
import com.example.shubhamsingh.cabsmaplocation.Threads.LatLngConsumer;
import com.example.shubhamsingh.cabsmaplocation.Threads.LatLngProducer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CabLocationActivity extends FragmentActivity implements OnMapReadyCallback,
        GetRequestListener, GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks, LocationListener, ConsumerListener{

    private static String TAG = CabLocationActivity.class.getSimpleName();

    private GoogleMap mMap;
    private Button buttonStartRide;
    private static Context context;
    private GoogleApiClient googleApiClient = null;
    private Location userLastLocation;
    private LocationRequest locationRequest;
    private boolean isRequestingUpdates;
    private boolean isRideStarted;
    private static Marker cabMarker = null;
    private static LatLng cabPrevLatLng = null;
    private static LatLng cabCurrLatLng = null;
    private boolean isFirstCabLocation = true;
    private MarkerOptions cabMarkerOptions;
    private LatLngBounds.Builder latLngBuilder;
    private boolean mapCameraUpdateNeeded;
    private Handler animationHandler;
    private TimerTask requestTimerTask;
    private boolean initialLocationReceived = false;
    private long locationUpdateTimeElapsed;
    private long prevLocationUpdateTime = 0;
    private long animationTimeElapsed = 0;
    private double intermediateLat;
    private double intermediateLng;
    private static ConcurrentLinkedQueue<AnimationObject> animationObjectsQueue;
    public static LinkedBlockingQueue<SimpleLatLngObject> latLngDataQueue =
            new LinkedBlockingQueue<>();
    private long lastAnimationDuration = 0;
    private static AnimationObject currAnimationObject = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cab_location);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        context = this.getApplicationContext();
        buttonStartRide = (Button) findViewById(R.id.button_start_ride);
        buttonStartRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makeRideRequest();
                buttonStartRide.setEnabled(false);
            }
        });
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
//        Bitmap cabIconBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.new_cab_icon);
        cabMarkerOptions = new MarkerOptions()
                .flat(true)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.new_cab_icon));
        latLngBuilder = new LatLngBounds.Builder();
        mapCameraUpdateNeeded = true;
        if (checkPlayServices() && CommonUtils.readFromSharePref(getApplicationContext(),
                Constants.GCM_TOKEN_KEY).equals("")) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
        else{
            Log.wtf(TAG, "Gcm Token: "+CommonUtils.readFromSharePref(getApplicationContext(),
                    Constants.GCM_TOKEN_KEY));
        }
        animationObjectsQueue = new ConcurrentLinkedQueue<>();
        createLocationRequest();
    }

    private void makeRideRequest() {
        try {
            String requestURL = Constants.START_RIDE_URL;
            GetRequest<StartRideObject> startRideRequest;
            startRideRequest = new GetRequest<>(this, this,
                    Constants.START_RIDE_URL, StartRideObject.class);
            Log.d(TAG, "Request Url:" + requestURL);
            startRideRequest.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        isRequestingUpdates = true;
        if (cabMarker == null && currAnimationObject != null){
            cabMarker = mMap.addMarker(cabMarkerOptions.position(currAnimationObject.getLatLng()));
        }
        Log.d(TAG, "googleApiClient.connect() called");
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        isRequestingUpdates = false;
        super.onStop();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        checkLocationPermission();
        mMap.setMyLocationEnabled(true);
//        cabMarker = mMap.addMarker(new MarkerOptions()
//                .title("Your Cab")
//                .flat(true)
//                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.cab_icon)));
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, 9000)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location Permission not granted");
            Toast.makeText(CabLocationActivity.this, "Location Permission not granted", Toast.
                    LENGTH_SHORT).show();
            requestLocationPermission();
            return;
        } else {
            Log.d(TAG, "Location Permission granted");
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed reached");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        checkLocationPermission();
        userLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (userLastLocation != null) {
            Log.d(TAG, "Last Location: <" + userLastLocation.getLatitude() + "," +
                    userLastLocation.getLongitude() + ">");
            latLngBuilder.include(new LatLng(userLastLocation.getLatitude(), userLastLocation
                    .getLongitude()));
            //TODO: Update latlng bounds with new locations
        }
        if (isRequestingUpdates)
            startLocationUpdates();
    }

    protected void resumeAfterPermissionGranted() {
        checkLocationPermission();
        userLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (userLastLocation != null) {
            Log.d(TAG, "Last Location: <" + userLastLocation.getLatitude() + "," +
                    userLastLocation.getLongitude() + ">");
        }
        if (isRequestingUpdates)
            startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    protected void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                Constants.LOCATION_PERMISSION_CODE);
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(60000);
        locationRequest.setFastestInterval(30000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull
    int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                resumeAfterPermissionGranted();
            } else {
                Toast.makeText(CabLocationActivity.this, "Please grant permission to continue",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (googleApiClient.isConnected() && !isRequestingUpdates) {
            startLocationUpdates();
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(Constants.GCM_LOCATION_EVENT));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (googleApiClient.isConnected())
            stopLocationUpdates();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
                mMessageReceiver);
    }

    protected void startLocationUpdates() {
        checkLocationPermission();
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, locationRequest, this);
        Log.d(TAG, "startLocationUpdates() called");
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                googleApiClient, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        Location newLocation = location;
        String newLocationDateFormat = DateFormat.getTimeInstance().format(new Date());
        LatLng newLocationLatLng = new LatLng(newLocation.getLatitude(),
                newLocation.getLongitude());
        Log.d(TAG, "New Location received: <" + newLocation.getLatitude() + "," + newLocation.
                getLongitude() + ">");
        Toast.makeText(CabLocationActivity.this, newLocation.getLatitude() + "," + newLocation.
                getLongitude(), Toast.LENGTH_SHORT).show();
    }

//    /**
//     * @param rideId
//     * @param timeInterval in milliseconds
//     */
//    private void getCabLocationRequest(final String rideId, final int timeInterval) {
//        requestHandler = new Handler();
//        requestRunnable = new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    String requestURL = Constants.CAB_LOCATION_URL + rideId;
//                    GetRequest<CabLocationObject> getCabLocationRequest = new GetRequest<>(
//                            CabLocationActivity.context, CabLocationActivity.this, requestURL,
//                            CabLocationObject.class);
//                    Log.d(TAG, "Request Url:" + requestURL);
//                    if (getCabLocationRequest.getStatus() == AsyncTask.Status.RUNNING)
//                        getCabLocationRequest.cancel(true);
//                    getCabLocationRequest.execute();
//                    Log.d(TAG, "Executing getCabLocationRequest");
//                    requestHandler.postDelayed(requestRunnable, timeInterval);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//        requestRunnable.run();
//    }

    /**
     * @param rideId
     * @param timeInterval in milliseconds
     */
    private void getCabLocationRequest(final String rideId, int timeInterval) {
        requestTimerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    String requestURL = Constants.CAB_LOCATION_URL + rideId;
                    GetRequest<CabLocationObject> getCabLocationRequest = new GetRequest<>(
                            CabLocationActivity.context, CabLocationActivity.this, requestURL,
                            CabLocationObject.class);
                    Log.d(TAG, "Request Url:" + requestURL);
                    if (getCabLocationRequest.getStatus() == AsyncTask.Status.RUNNING)
                        getCabLocationRequest.cancel(true);
                    getCabLocationRequest.execute();
                    Log.d(TAG, "Executing getCabLocationRequest");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        new Timer().scheduleAtFixedRate(requestTimerTask, 0, timeInterval);
    }

    private void stopRepeatingTask() {
//        requestHandler.removeCallbacks(requestRunnable);
        requestTimerTask.cancel();
    }

    //TODO: Equate to global cabCurrLatLng

    @Override
    public void onGetTaskCompleted(Object tObject) {
        if (tObject != null) {
            if (tObject instanceof StartRideObject) {
                StartRideObject startRideObject = (StartRideObject) tObject;
                Log.d(TAG, startRideObject.toString());
                if (startRideObject.getRide_id() != null) {
                    isRideStarted = true;
                    String rideId = startRideObject.getRide_id();
                    getCabLocationRequest(rideId, Constants.CAB_LOCATION_REQUEST_TIME_INTERVAL);
                } else {
                    Toast.makeText(CabLocationActivity.this, "Can not reach server",
                            Toast.LENGTH_SHORT).show();
                }
            } else if (tObject instanceof CabLocationObject) {
                CabLocationObject cabLocationObject = (CabLocationObject) tObject;
//                String startRideId = startRideObject.getRide_id();
                Log.d(TAG, cabLocationObject.toString());
                if (cabLocationObject.getCabCurrentLocationObject() != null) {
                    SimpleLatLngObject simpleLatLngObject = cabLocationObject.
                            getCabCurrentLocationObject();
                    LatLng cabLatLng = new LatLng(simpleLatLngObject.getLatitude(),
                            simpleLatLngObject.getLongitude());
                    if (isFirstCabLocation) {
                        isFirstCabLocation = false;
                        cabPrevLatLng = cabLatLng;
                        updateMap(null, cabLatLng, mapCameraUpdateNeeded);
                    } else if (cabPrevLatLng != null) {
                        mapCameraUpdateNeeded = false;
                        updateMap(cabPrevLatLng, cabLatLng, mapCameraUpdateNeeded);
                        cabPrevLatLng = cabLatLng;
                    }

                } else {
                    isRideStarted = false;
                    stopRepeatingTask();
                    buttonStartRide.setEnabled(true);
                }
            } else if (tObject instanceof SnapToRoadsObject) {
                SnapToRoadsObject snapToRoadsObject = (SnapToRoadsObject) tObject;
                Log.d(TAG, snapToRoadsObject.toString());
                SnappedPointObject[] snappedPointObjectArray = snapToRoadsObject.getSnappedPoints();
                Log.d(TAG, "Size of snappedPointObjectArray: "+snappedPointObjectArray.length);

                locationUpdateTimeElapsed = System.currentTimeMillis() - prevLocationUpdateTime;
                prevLocationUpdateTime = System.currentTimeMillis();

//                long animationDuration = (locationUpdateTimeElapsed/((snappedPointObjectArray
//                        .length)+1));

                long animationDuration = (locationUpdateTimeElapsed/(snappedPointObjectArray
                        .length));

                float totalDist = getLocationFromLatLng(cabPrevLatLng).distanceTo(getLocationFromLatLng(cabCurrLatLng));

                LatLng temp = null;

//                animationObjectsQueue.add(new AnimationObject(cabPrevLatLng, animationDuration));
                for (SnappedPointObject snappedPointObject : snappedPointObjectArray) {

                    LatLng latLng = new LatLng(snappedPointObject.getLocation().getLatitude(),
                            snappedPointObject.getLocation().getLongitude());

                    long animDuration;

                    if (temp == null){
                        float dist = getLocationFromLatLng(cabPrevLatLng).distanceTo(getLocationFromLatLng(latLng));
                        animDuration = (long) ((dist/totalDist) * animationDuration);
                        temp = latLng;
                    }
                    else {
                        float dist = getLocationFromLatLng(temp).distanceTo(getLocationFromLatLng(latLng));
                        animDuration = (long) ((dist/totalDist) * animationDuration);
                        temp = latLng;
                    }

                    animationObjectsQueue.add(new AnimationObject(latLng, animDuration));

                }

                cabPrevLatLng = cabCurrLatLng;

//                drawPolyline();

                currAnimationObject = animationObjectsQueue.poll();

                animateMakerNew(cabMarker, currAnimationObject.getLatLng(), currAnimationObject
                        .getDuration());
            }
        } else {
            Toast.makeText(CabLocationActivity.this, "Can not reach server", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void drawPolyline(){
        Log.d(TAG, "drawPolyline called()");

        Log.d(TAG, "Size of animationObjectsQueue: "+animationObjectsQueue.size());

        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);

        while (!animationObjectsQueue.isEmpty()){
            AnimationObject animationObject = animationObjectsQueue.poll();
            options.add(animationObject.getLatLng());
            Log.d(TAG, "adding Latlng: "+animationObject.getLatLng().toString());
        }
        mMap.addPolyline(options);
    }

    private static void animateMakerNew(final Marker marker, final LatLng toLatLng,
                                 final long duration){
        if (marker != null) {
            LatLng startPosition = marker.getPosition();
            Location startLoc = getLocationFromLatLng(startPosition);
            Location toLocation = getLocationFromLatLng(toLatLng);
            if (startLoc.distanceTo(toLocation)<35){
                moveMarker(marker, toLatLng, new LatLngInterpolator.LinearFixed(), duration);
            }
            else{
                Log.d(TAG, "Calling rotateMarker, distance: "+startLoc.distanceTo(toLocation)+", " +
                        "rotation: "+startLoc.bearingTo(toLocation));
                rotateMarker(marker, toLatLng, duration);
//                rotateMarker2(marker, toLatLng, duration);
            }
        }
        else {
            Log.d(TAG, "animateMakerNew: marker not found");
        }
    }

    private void animateMarker(final LatLng fromPosition, final LatLng toPosition,
                               final long duration) {
        animationHandler = new Handler();
        final long start = SystemClock.uptimeMillis();
        animationTimeElapsed = duration;
        final Interpolator interpolator = new LinearInterpolator();

        Location prevLoc = new Location("Cab Last Location");
        prevLoc.setLatitude(fromPosition.latitude);
        prevLoc.setLongitude(fromPosition.longitude);
        Location newLoc = new Location("Cab New Location");
        newLoc.setLatitude(toPosition.latitude);
        newLoc.setLongitude(toPosition.longitude);
        float bearing = prevLoc.bearingTo(newLoc);

        cabMarker.setRotation(bearing);

        animationHandler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                intermediateLat = t * toPosition.latitude + (1 - t)
                        * fromPosition.latitude;
                intermediateLng = t * toPosition.longitude + (1 - t)
                        * fromPosition.longitude;

                cabMarker.setPosition(new LatLng(intermediateLat, intermediateLng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    animationTimeElapsed -= 16;
//                    Log.wtf(TAG, "animationTimeElapsed= "+animationTimeElapsed+", duration= "+duration+", t= "+t);
                    animationHandler.postDelayed(this, 16);
                }
//                else if (animationLatLngCtr < animationLatLngs.size()-1){
//                    animateMarker(animationLatLngs.get(animationLatLngCtr),
//                            animationLatLngs.get(animationLatLngCtr+1),
//                            (locationUpdateTimeElapsed/animationLatLngs.size()-1));
//                    animationLatLngCtr++;
//                }
                else{
                    animationTimeElapsed = 0;
                    intermediateLat = 0;
                    intermediateLng = 0;
                }
            }
        });
    }


    protected void updateMap(LatLng prevLatLng, LatLng currLatLng, boolean forceUpdate){
        updateMap(prevLatLng, currLatLng, forceUpdate, 5000);
    }

    protected void updateMap(LatLng prevLatLng, LatLng currLatLng, boolean forceUpdate,
                             long animationDuration) {
        if (cabMarker == null) {
            cabMarker = mMap.addMarker(cabMarkerOptions.position(currLatLng));
            Log.d(TAG, "Marker not found");
        }
        else if (prevLatLng != null) {
            Log.d(TAG, "marker position: "+cabMarker.getPosition()+"; isMarkerVisible: "+cabMarker
                    .isVisible());
//            animateMarker(cabMarker, currLatLng, false, animationDuration);
            if( animationTimeElapsed>0){
                animationDuration =+ animationTimeElapsed;
                Log.d(TAG, "if: animationTimeElapsed: "+animationTimeElapsed);
                animationHandler.removeCallbacksAndMessages(null);
                animateMarker(new LatLng(intermediateLat, intermediateLng), currLatLng,
                        animationDuration);
            }
            else {
                Log.d(TAG, "else: animationTimeElapsed: "+animationTimeElapsed);
                animateMarker(prevLatLng, currLatLng, animationDuration);
            }
        }

        if (forceUpdate) {
            latLngBuilder.include(currLatLng);
            LatLngBounds bounds = latLngBuilder.build();

            int padding = 200; // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

            mMap.animateCamera(cu);
        }
        Log.d(TAG, "udpateMap() called");
    }

    private void newUpdateMap(boolean forceUpdate){
        if (cabMarker == null) {
            cabMarker = mMap.addMarker(cabMarkerOptions.position(cabPrevLatLng));
            Log.d(TAG, "Marker not found");
        }
        else if (cabPrevLatLng != null) {
            Log.d(TAG, "marker position: "+cabMarker.getPosition()+"; isMarkerVisible: "+cabMarker
                    .isVisible());
//            animateMarker(cabMarker, currLatLng, false, animationDuration);
            if(lastAnimationDuration>0){
                animationObjectsQueue.peek().setDuration((lastAnimationDuration+ animationObjectsQueue.peek()
                        .getDuration()));
                Log.d(TAG, "if: animationTimeElapsed: "+lastAnimationDuration);
                animationHandler.removeCallbacksAndMessages(null);
                newAnimateMarker(cabMarker);
            }
            else {
                Log.d(TAG, "else: animationTimeElapsed: "+animationTimeElapsed);
                newAnimateMarker(cabMarker);
            }
        }

        if (forceUpdate) {
            latLngBuilder.include(cabCurrLatLng);
            LatLngBounds bounds = latLngBuilder.build();

            int padding = 200; // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

            mMap.animateCamera(cu);
        }
    }

    private static void newerAnimateMarker(final Marker marker,
                                           final AnimationObject fromAnimationObject,
                                           final AnimationObject toAnimationObject) {

        final long duration = fromAnimationObject.getDuration();
        Log.d(TAG, "newerAnimateMarker() called, duration: "+duration);
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {

            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);

                double intermediateLat = t * toAnimationObject.getLatLng().latitude + (1 - t)
                        * fromAnimationObject.getLatLng().latitude;
                double intermediateLng = t * toAnimationObject.getLatLng().longitude + (1 - t)
                        * fromAnimationObject.getLatLng().longitude;

                marker.setPosition(new LatLng(intermediateLat, intermediateLng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
                else {
                    //Animation completed; poll next data value

                }

            }
        });
    }

    static public void rotateMarker2(final Marker marker, final LatLng toLatLng,
                                    final long duration) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final float startRotation = marker.getRotation();
        final float toRotation = getLocationFromLatLng(toLatLng).getBearing();
        Log.d(TAG, "toRotation: "+toRotation);

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / 5000);

                float rot = t * toRotation + (1 -t) * startRotation;

                marker.setRotation(-rot > 180 ? rot/2 : rot);
                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
                else {
                    moveMarker(cabMarker, toLatLng, new LatLngInterpolator.LinearFixed(), duration);
                }
            }
        });
    }


    private static void rotateMarker(final Marker marker, final LatLng toLatLng,
                                     final long duration) {
        Log.d(TAG, "rotateMarker called");
        final long animationDuration = 300;
        if (marker != null) {
            final float startRotation = marker.getRotation();
            final Location destination = getLocationFromLatLng(toLatLng);
            final Location startLocation = getLocationFromLatLng(marker.getPosition());
            Log.wtf(TAG, "startRotation:"+startRotation+", bearing: "+
                    startLocation.bearingTo(destination));

            final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
            valueAnimator.setDuration(animationDuration);
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator animation) {
                    try {

                        float v = animation.getAnimatedFraction();
                        marker.setRotation(computeRotation(v, startRotation, startLocation
                                .bearingTo(destination)));

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    Log.d(TAG,  "rotationAnimation ended");

                    //Check if animation duration is positive after rotation animaition
                    if(duration-animationDuration > 0){
                        moveMarker(cabMarker, toLatLng, new LatLngInterpolator.LinearFixed(),
                                duration-animationDuration);
                    }
                    else {
                        Log.d(TAG, "moveAnimation getting negative");
                        if (!animationObjectsQueue.isEmpty()){
                            Log.d(TAG, "moveAnimation ended");
                            currAnimationObject = animationObjectsQueue.poll();
                            animateMakerNew(cabMarker, currAnimationObject.getLatLng(), currAnimationObject
                                    .getDuration());
                        }
                    }
                }
            });
            valueAnimator.start();
        }
    }

    public static void moveMarker(final Marker marker, final LatLng finalPosition,
                                    final LatLngInterpolator latLngInterpolator,
                                    long duration) {

        Log.d(TAG, "moveAnimation called");
        TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>() {
            @Override
            public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
                return latLngInterpolator.interpolate(fraction, startValue, endValue);
            }
        };

        Property<Marker, LatLng> positionProperty = Property.of(Marker.class, LatLng.class,
                "position");

        ObjectAnimator positionAnimator = ObjectAnimator.ofObject(marker, positionProperty, typeEvaluator,
                finalPosition);

        positionAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animation) {
                //  animDrawable.stop();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                //  animDrawable.stop();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                //  animDrawable.stop();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                //  animDrawable.stop();
                if (!animationObjectsQueue.isEmpty()){
                    Log.d(TAG, "moveAnimation ended");
                    currAnimationObject = animationObjectsQueue.poll();
                    animateMakerNew(cabMarker, currAnimationObject.getLatLng(), currAnimationObject
                            .getDuration());
                }
            }
        });

        positionAnimator.setDuration(duration);
        positionAnimator.start();

    }

    /**
     * Method to animate marker to destination location
     * @param destination destination location (must contain bearing attribute, to ensure
     *                    marker rotation will work correctly)
     * @param marker marker to be animated
     */
    public static void animateMarker2(final Location destination, final Marker marker, long duration) {
        Log.d(TAG, "animateMarker2 called");
        if (marker != null) {
            final LatLng startPosition = marker.getPosition();
            final LatLng endPosition = new LatLng(destination.getLatitude(), destination.getLongitude());

            final float startRotation = marker.getRotation();

            final LatLngInterpolator latLngInterpolator = new LatLngInterpolator.LinearFixed();
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
            valueAnimator.setDuration(duration); // duration 1 second
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator animation) {
                    try {
                        float v = animation.getAnimatedFraction();
                        LatLng newPosition = latLngInterpolator.interpolate(v, startPosition, endPosition);
                        marker.setPosition(newPosition);
//                        marker.setRotation(computeRotation(v, startRotation, destination.getBearing()));
                        marker.setRotation(getLocationFromLatLng(startPosition).bearingTo(destination));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    Log.d(TAG,  "animation ended");
                    if(!animationObjectsQueue.isEmpty()){
                        AnimationObject animationObject = animationObjectsQueue.poll();
                        animateMarker2(getLocationFromLatLng(animationObject.getLatLng()), cabMarker,
                                animationObject.getDuration());
                    }
                }
            });
            valueAnimator.start();
        }
    }

    private static float computeRotation(float fraction, float start, float end) {
        float normalizeEnd = end - start; // rotate start to 0
        float normalizedEndAbs = (normalizeEnd + 360) % 360;

        float direction = (normalizedEndAbs > 180) ? -1 : 1; // -1 = anticlockwise, 1 = clockwise
        float rotation;
        if (direction > 0) {
            rotation = normalizedEndAbs;
        } else {
            rotation = normalizedEndAbs - 360;
        }

        float result = fraction * rotation + start;
        return (result + 360) % 360;
    }

    private void newAnimateMarker(final Marker marker) {
        Log.d(TAG, "newAnimateMarker called");
        for (AnimationObject animObj: animationObjectsQueue) {
            Log.d(TAG, animObj.toString());
        }
        animationHandler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final AnimationObject fromAnimationObject = animationObjectsQueue.poll();
        final AnimationObject toAnimationObject = animationObjectsQueue.peek();

        final long duration = toAnimationObject.getDuration();

        lastAnimationDuration = duration;

        final Interpolator interpolator = new LinearInterpolator();

        Location prevLoc = new Location("Cab Last Location");
        prevLoc.setLatitude(toAnimationObject.getLatLng().latitude);
        prevLoc.setLongitude(fromAnimationObject.getLatLng().longitude);
        Location newLoc = new Location("Cab New Location");
        newLoc.setLatitude(toAnimationObject.getLatLng().latitude);
        newLoc.setLongitude(toAnimationObject.getLatLng().longitude);
        float bearing = prevLoc.bearingTo(newLoc);
        marker.setRotation(bearing);
        float distance = prevLoc.distanceTo(newLoc);

        Log.d(TAG, "Distance: "+distance);

        if (distance < 20){
            newAnimateMarker(cabMarker);
        }

        animationHandler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                intermediateLat = t * toAnimationObject.getLatLng().latitude + (1 - t)
                        * fromAnimationObject.getLatLng().latitude;
                intermediateLng = t * toAnimationObject.getLatLng().longitude + (1 - t)
                        * fromAnimationObject.getLatLng().longitude;


                marker.setPosition(new LatLng(intermediateLat, intermediateLng));


                if (t < 1.0) {
                    // Post again 16ms later.
                    lastAnimationDuration -= 16;
//                    Log.wtf(TAG, "animationTimeElapsed= "+animationTimeElapsed+", duration= "+duration+", t= "+t);
                    animationHandler.postDelayed(this, 16);
                }
                else if (animationObjectsQueue.size()>1){
                    lastAnimationDuration = 0;
                    intermediateLat = 0;
                    intermediateLng = 0;
                    newAnimateMarker(cabMarker);
                }

            }
        });
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            // Get extra data included in the Intent
            String locationStr = intent.getStringExtra("location");
            SimpleLatLngObject location = CommonUtils.getGsonFromString(locationStr,
                    SimpleLatLngObject.class);
            Log.d(TAG, "Location: " + location.toString());

            if (prevLocationUpdateTime == 0 || !initialLocationReceived){
                initialLocationReceived = true;
                prevLocationUpdateTime = System.currentTimeMillis();
                cabPrevLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                updateMap(null, cabPrevLatLng, mapCameraUpdateNeeded, 0);
            }
            else {
                LatLngProducer latLngProducer = new LatLngProducer(latLngDataQueue, location);
                latLngProducer.run();
                LatLngConsumer latLngConsumer = new LatLngConsumer(latLngDataQueue,
                        CabLocationActivity.this);
                latLngConsumer.run();

                cabCurrLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mapCameraUpdateNeeded = false;

//                makeSnapRoadsRequest(cabCurrLatLng);

//                locationUpdateTimeElapsed = System.currentTimeMillis() - prevLocationUpdateTime;
//                prevLocationUpdateTime = System.currentTimeMillis();


//                cabPrevLatLng = cabCurrLatLng;
            }
        }
    };

    public void makeSnapRoadsRequest(LatLng latLngObject){
        LatLng[] latLngArray = new LatLng[]{cabPrevLatLng, latLngObject};
        try {
            String requestUrl = CommonUtils.formSnapRoadUrl(latLngArray, true, context.getString(
                    R.string.roads_api_key));
            Log.wtf(TAG, "SnapRoadsURL= "+requestUrl);
            GetRequest<SnapToRoadsObject> snapToRoadsRequest;
            snapToRoadsRequest = new GetRequest<>(this, this, requestUrl, SnapToRoadsObject.class);
            snapToRoadsRequest.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Location getLocationFromLatLng(LatLng latLng){
        Location location = new Location("Latlng Location");
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
        return location;
    }

    private static float getBearing(LatLng fromLatLng, LatLng toLatLng){
        Location fromLoc = getLocationFromLatLng(fromLatLng);
        Location toLoc = getLocationFromLatLng(toLatLng);
        return fromLoc.bearingTo(toLoc);
    }

    @Override
    public void onTakenFromQueue(SimpleLatLngObject latLngObject) {
        LatLng latLng = new LatLng(latLngObject.getLatitude(), latLngObject.getLongitude());
        if (getLocationFromLatLng(cabPrevLatLng).distanceTo(getLocationFromLatLng(latLng)) <
                500){
            makeSnapRoadsRequest(latLng);
        }
        else {
            Log.d(TAG, "distance between points greater than 500m");
            locationUpdateTimeElapsed = System.currentTimeMillis() - prevLocationUpdateTime;
            prevLocationUpdateTime = System.currentTimeMillis();
            animateMakerNew(cabMarker, latLng, locationUpdateTimeElapsed);
        }
    }

    //TODO: Save Instance state
    //https://developer.android.com/training/location/receive-location-updates.html#save-state
}
