package com.mapbox.geofencing.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.dimensionResource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapbox.geofencing.R
import com.mapbox.geofencing.model.GeofencingViewModel

@Composable
fun Particle(modifier: Modifier) {
    val viewModel: GeofencingViewModel = hiltViewModel()
    val isFired by viewModel.isFiredFlow.collectAsState()
    val color by viewModel.particleColorFlow.collectAsState()
    val addedArticle by viewModel.addedArticleFlow.collectAsState()

    val radiusDp = dimensionResource(id = R.dimen.particle_radius)
    val radius: Float
    val topPadding: Float
    val itemHeight: Float
    with(LocalDensity.current) {
        radius = radiusDp.toPx()
        topPadding = dimensionResource(id = R.dimen.list_top_padding).toPx()
        itemHeight = dimensionResource(id = R.dimen.image_size).toPx()
    }
    var topTranslation by remember { mutableStateOf(0f) }
    
    Canvas(modifier.size(radiusDp * 2)) {
        translate(top = topTranslation) {
            drawCircle(
                color = color,
                radius = radius
            )
        }
    }
    val animatedTopTranslation = remember { Animatable(0f) }
    LaunchedEffect(isFired) {
        if (isFired) {
            animatedTopTranslation.animateTo(
                targetValue = radius + topPadding + itemHeight / 2,
                animationSpec = spring(
                    stiffness = Spring.StiffnessLow
                )
            ) {
                topTranslation = value
            }
            animatedTopTranslation.snapTo(0f)
            topTranslation = 0f
            viewModel.setIsVisible(addedArticle,true)
            viewModel.setIsFired(false)
        }
    }
}
