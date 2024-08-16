package com.mapbox.geofencing.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapbox.geofencing.R
import com.mapbox.geofencing.model.GeofencingViewModel
import com.mapbox.geofencing.model.InfoArticle
import kotlin.math.hypot

private var maxRadiusPx = 0f

@ExperimentalAnimationApi
@Composable
fun InfoCard(infoArticle: InfoArticle, isVisible: Boolean) {
    val viewModel: GeofencingViewModel = hiltViewModel()
    val exitedTimes = viewModel.exitedTimesFlow.collectAsState()
    val dwelledTimes = viewModel.dwelledTimesFlow.collectAsState()
    val particleRadius: Float
    with(LocalDensity.current) {
        particleRadius = dimensionResource(id = R.dimen.particle_radius).toPx()
    }
    var radius by remember { mutableStateOf(particleRadius) }
    var visibilityAlpha by remember { mutableStateOf(0f) }
    val currentState by remember {
        derivedStateOf {
            when {
                exitedTimes.value[infoArticle.exitedId] != null -> R.drawable.`out`
                dwelledTimes.value[infoArticle.dwelledId] != null -> R.drawable.dwell
                else -> R.drawable.`in`
            }
        }
    }

    Box(
        Modifier.padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    color = Color.Transparent
                )
                .onGloballyPositioned { coordinates ->
                    if (maxRadiusPx == 0f) {
                        maxRadiusPx =
                            hypot(coordinates.size.width / 2f, coordinates.size.height / 2f)
                    }
                }
                .drawBehind {
                    drawCircle(
                        color = if (isVisible) infoArticle.color else Color.Transparent,
                        radius = radius
                    )
                }
                .padding(dimensionResource(id = R.dimen.slot_padding))
                .align(Alignment.CenterStart)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .alpha(visibilityAlpha),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                //Text(text = "ステータス [${currentState}]", fontSize = 12.sp, color = Color.White)
                Image(
                    modifier = Modifier
                        .size(dimensionResource(id = R.dimen.image_size))
                        .alpha(visibilityAlpha),
                    painter = painterResource(id = currentState),
                    contentDescription = ""
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    infoArticle.title.replace("\"", ""),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.alpha(visibilityAlpha)
                )

            }
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .alpha(visibilityAlpha),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = infoArticle.address.replace("\"", ""),
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .alpha(visibilityAlpha),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${MapFragment.context.getString(R.string.entered_time)} [${infoArticle.enteredTime}]",
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
            exitedTimes.value[infoArticle.exitedId]?.let {
                Row(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .alpha(visibilityAlpha),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${MapFragment.context.getString(R.string.exited_time)} [$it]",
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
            }
            dwelledTimes.value[infoArticle.dwelledId]?.let {
                Row(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .alpha(visibilityAlpha),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${MapFragment.context.getString(R.string.dwell_start_time)} [$it]",
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
            }
        }
//        Image(
//            modifier = Modifier
//                .align(Alignment.CenterEnd)
//                .size(dimensionResource(id = R.dimen.image_size))
//                .alpha(visibilityAlpha),
//            painter = painterResource(id = infoArticle.drawable),
//            contentDescription = ""
//        )
    }

    val animatedRadius = remember { Animatable(particleRadius) }
    val animatedAlpha = remember { Animatable(0f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            animatedRadius.animateTo(maxRadiusPx, animationSpec = tween()) {
                radius = value
            }
            animatedAlpha.animateTo(1f, animationSpec = tween()) {
                visibilityAlpha = value
            }
        }
    }
}