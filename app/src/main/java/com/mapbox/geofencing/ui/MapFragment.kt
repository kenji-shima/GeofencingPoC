package com.mapbox.geofencing.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapbox.geofencing.logic.Utils
import com.mapbox.geofencing.logic.navigation.LocationHandler
import com.mapbox.geofencing.logic.navigation.NavigationHandler
import com.mapbox.geofencing.model.GeofencingViewModel
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.animation.CameraAnimatorsFactory
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.addOnMapLongClickListener
import com.mapbox.maps.plugin.scalebar.scalebar
import kotlinx.coroutines.delay

class MapFragment : Fragment() {

    companion object {
        lateinit var mapView: MapView
        lateinit var context: Context
        const val initZoom = 14.0

        private val pointAnnotationManager: PointAnnotationManager by lazy {
            this.mapView.annotations.createPointAnnotationManager()
        }
        private val polygonAnnotationManager: PolygonAnnotationManager by lazy{
            this.mapView.annotations.createPolygonAnnotationManager()
        }

        var longClickedPoint: Point? = null
        private var placeHolder: PointAnnotation? = null
        private var startPointHolder: PointAnnotation? = null
        private var endPointHolder: PointAnnotation? = null

        var startPoint: Point? = null
        var endPoint: Point? = null

        @Composable
        fun CreateMap() {
            val viewModel: GeofencingViewModel = hiltViewModel()
            AndroidView(
                factory = {
                    mapView
                },
                update = { //mapView ->
                }
            )

            this.context = LocalContext.current

            this.mapView = remember {
                MapView(context).apply {
                    mapboxMap.loadStyle(
                        styleExtension = style("mapbox://styles/kenji-shima/clzc5yyso00cl01r25k54gzmk")
                        {
                        },
                        onStyleLoaded = { style ->
                            Utils.addIconsToStyle(style)
                            //style.localizeLabels(Locale.JAPANESE)
                        }
                    )
                }
            }
            this.mapView.apply {
                compass.enabled = false
                scalebar.enabled = false
            }

            LaunchedEffect(Unit){
                delay(1000L)
                LocationHandler.init()
            }
            //var showPanel by remember { mutableStateOf(false) }
            // var panelPosition by remember { mutableStateOf(Offset.Zero) }
            addLongClick(viewModel)
        }

        private fun addLongClick(viewModel: GeofencingViewModel){
            mapView.mapboxMap.addOnMapLongClickListener { point ->
                longClickedPoint = point
                placeHolder?.let { pointAnnotationManager.delete(it) }
                placeHolder = addAnnotationToMap(point, Utils.iconsMap["holder"], "")
                //val screenCoords = mapView.mapboxMap.pixelForCoordinate(point)
                //panelPosition = Offset(screenCoords.x.toFloat(), screenCoords.y.toFloat())
                //showPanel()
                viewModel.setShowLocationPanel(true)
                true
            }
        }

        fun updateCamera(point: Point, bearing: Double? = null) {
            val mapAnimationOptions = MapAnimationOptions.Builder().duration(1500L).interpolator(
                CameraAnimatorsFactory.CUBIC_BEZIER_INTERPOLATOR).build()
            mapView.camera.easeTo(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(17.0)
                    .bearing(bearing)
                    .pitch(45.0)
                    .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                    .build(),
                mapAnimationOptions
            )
        }

        fun addSymbolLayerToMap(id:String, featureCollection: FeatureCollection, icon: String){
            this.mapView.mapboxMap.style?.let{style ->
                style.addSource(
                    geoJsonSource(id) {
                        featureCollection(featureCollection)
                    }
                )
                style.addLayer(
                    symbolLayer(id, id) {
                        iconImage(icon)
                        iconSize(0.2)
                        textField(Expression.get("name"))
                        textOffset(listOf(0.0,3.0))
                        textColor("black")
                        textHaloColor("white")
                        textHaloWidth(1.0)
                        textSize(10.0)
                        textOpacity(
                            interpolate {
                                linear()
                                zoom()
                                stop(14.0,0.0)
                                stop(15.0, 1.0)
                            }
                        )
                        textAllowOverlap(true)
                        iconAllowOverlap(true)
                    }
                )
            }
        }

        fun addAnnotationToMap(point: Point, icon: Bitmap?, label: String): PointAnnotation? {
            icon?.let {
                val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                    .withPoint(point)
                    .withIconImage(it)
                    .withTextField(label)
                    .withTextColor("green")
                    .withTextHaloColor("white")
                    .withTextOffset(listOf(0.0,3.0))
                    .withTextSize(10.0)
                return this.pointAnnotationManager.create(pointAnnotationOptions)
            }
            return null
        }

        fun addPolygonToMap(options: PolygonAnnotationOptions){
            polygonAnnotationManager.slot = "middle"
            polygonAnnotationManager.create(options)
        }

        fun setStartPoint(viewModel: GeofencingViewModel){
            placeHolder?.let { pointAnnotationManager.delete(it) }
            startPointHolder?.let { pointAnnotationManager.delete(it) }
            longClickedPoint?.let {
                startPointHolder = addAnnotationToMap(it, Utils.iconsMap["start"], "")
                startPoint = it
            }
            NavigationHandler.fetchRoute(startPoint, endPoint, viewModel)
        }

        fun setEndPoint(viewModel: GeofencingViewModel){
            placeHolder?.let { pointAnnotationManager.delete(it) }
            endPointHolder?.let { pointAnnotationManager.delete(it) }
            longClickedPoint?.let {
                endPointHolder = addAnnotationToMap(it, Utils.iconsMap["end"], "")
                endPoint = it
            }
            NavigationHandler.fetchRoute(startPoint, endPoint, viewModel)
        }

        fun removePlaceholder(){
            placeHolder?.let { pointAnnotationManager.delete(it) }
        }

        fun clearPoints() {
            placeHolder?.let { pointAnnotationManager.delete(it) }
            startPointHolder?.let { pointAnnotationManager.delete(it) }
            endPointHolder?.let { pointAnnotationManager.delete(it) }
            startPoint = null
            endPoint = null
        }
        @SuppressLint("MissingPermission")
        fun snapToCurrent(){
            val location = LocationHandler.getLastLocation()
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .zoom(initZoom)
                    .center(location?.let { Point.fromLngLat(it.longitude(), location.latitude()) })
                    .build()
            )
        }


    }

}