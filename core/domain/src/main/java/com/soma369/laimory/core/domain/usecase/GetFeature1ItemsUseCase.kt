package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.model.Feature1Item
import com.soma369.laimory.core.domain.notification.MessageHelper
import com.soma369.laimory.core.domain.repository.Feature1Repository

class GetFeature1ItemsUseCase(
    private val repository: Feature1Repository,
    messageHelper: MessageHelper,
) : BaseUseCase(messageHelper) {
    suspend operator fun invoke(): Result<List<Feature1Item>> = execute { repository.getItems() }
}
