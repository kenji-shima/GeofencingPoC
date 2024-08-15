package com.mapbox.geofencing.ui

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapbox.geofencing.R
import com.mapbox.geofencing.logic.navigation.LocationHandler
import com.mapbox.geofencing.logic.navigation.NavigationHandler
import com.mapbox.geofencing.logic.search.SearchHandler
import com.mapbox.geofencing.model.GeofencingViewModel
import kotlinx.coroutines.launch

class Controls() {

    companion object {

        @Composable
        fun MarkerPanel() {
            val viewModel: GeofencingViewModel = hiltViewModel()
            //val density = LocalContext.current.resources.displayMetrics.density
            //val positionInDp = position.toDp(density)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000)),
                contentAlignment = Alignment.Center

            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = {
                        MapFragment.removePlaceholder()
                        LocationHandler.replayLocationObserver.onNewRawLocation(com.mapbox.common.location.Location.Builder().longitude(
                            MapFragment.longClickedPoint!!.longitude()).latitude(MapFragment.longClickedPoint!!.latitude()).build())
                        LocationHandler.setReplayLocationProvider()
                        viewModel.setShowLocationPanel(false)

                    }) {
                        Text(MapFragment.context.getString(R.string.move_to))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        MapFragment.setStartPoint(viewModel)
                        viewModel.setShowLocationPanel(false)
                    }) {
                        Text(MapFragment.context.getString(R.string.start_point))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        MapFragment.setEndPoint(viewModel)
                        viewModel.setShowLocationPanel(false)
                    }) {
                        Text(MapFragment.context.getString(R.string.end_point))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        MapFragment.removePlaceholder()
                        viewModel.setShowLocationPanel(false)
                    }) {
                        Text(MapFragment.context.getString(R.string.cancel))
                    }
                }
            }
        }

        @ExperimentalAnimationApi
        @Composable
        fun InfoList() {
            val lazyListState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            val viewModel: GeofencingViewModel = hiltViewModel()
            val infoArticles by viewModel.infoArticlesFlow.collectAsState()
            val isVisibleStates by viewModel.isVisibleStatesFlow.collectAsState()

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val maxHeight = maxHeight * 0.4f

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth(1.0f)
                        .padding(top = dimensionResource(id = R.dimen.list_top_padding))
                        .fillMaxWidth()
                        .heightIn(min = 0.dp, max = maxHeight)
                ) {
                    items(infoArticles.size) { index ->
                        val infoArticle = infoArticles.getOrNull(index)
                        if (infoArticle != null) {
                            key(infoArticle) {
                                InfoCard(infoArticle = infoArticle, isVisible = isVisibleStates[infoArticle] == true)
                            }
                        }
                    }
                    coroutineScope.launch {
                        lazyListState.scrollToItem(0)
                    }
                }
            }
        }

        private const val buttonSize = 60
        private const val buttonPadding = 10

        @SuppressLint("StateFlowValueCalledInComposition")
        @Composable
        fun Buttons() {
            val viewModel: GeofencingViewModel = hiltViewModel()
            val isNavigationReady = viewModel.isNavigationReadyFlow.collectAsState()
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .padding(buttonPadding.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    IconButton(
                        modifier = buttonsModifier(30, true),
                        onClick = { MapFragment.snapToCurrent() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_my_location_24),
                            tint = Color.Black,
                            contentDescription = "To Current Location"
                        )
                    }
                    IconButton(
                        modifier = buttonsModifier(30, true),
                        onClick = {
                            //SearchHandler.search(SearchHandler.coffeeQuery, "スターバックス", "starbucks")
                            SearchHandler.forwardSearch(MapFragment.context.getString(R.string.seven_eleven), "seveneleven")
                        }) {
                        Image(
                            painter = painterResource(id = R.drawable.seveneleven),
                            contentDescription = "Find Starbucks"
                        )
                    }
                    IconButton(
                        modifier = buttonsModifier(30, isNavigationReady.value),
                        onClick = { NavigationHandler.replayLocations() },
                        enabled = isNavigationReady.value) {
                        Image(
                            painter = painterResource(id = R.drawable.play),
                            contentDescription = "Play Navigation"
                        )
                    }
                    IconButton(
                        modifier = buttonsModifier(30, isNavigationReady.value),
                        onClick = {
                            NavigationHandler.stopReplayLocations(viewModel)
                            MapFragment.clearPoints()
                            viewModel.setShowLocationPanel(false)
                        },
                        enabled = isNavigationReady.value
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.stop),
                            contentDescription = "Stop Navigation"
                        )
                    }
                }
            }
        }

        private fun buttonsModifier(corners: Int, isEnabled: Boolean): Modifier {
            return Modifier
                .size(buttonSize.dp)
                .padding(buttonPadding.dp)
                .background(color = Color.White, shape = RoundedCornerShape(corners.dp)).then(
                    if(isEnabled)Modifier.alpha(1.0f) else Modifier.alpha(0.1f)
                )
        }

    }
}

//fun Offset.toDp(density: Float): Offset {
//    return Offset(this.x / density, this.y / density)
//}
