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
        /**
         * 출발·도착 주소. 해석하지 못한 쪽은 null 이다.
         *
         * 이미 주소를 가진 쪽은 그대로 돌려준다 — 해석은 없는 쪽만 한다. 저장 성공 여부가 아니라
         * 주소 자체를 돌려주는 이유는 [ResolveStayAddressUseCase] 와 같다.
         */
        suspend operator fun invoke(
            rawId: String,
            start: GeoPoint,
            end: GeoPoint,
        ): ResolvedMovementAddresses =
            coroutineScope {
                // 이동은 목록에 한 줄로만 보이므로 층위는 쓰지 않는다.
                val startAddress =
                    async {
                        start.address.normalized() ?: resolver.resolve(start.latitude, start.longitude)?.line.normalized()
                    }
                val endAddress =
                    async {
                        end.address.normalized() ?: resolver.resolve(end.latitude, end.longitude)?.line.normalized()
                    }
                ResolvedMovementAddresses(startAddress.await(), endAddress.await())
                    .also { repository.updateAddresses(rawId, it.start, it.end) }
            }

        private fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)
    }
