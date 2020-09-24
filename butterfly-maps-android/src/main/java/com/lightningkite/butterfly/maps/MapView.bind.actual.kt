package com.lightningkite.butterfly.maps

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.lightningkite.butterfly.location.GeoCoordinate
import com.lightningkite.butterfly.observables.MutableObservableProperty
import com.lightningkite.butterfly.observables.ObservableProperty
import com.lightningkite.butterfly.observables.addAndRunWeak
import com.lightningkite.butterfly.observables.subscribeBy
import com.lightningkite.butterfly.rx.DisposableLambda
import com.lightningkite.butterfly.rx.addWeak
import com.lightningkite.butterfly.rx.removed
import com.lightningkite.butterfly.rx.until
import com.lightningkite.butterfly.unownedSelf
import com.lightningkite.butterfly.views.ViewDependency

fun MapView.bind(dependency: ViewDependency, style: String? = null) {
    var resumed = true
    var destroyed = false
    this.onCreate(dependency.savedInstanceState)
    this.onResume()
    dependency.onResume.subscribe { value ->
        if(!resumed){
            resumed = true
            this.onResume()
        }
    }.until(removed)
    dependency.onPause.subscribe { value ->
        if(resumed){
            resumed = false
            this.onPause()
        }
    }.until(removed)
    dependency.onSaveInstanceState.subscribe { value ->
        this.onSaveInstanceState(value)
    }.until(removed)
    dependency.onLowMemory.subscribe { value ->
        this.onLowMemory()
    }.until(removed)
    dependency.onDestroy.subscribe { value ->
        if(!destroyed){
            destroyed = true
            this.onDestroy()
        }
    }.until(removed)
    this.removed.call(DisposableLambda  {
        if(resumed){
            resumed = false
            this.onPause()
        }
        if(!destroyed) {
            destroyed = true
            this.onDestroy()
        }
    })
    if (style != null) {
        getMapAsync { map ->
            map.setMapStyle(MapStyleOptions(style))
        }
    }
}

fun MapView.bindView(
    dependency: ViewDependency,
    position: ObservableProperty<GeoCoordinate?>,
    zoomLevel: Float = 15f,
    animate: Boolean = true,
    style: String? = null
) {
    bind(dependency, style)
    getMapAsync { map ->
        var marker: Marker? = null
        @Suppress("NAME_SHADOWING")
        position.subscribeBy { value ->
            if (value != null) {
                val newMarker = marker ?: map.addMarker(MarkerOptions().draggable(true).position(value.toMaps()))
                newMarker.position = value.toMaps()
                marker = newMarker
                if (animate) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(value.toMaps(), zoomLevel))
                } else {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(value.toMaps(), zoomLevel))
                }
            } else {
                marker?.remove()
                marker = null
            }
        }.until(this.removed)
    }
}


fun MapView.bindSelect(
    dependency: ViewDependency,
    position: MutableObservableProperty<GeoCoordinate?>,
    zoomLevel: Float = 15f,
    animate: Boolean = true,
    style: String? = null
) {
    bind(dependency)
    getMapAsync { map ->
        if (style != null) {
            map.setMapStyle(MapStyleOptions(style))
        }
        var suppress: Boolean = false
        var suppressAnimation: Boolean = false
        var marker: Marker? = null
        @Suppress("NAME_SHADOWING")
        position.subscribeBy { value ->
            if (!suppress) {
                suppress = true
                if (value != null) {
                    val newMarker = marker ?: map.addMarker(MarkerOptions().draggable(true).position(value.toMaps()))
                    newMarker.position = value.toMaps()
                    marker = newMarker
                    if (!suppressAnimation) {
                        if (animate) {
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(value.toMaps(), zoomLevel))
                        } else {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(value.toMaps(), zoomLevel))
                        }
                    }
                } else {
                    marker?.remove()
                    marker = null
                }
                suppress = false
            }
        }.until(this.removed)

        map.setOnMapLongClickListener { coord ->
            suppressAnimation = true
            position.value = coord.toButterfly()
            suppressAnimation = false
        }
        map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragEnd(marker: Marker) {
                if (!suppress) {
                    suppress = true
                    position.value = marker.position.toButterfly()
                    suppress = false
                }
            }

            override fun onMarkerDragStart(p0: Marker) {}
            override fun onMarkerDrag(p0: Marker) {}
        })
    }
}
