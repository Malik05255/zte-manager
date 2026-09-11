package com.malik.ztesmartmanager

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val OPEN_FREE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"

/**
 * Real interactive vector map backed by OpenFreeMap/OpenStreetMap through MapLibre Native.
 * We never invent a tower coordinate. Only verified coordinates are ever drawn as tower markers.
 */
@Composable
internal fun RealNetworkMap(
    modifier: Modifier = Modifier,
    towerLocationAvailable: Boolean = false,
    towerLatitude: Double? = null,
    towerLongitude: Double? = null
) {
    val context = LocalContext.current
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var locationMessage by remember { mutableStateOf("الخريطة حقيقية • موقع البرج لا يُعرض إلا إذا توفر مصدر إحداثيات موثوق") }

    val mapView = remember(context) {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply {
            onCreate(Bundle())
            getMapAsync { readyMap ->
                map = readyMap
                readyMap.setStyle(Style.Builder().fromUri(OPEN_FREE_MAP_STYLE))
                readyMap.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(20.0, 20.0))
                    .zoom(1.8)
                    .build()
            }
        }
    }

    DisposableEffect(mapView) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    fun moveToKnownLocation() {
        val location = bestLastKnownLocation(context)
        if (location == null) {
            locationMessage = "لم يتوفر موقع حديث من الجهاز؛ افتح خدمات الموقع ثم أعد المحاولة"
            return
        }
        map?.let { readyMap ->
            readyMap.clear()
            val point = LatLng(location.latitude, location.longitude)
            readyMap.addMarker(
                MarkerOptions()
                    .position(point)
                    .title("موقعي")
                    .snippet("دقة تقريبية ${location.accuracy.toInt()} م")
            )
            if (towerLocationAvailable && towerLatitude != null && towerLongitude != null) {
                readyMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(towerLatitude, towerLongitude))
                        .title("البرج الموثق")
                )
            }
            readyMap.cameraPosition = CameraPosition.Builder().target(point).zoom(14.5).build()
            locationMessage = "تم تحديد موقعك على الخريطة الفعلية"
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            moveToKnownLocation()
        } else {
            locationMessage = "إذن الموقع غير ممنوح؛ الخريطة تعمل بدون تحديد موقعك"
        }
    }

    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    Box(modifier.clip(RoundedCornerShape(24.dp)).background(Color(0xFFEAF1F7))) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        Column(
            Modifier.align(Alignment.TopStart).padding(10.dp)
                .clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .94f)).padding(horizontal = 11.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(MpGreen))
                Spacer(Modifier.size(6.dp))
                Text("خريطة مباشرة", color = MpInk, fontWeight = FontWeight.Black)
            }
            Text("OpenStreetMap / OpenFreeMap", color = MpMuted, fontSize = 9.sp)
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp)
                .clip(RoundedCornerShape(17.dp)).background(Color.White.copy(alpha = .96f)).padding(10.dp)
        ) {
            Text(locationMessage, color = MpInk, fontSize = 10.sp, lineHeight = 14.sp)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(MpBlue)
                    .clickable {
                        if (hasPermission) moveToKnownLocation()
                        else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (hasPermission) "◎ حدّد موقعي" else "◎ السماح بالموقع",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun bestLastKnownLocation(context: Context): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
    return providers.mapNotNull { provider ->
        runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
    }.maxByOrNull { it.time }
}
