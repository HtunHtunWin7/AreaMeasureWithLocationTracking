package com.example.areameasurewithlocationtracking

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.areameasurewithlocationtracking.utils.checkPermission
import com.example.areameasurewithlocationtracking.utils.isLocationEnabled
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.android.synthetic.main.sample_layout.*

class MainActivity : AppCompatActivity(),OnMapReadyCallback{
    private val TAG = MainActivity::class.java.simpleName
    lateinit var mMap: GoogleMap
    private var mLocation: Location ?= null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Used in checking for runtime permissions.
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34

    // The BroadcastReceiver used to listen from broadcasts from the service.
    private lateinit var myReceiver: MyReceiver

    // A reference to the service used to get location updates.
    private var mService: LocationUpdatesService? = null

    // Tracks the bound state of the service.
    private var mBound = false

    // UI elements.
    private val mRequestLocationUpdatesButton: Button? = null
    private val mRemoveLocationUpdatesButton: Button? = null

    // Monitors the state of the connection to the service.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: LocationUpdatesService.LocalBinder = service as LocationUpdatesService.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        myReceiver = MyReceiver()
        mLocation = myReceiver.location
        mService = LocationUpdatesService()
        setContentView(R.layout.sample_layout)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (!isLocationEnabled(this))
        {
            checkPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onStart() {
        super.onStart()
        requestLocationUpdate.setOnClickListener {
            if (!isLocationEnabled(this))
            {
                checkPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
            }else{
                mService!!.requestLocationUpdates()
                Toast.makeText(this,myReceiver.location?.latitude.toString(),Toast.LENGTH_LONG).show()
            }
        }
        removeLocationUpdate.setOnClickListener {
            mService!!.removeLocationUpdates()
        }

        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(
            Intent(this, LocationUpdatesService::class.java), mServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            myReceiver,
            IntentFilter(LocationUpdatesService.ACTION_BROADCAST)
        )
    }

    /**
     * Receiver for broadcasts sent by [LocationUpdatesService].
     */
    private class MyReceiver : BroadcastReceiver() {
        var location: Location ?=null
        var arrayPoints: MutableList<LatLng> = mutableListOf()
        override fun onReceive(context: Context, intent: Intent) {
             location = intent.getParcelableExtra<Location>(LocationUpdatesService.EXTRA_LOCATION)!!
            if (location != null){
                Toast.makeText(context,location?.latitude.toString(),Toast.LENGTH_LONG).show()
                val latLng = LatLng(location!!.latitude,location!!.longitude)
                arrayPoints.add(latLng)
            }
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
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
        fusedLocationClient.lastLocation.addOnSuccessListener {
            val latLng = LatLng(it.latitude, it.longitude)
            Toast.makeText(this,"Hello",Toast.LENGTH_LONG).show()
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
        }
    }


}