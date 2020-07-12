package com.example.areameasurewithlocationtracking

import android.app.Application
import android.location.Location
import com.robin.locationgetter.EasyLocation
import timber.log.Timber

class MyApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}