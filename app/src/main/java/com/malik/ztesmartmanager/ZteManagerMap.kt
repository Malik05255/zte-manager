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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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

private const val ZTE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val ZTE_USER_SOURCE = "zte-user-source"
private const val ZTE_TOWER_SOURCE = "zte-tower-source"
private const val ZTE_LINK_SOURCE = "zte-link-source"
private const val ZTE_USER_LAYER = "zte-user-layer"
private const val ZTE_TOWER_LAYER = "zte-tower-layer"
private const val ZTE_LINK_LAYER = "zte-link-layer"
private const val ZTE_EMPTY_GEOJSON = "{\"type\":\"FeatureCollection\",\"features\":[]}"

private data class ZteVerifiedTowerCoordinate(val latitude: Double, val longitude: Double)

@Composable
internal fun ZteManagerMap(snapshot: RouterSnapshot, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val verifiedTower = remember(snapshot.raw) { zteVerifiedTowerCoordinate(snapshot) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var userSource by remember { mutableStateOf<GeoJsonSource?>(null) }
    var towerSource by remember { mutableStateOf<GeoJsonSource?>(null) }
    var linkSource by remember { mutableStateOf<GeoJsonSource?>(null) }
    var mapReady by remember { mutableStateOf(false) }
    var message by remember(verifiedTower) {
        mutableStateOf(if (verifiedTower == null) "موقع البرج غير موثق بعد." else "البرج موثق؛ حدّث موقعك لعرض الخط الحقيقي.")
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
                readyMap.setStyle(Style.Builder().fromUri(ZTE_MAP_STYLE)) { style ->
                    val users = GeoJsonSource(ZTE_USER_SOURCE, ZTE_EMPTY_GEOJSON)
                    val towers = GeoJsonSource(ZTE_TOWER_SOURCE, ZTE_EMPTY_GEOJSON)
                    val links = GeoJsonSource(ZTE_LINK_SOURCE, ZTE_EMPTY_GEOJSON)
                    style.addSource(users)
                    style.addSource(towers)
                    style.addSource(links)
                    style.addLayer(
                        LineLayer(ZTE_LINK_LAYER, ZTE_LINK_SOURCE).withProperties(
                            PropertyFactory.lineColor(AndroidColor.parseColor("#1677FF")),
                            PropertyFactory.lineWidth(5.0f),
                            PropertyFactory.lineOpacity(0.90f)
                        )
                    )
                    style.addLayer(
                        CircleLayer(ZTE_USER_LAYER, ZTE_USER_SOURCE).withProperties(
                            PropertyFactory.circleColor(AndroidColor.parseColor("#0D3E88")),
                            PropertyFactory.circleRadius(9.5f),
                            PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
                            PropertyFactory.circleStrokeWidth(4.0f)
                        )
                    )
                    style.addLayer(
                        CircleLayer(ZTE_TOWER_LAYER, ZTE_TOWER_SOURCE).withProperties(
                            PropertyFactory.circleColor(AndroidColor.parseColor("#24B8C9")),
                            PropertyFactory.circleRadius(10.5f),
                            PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
                            PropertyFactory.circleStrokeWidth(4.0f)
                        )
                    )
                    userSource = users
                    towerSource = towers
                    linkSource = links
                    mapReady = true
                }
                readyMap.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(23.8859, 45.0792))
                    .zoom(4.5)
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
        val location = zteBestRecentLocation(context)
        if (location == null) {
            message = "لم نحصل على موقع حديث. فعّل الموقع ثم جرّب مرة أخرى."
            return
        }
        val users = userSource
        val towers = towerSource
        val links = linkSource
        if (!mapReady || users == null || towers == null || links == null) {
            message = "الخريطة ما زالت تُجهز. جرّب بعد لحظة."
            return
        }

        users.setGeoJson(ztePointGeoJson(location.latitude, location.longitude))
        val tower = verifiedTower
        if (tower != null) {
            towers.setGeoJson(ztePointGeoJson(tower.latitude, tower.longitude))
            links.setGeoJson(zteLineGeoJson(location.latitude, location.longitude, tower.latitude, tower.longitude))
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
            message = "موقعك والبرج والخط بينهما معروضة من بيانات موثقة."
        } else {
            towers.setGeoJson(ZTE_EMPTY_GEOJSON)
            links.setGeoJson(ZTE_EMPTY_GEOJSON)
            map?.cameraPosition = CameraPosition.Builder()
                .target(LatLng(location.latitude, location.longitude))
                .zoom(15.0)
                .build()
            message = "موقعك ظاهر. موقع البرج غير موثق، لذلك لن نرسم خطًا وهميًا."
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) locate() else message = "اسمح بالموقع حتى نضعك على الخريطة."
    }

    val locationAllowed = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(mapReady, locationAllowed, verifiedTower) {
        if (mapReady && locationAllowed) locate()
    }

    Box(modifier.clip(RoundedCornerShape(24.dp)).background(Color(0xFFE9F1FA))) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        Row(
            Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.96f))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(ZteDeepBlue))
            Spacer(Modifier.width(7.dp))
            Text("موقعك", color = ZteInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (verifiedTower != null) {
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(10.dp).clip(CircleShape).background(ZteCyan))
                Spacer(Modifier.width(7.dp))
                Text("البرج", color = ZteInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.96f))
                .padding(12.dp)
        ) {
            Text(message, color = ZteInk, fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(9.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ZteBlue)
                    .clickable {
                        if (locationAllowed) locate()
                        else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (locationAllowed) "حدّث موقعي" else "اسمح بتحديد موقعي", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

private fun zteVerifiedTowerCoordinate(snapshot: RouterSnapshot): ZteVerifiedTowerCoordinate? {
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
        if (lat in -90.0..90.0 && lon in -180.0..180.0 && !(lat == 0.0 && lon == 0.0)) return ZteVerifiedTowerCoordinate(lat, lon)
    }
    return null
}

private fun ztePointGeoJson(latitude: Double, longitude: Double): String =
    "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[$longitude,$latitude]}}]}"

private fun zteLineGeoJson(userLat: Double, userLon: Double, towerLat: Double, towerLon: Double): String =
    "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[$userLon,$userLat],[$towerLon,$towerLat]]}}]}"

@SuppressLint("MissingPermission")
private fun zteBestRecentLocation(context: Context): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }
}
