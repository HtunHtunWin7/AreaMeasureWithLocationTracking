package com.tech_kingsley.distancetracker.utils

import android.Manifest

object Constant {
    const val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    const val LOCATION_REQUEST_CODE = 200
    // Defined in minutes.
    const val UPDATE_INTERVAL = 3 * 60 * 1000.toLong()
    const val FASTEST_INTERVAL = 30 * 1000.toLong()
}