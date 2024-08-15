package com.mapbox.geofencing.logic.search

import com.mapbox.geofencing.logic.GeofenceHandler
import com.mapbox.geofencing.logic.Utils
import com.mapbox.geofencing.logic.navigation.LocationHandler
import com.mapbox.geofencing.ui.MapFragment
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.search.discover.Discover
import com.mapbox.search.discover.DiscoverOptions
import com.mapbox.search.discover.DiscoverQuery
import com.mapbox.search.discover.DiscoverResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

class SearchHandler {

    companion object{
        private val job = Job()
        private val scope = CoroutineScope(Dispatchers.Main + job)

        fun forwardSearch(search: String, iconName: String){
            scope.launch {
                val proximity = LocationHandler.getLastLocation()
                proximity?.let {
                    val json = Utils.callSearchBoxForwardApi(search, it)
                    var featureCollection = FeatureCollection.fromJson(json.toString())
                    val filteredFeatures = featureCollection.features()?.filterNot { feature ->
                        val mapboxId = feature.getStringProperty("mapbox_id")
                        val shouldRemove = mapboxId != null && shownOnMapList.contains(mapboxId)
                        if (!shouldRemove && mapboxId != null) {
                            shownOnMapList+=mapboxId
                        }
                        shouldRemove
                    } ?: listOf()
                    featureCollection = FeatureCollection.fromFeatures(filteredFeatures)
                    if(featureCollection.features()?.size == 0) return@launch
                    MapFragment.addSymbolLayerToMap(UUID.randomUUID().toString(), featureCollection, iconName)
                    featureCollection.features()?.let { features ->
                        for (feature in features) {
                            val geometry = feature.geometry() as? Point
                            geometry?.let{
                                val geo = Utils.callIsochroneApi("walking", "${geometry.longitude()},${geometry.latitude()}", listOf(3))
                                if (geo != null) {
                                    val options = Utils.jsonToPolygonOptions(geo)
                                    MapFragment.addPolygonToMap(options)
                                    val addProps = mutableMapOf<String, String>(
                                        "name" to feature.getStringProperty("name"),
                                        "address" to feature.getStringProperty("address"),
                                        "mapboxId" to feature.getStringProperty("mapbox_id"),
                                    )
                                    options.fillColor?.let{ fillColor -> addProps["geofenceColor"] = fillColor }
                                    val featureList = Utils.isochroneJsonToFeatures(geo, addProps)
                                    for(feature in featureList){
                                        GeofenceHandler.addFeature(feature)
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }

        val discover = Discover.create()
        val coffeeQuery = DiscoverQuery.Category.COFFEE_SHOP_CAFE

        fun search(categoryQuery: DiscoverQuery.Category, name: String, iconName: String){
            scope.launch {
                val response = LocationHandler.getLastLocation()
                    ?.let { discover.search(categoryQuery, it, DiscoverOptions(100)) }
                response?.onValue { results: List<DiscoverResult> ->
                    val mapboxId = results
                    showOnMap(results, name, iconName)
                }?.onError { e: Exception ->
                    e.printStackTrace()
                }
            }
        }

        private var shownOnMapList: List<String> = mutableListOf()
        private fun showOnMap(results: List<DiscoverResult>, name: String, iconName: String){
            for(result in results){
                val mapboxId = result.mapboxId ?: continue
                if(shownOnMapList.contains(mapboxId)) continue
                if(!result.name.startsWith(name) || result.name == name) continue
                MapFragment.addAnnotationToMap(result.coordinate, Utils.iconsMap[iconName], result.name)
                scope.launch {
                    val geo = Utils.callIsochroneApi("walking", "${result.coordinate.longitude()},${result.coordinate.latitude()}", listOf(3))
                    if (geo != null) {
                        val options = Utils.jsonToPolygonOptions(geo)
                        MapFragment.addPolygonToMap(options)
                        shownOnMapList+=mapboxId
                        val addProps = mutableMapOf<String, String>(
                            "name" to result.name
                        )
                        result.address.formattedAddress?.let { addProps["address"] = it }
                        result.mapboxId?.let { addProps["mapboxId"] = it }
                        options.fillColor?.let{ addProps["geofenceColor"] = it }
                        val featureList = Utils.isochroneJsonToFeatures(geo, addProps)
                        for(feature in featureList){
                            GeofenceHandler.addFeature(feature)
                        }
                    }
                }
            }
        }
    }
}