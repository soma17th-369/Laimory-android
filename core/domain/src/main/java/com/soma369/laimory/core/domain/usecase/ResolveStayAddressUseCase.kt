package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.provider.LocationAddressResolver
import com.soma369.laimory.core.domain.repository.StayAddressRepository
import javax.inject.Inject
import javax.inject.Singleton

/** STAY 좌표를 주소로 해석하고 같은 로컬 SourceItem에 저장한다. */
@Singleton
class ResolveStayAddressUseCase
    @Inject
    constructor(
        private val resolver: LocationAddressResolver,
        private val repository: StayAddressRepository,
    ) {
        /**
         * 해석한 주소. 주소를 찾지 못했으면 null.
         *
         * 저장 성공 여부가 아니라 **주소 자체**를 돌려준다 — 화면이 저장을 기다리지 않고 표시에
         * 바로 쓸 수 있어야 한다. 저장은 다음 조회를 위한 캐시라 대상 항목이 이미 사라졌더라도
         * 이번 화면의 표시까지 막을 이유가 없다.
         */
        suspend operator fun invoke(
            rawId: String,
            latitude: Double,
            longitude: Double,
        ): String? {
            val address = resolver.resolve(latitude, longitude)?.trim()?.takeIf(String::isNotEmpty) ?: return null
            repository.updateAddress(rawId, address)
            return address
        }
    }
