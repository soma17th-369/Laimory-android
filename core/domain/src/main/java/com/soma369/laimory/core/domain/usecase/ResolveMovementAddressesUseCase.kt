package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.provider.LocationAddressResolver
import com.soma369.laimory.core.domain.repository.MovementAddressRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/** MOVEMENT 출발·도착 좌표를 주소로 해석하고 같은 로컬 SourceItem에 한 번에 저장한다. */
@Singleton
class ResolveMovementAddressesUseCase
    @Inject
    constructor(
        private val resolver: LocationAddressResolver,
        private val repository: MovementAddressRepository,
    ) {
        /** 하나 이상의 주소를 해석해 저장했으면 true. */
        suspend operator fun invoke(
            rawId: String,
            start: GeoPoint,
            end: GeoPoint,
        ): Boolean =
            coroutineScope {
                val startAddress =
                    async {
                        start.address.normalized() ?: resolver.resolve(start.latitude, start.longitude).normalized()
                    }
                val endAddress =
                    async {
                        end.address.normalized() ?: resolver.resolve(end.latitude, end.longitude).normalized()
                    }
                repository.updateAddresses(
                    rawId = rawId,
                    startAddress = startAddress.await(),
                    endAddress = endAddress.await(),
                )
            }

        private fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)
    }
