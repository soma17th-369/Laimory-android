package com.soma369.laimory.core.domain.usecase.tutorial

import com.soma369.laimory.core.domain.model.tutorial.Tutorial
import com.soma369.laimory.core.domain.repository.TutorialRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** 튜토리얼을 띄울지 정하는 근거다. 보지 않은 상태로 되돌리면(debug 초기화) 다시 띄운다. */
@Singleton
class ObserveTutorialSeenUseCase
    @Inject
    constructor(
        private val repository: TutorialRepository,
    ) {
        operator fun invoke(tutorial: Tutorial): Flow<Boolean> = repository.observeSeen(tutorial)
    }
