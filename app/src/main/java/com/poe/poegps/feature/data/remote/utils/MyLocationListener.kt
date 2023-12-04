package com.poe.poegps.feature.data.remote.utils

import android.location.LocationListener
import android.os.Bundle

interface MyLocationListener: LocationListener {

    override fun onProviderDisabled(provider: String) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}