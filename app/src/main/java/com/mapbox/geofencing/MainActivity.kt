package com.mapbox.geofencing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapbox.geofencing.logic.AppPermissions
import com.mapbox.geofencing.logic.GeofenceHandler
import com.mapbox.geofencing.ui.Controls
import com.mapbox.geofencing.model.GeofencingViewModel
import com.mapbox.geofencing.ui.MapFragment
import com.mapbox.geofencing.ui.Particle
import com.mapbox.geofencing.ui.theme.Black
import com.mapbox.geofencing.ui.theme.GeofencingPoCTheme
import com.mapbox.maps.MapboxExperimental
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp


@MapboxExperimental
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        AppPermissions(this) {
        }.requestPermissions()
        MapboxNavigationApp.attach(this)

        setContent {
            GeofencingPoCTheme {
                Home()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
    @Composable
    fun Home(){
        val viewModel: GeofencingViewModel = hiltViewModel()
        val particleColor = viewModel.particleColorFlow.collectAsState()
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(Black)
                ) {
                    Particle(
                        modifier = Modifier
                            .align(Alignment.BottomCenter),
                    )
                    TopAppBar(
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = particleColor.value,
                        ),
                        title = {
                            Text(text = "")
                        },
                    )
                }
            },
            content = { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    MapFragment.CreateMap()
                    Controls.InfoList()
                    Controls.Buttons()
                    val showPanel = viewModel.showLocationPanelFlow.collectAsState()
                    if (showPanel.value) {
                        Controls.MarkerPanel()
                    }
                    GeofenceHandler.createGeofence()
                }
            }
        )
    }
}