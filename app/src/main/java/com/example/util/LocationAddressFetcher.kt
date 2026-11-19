package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class FetchedLocationAddress(
    val city: String,
    val fullAddress: String,
    val state: String = "",
    val postalCode: String = "",
)

object LocationAddressFetcher {

    fun hasLocationPermission(context: Context): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    suspend fun fetchCurrentAddress(context: Context): Result<FetchedLocationAddress> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) {
            return@withContext Result.failure(
                IllegalStateException("Location permission required to auto-fetch your address.")
            )
        }
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext Result.failure(IllegalStateException("Location service unavailable on this device."))

        val location = resolveBestLocation(context, locationManager)
            ?: return@withContext Result.failure(
                IllegalStateException("Unable to detect location right now. Please turn on Location/GPS or type your city manually.")
            )

        val geocoded = reverseGeocode(context, location.latitude, location.longitude)
        if (geocoded != null) {
            Result.success(geocoded)
        } else {
            val fallbackCoords = String.format(Locale.US, "%.4f, %.4f", location.latitude, location.longitude)
            Result.success(
                FetchedLocationAddress(
                    city = "Nearby Area",
                    fullAddress = "Location ($fallbackCoords)",
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun resolveBestLocation(
        context: Context,
        locationManager: LocationManager,
    ): Location? {
        val providers = buildList {
            if (runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
            if (runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (runCatching { locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER) }.getOrDefault(false)) {
                add(LocationManager.PASSIVE_PROVIDER)
            }
        }

        // 1. Try fresh current location on Android 11+ (fast timeout)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val primaryProvider = providers.firstOrNull { it != LocationManager.PASSIVE_PROVIDER }
                ?: providers.firstOrNull()
            if (primaryProvider != null) {
                val fresh = withTimeoutOrNull(4500L) {
                    suspendCancellableCoroutine<Location?> { cont ->
                        val signal = CancellationSignal()
                        cont.invokeOnCancellation { signal.cancel() }
                        runCatching {
                            locationManager.getCurrentLocation(
                                primaryProvider,
                                signal,
                                context.mainExecutor
                            ) { loc ->
                                if (cont.isActive) cont.resume(loc)
                            }
                        }.onFailure {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                }
                if (fresh != null) return fresh
            }
        }

        // 2. Fallback to most recent last known location across all enabled/available providers
        val allProviders = runCatching { locationManager.getProviders(true) }.getOrDefault(emptyList()) +
            listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        var best: Location? = null
        for (provider in allProviders.distinct()) {
            val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() ?: continue
            if (best == null || loc.time > best.time) {
                best = loc
            }
        }
        return best
    }

    private suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double,
    ): FetchedLocationAddress? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        val address: Address? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withTimeoutOrNull(4000L) {
                suspendCancellableCoroutine { cont ->
                    runCatching {
                        geocoder.getFromLocation(latitude, longitude, 1) { list ->
                            if (cont.isActive) cont.resume(list.firstOrNull())
                        }
                    }.onFailure {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            runCatching { geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull() }.getOrNull()
        }

        if (address == null) return null

        val city = listOfNotNull(
            address.locality,
            address.subAdminArea,
            address.adminArea
        ).firstOrNull { it.isNotBlank() }?.trim().orEmpty()

        val areaParts = listOfNotNull(
            address.subLocality?.takeIf { it.isNotBlank() },
            address.thoroughfare?.takeIf { it.isNotBlank() },
            city.takeIf { it.isNotBlank() },
            address.adminArea?.takeIf { it.isNotBlank() && !it.equals(city, ignoreCase = true) },
            address.postalCode?.takeIf { it.isNotBlank() }
        ).distinct()

        val fullAddress = address.getAddressLine(0)?.trim()?.takeIf { it.isNotBlank() }
            ?: areaParts.joinToString(", ")

        return FetchedLocationAddress(
            city = city.ifBlank { areaParts.firstOrNull().orEmpty() },
            fullAddress = fullAddress.ifBlank { city },
            state = address.adminArea.orEmpty(),
            postalCode = address.postalCode.orEmpty(),
        )
    }

    /**
     * Extracts normalized lowercase tokens (city, area, locality words) for smart nearby matching.
     */
    fun extractLocationTokens(city: String, address: String): Set<String> {
        val stopWords = setOf(
            "india", "road", "street", "lane", "near", "opposite", "sector", "block",
            "phase", "floor", "flat", "house", "building", "nagar", "colony", "area",
            "city", "district", "state", "pin", "code", "verified", "center"
        )
        return "$city $address"
            .lowercase(Locale.ROOT)
            .split(Regex("[^a-z0-9]+"))
            .map { it.trim() }
            .filter { it.length >= 3 && it !in stopWords && !it.all(Char::isDigit) }
            .toSet()
    }

    /**
     * Derives a clean public city label from an admin-entered address string.
     */
    fun extractPublicCityFromAddress(address: String): String {
        val clean = address.trim()
        if (clean.isEmpty()) return ""
        val parts = clean.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val candidate = when {
            parts.size >= 2 -> parts[parts.size - 2].takeIf { !it.all(Char::isDigit) } ?: parts.last()
            else -> parts.firstOrNull().orEmpty()
        }
        return candidate.replace(Regex("\\d{5,6}"), "").trim().take(60)
    }
}
