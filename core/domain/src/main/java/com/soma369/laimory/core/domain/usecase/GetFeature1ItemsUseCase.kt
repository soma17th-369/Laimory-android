package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.Feature1Item
import com.soma369.laimory.core.domain.repository.Feature1Repository

class GetFeature1ItemsUseCase(
    private val repository: Feature1Repository,
) {
    suspend operator fun invoke(): List<Feature1Item> = repository.getItems()
}
