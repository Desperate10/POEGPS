package com.poe.poegps.feature.data.remote.utils

import android.location.LocationListener

interface MyLocationListener: LocationListener {

    override fun onProviderDisabled(provider: String) {}

    override fun onProviderEnabled(provider: String) {}
}