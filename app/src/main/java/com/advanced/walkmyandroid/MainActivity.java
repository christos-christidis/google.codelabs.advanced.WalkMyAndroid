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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Collections;
import java.util.Date;
import java.util.List;

// SOS: Followed instructions in following link to replace deprecated API w the new one:
// https://developers.google.com/places/android-sdk/client-migration
public class MainActivity extends AppCompatActivity implements FetchAddressTask.OnTaskCompleted {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private static final int GRANTED = PackageManager.PERMISSION_GRANTED;
    private static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;

    private static final String STATE_TRACKING_LOCATION = "state_tracking_location";

    private TextView mLocationTextView;
    private ImageView mImageView;
    private AnimatorSet mRotateAnim;
    private Button mButton;

    private FusedLocationProviderClient mFusedLocationClient;
    private PlacesClient mPlacesClient;
    private boolean mTrackingLocation;
    private LocationCallback mLocationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLocationTextView = findViewById(R.id.textview_location);
        mImageView = findViewById(R.id.imageview_android);
        mButton = findViewById(R.id.button_location);

        mRotateAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.rotate);
        mRotateAnim.setTarget(mImageView);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Places.initialize(this, getString(R.string.places_key));
        mPlacesClient = Places.createClient(this);

        if (savedInstanceState != null) {
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
        if (ActivityCompat.checkSelfPermission(this, LOCATION_PERMISSION) != GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{LOCATION_PERMISSION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            mFusedLocationClient.requestLocationUpdates(getLocationRequest(),
                    mLocationCallback, null);

            mLocationTextView.setText(getString(R.string.address_text,
                    getString(R.string.loading),
                    getString(R.string.loading),
                    new Date()));

            mRotateAnim.start();
            mTrackingLocation = true;
            mButton.setText(getString(R.string.stop_tracking_location));
        }
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
    public void onTaskCompleted(final String result) {
        if (mTrackingLocation) {
            // SOS: we check again because it's required by findCurrentPlace
            if (ActivityCompat.checkSelfPermission(this, LOCATION_PERMISSION) != GRANTED) {
                return;
            }

            // SOS: Select which place data to return (billing varies...)
            List<Place.Field> placeFields = Collections.singletonList(Place.Field.NAME);

            FindCurrentPlaceRequest request = FindCurrentPlaceRequest.builder(placeFields).build();
            mPlacesClient.findCurrentPlace(request).addOnSuccessListener(new OnSuccessListener<FindCurrentPlaceResponse>() {
                @Override
                public void onSuccess(FindCurrentPlaceResponse response) {
                    double maxLikelihood = 0;
                    Place currentPlace = null;
                    for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
                        if (maxLikelihood < placeLikelihood.getLikelihood()) {
                            maxLikelihood = placeLikelihood.getLikelihood();
                            currentPlace = placeLikelihood.getPlace();
                        }
                    }

                    if (currentPlace != null) {
                        mLocationTextView.setText(getString(R.string.address_text,
                                currentPlace.getName(), result,
                                System.currentTimeMillis()));

                        setPlaceTypeImage(currentPlace);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    mLocationTextView.setText(getString(R.string.address_text,
                            "No Place name found!",
                            result, System.currentTimeMillis()));
                }
            });
        }
    }

    private void setPlaceTypeImage(Place currentPlace) {
        int drawableID = R.drawable.android_plain;

        List<Place.Type> types = currentPlace.getTypes();
        if (types != null) {
            for (Place.Type placeType : currentPlace.getTypes()) {
                switch (placeType) {
                    case SCHOOL:
                        drawableID = R.drawable.android_school;
                        break;
                    case GYM:
                        drawableID = R.drawable.android_gym;
                        break;
                    case RESTAURANT:
                        drawableID = R.drawable.android_restaurant;
                        break;
                    case LIBRARY:
                        drawableID = R.drawable.android_library;
                        break;
                }
            }
        }

        mImageView.setImageResource(drawableID);
    }
}
