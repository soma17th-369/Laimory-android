package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.IntroInfo
import com.soma369.laimory.core.domain.repository.IntroRepository

class GetIntroInfoUseCase(
    private val repository: IntroRepository,
) {
    suspend operator fun invoke(): IntroInfo = repository.getIntroInfo()
}
