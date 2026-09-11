package com.malik.ztesmartmanager

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
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
import androidx.compose.runtime.LaunchedEffect
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
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

private const val PULSE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val PULSE_USER_SOURCE = "pulse-user-source"
private const val PULSE_TOWER_SOURCE = "pulse-tower-source"
private const val PULSE_LINK_SOURCE = "pulse-link-source"
private const val PULSE_USER_LAYER = "pulse-user-layer"
private const val PULSE_TOWER_LAYER = "pulse-tower-layer"
private const val PULSE_LINK_LAYER = "pulse-link-layer"
private const val EMPTY_GEOJSON = "{\"type\":\"FeatureCollection\",\"features\":[]}"

private data class VerifiedTowerCoordinate(val latitude: Double, val longitude: Double)

@Composable
internal fun PulseNetworkMap(
    snapshot: RouterSnapshot,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val verifiedTower = remember(snapshot.raw) { verifiedTowerCoordinate(snapshot) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var userSource by remember { mutableStateOf<GeoJsonSource?>(null) }
    var towerSource by remember { mutableStateOf<GeoJsonSource?>(null) }
    var linkSource by remember { mutableStateOf<GeoJsonSource?>(null) }
    var mapReady by remember { mutableStateOf(false) }
    var message by remember(verifiedTower) {
        mutableStateOf(
            if (verifiedTower == null) {
                "سنحدد موقعك بدقة. الراوتر لم يرسل موقعًا جغرافيًا موثقًا للبرج، لذلك لن نرسم خطًا وهميًا."
            } else {
                "موقع البرج متوفر من بيانات موثقة في الراوتر؛ سنرسم الاتصال الحقيقي بعد تحديد موقعك."
            }
        )
    }

    val mapView = remember(context) {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply {
            onCreate(Bundle())
            getMapAsync { readyMap ->
                map = readyMap
                readyMap.uiSettings.apply {
                    isCompassEnabled = false
                    isLogoEnabled = false
                    isAttributionEnabled = true
                }
                readyMap.setStyle(Style.Builder().fromUri(PULSE_MAP_STYLE)) { style ->
                    val users = GeoJsonSource(PULSE_USER_SOURCE, EMPTY_GEOJSON)
                    val towers = GeoJsonSource(PULSE_TOWER_SOURCE, EMPTY_GEOJSON)
                    val links = GeoJsonSource(PULSE_LINK_SOURCE, EMPTY_GEOJSON)
                    style.addSource(users)
                    style.addSource(towers)
                    style.addSource(links)

                    style.addLayer(
                        LineLayer(PULSE_LINK_LAYER, PULSE_LINK_SOURCE).withProperties(
                            PropertyFactory.lineColor(AndroidColor.parseColor("#20C888")),
                            PropertyFactory.lineWidth(4.0f),
                            PropertyFactory.lineOpacity(0.82f)
                        )
                    )
                    style.addLayer(
                        CircleLayer(PULSE_USER_LAYER, PULSE_USER_SOURCE).withProperties(
                            PropertyFactory.circleColor(AndroidColor.parseColor("#176BFF")),
                            PropertyFactory.circleRadius(8.0f),
                            PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
                            PropertyFactory.circleStrokeWidth(3.0f),
                            PropertyFactory.circleOpacity(1.0f)
                        )
                    )
                    style.addLayer(
                        CircleLayer(PULSE_TOWER_LAYER, PULSE_TOWER_SOURCE).withProperties(
                            PropertyFactory.circleColor(AndroidColor.parseColor("#20C888")),
                            PropertyFactory.circleRadius(9.0f),
                            PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
                            PropertyFactory.circleStrokeWidth(3.0f),
                            PropertyFactory.circleOpacity(1.0f)
                        )
                    )
                    userSource = users
                    towerSource = towers
                    linkSource = links
                    mapReady = true
                }
                readyMap.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(23.8859, 45.0792))
                    .zoom(4.2)
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

    fun locate() {
        val location = bestRecentLocation(context)
        if (location == null) {
            message = "لم نحصل على موقع حديث من الهاتف. فعّل خدمات الموقع ثم جرّب مرة أخرى."
            return
        }
        val users = userSource
        val towers = towerSource
        val links = linkSource
        if (!mapReady || users == null || towers == null || links == null) {
            message = "الخريطة ما زالت تُجهز. أعد المحاولة بعد لحظة."
            return
        }

        users.setGeoJson(pointGeoJson(location.latitude, location.longitude))
        val tower = verifiedTower
        if (tower != null) {
            towers.setGeoJson(pointGeoJson(tower.latitude, tower.longitude))
            links.setGeoJson(lineGeoJson(location.latitude, location.longitude, tower.latitude, tower.longitude))

            val results = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, tower.latitude, tower.longitude, results)
            val distanceM = results.firstOrNull()?.toDouble() ?: 0.0
            val midLat = (location.latitude + tower.latitude) / 2.0
            val midLon = (location.longitude + tower.longitude) / 2.0
            val zoom = when {
                distanceM < 800 -> 14.0
                distanceM < 2_500 -> 12.5
                distanceM < 7_000 -> 11.0
                distanceM < 20_000 -> 9.5
                else -> 8.0
            }
            map?.cameraPosition = CameraPosition.Builder().target(LatLng(midLat, midLon)).zoom(zoom).build()
            message = "تم تحديد موقعك والبرج ورسم خط الاتصال بينهما من إحداثيات موثقة."
        } else {
            towers.setGeoJson(EMPTY_GEOJSON)
            links.setGeoJson(EMPTY_GEOJSON)
            map?.cameraPosition = CameraPosition.Builder()
                .target(LatLng(location.latitude, location.longitude))
                .zoom(14.8)
                .build()
            message = "موقعك ظاهر الآن. موقع البرج الجغرافي غير متوفر من الراوتر؛ نعرض الحقيقة بدل خط تخميني."
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) locate() else message = "بدون إذن الموقع يمكن عرض الخريطة، لكن لا يمكن وضع موقعك عليها."
    }

    val locationAllowed = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(mapReady, locationAllowed, verifiedTower) {
        if (mapReady && locationAllowed) locate()
    }

    Box(modifier.clip(RoundedCornerShape(26.dp)).background(Color(0xFFE9EFF5))) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        Row(
            Modifier.align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.94f))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF176BFF)))
            Spacer(Modifier.size(5.dp))
            Text("أنت", color = Color(0xFF0C1D33), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(10.dp))
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF20C888)))
            Spacer(Modifier.size(5.dp))
            Text("البرج", color = Color(0xFF0C1D33), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(10.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.96f))
                .padding(11.dp)
        ) {
            Text(message, color = Color(0xFF21344D), fontSize = 11.sp, lineHeight = 16.sp)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0C1D33))
                    .clickable {
                        if (locationAllowed) locate()
                        else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (locationAllowed) "حدّث موقعي" else "اسمح بتحديد موقعي",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun verifiedTowerCoordinate(snapshot: RouterSnapshot): VerifiedTowerCoordinate? {
    val pairs = listOf(
        "tower_lat" to "tower_lng",
        "tower_lat" to "tower_lon",
        "tower_latitude" to "tower_longitude",
        "cell_lat" to "cell_lng",
        "cell_lat" to "cell_lon",
        "cell_latitude" to "cell_longitude",
        "base_station_latitude" to "base_station_longitude"
    )
    for ((latKey, lonKey) in pairs) {
        val lat = snapshot.raw[latKey]?.trim()?.toDoubleOrNull() ?: continue
        val lon = snapshot.raw[lonKey]?.trim()?.toDoubleOrNull() ?: continue
        if (lat in -90.0..90.0 && lon in -180.0..180.0 && !(lat == 0.0 && lon == 0.0)) {
            return VerifiedTowerCoordinate(lat, lon)
        }
    }
    return null
}

private fun pointGeoJson(latitude: Double, longitude: Double): String =
    "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[$longitude,$latitude]}}]}"

private fun lineGeoJson(userLat: Double, userLon: Double, towerLat: Double, towerLon: Double): String =
    "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[$userLon,$userLat],[$towerLon,$towerLat]]}}]}"

@SuppressLint("MissingPermission")
private fun bestRecentLocation(context: Context): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }
}
