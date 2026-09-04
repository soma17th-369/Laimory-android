package com.soma369.laimory.core.domain.usecase.settings

import com.soma369.laimory.core.domain.model.settings.AppThemeMode
import com.soma369.laimory.core.domain.repository.AppThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** 앱 루트와 설정 화면이 같은 흐름을 본다. 고른 값이 곧바로 전체에 반영되는 근거다. */
@Singleton
class ObserveAppThemeModeUseCase
    @Inject
    constructor(
        private val repository: AppThemeRepository,
    ) {
        operator fun invoke(): Flow<AppThemeMode> = repository.themeMode
    }
