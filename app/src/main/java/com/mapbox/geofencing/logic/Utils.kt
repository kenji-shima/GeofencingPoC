package com.mapbox.geofencing.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import com.mapbox.geofencing.R
import com.mapbox.geofencing.ui.MapFragment
import com.mapbox.geojson.Point
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Polygon
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.random.Random

class Utils {

    companion object {

        val iconsMap: MutableMap<String, Bitmap> = mutableMapOf()
        init {
            var starbucks = BitmapFactory.decodeResource(MapFragment.context.resources, R.drawable.starbucks)
            starbucks = Bitmap.createScaledBitmap(starbucks, 64,64,false)
            iconsMap["starbucks"] = starbucks
            val startMarker = bitmapFromDrawableRes(MapFragment.context, R.drawable.ic_red_marker)
            startMarker?.let { iconsMap["start"] = startMarker }
            val endMarker = bitmapFromDrawableRes(MapFragment.context, R.drawable.ic_blue_marker)
            endMarker?.let { iconsMap["end"] = endMarker }
            val blankMarker = bitmapFromDrawableRes(MapFragment.context, R.drawable.ic_placeholder_marker)
            blankMarker?.let { iconsMap["holder"] = blankMarker }
            val cheers = bitmapFromDrawableRes(MapFragment.context, R.drawable.cheers)
            cheers?.let { iconsMap["cheers"] = cheers }
            val mcdonalds = bitmapFromDrawableRes(MapFragment.context, R.drawable.mcdonalds)
            mcdonalds?.let { iconsMap["mcdonalds"] = mcdonalds }
        }

        fun addIconsToStyle(style: Style){
            val starbucks = BitmapFactory.decodeResource(MapFragment.context.resources, R.drawable.starbucks)
            style.addImage("starbucks", starbucks)
            val seveneleven = BitmapFactory.decodeResource(MapFragment.context.resources, R.drawable.seveneleven)
            style.addImage("seveneleven", seveneleven)
            val cheers = BitmapFactory.decodeResource(MapFragment.context.resources, R.drawable.cheers)
            style.addImage("cheers", cheers)
            val mcdonalds = BitmapFactory.decodeResource(MapFragment.context.resources, R.drawable.mcdonalds)
            style.addImage("mcdonalds", mcdonalds)
        }

        private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) : Bitmap? =
            convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

        private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
            if (sourceDrawable == null) {
                return null
            }
            return if (sourceDrawable is BitmapDrawable) {
                sourceDrawable.bitmap
            } else {
                val constantState = sourceDrawable.constantState ?: return null
                val drawable = constantState.newDrawable().mutate()
                val bitmap: Bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth, drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
        }

        suspend fun callIsochroneApi(
            profile: String,
            coordinates: String,
            contoursMinutes: List<Int>,
        ): JsonObject? {
            val contoursMinutesParam = contoursMinutes.joinToString(",")
            val url = "https://api.mapbox.com/isochrone/v1/mapbox/$profile/$coordinates?contours_minutes=$contoursMinutesParam"
            return doGet(url)
        }

        suspend fun callSearchBoxForwardApi(search: String, proximity: Point): JsonObject?{
            val country = getCountry(proximity)
            val url = "https://api.mapbox.com/search/searchbox/v1/forward?q=$search&proximity=${proximity.longitude()},${proximity.latitude()}&country=${country}&language=${Locale.current.language}&limit=10"
            return doGet(url)
        }

        private suspend fun getCountry(point: Point): String {
            val url =
                "https://api.mapbox.com/search/geocode/v6/reverse?longitude=${point.longitude()}&latitude=${point.latitude()}"
            val jsonObject = doGet(url)
            val features = jsonObject?.getAsJsonArray("features")

            return if (features != null && features.size() > 0) {
                val feature = features[0].asJsonObject
                val properties = feature.getAsJsonObject("properties")
                val context = properties.getAsJsonObject("context")
                val country = context.getAsJsonObject("country")
                country.get("country_code").asString.lowercase()
            } else {
                ""
            }

        }

        private suspend fun doGet(url: String): JsonObject?{
            val finalUrl = "$url&access_token=${MapFragment.context.getString(R.string.mapbox_access_token)}"
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(finalUrl)
                .build()
            return withContext(Dispatchers.IO) {
                try {
                    val response: Response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        responseBody?.let {
                            Gson().fromJson(it, JsonObject::class.java)
                        }
                    } else {
                        null
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    null
                }
            }
        }

        fun jsonToPolygonOptions(jsonObject: JsonObject): PolygonAnnotationOptions {
            val featuresArray = jsonObject.getAsJsonArray("features")
            val firstFeature = featuresArray[0].asJsonObject
            val properties = firstFeature.getAsJsonObject("properties")
            val geometry = firstFeature.getAsJsonObject("geometry")
            val coordinatesArray = geometry.getAsJsonArray("coordinates")

            val polygonPoints = mutableListOf<Point>()

            for (i in 0 until coordinatesArray.size()) {
                val coordinate = coordinatesArray[i].asJsonArray
                val longitude = coordinate[0].asDouble
                val latitude = coordinate[1].asDouble
                polygonPoints.add(Point.fromLngLat(longitude, latitude))
            }

            val fillColor = generateRandomHexColor()
            val fillOpacity = 0.6

            return PolygonAnnotationOptions()
                .withPoints(listOf(polygonPoints))
                .withFillColor(fillColor)
                .withFillOpacity(fillOpacity)
        }

        fun jsonFeatureCollectionToFeatureList(jsonObject: JsonObject): List<Feature> {
            val featureCollection = FeatureCollection.fromJson(jsonObject.toString())
            return featureCollection.features() ?: emptyList()
        }

        fun isochroneJsonToFeatures(jsonObject: JsonObject, addProps: Map<String, String>): List<Feature> {
            val featuresArray = jsonObject.getAsJsonArray("features")
            val features = mutableListOf<Feature>()

            for (i in 0 until featuresArray.size()) {
                val featureJson = featuresArray[i].asJsonObject
                val properties = featureJson.getAsJsonObject("properties")
                val geometry = featureJson.getAsJsonObject("geometry")
                val coordinatesArray = geometry.getAsJsonArray("coordinates")

                val coordinates = mutableListOf<Point>()
                for (j in 0 until coordinatesArray.size()) {
                    val coordinate = coordinatesArray[j].asJsonArray
                    val longitude = coordinate[0].asDouble
                    val latitude = coordinate[1].asDouble
                    coordinates.add(Point.fromLngLat(longitude, latitude))
                }

                val polygon = Polygon.fromLngLats(listOf(coordinates))
                val feature = Feature.fromGeometry(polygon, null, UUID.randomUUID().toString())
                for (entry in properties.entrySet()) {
                    feature.addStringProperty(entry.key, entry.value.asString)
                }
                for((key, value) in addProps){
                    feature.addStringProperty(key, value)
                }
                feature.addNumberProperty("MBX_GEOFENCE_DWELL_TIME",1)
                features.add(feature)
            }

            return features
        }

        private fun generateRandomHexColor(): String {
            val red = Random.nextInt(256)
            val green = Random.nextInt(256)
            val blue = Random.nextInt(256)

            // Format the RGB values as a hex string
            return String.format("#%02X%02X%02X", red, green, blue)
        }

        fun hexToComposeColor(hex: String): Color {
            val cleanHex = hex.removePrefix("#")
            val colorInt = android.graphics.Color.parseColor("#$cleanHex")
            return Color(colorInt)
        }

        fun getCurrentTime(): String {
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
            return current.format(formatter)
        }
    }

}