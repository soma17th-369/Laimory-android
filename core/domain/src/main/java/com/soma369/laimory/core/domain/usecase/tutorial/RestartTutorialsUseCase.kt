package com.soma369.laimory.core.domain.usecase.tutorial

import com.soma369.laimory.core.domain.repository.TutorialRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 설정 `사용 방법 › 처음부터 따라 하기`. 모든 튜토리얼을 보지 않은 상태로 되돌려 홈 추천 날짜부터 다시 뜨게 하되,
 * 다시 시작한 것으로 표시해 분석에서 처음 보는 회차와 섞이지 않게 한다([GetTutorialEntryModeUseCase]).
 */
@Singleton
class RestartTutorialsUseCase
    @Inject
    constructor(
        private val repository: TutorialRepository,
    ) {
        suspend operator fun invoke() = repository.restartAll()
    }
