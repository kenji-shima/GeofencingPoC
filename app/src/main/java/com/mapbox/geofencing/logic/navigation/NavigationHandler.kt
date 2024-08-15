package com.mapbox.geofencing.logic.navigation

import android.annotation.SuppressLint
import android.util.Log
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geofencing.model.GeofencingViewModel
import com.mapbox.geofencing.ui.MapFragment
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.tripdata.progress.api.MapboxTripProgressApi
import com.mapbox.navigation.tripdata.progress.model.DistanceRemainingFormatter
import com.mapbox.navigation.tripdata.progress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.tripdata.progress.model.TimeRemainingFormatter
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class NavigationHandler() {

    companion object {

        private val context by lazy {
            MapFragment.context
        }
        private lateinit var mapboxNavigation: MapboxNavigation
        private lateinit var mapboxReplayer: MapboxReplayer
        private lateinit var replayProgressObserver: ReplayProgressObserver
        private val options: MapboxRouteLineApiOptions by lazy {
            MapboxRouteLineApiOptions.Builder()
                //.withRouteLineResources(RouteLineResources.Builder().build())
                //.withRouteLineBelowLayerId("road-label-navigation")
                .build()
        }
        private val routeLineViewOptions: MapboxRouteLineViewOptions by lazy {
            MapboxRouteLineViewOptions.Builder(context)
                .routeLineColorResources(RouteLineColorResources.Builder().build())
                .routeLineBelowLayerId("road-label-navigation")
                .build()
        }
        private val routeLineView by lazy {
            MapboxRouteLineView(routeLineViewOptions)
        }
        private val routeLineApi: MapboxRouteLineApi by lazy {
            MapboxRouteLineApi(options)
        }
        private val routesObserver: RoutesObserver = RoutesObserver { routeUpdateResult ->
            routeLineApi.setNavigationRoutes(
                routeUpdateResult.navigationRoutes
            ) { value ->
                MapFragment.mapView.mapboxMap.style?.apply {
                    routeLineView.renderRouteDrawData(this, value)
                }
            }
        }
        private val tripProgressApi: MapboxTripProgressApi by lazy {
            TripProgressUpdateFormatter.Builder(context)
                .distanceRemainingFormatter(
                    DistanceRemainingFormatter(
                        DistanceFormatterOptions.Builder(
                            context
                        ).build()
                    )
                )
                .timeRemainingFormatter(TimeRemainingFormatter(context))
                .estimatedTimeToArrivalFormatter(EstimatedTimeToArrivalFormatter(context))
                .build()
                .let { MapboxTripProgressApi(it) }
        }
        private val routeProgressObserver = RouteProgressObserver { progress ->
            val tripProgress = tripProgressApi.getTripProgress(progress)
//        println("@@@@TripProgress"+tripProgress)
//        replayLocationProvider.getLastLocation(){ location ->
//            println("@@@@@Location"+location)
//            if (location != null) {
//                locationObserver.onNewRawLocation(location)
//
//            }
//        }
        }

        val replayEventsObserver = object : ReplayEventsObserver {
            override fun replayEvents(events: List<ReplayEventBase>) {
                /*if (events.size > 1) {
                // All events have been played
                mapboxReplayer?.finish()
                mapboxReplayer?.clearEvents()
                MapboxNavigationApp.current()?.stopTripSession()
                routeLineApi.cancel()
                routeLineView.cancel()
                mapView.location.setLocationProvider(DefaultLocationProvider(context))
                MapboxNavigationApp.current()?.setNavigationRoutes(emptyList())
            }*/
            }
        }

        private fun setupMapboxNavigation(){
            MapboxNavigationApp.setup {
                NavigationOptions.Builder(context).build()
            }
            mapboxNavigation = MapboxNavigationApp.current()!!
            mapboxReplayer = mapboxNavigation?.mapboxReplayer!!
            replayProgressObserver = ReplayProgressObserver(mapboxReplayer)
            mapboxNavigation?.apply {
                registerRoutesObserver(routesObserver)
                registerRouteProgressObserver(replayProgressObserver)
                registerRouteProgressObserver(routeProgressObserver)
                registerLocationObserver(LocationHandler.replayLocationObserver)
                startReplayTripSession(false)
                startReplayTripSession()
            }
            LocationHandler.setReplayLocationProvider()
        }

        private fun setReplayRoute(routes: List<NavigationRoute>, viewModel: GeofencingViewModel){
            mapboxNavigation!!.setNavigationRoutes(routes)
            val directions: List<DirectionsRoute> =
                routes.map { it.directionsRoute }
            val events = directions.map { mapper.mapDirectionsRouteGeometry(it) }
            mapboxReplayer.clearEvents()
            mapboxReplayer.pushEvents(
                events[0]
            )
            viewModel.setNavigationReady(true)
        }

        private val mapper = ReplayRouteMapper()
        @SuppressLint("MissingPermission")
        fun replayLocations() {
            mapboxReplayer.play()
            mapboxReplayer.playbackSpeed(1.0)
        }

        fun stopReplayLocations(viewModel: GeofencingViewModel){
            if (!::mapboxReplayer.isInitialized) return
            mapboxReplayer.clearEvents()
            mapboxNavigation?.stopTripSession()
            routeLineApi.cancel()
            routeLineView.cancel()
            mapboxReplayer.finish()
            mapboxNavigation.setNavigationRoutes(emptyList())
            viewModel.setNavigationReady(false)
        }

        @SuppressLint("MissingPermission")
        @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
        fun fetchRoute(startPoint: Point?, endPoint: Point?, viewModel: GeofencingViewModel) {
            if (startPoint == null || endPoint == null) {
                return
            }
            val routeOptions = RouteOptions.builder()
                //.applyLanguageAndVoiceUnitOptions(context)
                .applyDefaultNavigationOptions()
                .coordinatesList(listOf(startPoint, endPoint))
                .profile(DirectionsCriteria.PROFILE_WALKING)
                .alternatives(false)
                .build()

            viewModel.setNavigationReady(false)
            setupMapboxNavigation()

            mapboxNavigation?.apply {
                requestRoutes(routeOptions,
                    object : NavigationRouterCallback {

                        override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
                            //do nothing
                        }

                        override fun onFailure(
                            reasons: List<RouterFailure>,
                            routeOptions: RouteOptions
                        ) {
                            Log.e("NavigationReplayService", "route request failed with $reasons")
                        }

                        @SuppressLint("RestrictedApi")
                        override fun onRoutesReady(
                            routes: List<NavigationRoute>,
                            routerOrigin: String
                        ) {
                            setReplayRoute(routes, viewModel)
                        }

                    })
            }
        }
    }
}

