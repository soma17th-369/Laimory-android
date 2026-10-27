package com.soma369.laimory.core.domain.usecase.tutorial

import com.soma369.laimory.core.domain.model.tutorial.Tutorial
import com.soma369.laimory.core.domain.repository.TutorialRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 튜토리얼을 끝냈거나 건너뛰었다. 이 기기에서는 다시 띄우지 않는다. */
@Singleton
class MarkTutorialSeenUseCase
    @Inject
    constructor(
        private val repository: TutorialRepository,
    ) {
        suspend operator fun invoke(tutorial: Tutorial) = repository.markSeen(tutorial)
    }
