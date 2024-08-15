package com.mapbox.geofencing.logic.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.SystemClock
import com.mapbox.common.location.Location
import com.mapbox.geofencing.ui.MapFragment
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider

class LocationHandler {

    companion object {

        private val locationManager: LocationManager =
            MapFragment.context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        private val view by lazy {
            MapFragment.mapView
        }
        private val map by lazy {
            this.view.mapboxMap
        }
        private val locationComponentPlugin by lazy {
            this.view.location
        }
        private val replayLocationProvider = NavigationLocationProvider()
        val replayLocationObserver =  object: LocationObserver {
            override fun onNewRawLocation(rawLocation: Location) {
                replayLocationProvider.changePosition(
                rawLocation,
            )
                mockLocation(rawLocation.latitude, rawLocation.longitude)
//            updateCamera(
//                Point.fromLngLat(
//                    rawLocation.longitude,
//                    rawLocation.latitude
//                ),
//                rawLocation.bearing?.toDouble()
//            )
            }
            override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
                val enhancedLocation = locationMatcherResult.enhancedLocation
                replayLocationProvider.changePosition(
                    enhancedLocation,
                    locationMatcherResult.keyPoints,
                )
                mockLocation(enhancedLocation.latitude, enhancedLocation.longitude)
                MapFragment.updateCamera(
                    Point.fromLngLat(
                        enhancedLocation.longitude,
                        enhancedLocation.latitude
                    ),
                    enhancedLocation.bearing
                )
            }
        }

        @SuppressLint("MissingPermission")
        fun init() {
            locationComponentPlugin.updateSettings {
                puckBearing = PuckBearing.COURSE
                puckBearingEnabled = true
                enabled = true
                locationPuck = createDefault2DPuck(true)
            }

            //Get location only the first time. After addTestProvider() runs, the location will be null
            var location = locationManager.getLastKnownLocation("fused")
            if(location == null){
                location = android.location.Location("custom")
                location.longitude = 139.76571635075032
                location.latitude = 35.68151427068749
            }
            val camera = CameraOptions.Builder()
                .center(location.let { Point.fromLngLat(it.longitude,location.latitude) })
                .zoom(MapFragment.initZoom)
                .build()
            map.easeTo(
                camera,
                MapAnimationOptions.Builder().duration(2000).build()
            )
            enableTestProvider(LocationManager.GPS_PROVIDER)
            enableTestProvider(LocationManager.FUSED_PROVIDER)
            enableTestProvider(LocationManager.NETWORK_PROVIDER)

            replayLocationObserver.onNewRawLocation(Location.Builder().longitude(location.longitude).latitude(location.latitude).build())
        }

        private fun enableTestProvider(provider: String){
            locationManager.addTestProvider(
                provider, false, false,
                false, false, true, true, true, ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_FINE
            )
            locationManager.setTestProviderEnabled(provider, true)
        }

        fun mockLocation(latitude: Double, longitude: Double){
            mockLocation(LocationManager.GPS_PROVIDER, latitude, longitude)
            mockLocation(LocationManager.NETWORK_PROVIDER, latitude, longitude)
            mockLocation(LocationManager.FUSED_PROVIDER, latitude, longitude)
        }

        private fun mockLocation(provider: String, latitude: Double, longitude: Double){
            val mockLocation = android.location.Location(provider)
            mockLocation.latitude = latitude
            mockLocation.longitude = longitude
            mockLocation.altitude = 0.0
            mockLocation.time = System.currentTimeMillis()
            mockLocation.accuracy = 1F
            mockLocation.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            locationManager.setTestProviderLocation(provider, mockLocation)
        }

        fun setReplayLocationProvider(){
            locationComponentPlugin.apply {
                setLocationProvider(replayLocationProvider)
            }
        }

        @SuppressLint("MissingPermission")
        fun getLastLocation() : Point?{
            if(replayLocationProvider.lastLocation != null){
                return Point.fromLngLat(replayLocationProvider.lastLocation!!.longitude, replayLocationProvider.lastLocation!!.latitude)
            }
            val location = locationManager.getLastKnownLocation("fused")
            if (location != null) {
                return Point.fromLngLat(location.longitude, location.latitude)
            }
            if(MapFragment.startPoint != null){
                return MapFragment.startPoint
            }
            if(MapFragment.endPoint != null){
                return MapFragment.endPoint
            }
            return null
        }
    }

}