package com.soma369.laimory.core.domain.usecase.settings

import com.soma369.laimory.core.domain.model.settings.AppThemeMode
import com.soma369.laimory.core.domain.repository.AppThemeRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 고른 화면 모드를 저장한다.
 *
 * 실패를 값으로 돌려준다 — 저장에 실패했는데 화면만 바뀌면, 다음 실행에서 되돌아온 테마를 보고
 * 앱이 제 설정을 잊었다고 여기게 된다.
 */
@Singleton
class SetAppThemeModeUseCase
    @Inject
    constructor(
        private val repository: AppThemeRepository,
    ) {
        suspend operator fun invoke(mode: AppThemeMode): Result<Unit> = runCatching { repository.setThemeMode(mode) }
    }
