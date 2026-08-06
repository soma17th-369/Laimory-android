package com.soma369.laimory.core.collection.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import com.soma369.laimory.core.domain.provider.LocationAddressResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Android [Geocoder]로 수집 위치 좌표를 로컬 주소로 변환한다. */
@Singleton
internal class AndroidLocationAddressResolver
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : LocationAddressResolver {
        override suspend fun resolve(
            latitude: Double,
            longitude: Double,
        ): String? {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(context, Locale.getDefault())
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.resolveAsync(latitude, longitude)
                } else {
                    geocoder.resolveBlocking(latitude, longitude)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private suspend fun Geocoder.resolveAsync(
            latitude: Double,
            longitude: Double,
        ): String? =
            suspendCancellableCoroutine { continuation ->
                getFromLocation(
                    latitude,
                    longitude,
                    MAX_RESULTS,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses.firstDisplayAddress())
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    },
                )
            }

        @Suppress("DEPRECATION")
        private suspend fun Geocoder.resolveBlocking(
            latitude: Double,
            longitude: Double,
        ): String? =
            withContext(Dispatchers.IO) {
                getFromLocation(latitude, longitude, MAX_RESULTS).orEmpty().firstDisplayAddress()
            }

        private fun List<Address>.firstDisplayAddress(): String? =
            firstNotNullOfOrNull { address ->
                address.getAddressLine(0)?.trim()?.takeIf(String::isNotEmpty)
            }

        private companion object {
            const val MAX_RESULTS = 1
        }
    }
