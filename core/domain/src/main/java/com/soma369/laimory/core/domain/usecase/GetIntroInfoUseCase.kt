package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.IntroInfo
import com.soma369.laimory.core.domain.repository.IntroRepository

class GetIntroInfoUseCase(
    private val repository: IntroRepository,
    messageHelper: MessageHelper,
) : BaseUseCase(messageHelper) {
    suspend operator fun invoke(): Result<IntroInfo> = execute { repository.getIntroInfo() }
}
