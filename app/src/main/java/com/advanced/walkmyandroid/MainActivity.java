package com.advanced.walkmyandroid;

import android.Manifest;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity implements FetchAddressTask.OnTaskCompleted {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int GRANTED = PackageManager.PERMISSION_GRANTED;

    private static final String STATE_TRACKING_LOCATION = "state_tracking_location";

    private TextView mLocationTextView;
    private AnimatorSet mRotateAnim;
    private Button mButton;

    private FusedLocationProviderClient mFusedLocationClient;
    private boolean mTrackingLocation;
    private LocationCallback mLocationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLocationTextView = findViewById(R.id.textview_location);
        ImageView imageView = findViewById(R.id.imageview_android);
        mButton = findViewById(R.id.button_location);

        mRotateAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.rotate);
        mRotateAnim.setTarget(imageView);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (savedInstanceState != null){
            mTrackingLocation = savedInstanceState.getBoolean(STATE_TRACKING_LOCATION);
        }

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (mTrackingLocation) {
                    new FetchAddressTask(MainActivity.this, MainActivity.this)
                            .execute(locationResult.getLastLocation());
                }
            }
        };
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_TRACKING_LOCATION, mTrackingLocation);
    }

    public void onClickTrackLocation(View view) {
        if (!mTrackingLocation) {
            startTrackingLocation();
        } else {
            stopTrackingLocation();
        }
    }

    private void startTrackingLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            mFusedLocationClient.requestLocationUpdates(getLocationRequest(),
                    mLocationCallback, null);
        }

        mRotateAnim.start();
        mTrackingLocation = true;
        mButton.setText(getString(R.string.stop_tracking_location));
    }

    private void stopTrackingLocation() {
        if (mTrackingLocation) {
            mRotateAnim.end();
            mTrackingLocation = false;
            mButton.setText(R.string.start_tracking_location);
            mLocationTextView.setText(R.string.textview_hint);
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == GRANTED) {
                startTrackingLocation();
            }
        } else {
            Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onTaskCompleted(String result) {
        if (mTrackingLocation) {
            mLocationTextView.setText(getString(R.string.address_text, result, System.currentTimeMillis()));
        }
    }
}
