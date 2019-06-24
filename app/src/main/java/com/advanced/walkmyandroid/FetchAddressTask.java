package com.advanced.walkmyandroid;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class FetchAddressTask extends AsyncTask<Location, Void, String> {

    interface OnTaskCompleted {
        void onTaskCompleted(String result);
    }

    private final OnTaskCompleted mListener;

    private static final String LOG_TAG = FetchAddressTask.class.getSimpleName();

    private final WeakReference<Context> mContextRef;

    FetchAddressTask(Context applicationContext, OnTaskCompleted listener) {
        mContextRef = new WeakReference<>(applicationContext);
        mListener = listener;
    }

    @Override
    protected String doInBackground(Location... locations) {
        Context context = mContextRef.get();
        if (context == null) {
            return "can't get context in FetchAddressTask";
        }

        Location location = locations[0];
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        List<Address> addresses = null;
        String resultMessage = "";

        try {
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } catch (IOException e) {
            resultMessage = context.getString(R.string.service_not_available);
            Log.e(LOG_TAG, resultMessage, e);
        }

        if (addresses == null || addresses.size() == 0) {
            if (resultMessage.isEmpty()) {
                resultMessage = context.getString(R.string.no_address_found);
                Log.e(LOG_TAG, resultMessage);
            }
        } else {
            Address address = addresses.get(0);

            List<String> addressParts = new ArrayList<>();
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressParts.add(address.getAddressLine(i));
            }

            resultMessage = TextUtils.join("\n", addressParts);
        }

        return resultMessage;
    }

    @Override
    protected void onPostExecute(String address) {
        mListener.onTaskCompleted(address);
    }
}
