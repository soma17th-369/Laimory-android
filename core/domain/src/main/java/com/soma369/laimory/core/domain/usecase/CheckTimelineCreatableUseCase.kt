package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 보낼 목록으로 타임라인을 만들 수 있는지 — 기록이 너무 적은 날은 `false` 다. 기준은 서버가 갖는다.
 *
 * **확인하지 못하면 `true` 다(fail-open).** 이 판정은 `만들기` 를 끌지 정하는 힌트일 뿐이고 생성 요청은 이를 강제하지
 * 않는다. 끊김 · 무응답 · 아직 이 API 가 없는 서버(404) 때문에 만들기가 막히면 안 된다.
 *
 * [com.soma369.laimory.core.domain.base.BaseUseCase] 를 쓰지 않는다. 그쪽은 404 · 5xx 에 공통 안내를 띄우는데, 확인에
 * 실패했다고 다이얼로그 뒤로 안내가 뜨면 안 된다.
 */
class CheckTimelineCreatableUseCase
    @Inject
    constructor(
        private val repository: TimelineDraftRepository,
    ) {
        suspend operator fun invoke(items: List<SourceItem>): Boolean =
            try {
                withTimeoutOrNull(TIMEOUT_MILLIS) { repository.isCreatable(items) } ?: true
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                true
            }

        private companion object {
            /** 다이얼로그는 이 값을 받은 뒤에 뜬다. 크레딧 조회와 같은 상한 — 넘으면 켠 채로 띄운다. */
            const val TIMEOUT_MILLIS = 3_000L
        }
    }
