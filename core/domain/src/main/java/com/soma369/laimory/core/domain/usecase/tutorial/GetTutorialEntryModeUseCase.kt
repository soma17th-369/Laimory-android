package com.soma369.laimory.core.domain.usecase.tutorial

import com.soma369.laimory.core.domain.model.analytics.AnalyticsTutorialEntryMode
import com.soma369.laimory.core.domain.model.tutorial.Tutorial
import com.soma369.laimory.core.domain.repository.TutorialRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 봤음 기록이 없어 자동으로 뜨는 [tutorial] 회차가 처음 보는 것인지, 설정 `처음부터 따라 하기` 로 다시 시작한 것인지.
 *
 * debug 실험실 초기화([ResetTutorialsUseCase]) 뒤에 뜬 것은 처음 보는 것으로 센다.
 */
@Singleton
class GetTutorialEntryModeUseCase
    @Inject
    constructor(
        private val repository: TutorialRepository,
    ) {
        suspend operator fun invoke(tutorial: Tutorial): AnalyticsTutorialEntryMode =
            if (repository.isRestarted(tutorial)) AnalyticsTutorialEntryMode.REPLAY else AnalyticsTutorialEntryMode.FIRST_RUN
    }
