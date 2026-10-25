package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.coordinator.TutorialReplayRequests
import com.soma369.laimory.core.domain.coordinator.UserProfileCoordinator
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.AnalyticsHelper
import com.soma369.laimory.core.domain.helper.GlobalLoadingHelper
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.DialogRequest
import com.soma369.laimory.core.domain.message.DialogResult
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKey
import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKeys
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionType
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPromptContext
import com.soma369.laimory.core.domain.model.analytics.InstallAttribution
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.auth.SignedInAccount
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import com.soma369.laimory.core.domain.model.credit.CreditCosts
import com.soma369.laimory.core.domain.model.notice.NewNoticePolicy
import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.domain.model.terms.TermAgreement
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.model.timeline.CreateTimelineEventCommand
import com.soma369.laimory.core.domain.model.timeline.DailyRecordStatus
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.MonthlyDailyRecord
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.model.tutorial.Tutorial
import com.soma369.laimory.core.domain.model.user.UserProfile
import com.soma369.laimory.core.domain.navigation.CreditPage
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.navigation.InquiryPage
import com.soma369.laimory.core.domain.navigation.LoginPage
import com.soma369.laimory.core.domain.navigation.NoticesPage
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.navigation.TimelinePage
import com.soma369.laimory.core.domain.provider.PushInstallationIdProvider
import com.soma369.laimory.core.domain.repository.AuthRepository
import com.soma369.laimory.core.domain.repository.CreditRepository
import com.soma369.laimory.core.domain.repository.LocationTrackingRepository
import com.soma369.laimory.core.domain.repository.NoticeRepository
import com.soma369.laimory.core.domain.repository.PushRegistrationRepository
import com.soma369.laimory.core.domain.repository.TermsRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TutorialRepository
import com.soma369.laimory.core.domain.repository.UserRepository
import com.soma369.laimory.core.domain.usecase.GetDailyRecordsUseCase
import com.soma369.laimory.core.domain.usecase.ObserveLocationTrackingUseCase
import com.soma369.laimory.core.domain.usecase.SetLocationTrackingUseCase
import com.soma369.laimory.core.domain.usecase.analytics.LogPermissionEventUseCase
import com.soma369.laimory.core.domain.usecase.auth.LogoutUseCase
import com.soma369.laimory.core.domain.usecase.auth.ObserveSignedInAccountUseCase
import com.soma369.laimory.core.domain.usecase.credit.GetRemainingCreditsUseCase
import com.soma369.laimory.core.domain.usecase.credit.GetTimelineCreditUseCase
import com.soma369.laimory.core.domain.usecase.notice.HasNewNoticeUseCase
import com.soma369.laimory.core.domain.usecase.push.UnregisterCurrentPushInstallationUseCase
import com.soma369.laimory.core.domain.usecase.terms.GetPublicTermLinksUseCase
import com.soma369.laimory.core.domain.usecase.tutorial.ResetTutorialsUseCase
import com.soma369.laimory.core.domain.usecase.user.ObserveUserProfileUseCase
import com.soma369.laimory.core.domain.usecase.user.RefreshUserProfileUseCase
import com.soma369.laimory.core.domain.usecase.user.RequestAccountWithdrawalUseCase
import com.soma369.laimory.core.domain.usecase.user.WithdrawAccountUseCase
import com.soma369.laimory.core.ui.permission.DataPermissionEvent
import com.soma369.laimory.feature.settings.credit.CreditBalanceStore
import com.soma369.laimory.feature.settings.state.CreditUiIntent
import com.soma369.laimory.feature.settings.state.SettingsUiIntent
import com.soma369.laimory.feature.settings.state.UsageGuideTimelineTarget
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val navigationHelper = FakeNavigationHelper()
    private val messageHelper = FakeMessageHelper()
    private val globalLoadingHelper = RecordingGlobalLoadingHelper()
    private val userProfileCoordinator = FakeUserProfileCoordinator()
    private val userRepository = FakeUserRepository()
    private val locationTrackingRepository = FakeLocationTrackingRepository()
    private val analyticsHelper = RecordingAnalyticsHelper()
    private val noticeRepository = FakeNoticeRepository()
    private val creditRepository = FakeCreditRepository()
    private val creditBalanceStore = CreditBalanceStore()
    private val tutorialRepository = FakeTutorialRepository()
    private val tutorialReplayRequests = TutorialReplayRequests()
    private val recordRepository = FakeTimelineRecordRepository()

    @Test
    fun `읽지 않은 최근 공지가 있으면 공지사항 줄에 표시를 띄우고, 읽고 돌아오면 지운다`() =
        runTest {
            noticeRepository.notices = listOf(recentNotice(1))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.RefreshNoticeBadge)
            runCurrent()
            assertTrue(viewModel.state.value.hasNewNotice)

            noticeRepository.readIds = setOf(1)
            viewModel.sendIntent(SettingsUiIntent.RefreshNoticeBadge)
            runCurrent()
            assertFalse(viewModel.state.value.hasNewNotice)
        }

    @Test
    fun `공지를 받지 못하면 표시를 띄우지 않는다`() =
        runTest {
            noticeRepository.failure = IllegalStateException("offline")
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.RefreshNoticeBadge)
            runCurrent()

            assertFalse(viewModel.state.value.hasNewNotice)
        }

    @Test
    fun `약관 주소를 못 받으면 화면이 뜰 때마다 다시 묻는다`() =
        runTest {
            // 한 번만 물으면 첫 조회가 실패한 세션 내내 눌리지 않는 줄로 남는다. 처리방침은
            // 설정에서 볼 수 있어야 하는 문서라 그렇게 막히면 안 된다.
            EmptyTermsRepository.fetchCount = 0
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.RefreshTermLinks)
            runCurrent()
            viewModel.sendIntent(SettingsUiIntent.RefreshTermLinks)
            runCurrent()

            assertEquals(2, EmptyTermsRepository.fetchCount)
        }

    @Test
    fun `공지사항을 누르면 공지 목록으로 간다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.NoticesClicked)
            runCurrent()

            assertEquals(listOf<Page>(NoticesPage), navigationHelper.navigatedTo)
        }

    @Test
    fun `남은 크레딧을 누르면 크레딧 화면으로 간다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.CreditsClicked)
            runCurrent()

            assertEquals(listOf<Page>(CreditPage), navigationHelper.navigatedTo)
        }

    @Test
    fun `문의하기를 누르면 문의 화면으로 간다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.InquiryClicked)
            runCurrent()

            assertEquals(listOf<Page>(InquiryPage), navigationHelper.navigatedTo)
        }

    @Test
    fun `저장된 로그인 제공자를 화면 상태에 반영한다`() =
        runTest {
            repository.account.value = SignedInAccount(SocialLoginProvider.KAKAO)

            val viewModel = createViewModel()
            runCurrent()

            assertEquals(SocialLoginProvider.KAKAO, viewModel.state.value.accountProvider)
        }

    @Test
    fun `로그아웃 클릭은 공통 Dialog로 확인을 요청하고 취소 결과는 로그아웃하지 않는다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            runCurrent()

            val request = messageHelper.dialogRequests.single()
            assertEquals("로그아웃할까요?", request.title)
            assertEquals("로그아웃", request.primaryLabel)

            messageHelper.respond(DialogResult.Secondary)
            runCurrent()

            assertEquals(0, repository.logoutCount)
            assertFalse(viewModel.state.value.isLoggingOut)
        }

    @Test
    fun `확인 결과는 로그아웃을 실행하고 로그인 화면으로 루트를 교체한다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            runCurrent()
            messageHelper.respond(DialogResult.Primary)
            runCurrent()

            assertEquals(1, repository.logoutCount)
            assertEquals(LoginPage, navigationHelper.replacedRoot)
            assertTrue(viewModel.state.value.isLoggingOut)
            assertEquals(listOf("settings-logout"), globalLoadingHelper.startedKeys)
            assertTrue(globalLoadingHelper.activeKeys.value.isEmpty())
        }

    @Test
    fun `로그아웃 진행 동안 전역 로딩 key를 유지한다`() =
        runTest {
            repository.logoutGate = CompletableDeferred()
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            runCurrent()
            messageHelper.respond(DialogResult.Primary)
            runCurrent()

            assertTrue(globalLoadingHelper.activeKeys.value.contains("settings-logout"))

            repository.logoutGate?.complete(Unit)
            runCurrent()

            assertTrue(globalLoadingHelper.activeKeys.value.isEmpty())
            assertEquals(LoginPage, navigationHelper.replacedRoot)
        }

    @Test
    fun `연속 탭은 확인 Dialog를 한 번만 요청한다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            runCurrent()
            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            runCurrent()

            assertEquals(1, messageHelper.dialogRequests.size)
        }

    @Test
    fun `로그아웃 처리 중 중복 확인은 한 번만 실행한다`() =
        runTest {
            repository.logoutGate = CompletableDeferred()
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutConfirmed)
            runCurrent()
            assertTrue(viewModel.state.value.isLoggingOut)

            viewModel.sendIntent(SettingsUiIntent.LogoutConfirmed)
            repository.logoutGate?.complete(Unit)
            runCurrent()

            assertEquals(1, repository.logoutCount)
            assertEquals(LoginPage, navigationHelper.replacedRoot)
        }

    @Test
    fun `로그아웃 처리 중에는 확인 Dialog를 다시 요청하지 않는다`() =
        runTest {
            repository.logoutGate = CompletableDeferred()
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutConfirmed)
            runCurrent()
            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            repository.logoutGate?.complete(Unit)
            runCurrent()

            assertTrue(messageHelper.dialogRequests.isEmpty())
        }

    @Test
    fun `재로그인 계정을 관찰하면 로그아웃 진행 상태를 초기화한다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            runCurrent()
            messageHelper.respond(DialogResult.Primary)
            runCurrent()
            repository.account.value = SignedInAccount(SocialLoginProvider.GOOGLE)
            runCurrent()

            assertFalse(viewModel.state.value.isLoggingOut)
            assertEquals(SocialLoginProvider.GOOGLE, viewModel.state.value.accountProvider)
        }

    @Test
    fun `로그아웃 실패는 진행 상태와 전역 로딩을 해제하고 사용자에게 안내한다`() =
        runTest {
            repository.logoutError = ApiException.NetworkException()
            val viewModel = createViewModel()
            val snackbarMessage = async { viewModel.snackbar.first() }
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            runCurrent()
            messageHelper.respond(DialogResult.Primary)
            runCurrent()

            assertEquals(1, repository.logoutCount)
            assertFalse(viewModel.state.value.isLoggingOut)
            assertTrue(globalLoadingHelper.activeKeys.value.isEmpty())
            assertNull(navigationHelper.replacedRoot)
            assertEquals(ApiException.NETWORK_ERROR, snackbarMessage.await())
        }

    @Test
    fun `닉네임을 받으면 계정 카드 제목에 반영한다`() =
        runTest {
            repository.account.value = SignedInAccount(SocialLoginProvider.KAKAO)
            val viewModel = createViewModel()
            runCurrent()

            userProfileCoordinator.emit(UserProfile.of("김소마"))
            runCurrent()

            assertEquals("김소마", viewModel.state.value.nickname)
            // 제공자 정보는 보조 문구·아이콘으로 남아야 하므로 함께 유지한다.
            assertEquals(SocialLoginProvider.KAKAO, viewModel.state.value.accountProvider)
        }

    @Test
    fun `닉네임이 없는 계정은 상태를 비워 제공자 문구로 돌아가게 한다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            userProfileCoordinator.emit(UserProfile.of(null))
            runCurrent()

            assertNull(viewModel.state.value.nickname)
        }

    @Test
    fun `로그아웃으로 공용 프로필이 비면 닉네임도 사라진다`() =
        runTest {
            repository.account.value = SignedInAccount(SocialLoginProvider.GOOGLE)
            val viewModel = createViewModel()
            runCurrent()
            userProfileCoordinator.emit(UserProfile.of("김소마"))
            runCurrent()

            userProfileCoordinator.emit(null)
            runCurrent()

            // 설정 화면에 이전 계정 정보가 남으면 안 된다.
            assertNull(viewModel.state.value.nickname)
        }

    @Test
    fun `화면이 뜰 때마다 아직 못 받은 프로필을 다시 요청한다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()
            assertEquals(0, userProfileCoordinator.refreshCount)

            // 재진입해도 같은 ViewModel 인스턴스라 init 트리거로는 다시 시도하지 못한다.
            viewModel.sendIntent(SettingsUiIntent.RefreshProfile)
            runCurrent()
            assertEquals(1, userProfileCoordinator.refreshCount)

            viewModel.sendIntent(SettingsUiIntent.RefreshProfile)
            runCurrent()

            assertEquals(2, userProfileCoordinator.refreshCount)
        }

    // --- 계정 삭제 ---

    @Test
    fun `계정 삭제 클릭은 확인 체크박스가 달린 Dialog로 동의를 요청한다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.AccountDeleteClicked)
            runCurrent()

            val request = messageHelper.consentRequests.single()
            assertEquals("계정을 삭제할까요?", request.title)
            assertEquals("계정 삭제", request.primaryLabel)
            assertTrue(request.consentLabel.contains("동의"))
            // 되돌릴 수 없다는 것과 재로그인해도 복구되지 않는다는 것을 문구가 담아야 한다.
            assertTrue(request.body.contains("되돌릴 수 없습니다"))
            assertTrue(request.body.contains("복구되지 않습니다"))
            assertEquals(0, userRepository.withdrawalCount)
        }

    @Test
    fun `취소하거나 닫으면 탈퇴를 요청하지 않는다`() =
        runTest {
            listOf(DialogResult.Secondary, DialogResult.Dismissed).forEach { result ->
                val viewModel = createViewModel()
                runCurrent()

                viewModel.sendIntent(SettingsUiIntent.AccountDeleteClicked)
                runCurrent()
                messageHelper.respond(result)
                runCurrent()

                assertEquals(0, userRepository.withdrawalCount)
                assertFalse(viewModel.state.value.isWithdrawing)
            }
        }

    @Test
    fun `동의 후 삭제는 탈퇴를 요청하고 세션을 정리한 뒤 로그인 화면으로 교체한다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.AccountDeleteClicked)
            runCurrent()
            messageHelper.respond(DialogResult.Primary)
            runCurrent()

            assertEquals(1, userRepository.withdrawalCount)
            assertEquals(1, repository.clearSessionCount)
            // 서버가 이미 credential 을 무효화했으므로 실패가 예정된 logout 요청은 보내지 않는다.
            assertEquals(0, repository.logoutCount)
            assertEquals(LoginPage, navigationHelper.replacedRoot)
            assertTrue(globalLoadingHelper.startedKeys.contains("settings-withdraw"))
            assertEquals(listOf(UserMessage.AccountWithdrawalAccepted), messageHelper.sentMessages)
            // 같은 기기에서 다시 가입하면 새 가입으로 한 번 더 나가야 한다.
            assertEquals(listOf(AnalyticsDedupeKeys.SIGN_UP), analyticsHelper.forgotten)
        }

    @Test
    fun `401로 끝난 탈퇴는 완료로 안내하지 않는다`() =
        runTest {
            // 서버가 만료 세션과 이미 탈퇴한 회원을 같은 401 로 합치므로 삭제됐다고 단정할 수 없다.
            userRepository.withdrawalError = ApiException.fromCode(401, null, -2001)
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.AccountDeleteClicked)
            runCurrent()
            messageHelper.respond(DialogResult.Primary)
            runCurrent()

            assertEquals(1, repository.clearSessionCount)
            assertEquals(LoginPage, navigationHelper.replacedRoot)
            assertEquals(listOf(UserMessage.AccountWithdrawalUnverified), messageHelper.sentMessages)
            // 탈퇴됐는지 모르므로 가입 판정은 그대로 둔다.
            assertTrue(analyticsHelper.forgotten.isEmpty())
        }

    @Test
    fun `탈퇴가 실패하면 진행 상태를 풀고 화면에 남는다`() =
        runTest {
            userRepository.withdrawalError = ApiException.ServerException(rawCode = 500)
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.AccountDeleteClicked)
            runCurrent()
            messageHelper.respond(DialogResult.Primary)
            runCurrent()

            assertEquals(0, repository.clearSessionCount)
            assertNull(navigationHelper.replacedRoot)
            assertFalse(viewModel.state.value.isWithdrawing)
            // 5xx 는 탈퇴 전용 처리를 만들지 않고 공통 정책 메시지로 내려간다.
            assertEquals(listOf(UserMessage.TemporaryUnavailable), messageHelper.sentMessages)
        }

    @Test
    fun `로그아웃 진행 중에는 계정 삭제 확인을 열지 않는다`() =
        runTest {
            repository.logoutGate = CompletableDeferred()
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(SettingsUiIntent.LogoutConfirmed)
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.AccountDeleteClicked)
            runCurrent()

            assertTrue(messageHelper.consentRequests.isEmpty())
        }

    /** 공용 프로필 상태를 시험에서 직접 밀어 넣는 대역. */
    private class FakeUserProfileCoordinator : UserProfileCoordinator {
        private val mutableProfile = MutableStateFlow<UserProfile?>(null)

        var refreshCount = 0
            private set

        override val profile: StateFlow<UserProfile?> = mutableProfile

        override fun refresh() {
            refreshCount++
        }

        fun emit(profile: UserProfile?) {
            mutableProfile.value = profile
        }
    }

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(
            logoutUseCase =
                LogoutUseCase(
                    repository = repository,
                    unregisterCurrentPushInstallation =
                        UnregisterCurrentPushInstallationUseCase(
                            installationIdProvider = FakePushInstallationIdProvider,
                            repository = FakePushRegistrationRepository,
                        ),
                ),
            withdrawAccountUseCase =
                WithdrawAccountUseCase(
                    requestAccountWithdrawal = RequestAccountWithdrawalUseCase(userRepository, messageHelper),
                    authRepository = repository,
                ),
            observeSignedInAccount = ObserveSignedInAccountUseCase(repository),
            observeUserProfileUseCase = ObserveUserProfileUseCase(userProfileCoordinator),
            refreshUserProfileUseCase = RefreshUserProfileUseCase(userProfileCoordinator),
            navigationHelper = navigationHelper,
            messageHelper = messageHelper,
            globalLoadingHelper = globalLoadingHelper,
            getPublicTermLinks = GetPublicTermLinksUseCase(EmptyTermsRepository),
            hasNewNotice = HasNewNoticeUseCase(noticeRepository, NewNoticePolicy(Clock.systemUTC())),
            observeLocationTracking = ObserveLocationTrackingUseCase(locationTrackingRepository),
            setLocationTracking = SetLocationTrackingUseCase(locationTrackingRepository),
            logPermissionEvent = LogPermissionEventUseCase(analyticsHelper),
            analyticsHelper = analyticsHelper,
            getRemainingCredits = GetRemainingCreditsUseCase(creditRepository),
            creditBalanceStore = creditBalanceStore,
            resetTutorials = ResetTutorialsUseCase(tutorialRepository),
            tutorialReplayRequests = tutorialReplayRequests,
            getDailyRecords = GetDailyRecordsUseCase(recordRepository, messageHelper),
        )

    /** 같은 잔액 저장소를 쓰는 크레딧 화면. 두 화면의 조회가 겹치는 경우를 만든다. */
    private fun createCreditViewModel() =
        CreditViewModel(
            getTimelineCredit = GetTimelineCreditUseCase(creditRepository),
            balanceStore = creditBalanceStore,
            navigationHelper = navigationHelper,
        )

    @Test
    fun `화면에 들어오면 남은 크레딧을 받아 계정 카드에 싣는다`() =
        runTest {
            creditRepository.remaining = 42
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.RefreshCredits)
            runCurrent()

            assertEquals(42, viewModel.state.value.remainingCredits)
        }

    @Test
    fun `남은 크레딧을 받는 동안 값이 없으면 숫자 자리에 로딩을 보인다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()
            val slow = CompletableDeferred<Unit>()
            creditRepository.remaining = 42
            creditRepository.gate = slow

            viewModel.sendIntent(SettingsUiIntent.RefreshCredits)
            runCurrent()
            assertTrue(viewModel.state.value.isCreditsLoading)
            assertNull(viewModel.state.value.remainingCredits)

            slow.complete(Unit)
            runCurrent()

            assertFalse(viewModel.state.value.isCreditsLoading)
            assertEquals(42, viewModel.state.value.remainingCredits)
        }

    @Test
    fun `이미 받은 잔액이 있으면 다시 받는 동안에도 그 숫자를 둔다`() =
        runTest {
            // 화면 복귀마다 다시 받는다. 있던 숫자를 그때마다 로딩으로 바꾸면 깜빡인다.
            creditRepository.remaining = 42
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(SettingsUiIntent.RefreshCredits)
            runCurrent()

            creditRepository.gate = CompletableDeferred()
            viewModel.sendIntent(SettingsUiIntent.RefreshCredits)
            runCurrent()

            // 칸은 값이 있으면 받는 중이어도 숫자를 그린다.
            assertEquals(42, viewModel.state.value.remainingCredits)
        }

    @Test
    fun `남은 크레딧을 못 받으면 0 이 아니라 빈칸이다`() =
        runTest {
            // 크레딧 행이 없는 500 · 운영 서버에 API 가 없는 404. 0 으로 보이면 남은 게 없는 줄 안다.
            creditRepository.remaining = 42
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(SettingsUiIntent.RefreshCredits)
            runCurrent()

            creditRepository.failure = ApiException.ServerException(rawCode = 500)
            viewModel.sendIntent(SettingsUiIntent.RefreshCredits)
            runCurrent()

            assertNull(viewModel.state.value.remainingCredits)
            assertTrue(messageHelper.sentMessages.isEmpty())
        }

    @Test
    fun `조회가 겹치면 늦게 도착한 이전 응답이 최신 잔액을 덮지 않는다`() =
        runTest {
            // 설정을 떠나 크레딧을 쓰고 돌아오면 복귀마다 조회가 겹친다. 앞선 느린 응답(42)이 나중에 와도 41 이 남아야 한다.
            val viewModel = createViewModel()
            runCurrent()
            val slow = CompletableDeferred<Unit>()
            creditRepository.remaining = 42
            creditRepository.gate = slow
            viewModel.sendIntent(SettingsUiIntent.RefreshCredits)
            runCurrent()

            creditRepository.gate = null
            creditRepository.remaining = 41
            viewModel.sendIntent(SettingsUiIntent.RefreshCredits)
            runCurrent()
            assertEquals(41, viewModel.state.value.remainingCredits)

            creditRepository.remaining = 42
            slow.complete(Unit)
            runCurrent()

            assertEquals(41, viewModel.state.value.remainingCredits)
        }

    @Test
    fun `로그아웃하면 남은 크레딧을 비우고 늦게 온 이전 계정의 응답은 버린다`() =
        runTest {
            repository.account.value = SignedInAccount(SocialLoginProvider.GOOGLE)
            creditRepository.remaining = 42
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(SettingsUiIntent.RefreshCredits)
            runCurrent()
            assertEquals(42, viewModel.state.value.remainingCredits)

            val late = CompletableDeferred<Unit>()
            creditRepository.gate = late
            viewModel.sendIntent(SettingsUiIntent.RefreshCredits)
            runCurrent()
            repository.account.value = null
            // 계정 경계에서 앱이 저장소를 비운다(MainActivity).
            creditBalanceStore.clear()
            runCurrent()
            late.complete(Unit)
            runCurrent()

            assertNull(viewModel.state.value.remainingCredits)
        }

    @Test
    fun `설정의 느린 조회가 그 뒤 크레딧 화면이 받은 잔액을 덮지 않는다`() =
        runTest {
            val settings = createViewModel()
            val credit = createCreditViewModel()
            runCurrent()
            val slow = CompletableDeferred<Unit>()
            creditRepository.remaining = 42
            creditRepository.gate = slow
            settings.sendIntent(SettingsUiIntent.RefreshCredits)
            runCurrent()

            creditRepository.gate = null
            creditRepository.remaining = 41
            credit.sendIntent(CreditUiIntent.Refresh)
            runCurrent()
            slow.complete(Unit)
            runCurrent()

            assertEquals(41, creditBalanceStore.balance.value.remaining)
            assertEquals(41, settings.state.value.remainingCredits)
            assertEquals(41, credit.state.value.remaining)
        }

    @Test
    fun `크레딧 화면의 느린 조회가 늦게 실패해도 그 뒤 설정이 받은 잔액을 비우지 않는다`() =
        runTest {
            val settings = createViewModel()
            val credit = createCreditViewModel()
            runCurrent()
            val slow = CompletableDeferred<Unit>()
            creditRepository.gate = slow
            credit.sendIntent(CreditUiIntent.Refresh)
            runCurrent()

            creditRepository.gate = null
            creditRepository.remaining = 41
            settings.sendIntent(SettingsUiIntent.RefreshCredits)
            runCurrent()
            creditRepository.failure = ApiException.ServerException(rawCode = 500)
            slow.complete(Unit)
            runCurrent()

            assertEquals(41, creditBalanceStore.balance.value.remaining)
            assertEquals(41, settings.state.value.remainingCredits)
        }

    @Test
    fun `기억해 둔 잔액이 있으면 다시 받기 전부터 보인다`() =
        runTest {
            // 크레딧 화면이 받아 둔 값. 설정에 들어오자마자 그 숫자가 보여야 로딩이 번쩍이지 않는다.
            creditBalanceStore.updateRemaining(creditBalanceStore.beginRequest(), 42)
            creditRepository.gate = CompletableDeferred()
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.RefreshCredits)
            runCurrent()

            assertEquals(42, viewModel.state.value.remainingCredits)
        }

    @Test
    fun `설정에서 연 권한 요청을 설정 자리로 기록한다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.PermissionEvent(DataPermissionEvent.Requested(AnalyticsPermissionType.CALENDAR)))
            runCurrent()

            assertEquals(
                listOf(AnalyticsEvent.PermissionRequestStarted(AnalyticsPermissionType.CALENDAR, AnalyticsPromptContext.SETTINGS)),
                analyticsHelper.logged,
            )
        }

    @Test
    fun `위치 수집을 끄면 사용자의 의사로 저장하고 화면도 꺼진 것으로 보인다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()
            assertTrue(viewModel.state.value.isLocationCollectionEnabled)

            viewModel.sendIntent(SettingsUiIntent.LocationCollectionToggled(enabled = false))
            runCurrent()

            // 화면 값과 저장이 함께 내려가야 한다. 한쪽만 내려가면 앱을 다시 열었을 때 어긋난다.
            assertFalse(viewModel.state.value.isLocationCollectionEnabled)
            assertFalse(locationTrackingRepository.enabled.value)
        }

    @Test
    fun `설정에서 다시 켜면 수집이 살아난다`() =
        runTest {
            // 사용자가 끈 수집은 전경 진입의 상태 맞추기가 되살리지 않으므로, 이 자리가 유일한 복구 경로다.
            locationTrackingRepository.enabled.value = false
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LocationCollectionToggled(enabled = true))
            runCurrent()

            assertTrue(viewModel.state.value.isLocationCollectionEnabled)
            assertTrue(locationTrackingRepository.enabled.value)
        }

    private class FakeLocationTrackingRepository : LocationTrackingRepository {
        val enabled = MutableStateFlow(true)

        override fun observeEnabled(): Flow<Boolean> = enabled

        override fun observeStatus(): Flow<LocationTrackingStatus?> = MutableStateFlow(null)

        override suspend fun setEnabled(enabled: Boolean) {
            this.enabled.value = enabled
        }

        override suspend fun reconcile() = Unit
    }

    /** 지금 막 올라온 공지. 실제 시계로 판정하므로 기간 안에 들게 현재 시각을 쓴다. */
    private fun recentNotice(id: Long) =
        Notice(
            id = id,
            title = "공지 $id",
            contentUrl = "https://www.laimory.app/notices/$id",
            publishedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")),
        )

    private class FakeNoticeRepository : NoticeRepository {
        var notices: List<Notice> = emptyList()
        var readIds: Set<Long> = emptySet()
        var failure: Throwable? = null

        override suspend fun getNotices(): List<Notice> {
            failure?.let { throw it }
            return notices
        }

        override suspend fun getReadNoticeIds(): Set<Long> = readIds

        override suspend fun markRead(
            noticeId: Long,
            keepIds: Set<Long>,
        ) = Unit
    }

    /** 약관 주소는 정보 항목이 여는 곁가지라 조회가 비어도 계정 동작이 달라지지 않는다. */
    private object EmptyTermsRepository : TermsRepository {
        var fetchCount = 0

        override suspend fun getCurrentTerms(types: List<TermType>): List<TermDocument> {
            fetchCount++
            return emptyList()
        }

        override suspend fun getPublishedTerms(types: List<TermType>) = getCurrentTerms(types)

        override suspend fun getMyAgreements() = emptyList<TermAgreement>()

        override suspend fun agree(documents: List<TermDocument>) = Unit
    }

    private class FakeMessageHelper : MessageHelper {
        val dialogRequests = mutableListOf<DialogRequest.TwoButton>()
        val consentRequests = mutableListOf<DialogRequest.Consent>()
        val sentMessages = mutableListOf<UserMessage>()
        private var pendingResponse: CompletableDeferred<DialogResult>? = null

        override fun send(message: UserMessage) {
            sentMessages += message
        }

        override suspend fun showTwoButtonDialog(request: DialogRequest.TwoButton): DialogResult {
            dialogRequests += request
            return awaitResponse()
        }

        override suspend fun showConsentDialog(request: DialogRequest.Consent): DialogResult {
            consentRequests += request
            return awaitResponse()
        }

        private suspend fun awaitResponse(): DialogResult {
            val response = CompletableDeferred<DialogResult>()
            pendingResponse = response
            return response.await()
        }

        fun respond(result: DialogResult) {
            val response = checkNotNull(pendingResponse) { "대기 중인 Dialog 요청이 없습니다" }
            pendingResponse = null
            response.complete(result)
        }
    }

    private class RecordingGlobalLoadingHelper : GlobalLoadingHelper {
        private val _activeKeys = MutableStateFlow<Set<String>>(emptySet())
        override val activeKeys: StateFlow<Set<String>> = _activeKeys.asStateFlow()
        val startedKeys = mutableListOf<String>()

        override suspend fun <T> withLoading(
            key: String,
            block: suspend () -> T,
        ): T {
            startedKeys += key
            _activeKeys.value = _activeKeys.value + key
            return try {
                block()
            } finally {
                _activeKeys.value = _activeKeys.value - key
            }
        }
    }

    private class FakeAuthRepository : AuthRepository {
        val account = MutableStateFlow<SignedInAccount?>(null)
        var logoutCount = 0
        var logoutGate: CompletableDeferred<Unit>? = null
        var logoutError: Throwable? = null
        var clearSessionCount = 0

        override fun observeSessionState(): Flow<AuthSessionState> = MutableStateFlow(AuthSessionState.Authenticated)

        override fun observeSignedInAccount(): Flow<SignedInAccount?> = account

        override suspend fun issueTokens(
            appCode: String,
            appVerifier: String,
        ) = Unit

        override suspend fun logout() {
            logoutCount++
            logoutError?.let { throw it }
            logoutGate?.await()
            account.value = null
        }

        override suspend fun clearSession() {
            clearSessionCount++
            account.value = null
        }
    }

    private class FakeUserRepository : UserRepository {
        var withdrawalCount = 0
            private set
        var withdrawalError: Throwable? = null

        override suspend fun getMyProfile(): UserProfile = UserProfile.of(null)

        override suspend fun requestAccountWithdrawal() {
            withdrawalCount++
            withdrawalError?.let { throw it }
        }
    }

    @Test
    fun `사용 방법을 열면 이벤트가 있는 가장 최근 기록을 타임라인 편집 둘러보기 대상으로 잡는다`() =
        runTest {
            recordRepository.records =
                listOf(
                    record(LocalDate.of(2026, 10, 20), hasEvents = false),
                    record(LocalDate.of(2026, 10, 16), hasEvents = true),
                    record(LocalDate.of(2026, 10, 9), hasEvents = true),
                )
            val viewModel = createViewModel()

            viewModel.sendIntent(SettingsUiIntent.UsageGuideClicked)
            runCurrent()

            assertEquals(UsageGuideTimelineTarget.Available(LocalDate.of(2026, 10, 16)), viewModel.state.value.usageGuide)
        }

    @Test
    fun `이벤트가 있는 기록이 없으면 타임라인 편집 둘러보기를 막는다`() =
        runTest {
            recordRepository.records = listOf(record(LocalDate.of(2026, 10, 20), hasEvents = false))
            val viewModel = createViewModel()
            viewModel.sendIntent(SettingsUiIntent.UsageGuideClicked)
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.ReplayTimelineTutorial)
            runCurrent()

            assertEquals(UsageGuideTimelineTarget.Unavailable, viewModel.state.value.usageGuide)
            assertTrue(navigationHelper.navigatedTo.isEmpty())
            assertNull(tutorialReplayRequests.pending.value)
        }

    @Test
    fun `기록을 받지 못했으면 타임라인 편집 둘러보기를 누를 때 다시 받는다`() =
        runTest {
            recordRepository.failure = IOException("network")
            val viewModel = createViewModel()
            viewModel.sendIntent(SettingsUiIntent.UsageGuideClicked)
            runCurrent()
            assertEquals(UsageGuideTimelineTarget.Failed, viewModel.state.value.usageGuide)

            recordRepository.failure = null
            recordRepository.records = listOf(record(LocalDate.of(2026, 10, 16), hasEvents = true))
            viewModel.sendIntent(SettingsUiIntent.ReplayTimelineTutorial)
            runCurrent()

            assertEquals(UsageGuideTimelineTarget.Available(LocalDate.of(2026, 10, 16)), viewModel.state.value.usageGuide)
        }

    @Test
    fun `타임라인 편집 둘러보기는 그 기록을 열고 다시 보기를 요청한다`() =
        runTest {
            recordRepository.records = listOf(record(LocalDate.of(2026, 10, 16), hasEvents = true))
            val viewModel = createViewModel()
            viewModel.sendIntent(SettingsUiIntent.UsageGuideClicked)
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.ReplayTimelineTutorial)
            runCurrent()

            assertNull(viewModel.state.value.usageGuide)
            assertEquals(Tutorial.TIMELINE, tutorialReplayRequests.pending.value)
            assertEquals(listOf<Page>(TimelinePage(LocalDate.of(2026, 10, 16))), navigationHelper.navigatedTo)
        }

    @Test
    fun `홈 화면 둘러보기는 봤다는 기록을 두고 홈으로 가며 다시 보기를 요청한다`() =
        runTest {
            tutorialRepository.resetCount = 0
            val viewModel = createViewModel()
            viewModel.sendIntent(SettingsUiIntent.UsageGuideClicked)
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.ReplayHomeTutorial)
            runCurrent()

            assertNull(viewModel.state.value.usageGuide)
            assertEquals(Tutorial.HOME, tutorialReplayRequests.pending.value)
            assertEquals(HomePage, navigationHelper.replacedRoot)
            assertEquals(0, tutorialRepository.resetCount)
        }

    @Test
    fun `처음부터 따라 하기는 봤다는 기록을 지우고 홈으로 간다`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.sendIntent(SettingsUiIntent.UsageGuideClicked)
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.ReplayFullTutorial)
            runCurrent()

            assertNull(viewModel.state.value.usageGuide)
            assertEquals(1, tutorialRepository.resetCount)
            assertEquals(HomePage, navigationHelper.replacedRoot)
            assertNull(tutorialReplayRequests.pending.value)
        }

    private fun record(
        date: LocalDate,
        hasEvents: Boolean,
    ) = DailyTimeline(
        dailyRecordId = date.toEpochDay(),
        recordDate = date,
        emotion = null,
        events =
            if (hasEvents) {
                listOf(
                    TimelineEvent(
                        timelineEventId = date.toEpochDay(),
                        eventType = TimelineEventType.WORK,
                        startAt = date.atTime(9, 0),
                        endAt = null,
                        title = "업무",
                        subtitle = null,
                        memo = null,
                        question = null,
                        items = emptyList(),
                    ),
                )
            } else {
                emptyList()
            },
        status = DailyRecordStatus.DRAFT,
    )

    private class FakeTutorialRepository : TutorialRepository {
        var resetCount = 0

        override fun observeSeen(tutorial: Tutorial): Flow<Boolean> = flowOf(true)

        override suspend fun markSeen(tutorial: Tutorial) = Unit

        override suspend fun resetAll() {
            resetCount++
        }
    }

    /** 사용 방법이 기록 목록만 읽는다. 나머지는 부르지 않는다. */
    private class FakeTimelineRecordRepository : TimelineRecordRepository {
        var records: List<DailyTimeline> = emptyList()
        var failure: Exception? = null

        override suspend fun getDailyRecords(): List<DailyTimeline> {
            failure?.let { throw it }
            return records
        }

        override suspend fun getDailyRecord(recordDate: LocalDate): DailyTimeline = error("not used")

        override suspend fun createEvent(command: CreateTimelineEventCommand): TimelineEvent = error("not used")

        override suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent = error("not used")

        override suspend fun updateEventMemo(
            timelineEventId: Long,
            memo: String?,
        ): TimelineEvent = error("not used")

        override suspend fun deleteEvent(timelineEventId: Long) = error("not used")

        override suspend fun deleteEventPhoto(
            timelineEventId: Long,
            timelineItemId: Long,
        ) = error("not used")

        override suspend fun deleteDailyRecord(recordDate: LocalDate) = error("not used")

        override suspend fun getMonthlyDailyRecords(month: YearMonth): List<MonthlyDailyRecord> = error("not used")

        override suspend fun updateDailyRecordEmotion(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ) = error("not used")

        override suspend fun saveDailyRecord(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ) = error("not used")
    }

    private class FakeNavigationHelper : NavigationHelper {
        var replacedRoot: Page? = null
        val navigatedTo = mutableListOf<Page>()

        override fun navigateTo(page: Page) {
            navigatedTo += page
        }

        override fun replaceRoot(page: Page) {
            replacedRoot = page
        }

        override fun navigateToBack() = Unit
    }

    private data object FakePushInstallationIdProvider : PushInstallationIdProvider {
        override suspend fun getCurrentId(): String = "fid"
    }

    private data object FakePushRegistrationRepository : PushRegistrationRepository {
        override suspend fun unregister(firebaseInstallationId: String) = Unit

        override suspend fun register(firebaseInstallationId: String) = Unit
    }

    private class RecordingAnalyticsHelper : AnalyticsHelper {
        val logged = mutableListOf<AnalyticsEvent>()

        override suspend fun log(event: AnalyticsEvent) {
            logged += event
        }

        override suspend fun logOnce(
            key: AnalyticsDedupeKey,
            event: AnalyticsEvent,
        ) {
            logged += event
        }

        val forgotten = mutableListOf<AnalyticsDedupeKey>()

        override suspend fun forgetOnce(key: AnalyticsDedupeKey) {
            forgotten += key
        }

        override fun setUserId(userId: Long?) = Unit

        override fun setInstallAttribution(attribution: InstallAttribution) = Unit
    }

    private class FakeCreditRepository : CreditRepository {
        var remaining = 60
        var failure: Throwable? = null

        /** 채우면 완료될 때까지 응답하지 않는다. 응답이 늦게 오는 경우를 만든다. */
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun getRemainingCredits(): Int {
            // 부른 시점의 값과 문을 붙잡는다 — 같은 저장소를 두 화면이 부를 때 요청마다 다른 응답을 만들 수 있게.
            val pending = gate
            val value = remaining
            pending?.await()
            failure?.let { throw it }
            return value
        }

        override suspend fun getCosts(): CreditCosts = CreditCosts(timelineCreation = 1)
    }
}
