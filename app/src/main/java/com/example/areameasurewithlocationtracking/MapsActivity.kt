package com.example.areameasurewithlocationtracking

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.areameasurewithlocationtracking.utils.checkPermission
import com.example.areameasurewithlocationtracking.utils.isLocationEnabled
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.robin.locationgetter.EasyLocation
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    private lateinit var mMap: GoogleMap
    var mLastLocation: Location? = null
    var distanceCovered = 0f
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    private val MIN_DISTANCE = 10f // 5 meters
    private val MIN_TIME: Long = 10000 // 10 seconds


    private fun trackLocation() {
         locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val criteria = Criteria()
        criteria.horizontalAccuracy = Criteria.ACCURACY_HIGH
        locationManager.requestLocationUpdates(MIN_TIME, MIN_DISTANCE,criteria,this, mainLooper)
        locationListener = LocationListener {
            if (it != null) {
                if (mLastLocation == null) {
                    mLastLocation = it
                    Log.d("Location :", it.latitude.toString())
                } else {
                    Log.d("Location :", it.latitude.toString())
                    val distanceResults = FloatArray(1)
                    Location.distanceBetween(mLastLocation!!.latitude, mLastLocation!!.longitude, it.latitude, it.longitude, distanceResults)
                    distanceCovered += distanceResults[0]
                    Toast.makeText(this, "Distance covered : " + distanceResults[0], Toast.LENGTH_SHORT).show()
                    mLastLocation = it
                    distanceCoveredTextView.text = distanceCovered.toString()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //startTrackingService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        btnStart.visibility = View.VISIBLE
        linearLayout.visibility = View.GONE
        btnStop.visibility = View.GONE
        if (!isLocationEnabled(this)) {
            checkPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        btnStart.setOnClickListener {
            onClickStart()
            trackLocation()
        }
        btnStop.setOnClickListener {
            onClickStop()
            locationManager.removeUpdates(locationListener)
        }
    }

    override fun onStop() {
        super.onStop()
    }

    private fun onClickStart() {
        linearLayout.visibility = View.VISIBLE
        btnStart.visibility = View.GONE
        btnStop.visibility = View.VISIBLE
        //getTrackingDistance()
    }

    private fun onClickStop() {
        btnStop.visibility = View.GONE
        btnStart.visibility = View.VISIBLE
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        }
        currentLocationImageButton.setOnClickListener {
            getCurrentLocation(mMap)
        }
        // Add a marker in Sydney and move the camera
        /*val sydney = LatLng(mLocation!!.latitude, mLocation!!.longitude)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 16f))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))*/
    }

    private fun getCurrentLocation(googleMap: GoogleMap) {
        EasyLocation(this, object : EasyLocation.EasyLocationCallBack {
            @SuppressLint("MissingPermission")
            override fun getLocation(location: Location) {
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            location.latitude,
                            location.longitude
                        ), 18f
                    )
                )
                googleMap.isMyLocationEnabled = true
            }

            override fun locationSettingFailed() {
                Log.d("Location :", "Fail")
            }

            override fun permissionDenied() {
                checkPermission(this@MapsActivity, Manifest.permission.ACCESS_FINE_LOCATION)
            }

        })
    }

    override fun onLocationChanged(p0: Location) {
        Toast.makeText(this,p0.latitude.toString(),Toast.LENGTH_LONG).show()
    }

    /*override fun onLocationChanged(location: Location) {
        if (location != null) {
            if (mLastLocation == null) {
                mLastLocation = location
                Log.d("Location :", location.latitude.toString())
            } else {
                Log.d("Location :", location.latitude.toString())
                val distanceResults = FloatArray(1)
                Location.distanceBetween(mLastLocation!!.latitude, mLastLocation!!.longitude, location.latitude, location.longitude, distanceResults)
                distanceCovered += distanceResults[0]
                Toast.makeText(this, "Distance covered : " + distanceResults[0], Toast.LENGTH_SHORT).show()
                mLastLocation = location
                distanceCoveredTextView.text = distanceCovered.toString()
            }
        }

    }*/

}