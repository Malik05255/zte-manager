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
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

private const val NOVA_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val NOVA_USER_SOURCE = "nova-user-source"
private const val NOVA_USER_LAYER = "nova-user-layer"
private const val NOVA_TOWER_SOURCE = "nova-tower-source"
private const val NOVA_TOWER_LAYER = "nova-tower-layer"
private const val NOVA_EMPTY_GEOJSON = "{\"type\":\"FeatureCollection\",\"features\":[]}"

@Composable
internal fun RealNetworkMap(
    modifier: Modifier = Modifier,
    towerLocationAvailable: Boolean = false,
    towerLatitude: Double? = null,
    towerLongitude: Double? = null
) {
    val context = LocalContext.current
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var userSource by remember { mutableStateOf<GeoJsonSource?>(null) }
    var towerSource by remember { mutableStateOf<GeoJsonSource?>(null) }
    var message by remember { mutableStateOf("الخريطة حقيقية • لا نضع برجًا بلا إحداثيات موثقة") }

    val mapView = remember(context) {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply {
            onCreate(Bundle())
            getMapAsync { ready ->
                map = ready
                ready.setStyle(Style.Builder().fromUri(NOVA_MAP_STYLE)) { style ->
                    val users = GeoJsonSource(NOVA_USER_SOURCE, NOVA_EMPTY_GEOJSON)
                    val towers = GeoJsonSource(NOVA_TOWER_SOURCE, NOVA_EMPTY_GEOJSON)
                    style.addSource(users)
                    style.addSource(towers)
                    style.addLayer(
                        CircleLayer(NOVA_USER_LAYER, NOVA_USER_SOURCE).withProperties(
                            PropertyFactory.circleColor(AndroidColor.parseColor("#2D7CFF")),
                            PropertyFactory.circleRadius(7.5f),
                            PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
                            PropertyFactory.circleStrokeWidth(2.5f),
                            PropertyFactory.circleOpacity(.96f)
                        )
                    )
                    style.addLayer(
                        CircleLayer(NOVA_TOWER_LAYER, NOVA_TOWER_SOURCE).withProperties(
                            PropertyFactory.circleColor(AndroidColor.parseColor("#20C888")),
                            PropertyFactory.circleRadius(8.5f),
                            PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
                            PropertyFactory.circleStrokeWidth(2.5f),
                            PropertyFactory.circleOpacity(.96f)
                        )
                    )
                    userSource = users
                    towerSource = towers
                }
                ready.cameraPosition = CameraPosition.Builder().target(LatLng(20.0, 20.0)).zoom(1.8).build()
            }
        }
    }

    DisposableEffect(mapView) {
        mapView.onStart(); mapView.onResume()
        onDispose { mapView.onPause(); mapView.onStop(); mapView.onDestroy() }
    }

    fun locate() {
        val location = novaBestLastLocation(context)
        if (location == null) {
            message = "لا يوجد موقع حديث؛ فعّل خدمات الموقع ثم أعد المحاولة"
            return
        }
        val users = userSource
        val towers = towerSource
        if (users == null || towers == null) {
            message = "جاري تجهيز طبقات الخريطة"
            return
        }
        users.setGeoJson(novaPoint(location.latitude, location.longitude))
        if (towerLocationAvailable && towerLatitude != null && towerLongitude != null) {
            towers.setGeoJson(novaPoint(towerLatitude, towerLongitude))
        } else {
            towers.setGeoJson(NOVA_EMPTY_GEOJSON)
        }
        map?.cameraPosition = CameraPosition.Builder().target(LatLng(location.latitude, location.longitude)).zoom(14.5).build()
        message = "موقعك محدد • دقة تقريبية ${location.accuracy.toInt()} م"
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) locate()
        else message = "إذن الموقع غير ممنوح؛ الخريطة تعمل بدون تحديد موقعك"
    }

    val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    Box(modifier.clip(RoundedCornerShape(28.dp)).background(Color(0xFFEAF0F6))) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Column(
            Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = .94f)).padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF20C888)))
                Spacer(Modifier.size(5.dp))
                Text("خريطة حيّة", color = Color(0xFF071525), fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Text("OpenStreetMap • OpenFreeMap", color = Color(0xFF728096), fontSize = 8.sp)
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp).clip(RoundedCornerShape(17.dp))
                .background(Color.White.copy(alpha = .96f)).padding(10.dp)
        ) {
            Text(message, color = Color(0xFF071525), fontSize = 9.sp, lineHeight = 13.sp)
            Spacer(Modifier.height(7.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF06101F))
                    .clickable {
                        if (allowed) locate()
                        else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (allowed) "◎ حدّد موقعي" else "◎ السماح بالموقع", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

private fun novaPoint(latitude: Double, longitude: Double): String =
    "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Point\",\"coordinates\":[$longitude,$latitude]}}]}"

@SuppressLint("MissingPermission")
private fun novaBestLastLocation(context: Context): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
}
