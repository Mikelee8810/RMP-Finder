package com.mike.rmpfinder

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mike.rmpfinder.ui.RmpFinderRoot
import com.mike.rmpfinder.ui.RmpFinderTheme
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var mapView: MapView

    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            loadCurrentLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)
        setContent {
            RmpFinderTheme {
                RmpFinderRoot(
                    viewModel = viewModel,
                    mapView = mapView,
                    onRequestLocation = ::requestLocation,
                    onOpenLocationSettings = ::openLocationSettings,
                )
            }
        }
        requestLocation()
    }

    private fun requestLocation() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            loadCurrentLocation()
        } else {
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun loadCurrentLocation() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return
        val priority = if (fine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        val token = CancellationTokenSource()
        LocationServices.getFusedLocationProviderClient(this)
            .getCurrentLocation(priority, token.token)
            .addOnSuccessListener { location ->
                location?.let { viewModel.setUserLocation(it.latitude, it.longitude) }
            }
    }

    private fun openLocationSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onStop() { mapView.onStop(); super.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onDestroy() { mapView.onDestroy(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState) }
}
