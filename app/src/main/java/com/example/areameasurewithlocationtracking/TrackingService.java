package com.example.areameasurewithlocationtracking;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class TrackingService extends Service implements LocationListener {
    private static final float MIN_DISTANCE = 5;           // 5 meters
    private static final long MIN_TIME = 10000;             // 10 seconds
    public static final String UPDATE_UI = "update_ui";

    public Location mLastLocation;
    public  float distanceCovered;
    private boolean isServiceRunning;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!isServiceRunning) {
            isServiceRunning = true;
            trackLocation();
        }
        return START_STICKY;
    }

    private void trackLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Criteria criteria = new Criteria();
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        locationManager.requestLocationUpdates(MIN_TIME, MIN_DISTANCE, criteria, this, getMainLooper());
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location!=null) {
            if (mLastLocation == null) {
                mLastLocation = location;
                Log.d("Location :",String.valueOf(location.getLatitude()));
            } else {
                Log.d("Location :",String.valueOf(location.getLatitude()));
                float[] distanceResults = new float[1];
                Location.distanceBetween(mLastLocation.getLatitude(),
                        mLastLocation.getLongitude(), location.getLatitude(), location.getLongitude(), distanceResults);
                distanceCovered += distanceResults[0]/1000;
                Toast.makeText(this, "Distance covered : " + distanceResults[0], Toast.LENGTH_SHORT).show();

                mLastLocation = location;
                Intent intent = new Intent(UPDATE_UI);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }
}
