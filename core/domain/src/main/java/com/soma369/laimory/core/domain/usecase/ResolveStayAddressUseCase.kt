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
        /** 주소를 해석해 저장했으면 true, 주소를 찾지 못했거나 대상이 없으면 false. */
        suspend operator fun invoke(
            rawId: String,
            latitude: Double,
            longitude: Double,
        ): Boolean {
            val address = resolver.resolve(latitude, longitude)?.trim()?.takeIf(String::isNotEmpty) ?: return false
            return repository.updateAddress(rawId, address)
        }
    }
