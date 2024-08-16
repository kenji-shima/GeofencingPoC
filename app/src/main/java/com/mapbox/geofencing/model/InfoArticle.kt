package com.mapbox.geofencing.model

import androidx.compose.ui.graphics.Color
import com.mapbox.geofencing.R

data class InfoArticle(
    var id: String = "0",
    var title: String = "",
    var address: String = "",
    var enteredTime: String = "",
    var exitedId: String = "",
    var exitedTime: String = "",
    var dwelledId: String = "",
    var dwelledTime: String = "",
    var width: String = "",
    var drawable: Int = R.drawable.starbucks,
    var color: Color = Color.Transparent
)
