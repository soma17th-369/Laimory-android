package com.soma369.laimory.feature.home.draft

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 진행 중인 초안 작업 하나의 로딩 표시용 스냅샷을 들고 있는 인메모리 보관소.
 *
 * 동의 화면이 제출에 성공한 시점에 넣고, 로딩 화면이 꺼내 쓴다. 홈의 생성 중 카드로 다시 들어와도
 * 같은 스냅샷을 재사용한다. 작업이 끝나거나(성공·실패) 다음 생성 시도가 시작되면 버린다.
 *
 * 프로세스 종료 후에는 복원하지 않는다 — 사진 URI는 영속 저장 대상이 아니고, 건수는 화면 장식이라
 * 이를 위해 서버를 다시 부를 이유가 없다.
 */
@Singleton
class DraftLoadingSessionStore
    @Inject
    constructor() {
        private val mutableSession = MutableStateFlow<DraftLoadingSession?>(null)

        /** 현재 작업의 로딩 스냅샷. null 이면 표시할 사진·건수가 없다. */
        val session: StateFlow<DraftLoadingSession?> = mutableSession.asStateFlow()

        /** 새 작업의 스냅샷을 넣는다. 이전 작업의 스냅샷은 이 시점에 버려진다. */
        fun start(session: DraftLoadingSession) {
            mutableSession.value = session
        }

        /** [taskId]의 스냅샷을 꺼낸다. 다른 작업이거나 없으면 null. */
        fun sessionFor(taskId: String): DraftLoadingSession? = mutableSession.value?.takeIf { it.taskId == taskId }

        /** 해당 작업이 terminal에 도달했을 때 버린다. 다른 작업의 스냅샷은 건드리지 않는다. */
        fun clear(taskId: String) {
            if (mutableSession.value?.taskId == taskId) mutableSession.value = null
        }
    }
