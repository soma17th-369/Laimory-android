package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.auth.SignedInAccount
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.model.terms.TermAgreement
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermStage
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.model.terms.TermsGateState
import com.soma369.laimory.core.domain.repository.AuthRepository
import com.soma369.laimory.core.domain.repository.TermsRepository
import com.soma369.laimory.core.domain.usecase.auth.ObserveSignedInAccountUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultTermsAgreementCoordinatorTest {
    private val google = SignedInAccount(provider = SocialLoginProvider.GOOGLE)
    private val termsOfService = document(TermType.TERMS_OF_SERVICE, "1.0")
    private val sensitive = document(TermType.SENSITIVE_INFORMATION_CONSENT, "1.0")
    private val thirdParty = document(TermType.THIRD_PARTY_PROVISION_CONSENT, "1.0")
    private val crossBorder = document(TermType.CROSS_BORDER_TRANSFER_CONSENT, "1.0")

    @Test
    fun `로그인 전에는 판정하지 않는다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 여기서 한쪽을 단정하면 인증도 되기 전에 약관 화면이 한 프레임 뜬다.
            val repository = FakeTermsRepository(documents = listOf(termsOfService))
            val coordinator = coordinator(repository, MutableStateFlow(null))

            runCurrent()

            assertEquals(TermsGateState.Unknown, coordinator.loginGate.value)
            assertEquals(0, repository.fetchCount)
        }

    @Test
    fun `이용약관에 동의가 없으면 요구로 판정한다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeTermsRepository(documents = listOf(termsOfService))
            val coordinator = coordinator(repository, MutableStateFlow(google))

            runCurrent()

            assertEquals(TermsGateState.Required(listOf(termsOfService)), coordinator.loginGate.value)
        }

    @Test
    fun `현재 버전에 동의했으면 통과로 판정한다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository =
                FakeTermsRepository(documents = listOf(termsOfService), agreements = listOf(agreement(termsOfService)))
            val coordinator = coordinator(repository, MutableStateFlow(google))

            runCurrent()

            assertEquals(TermsGateState.Satisfied, coordinator.loginGate.value)
        }

    @Test
    fun `지난 버전 동의는 통과로 보지 않는다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 개정되면 새 버전 재동의를 받아야 한다. 서버 판정도 같다.
            val revised = document(TermType.TERMS_OF_SERVICE, "2.0")
            val repository =
                FakeTermsRepository(documents = listOf(revised), agreements = listOf(agreement(termsOfService)))
            val coordinator = coordinator(repository, MutableStateFlow(google))

            runCurrent()

            assertEquals(TermsGateState.Required(listOf(revised)), coordinator.loginGate.value)
        }

    @Test
    fun `catalog 가 비어 있으면 열어 둔다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 서버도 활성화 전에는 강제하지 않는다. 앱이 막으면 열린 문을 앞에서 잠그는 꼴이다.
            val repository = FakeTermsRepository(documents = emptyList())
            val coordinator = coordinator(repository, MutableStateFlow(google))

            runCurrent()

            assertEquals(TermsGateState.Satisfied, coordinator.loginGate.value)
        }

    @Test
    fun `첫 판정이 실패하면 실패로 드러낸다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 빈 catalog 로 치환하면 이후 API 가 계속 거절당하고, 모름으로 두면 로딩에서 못 나온다.
            val repository = FakeTermsRepository(failure = IllegalStateException("offline"))
            val coordinator = coordinator(repository, MutableStateFlow(google))

            runCurrent()

            assertEquals(TermsGateState.Failed, coordinator.loginGate.value)
        }

    @Test
    fun `이미 판정이 서 있으면 재조회 실패로 뒤집지 않는다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 잘 쓰던 사용자를 통신 사정 하나로 오류 화면에 밀어 넣지 않는다.
            val repository =
                FakeTermsRepository(documents = listOf(termsOfService), agreements = listOf(agreement(termsOfService)))
            val coordinator = coordinator(repository, MutableStateFlow(google))
            runCurrent()
            repository.failure = IllegalStateException("offline")

            coordinator.refresh()
            runCurrent()

            assertEquals(TermsGateState.Satisfied, coordinator.loginGate.value)
        }

    @Test
    fun `미동의 신호가 몰려도 조회는 신호 수만큼 늘지 않는다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeTermsRepository(documents = listOf(termsOfService))
            val signal = TermsGateSignal()
            val coordinator = coordinator(repository, MutableStateFlow(google), signal)
            runCurrent()
            val fetchesBefore = repository.fetchCount
            repository.holdFetch()

            repeat(5) { signal.notifyAgreementRequired() }
            runCurrent()
            repository.releaseFetch()
            runCurrent()

            // 다섯 번 거절당해도 조회는 진행 중인 것 하나에 합류하고, 조회 도중 들어온 신호는
            // 꼬리로 한 번만 남는다. 신호 수에 비례해 늘지 않는 것이 이 계약의 요점이다.
            assertEquals(fetchesBefore + 2, repository.fetchCount)
            assertEquals(TermsGateState.Required(listOf(termsOfService)), coordinator.loginGate.value)
        }

    @Test
    fun `동의에 성공하면 서버를 다시 읽지 않고 판정을 올린다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeTermsRepository(documents = listOf(termsOfService))
            val coordinator = coordinator(repository, MutableStateFlow(google))
            runCurrent()
            val fetchesBefore = repository.fetchCount

            val result = coordinator.agree(listOf(termsOfService))
            runCurrent()

            assertTrue(result.isSuccess)
            assertEquals(TermsGateState.Satisfied, coordinator.loginGate.value)
            assertEquals(fetchesBefore, repository.fetchCount)
        }

    @Test
    fun `조회되지 않은 종류는 요구로 세우지 않는다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 서버는 한 종류라도 현재 문서가 없으면 그 단계를 통째로 열어 준다.
            val repository = FakeTermsRepository(documents = listOf(termsOfService, sensitive))
            val coordinator = coordinator(repository, MutableStateFlow(google))
            runCurrent()

            val requirement = coordinator.requirementOf(TermStage.TIMELINE_FIRST_CREATE).getOrThrow()

            assertEquals(listOf(sensitive), requirement.pending)
        }

    @Test
    fun `초안 생성 단계는 세 종류를 모두 요구한다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository =
                FakeTermsRepository(
                    documents = listOf(termsOfService, sensitive, thirdParty, crossBorder),
                    agreements = listOf(agreement(sensitive)),
                )
            val coordinator = coordinator(repository, MutableStateFlow(google))
            runCurrent()

            val requirement = coordinator.requirementOf(TermStage.TIMELINE_FIRST_CREATE).getOrThrow()

            assertEquals(listOf(thirdParty, crossBorder), requirement.pending)
        }

    @Test
    fun `로그아웃하면 판정과 캐시를 비운다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 비우지 않으면 다음 계정이 이전 계정의 동의로 통과한다.
            val repository =
                FakeTermsRepository(documents = listOf(termsOfService), agreements = listOf(agreement(termsOfService)))
            val accounts = MutableStateFlow<SignedInAccount?>(google)
            val coordinator = coordinator(repository, accounts)
            runCurrent()

            accounts.value = null
            runCurrent()
            accounts.value = google
            runCurrent()

            assertEquals(2, repository.fetchCount)
        }

    private fun TestScope.coordinator(
        repository: TermsRepository,
        accounts: MutableStateFlow<SignedInAccount?>,
        gateSignal: TermsGateSignal = TermsGateSignal(),
    ) = DefaultTermsAgreementCoordinator(
        repository = repository,
        observeSignedInAccountUseCase = ObserveSignedInAccountUseCase(FakeAuthRepository(accounts)),
        gateSignal = gateSignal,
        // backgroundScope 는 테스트가 끝날 때 취소된다. this 로 주면 계정 관찰이 끝나지 않아 멈춘다.
        applicationScope = backgroundScope,
    )

    private fun document(
        type: TermType,
        version: String,
    ) = TermDocument(
        termType = type,
        version = version,
        title = type.name,
        contentUrl = "https://laimory.app/terms/${type.name}/$version",
        effectiveAt = LocalDateTime.of(2026, 8, 28, 0, 0),
    )

    private fun agreement(document: TermDocument) = TermAgreement(document = document, acceptedAt = LocalDateTime.of(2026, 8, 29, 9, 30))

    private class FakeTermsRepository(
        private val documents: List<TermDocument> = emptyList(),
        agreements: List<TermAgreement> = emptyList(),
        var failure: Throwable? = null,
    ) : TermsRepository {
        var fetchCount = 0

        /** 조회를 붙잡아 두고 그동안 들어온 신호가 조회를 새로 띄우는지 본다. */
        private var held: CompletableDeferred<Unit>? = null
        private val recorded = agreements.toMutableList()

        override suspend fun getCurrentTerms(types: List<TermType>): List<TermDocument> {
            fetchCount++
            held?.await()
            failure?.let { throw it }
            return documents.filter { it.termType in types }
        }

        override suspend fun getMyAgreements(): List<TermAgreement> {
            failure?.let { throw it }
            return recorded.toList()
        }

        override suspend fun agree(documents: List<TermDocument>) {
            recorded += documents.map { TermAgreement(it, LocalDateTime.of(2026, 8, 30, 0, 0)) }
        }

        fun holdFetch() {
            held = CompletableDeferred()
        }

        fun releaseFetch() {
            held?.complete(Unit)
            held = null
        }
    }

    private class FakeAuthRepository(
        private val accounts: Flow<SignedInAccount?>,
    ) : AuthRepository {
        override fun observeSignedInAccount(): Flow<SignedInAccount?> = accounts

        override fun observeSessionState(): Flow<AuthSessionState> = emptyFlow()

        override suspend fun issueTokens(
            appCode: String,
            appVerifier: String,
        ) = Unit

        override suspend fun logout() = Unit

        override suspend fun clearSession() = Unit
    }
}
