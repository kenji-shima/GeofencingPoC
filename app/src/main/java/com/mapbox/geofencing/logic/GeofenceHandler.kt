package com.mapbox.geofencing.logic

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapbox.common.experimental.geofencing.GeofencingError
import com.mapbox.common.experimental.geofencing.GeofencingEvent
import com.mapbox.common.experimental.geofencing.GeofencingFactory
import com.mapbox.common.experimental.geofencing.GeofencingObserver
import com.mapbox.geofencing.model.GeofencingViewModel
import com.mapbox.geofencing.model.InfoArticle
import com.mapbox.geojson.Feature

class GeofenceHandler {

    @SuppressLint("RestrictedApi")
    companion object{
        @SuppressLint("RestrictedApi")
        private val geofencing = GeofencingFactory.getOrCreate()
        init {
            geofencing.clearFeatures {  }
        }

        @SuppressLint("RestrictedApi")
        @Composable
        fun createGeofence() {
            val viewModel: GeofencingViewModel = hiltViewModel()
//            var state: GeofencingState? by remember { mutableStateOf(null) }

             var infoArticles = viewModel.infoArticlesFlow.collectAsState()
            var lastEvent: String? by remember { mutableStateOf(null) }

            // Create an observer to listen for geofencing events (entry, exit, dwell, error)
            val observer = object : GeofencingObserver {
                override fun onEntry(event: GeofencingEvent) {
                    val message = "Entered ${event.feature.properties()?.get("name")} (${event.feature.id()})"
                    if (message == lastEvent) return
                    lastEvent = message
                    addToInfoList(feature = event.feature)
                }

                override fun onDwell(event: GeofencingEvent) {
//                    state = GeofencingState.Dwell(event.feature)
                    val message = "Dwelling in ${event.feature.properties()?.get("name")} (${event.feature.id()})"
                    if(message == lastEvent) return
                    lastEvent = message
                    event.feature.id()?.let {featureIt ->
                        val count = infoArticles.value.count { it.id == featureIt }
                        val article = infoArticles.value.first { it.id == featureIt}
                        article.dwelledId = "$featureIt-$count"
                        viewModel.setDwelledTime(article.dwelledId, Utils.getCurrentTime())
                    }
                }

                override fun onExit(event: GeofencingEvent) {
//                    state = GeofencingState.Exit(event.feature)
                    val message = "Exited ${event.feature.properties()?.get("name")} (${event.feature.id()})"
                    if(message == lastEvent) return
                    lastEvent = message
                    event.feature.id()?.let { featureIt ->
                        val count = infoArticles.value.count { it.id == featureIt }
                        val article = infoArticles.value.first { it.id == featureIt}
                        article.exitedId = "$featureIt-$count"
                        viewModel.setExitedTime(article.exitedId, Utils.getCurrentTime())
                    }
                }

                override fun onError(error: GeofencingError) {
                    Log.w(TAG, "onError: Geofencing error: ${error.message}")
                }
                
                private fun addToInfoList(feature: Feature){
                    val color = Utils.hexToComposeColor(feature.getStringProperty("geofenceColor"))
                    val article = InfoArticle(
                        title = "${feature.properties()?.get("name").toString()}",
                        address = "${feature.properties()?.get("address").toString()}",
//                        drawable = R.drawable.starbucks,
                        color = color,
                        enteredTime = Utils.getCurrentTime()
                    )
                    article.id = feature.id().toString()
                    viewModel.setParticleColor(color)
                    viewModel.addArticleToTop(article)
                    viewModel.incrementId()
                    viewModel.setIsFired(true)
                }
            }

            geofencing.addObserver(observer) {
                it.error?.let { Log.e(TAG, "Geofencing: Failed to add observer ") }
            }

//            state?.let {
//                // Define the fill color based on the current geofencing state
//                val fillColor: Int = when (it) {
//                    is GeofencingState.Dwell -> Color.parseColor("#ffffff")
//                    is GeofencingState.Entry -> Color.parseColor("#000000")
//                    is GeofencingState.Exit -> Color.parseColor("#ffffff")
//                }
//            }
        }

        @SuppressLint("RestrictedApi")
        fun addFeature(feature: Feature){
            geofencing.addFeature(feature) {
                it.onError { Log.w(TAG, "geofence.addFeature() error: $it") }
                it.onValue { Log.d(TAG, "geofence.addFeature() success: $it") }
            }
        }

        sealed class GeofencingState {
            abstract val feature: Feature

            data class Entry(override val feature: Feature) : GeofencingState()
            data class Exit(override val feature: Feature) : GeofencingState()
            data class Dwell(override val feature: Feature) : GeofencingState()
        }

        private const val TAG = "GeofenceHandler"
    }
}