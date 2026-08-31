package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermRequirement
import com.soma369.laimory.core.domain.model.terms.TermStage
import com.soma369.laimory.core.domain.model.terms.TermStageRequirement
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.model.terms.TermsGateState
import com.soma369.laimory.core.domain.repository.TermsRepository
import com.soma369.laimory.core.domain.usecase.auth.ObserveSignedInAccountUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class DefaultTermsAgreementCoordinator
    @Inject
    constructor(
        private val repository: TermsRepository,
        observeSignedInAccountUseCase: ObserveSignedInAccountUseCase,
        gateSignal: TermsGateSignal,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) : TermsAgreementCoordinator {
        private val mutex = Mutex()
        private val mutableLoginGate = MutableStateFlow<TermsGateState>(TermsGateState.Unknown)

        private var snapshot: TermsSnapshot? = null

        /**
         * 진행 중인 조회.
         *
         * 미동의 거절은 여러 요청에서 동시에 온다. 신호마다 조회를 새로 띄우면 같은 것을 여러 번
         * 묻게 되므로, 이미 도는 조회가 있으면 그것을 기다린다.
         */
        private var inFlight: Deferred<Result<TermsSnapshot>>? = null

        /**
         * 지금 유효한 세션의 일련번호.
         *
         * 로그아웃할 때마다 올린다. 늦게 도착한 이전 세션의 응답이 새 세션 판정을 덮어쓰면 계정을
         * 바꿨는데 이전 계정의 동의가 적용된다.
         */
        private var sessionEpoch = 0L

        override val loginGate: StateFlow<TermsGateState> = mutableLoginGate.asStateFlow()

        init {
            applicationScope.launch {
                observeSignedInAccountUseCase().collect { account ->
                    if (account == null) clearSession() else evaluateLoginGate(force = false)
                }
            }
            applicationScope.launch {
                gateSignal.events.collect { evaluateLoginGate(force = true) }
            }
        }

        override fun refresh() {
            applicationScope.launch { evaluateLoginGate(force = true) }
        }

        override suspend fun requirementOf(stage: TermStage): Result<TermStageRequirement> =
            loadSnapshot(force = false).map {
                it.requirementOf(stage)
            }

        override suspend fun documentOf(type: TermType): TermDocument? = loadSnapshot(force = false).getOrNull()?.documents?.get(type)

        override suspend fun agree(documents: List<TermDocument>): Result<Unit> {
            if (documents.isEmpty()) return Result.success(Unit)
            val epoch = mutex.withLock { sessionEpoch }
            return runCatching { repository.agree(documents) }
                .onSuccess { markAgreed(documents, epoch) }
        }

        /**
         * 등록에 성공한 것을 세션 판정에 반영한다.
         *
         * 서버를 다시 읽지 않는다 — 일괄 등록은 all-or-nothing 이라 성공했다면 보낸 그대로
         * 기록됐고, 다시 읽는 사이에 사용자가 다음 화면을 기다려야 할 이유가 없다.
         */
        private suspend fun markAgreed(
            documents: List<TermDocument>,
            epoch: Long,
        ) {
            mutex.withLock {
                if (sessionEpoch != epoch) return
                val current = snapshot ?: return
                snapshot = current.withAgreed(documents)
                publishLoginGate(snapshot)
            }
        }

        /**
         * 이용약관 단계를 다시 판정한다.
         *
         * 조회에 실패했을 때 **이미 판정이 서 있으면 그 값을 지킨다.** 미동의 거절 때문에 다시
         * 묻다가 네트워크가 잠깐 끊긴 경우까지 오류 화면으로 보내면, 잘 쓰던 사용자를 통신 사정
         * 하나로 앱 밖으로 밀어내는 셈이 된다. 아직 아무것도 모르는 첫 판정만 실패를 드러낸다.
         */
        private suspend fun evaluateLoginGate(force: Boolean) {
            val epoch = mutex.withLock { sessionEpoch }
            val result = loadSnapshot(force)
            mutex.withLock {
                if (sessionEpoch != epoch) return
                if (result.isFailure) {
                    if (mutableLoginGate.value == TermsGateState.Unknown) {
                        mutableLoginGate.value = TermsGateState.Failed
                    }
                    return
                }
                publishLoginGate(result.getOrNull())
            }
        }

        /** [mutex] 안에서만 부른다. */
        private fun publishLoginGate(loaded: TermsSnapshot?) {
            val requirement = loaded?.requirementOf(TermStage.LOGIN) ?: return
            mutableLoginGate.value =
                if (requirement.isSatisfied) {
                    TermsGateState.Satisfied
                } else {
                    TermsGateState.Required(requirement.pending)
                }
        }

        private suspend fun loadSnapshot(force: Boolean): Result<TermsSnapshot> {
            val epoch: Long
            val awaited: Deferred<Result<TermsSnapshot>>
            mutex.withLock {
                snapshot?.let { if (!force) return Result.success(it) }
                epoch = sessionEpoch
                awaited = inFlight ?: applicationScope.async { fetch() }.also { inFlight = it }
            }
            val result = awaited.await()
            mutex.withLock {
                if (inFlight === awaited) inFlight = null
                if (sessionEpoch == epoch) result.getOrNull()?.let { snapshot = it }
            }
            return result
        }

        /**
         * catalog 와 동의 이력을 함께 읽는다.
         *
         * 종류를 나눠 여러 번 묻지 않고 앱이 쓰는 전부를 한 번에 가져온다 — 로그인 단계 판정,
         * 초안 생성 단계 판정, 설정의 원문 열람이 같은 결과를 나눠 쓴다.
         */
        private suspend fun fetch(): Result<TermsSnapshot> =
            runCatching {
                val documents = repository.getCurrentTerms(TermType.entries)
                val agreements = repository.getMyAgreements()
                TermsSnapshot(
                    documents = documents.associateBy { it.termType },
                    agreedVersions = agreements.map { it.document.termType to it.document.version }.toSet(),
                )
            }

        private suspend fun clearSession() {
            mutex.withLock {
                sessionEpoch++
                inFlight?.cancel()
                inFlight = null
                snapshot = null
                mutableLoginGate.value = TermsGateState.Unknown
            }
        }

        private data class TermsSnapshot(
            val documents: Map<TermType, TermDocument>,
            val agreedVersions: Set<Pair<TermType, String>>,
        ) {
            /**
             * 조회된 문서만 요구로 세운다.
             *
             * 서버는 요구 종류 중 하나라도 현재 문서가 없으면 그 단계를 통째로 열어 준다. 앱이
             * 없는 문서를 미동의로 세우면 열려 있는 문을 앞에서 막는 꼴이다.
             */
            fun requirementOf(stage: TermStage): TermStageRequirement =
                TermStageRequirement(
                    stage = stage,
                    items =
                        stage.requiredTypes.mapNotNull { type ->
                            documents[type]?.let { document ->
                                TermRequirement(
                                    document = document,
                                    isAgreed = (document.termType to document.version) in agreedVersions,
                                )
                            }
                        },
                )

            fun withAgreed(documents: List<TermDocument>): TermsSnapshot =
                copy(agreedVersions = agreedVersions + documents.map { it.termType to it.version })
        }
    }
