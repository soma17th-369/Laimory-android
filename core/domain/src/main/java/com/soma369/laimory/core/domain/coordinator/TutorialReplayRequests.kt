package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.model.tutorial.Tutorial
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 설정 `사용 방법` 에서 고른 튜토리얼을 그 화면에 전한다. 설정은 화면을 옮기기만 하고, 코치마크는 도착한 화면이 띄운다.
 *
 * 봤다는 기록과 따로 둔다 — 다시 보는 것은 기록을 지우는 일이 아니다. 요청은 한 번에 하나이고, 도착한 화면이 받아 가면
 * 비운다. 메모리에만 둔다.
 */
@Singleton
class TutorialReplayRequests
    @Inject
    constructor() {
        private val _pending = MutableStateFlow<Tutorial?>(null)

        /** 아직 받아 가지 않은 다시 보기 요청. */
        val pending: StateFlow<Tutorial?> = _pending.asStateFlow()

        fun request(tutorial: Tutorial) {
            _pending.value = tutorial
        }

        /** [tutorial] 요청을 받아 간다. 그 요청이 남아 있었으면 true — 둘이 동시에 받아도 한쪽만 띄운다. */
        fun consume(tutorial: Tutorial): Boolean = _pending.compareAndSet(tutorial, null)
    }
