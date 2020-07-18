package com.example.areameasurewithlocationtracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.areameasurewithlocationtracking.utils.checkPermission
import com.example.areameasurewithlocationtracking.utils.isLocationEnabled
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_maps.*
import java.text.DecimalFormat

class MapsActivity : OnMapReadyCallback, AppCompatActivity() {


    private val TAG = MapsActivity::class.java.simpleName
    private var mMap: GoogleMap? = null

    /**
     * Code used in requesting runtime permissions.
     */
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34

    /**
     * Constant used in the location settings dialog.
     */
    private val REQUEST_CHECK_SETTINGS = 0x1

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
        UPDATE_INTERVAL_IN_MILLISECONDS / 2

    // Keys for storing activity state in the Bundle.
    private val KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates"
    private val KEY_LOCATION = "location"

    /**
     * Provides access to the Fused Location Provider API.
     */
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    /**
     * Provides access to the Location Settings API.
     */
    private var mSettingsClient: SettingsClient? = null

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private var mLocationRequest: LocationRequest? = null

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private var mLocationSettingsRequest: LocationSettingsRequest? = null

    /**
     * Callback for Location events.
     */
    private var mLocationCallback: LocationCallback? = null

    /**
     * Represents a geographical location.
     */
    private var mCurrentLocation: Location? = null
    private var mRequestingLocationUpdates: Boolean = true

    private var tempLocation: Location? = null
    var distanceCovered = 0f

    private var arrayPoints: MutableList<LatLng> = mutableListOf()
    private lateinit var polylineOptions: PolylineOptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        mRequestingLocationUpdates = false
        setUpInitialView()


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)

        createLocationRequest()
        createLocationCallback()
        buildLocationSettingsRequest()
    }

    override fun onResume() {
        super.onResume()
        // Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.
        if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates()
        } else if (!checkPermissions()) {
            requestPermissions()
        }
    }

    private fun checkPermissions(): Boolean {
        val permissionState =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun setUpInitialView() {
        btnStart.visibility = View.VISIBLE
        linearLayout.visibility = View.GONE
        btnStop.visibility = View.GONE
        if (!isLocationEnabled(this)) {
            checkPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    private fun onClickStartButton(){
        mMap!!.clear()
        btnStart.visibility = View.GONE
        btnStop.visibility = View.VISIBLE
        linearLayout.visibility = View.VISIBLE
    }
    private fun onClickStopButton(){
        btnStart.visibility = View.VISIBLE
        btnStop.visibility = View.GONE
        linearLayout.visibility = View.GONE
        arrayPoints.clear()
        distanceCovered = 0f
        mMap!!.clear()
    }
    /*private fun setButtonState(){
        if (mRequestingLocationUpdates){
            btnStart.visibility = View.GONE
            btnStop.visibility = View.VISIBLE
            linearLayout.visibility = View.VISIBLE
        }else{
            btnStart.visibility = View.VISIBLE
            btnStop.visibility = View.GONE
            linearLayout.visibility = View.GONE
            arrayPoints.clear()
            distanceCovered = 0f
        }
    }*/

    fun startUpdatesButtonHandler(view: View?) {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true
            onClickStartButton()
            startLocationUpdates()
        }
    }

    fun stopUpdatesButtonHandler(view: View?) {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        stopLocationUpdates()
        onClickStopButton()
    }

    private fun stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.")
            return
        }
        mFusedLocationClient!!.removeLocationUpdates(mLocationCallback)
            .addOnCompleteListener(this) {
                mRequestingLocationUpdates = false
            }
    }

    /**
     * Creates a callback for receiving location events.
     */
    private fun createLocationCallback() {
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                mCurrentLocation = locationResult.lastLocation
                updateLocationUI()
            }
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates)
        savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation)
        super.onSaveInstanceState(savedInstanceState)
    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest!!.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest!!.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest!!.smallestDisplacement = 3f
    }

    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        mLocationSettingsRequest = builder.build()
    }

    /**
     * Sets the value of the UI fields for the location latitude, longitude
     */
    private fun updateLocationUI() {
        val distanceResults = FloatArray(1)
        if (mCurrentLocation != null) {
            if (tempLocation != null) {
                val temp = Location.distanceBetween(
                    tempLocation!!.latitude, tempLocation!!.longitude,
                    mCurrentLocation!!.latitude, mCurrentLocation!!.longitude, distanceResults
                )
                distanceCovered += distanceResults[0]
                distanceCoveredTextViewInMeter.text = getDistanceInKm(distanceCovered.toDouble())
                distanceCoveredTextViewInAcre.text = getDistanceInAcre(distanceCovered.toDouble())
            } else {
                distanceCoveredTextViewInMeter.text = getDistanceInKm(0.0)
                distanceCoveredTextViewInAcre.text = getDistanceInAcre(0.0)
            }
            tempLocation = mCurrentLocation
            arrayPoints.add(LatLng(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude))
            polyLineOptions()
        }
    }

    private fun getDistanceInAcre(totalDistance: Double): String {
        val df = DecimalFormat("#.####")
        return when (totalDistance) {
            0.0 -> {
                "0 Acre"
            }
            1.0 -> {
                df.format(totalDistance*0.000247105).toString()+ " Acre"
            }
            else -> {
                df.format(totalDistance * 0.000247105).toString() + " Acres"
            }
        }
    }

    private fun getDistanceInKm(totalDistance: Double): String {
        if (totalDistance == 0.0 || totalDistance < -1)
            return "0 Km"
        else if (totalDistance > 0 && totalDistance < 1000)
            return totalDistance.toInt().toString() + " meters"
        val df = DecimalFormat("#.##")
        return df.format(totalDistance / 1000) + " Km"
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient!!.checkLocationSettings(mLocationSettingsRequest)
            .addOnSuccessListener(this) {
                mFusedLocationClient!!.requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback,
                    Looper.myLooper()
                )
                updateLocationUI()
            }.addOnFailureListener(this) { e ->
                when ((e as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the
                            // result in onActivityResult().
                            val rae = e as ResolvableApiException
                            rae.startResolutionForResult(
                                this, REQUEST_CHECK_SETTINGS
                            )
                        } catch (sie: SendIntentException) {

                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val errorMessage =
                            "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                        mRequestingLocationUpdates = false
                    }
                }
                updateLocationUI()
            }
    }

    private fun requestPermissions() {
        val shouldProvideRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        if (shouldProvideRationale) {
            showSnackbar(R.string.permission_rationale,
                android.R.string.ok, View.OnClickListener { // Request permission
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_PERMISSIONS_REQUEST_CODE
                    )
                })
        } else {
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> when (resultCode) {
                Activity.RESULT_OK -> Log.i(
                    TAG,
                    "User agreed to make required location settings changes."
                )
                Activity.RESULT_CANCELED -> {
                    Log.i(TAG, "User chose not to make required location settings changes.")
                    mRequestingLocationUpdates = false
                    updateLocationUI()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isEmpty()) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.d("Cancel ", "User iteraction was cancelled")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                    startLocationUpdates()
                }
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                    R.string.settings,
                    View.OnClickListener { // Build intent that displays the App settings screen.
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts(
                            "package",
                            BuildConfig.APPLICATION_ID, null
                        )
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    })
            }
        }
    }

    private fun showSnackbar(
        mainTextStringId: Int,
        actionStringId: Int,
        listener: View.OnClickListener
    ) {
        Snackbar.make(
            findViewById<View>(android.R.id.content), getString(mainTextStringId),
            Snackbar.LENGTH_INDEFINITE
        ).setAction(getString(actionStringId), listener).show()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap!!.mapType = GoogleMap.MAP_TYPE_HYBRID
        if (mCurrentLocation != null) {
            Toast.makeText(this, "Hello", Toast.LENGTH_LONG).show()
            val latLng = LatLng(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude)
            /*val options = MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                .position(latLng)
                .flat(true)
            mMap!!.addMarker(options)*/
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
        } else {
            mFusedLocationClient?.lastLocation?.addOnSuccessListener {
                val latLng = LatLng(it!!.latitude, it!!.longitude)
                /* val options = MarkerOptions()
                     .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                     .position(latLng)
                     .flat(true)
                 mMap!!.addMarker(options)*/
                mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
            }
        }
        mMap!!.isMyLocationEnabled = true

    }

    private fun polyLineOptions() {
        val marker = MarkerOptions()
        //mMap.addMarker(marker);
        polylineOptions = PolylineOptions()
        polylineOptions.color(Color.BLUE)
        polylineOptions.width(6f)
        polylineOptions.addAll(arrayPoints)
        mMap?.addPolyline(polylineOptions)
    }


}