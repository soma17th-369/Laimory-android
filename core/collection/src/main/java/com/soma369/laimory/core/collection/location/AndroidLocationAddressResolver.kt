package com.soma369.laimory.core.collection.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import com.soma369.laimory.core.domain.model.collection.ResolvedAddress
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
        ): ResolvedAddress? {
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
        ): ResolvedAddress? =
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
        ): ResolvedAddress? =
            withContext(Dispatchers.IO) {
                getFromLocation(latitude, longitude, MAX_RESULTS).orEmpty().firstDisplayAddress()
            }

        /**
         * 한 줄 주소가 있는 첫 결과를 표시용 주소로 옮긴다.
         *
         * 층위는 **시·군·구 + 읍·면·동** 이다 — `오산시 부산동`, `강남구 역삼동`. 광역을 앞세우면
         * 같은 도 안에서 어디를 다녀도 카드 문구가 한 가지라 하루가 구분되지 않는다.
         * - [ResolvedAddress.city] = 시·군·구(`locality` → `subAdminArea` → `adminArea`)
         * - [ResolvedAddress.district] = 읍·면·동(`subLocality` → `thoroughfare`)
         *
         * 지역마다 어느 필드가 차는지 달라 순서대로 훑는다. 필드가 모두 비면 한 줄 주소에서
         * 읽어 내고([addressLineLayers]), 광역은 그래도 시·군·구가 없을 때만 마지막으로 쓴다 —
         * 광역시는 `locality` 가 비고 `adminArea` 에 `서울특별시` 만 오기도 한다.
         *
         * 한 줄 주소가 없으면 층위가 있어도 버린다 — 목록·말풍선이 쓰는 값이 없으면 표시가 반쪽이다.
         */
        private fun List<Address>.firstDisplayAddress(): ResolvedAddress? =
            firstNotNullOfOrNull { address ->
                val line = address.getAddressLine(0).normalized() ?: return@firstNotNullOfOrNull null
                val fromLine = addressLineLayers(line)
                val city =
                    address.locality.normalized()
                        ?: address.subAdminArea.normalized()
                        ?: fromLine.city
                        ?: address.adminArea.normalized()
                val district =
                    address.subLocality.normalized()
                        ?: fromLine.district
                        ?: address.thoroughfare.normalized()
                ResolvedAddress(
                    line = line,
                    city = city,
                    // 같은 이름이면 아래 층위를 버린다 — `locality` 와 `subLocality` 에 똑같이
                    // `마포구` 가 실려 오는 지역이 있어, 그대로 두면 `마포구 마포구` 가 된다.
                    district = district?.takeIf { it != city },
                )
            }

        private fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)

        private companion object {
            const val MAX_RESULTS = 1
        }
    }
