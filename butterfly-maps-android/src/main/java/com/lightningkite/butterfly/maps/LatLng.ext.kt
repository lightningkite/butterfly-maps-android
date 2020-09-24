package com.lightningkite.butterfly.maps

import com.google.android.gms.maps.model.LatLng
import com.lightningkite.butterfly.location.GeoCoordinate

fun GeoCoordinate.toMaps(): LatLng = LatLng(latitude, longitude)
fun LatLng.toButterfly(): GeoCoordinate =
    GeoCoordinate(latitude, longitude)
