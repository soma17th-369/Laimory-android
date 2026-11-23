package com.soma369.laimory.core.domain.usecase.tutorial

import com.soma369.laimory.core.domain.repository.TutorialRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 모든 튜토리얼을 보지 않은 상태로 되돌린다.
 *
 * 실기기에서 튜토리얼을 처음부터 다시 보기 위한 debug 실험실 도구다 — 다시 뜨는 회차는 처음 보는 것으로 센다. 사용자가
 * 설정에서 다시 시작하는 것은 [RestartTutorialsUseCase] 다. 앱 데이터를 지우는 것과 달리 수집해 둔
 * 데이터는 그대로다.
 */
@Singleton
class ResetTutorialsUseCase
    @Inject
    constructor(
        private val repository: TutorialRepository,
    ) {
        suspend operator fun invoke() = repository.resetAll()
    }
