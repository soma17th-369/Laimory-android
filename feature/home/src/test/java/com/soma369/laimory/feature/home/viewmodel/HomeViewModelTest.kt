package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.coordinator.AutoCollectionCoordinator
import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.coordinator.TermsAgreementCoordinator
import com.soma369.laimory.core.domain.coordinator.UserProfileCoordinator
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.GlobalLoadingHelper
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.collection.AutoCollectionResult
import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.NotificationPrivacyPolicy
import com.soma369.laimory.core.domain.model.collection.PhotoCandidate
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.ResolvedAddress
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.collection.StayPayload
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermRequirement
import com.soma369.laimory.core.domain.model.terms.TermStage
import com.soma369.laimory.core.domain.model.terms.TermStageRequirement
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.model.terms.TermsGateState
import com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask
import com.soma369.laimory.core.domain.model.timeline.CreateTimelineEventCommand
import com.soma369.laimory.core.domain.model.timeline.DailyRecordStatus
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionPolicy
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionReporter
import com.soma369.laimory.core.domain.model.timeline.DraftTaskCompletion
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.model.timeline.MonthlyDailyRecord
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.TimelineItem
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.model.user.UserProfile
import com.soma369.laimory.core.domain.navigation.DraftConsentPage
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.navigation.TimelinePage
import com.soma369.laimory.core.domain.provider.LocationAddressResolver
import com.soma369.laimory.core.domain.repository.SourceItemRepository
import com.soma369.laimory.core.domain.repository.StayAddressRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.source.PhotoSource
import com.soma369.laimory.core.domain.usecase.GetDailyRecordsUseCase
import com.soma369.laimory.core.domain.usecase.GetMonthlyDailyRecordsUseCase
import com.soma369.laimory.core.domain.usecase.GetPhotosInWindowUseCase
import com.soma369.laimory.core.domain.usecase.GetSourceItemsInWindowUseCase
import com.soma369.laimory.core.domain.usecase.ObserveSourceItemsUseCase
import com.soma369.laimory.core.domain.usecase.PrepareSelectedPhotosUseCase
import com.soma369.laimory.core.domain.usecase.PrepareTimelineDraftSelectionUseCase
import com.soma369.laimory.core.domain.usecase.ResolveStayAddressUseCase
import com.soma369.laimory.core.domain.usecase.user.ObserveUserProfileUseCase
import com.soma369.laimory.core.domain.usecase.user.RefreshUserProfileUseCase
import com.soma369.laimory.core.ui.permission.DataSourceStatus
import com.soma369.laimory.core.ui.theme.Emotion
import com.soma369.laimory.feature.home.draft.DraftConsentSessionStore
import com.soma369.laimory.feature.home.state.DraftCreationStatus
import com.soma369.laimory.feature.home.state.DraftEndDay
import com.soma369.laimory.feature.home.state.HomePastRecordsUiState
import com.soma369.laimory.feature.home.state.HomeTimeField
import com.soma369.laimory.feature.home.state.HomeUiIntent
import com.soma369.laimory.feature.home.state.HomeUiSideEffect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.ArrayDeque

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sourceRepository = FakeSourceItemRepository()
    private val autoCollectionCoordinator = FakeAutoCollectionCoordinator()
    private var isCollectionLabAccessible = true
    private val recordRepository = FakeTimelineRecordRepository()
    private val photoSource = FakePhotoSource()
    private val sessionStore = DraftConsentSessionStore()
    private val draftTaskCoordinator = FakeDraftTaskCoordinator()
    private val userProfileCoordinator = FakeUserProfileCoordinator()
    private val navigationHelper = RecordingNavigationHelper()
    private val termsCoordinator = FakeHomeTermsCoordinator()
    private val addressResolver = FakeHomeAddressResolver()

    @Test
    fun `빈 범위에서는 동의 화면으로 이동하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()

            assertNull(sessionStore.preparation.value)
            assertTrue(navigationHelper.destinations.isEmpty())
            assertEquals(DraftCreationStatus.IDLE, viewModel.state.value.draftStatus)
        }

    @Test
    fun `동의 준비는 전송 스냅샷을 확정하고 동의 화면으로 이동한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val zone = ZoneId.systemDefault()
            sourceRepository.items.value = listOf(todayItem("first"))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()

            assertEquals(listOf<Page>(DraftConsentPage), navigationHelper.destinations)
            val preparation = sessionStore.preparation.value
            assertNotNull(preparation)
            assertEquals(listOf("first"), preparation!!.selection.items.map(SourceItem::rawId))
            assertEquals(LocalDate.now(zone), preparation.recordDate)
            // 제출 전이므로 홈 상태는 그대로다 — 생성은 동의 완료 후에만 시작된다.
            assertEquals(DraftCreationStatus.IDLE, viewModel.state.value.draftStatus)
        }

    @Test
    fun `소비되지 않은 준비가 남아 있으면 중복 진입하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("first"))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()

            assertEquals(1, navigationHelper.destinations.size)
            assertEquals(1L, sessionStore.preparation.value?.attemptId)
        }

    @Test
    fun `동의 화면에서 돌아와 다시 시도하면 새 시도로 준비한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("first"))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()
            // 동의 화면 뒤로가기 = 준비 상태 폐기
            sessionStore.clearPreparation()
            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()

            assertEquals(2, navigationHelper.destinations.size)
            assertEquals(2L, sessionStore.preparation.value?.attemptId)
        }

    @Test
    fun `생성 추적 중에는 설정 변경과 동의 준비를 무시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("first"))
            draftTaskCoordinator.emitProcessing(LocalDate.now(ZoneId.systemDefault()))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.ShowTimePicker(HomeTimeField.END))
            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()

            assertEquals(DraftCreationStatus.PROCESSING, viewModel.state.value.draftStatus)
            // 추적 중에는 시각 시트를 열지도 못한다.
            assertNull(viewModel.state.value.timeSheet)
            assertEquals(DraftEndDay.NEXT_DAY, viewModel.state.value.endDay)
            assertNull(sessionStore.preparation.value)
            assertTrue(navigationHelper.destinations.isEmpty())
        }

    @Test
    fun `저장된 날짜만 피커에서 고를 수 없게 모은다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 초안 날짜는 서버가 이어 붙이기로 받아 주므로 막지 않는다. 저장된 날짜만 409 다.
            val month = YearMonth.from(LocalDate.now(ZoneId.systemDefault()))
            val saved = month.atDay(3)
            val draft = month.atDay(4)
            recordRepository.monthlyRecords =
                mapOf(
                    month to
                        listOf(
                            MonthlyDailyRecord(saved, DailyRecordStatus.SAVED, null),
                            MonthlyDailyRecord(draft, DailyRecordStatus.DRAFT, null),
                        ),
                )
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.LoadMonthlyRecords(month))
            runCurrent()

            assertEquals(setOf(saved), viewModel.state.value.savedRecordDates)
        }

    @Test
    fun `같은 달을 두 번 요청해도 한 번만 조회한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val month = YearMonth.from(LocalDate.now(ZoneId.systemDefault()))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.LoadMonthlyRecords(month))
            viewModel.sendIntent(HomeUiIntent.LoadMonthlyRecords(month))
            runCurrent()

            assertEquals(1, recordRepository.monthlyCallCount)
        }

    @Test
    fun `피커를 다시 열면 받아 둔 달을 다시 조회한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 이 화면에서 만든 초안을 저장하고 돌아오면 그 날짜가 저장됨으로 바뀐다.
            val month = YearMonth.from(LocalDate.now(ZoneId.systemDefault()))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.LoadMonthlyRecords(month))
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.ShowDatePicker)
            viewModel.sendIntent(HomeUiIntent.LoadMonthlyRecords(month))
            runCurrent()

            assertEquals(2, recordRepository.monthlyCallCount)
        }

    @Test
    fun `제출 결과가 없는 복귀는 화면 상태를 바꾸지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.ConsumeDraftConsentResult)
            runCurrent()

            val state = viewModel.state.value
            assertEquals(DraftCreationStatus.IDLE, state.draftStatus)
            assertFalse(state.isPhotoSheetVisible)
        }

    @Test
    fun `동의 제출 중 사진 접근 실패 복귀는 사진 재선택 흐름을 연다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("first"))
            val viewModel = createViewModel()
            runCurrent()
            sessionStore.markPhotoReselectionNeeded()
            val effects = async { viewModel.sideEffect.take(2).toList() }
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.ConsumeDraftConsentResult)
            runCurrent()

            val state = viewModel.state.value
            assertEquals(DraftCreationStatus.FAILED, state.draftStatus)
            assertEquals(
                listOf(
                    HomeUiSideEffect.ShowSnackbar("선택한 사진에 접근할 수 없어요. 사진을 다시 선택해주세요."),
                    HomeUiSideEffect.RequestPhotoAccess(),
                ),
                effects.await(),
            )
        }

    @Test
    fun `인증 경계 초기화는 이전 계정 시도 흔적을 지우고 새 생성 시작을 허용한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("first"))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()
            sessionStore.markPhotoReselectionNeeded()

            // 세션 만료·로그아웃으로 인증 root 가 교체되는 순간(MainActivity onAuthRootReplaced 경로)
            sessionStore.clearAll()

            // 이전 계정의 일회성 결과가 새 계정 홈에서 소비되지 않는다.
            viewModel.sendIntent(HomeUiIntent.ConsumeDraftConsentResult)
            runCurrent()
            assertEquals(DraftCreationStatus.IDLE, viewModel.state.value.draftStatus)

            // 남은 준비물 가드에 걸리지 않고 새 시도를 시작할 수 있다.
            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()
            assertEquals(2, navigationHelper.destinations.size)
            assertNotNull(sessionStore.preparation.value)
        }

    @Test
    fun `전체 사진 선택은 최대 20장까지만 반영한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            photoSource.candidates = (1L..21L).map(::todayPhotoCandidate)
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.ResolvePhotoAccess(granted = true))
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.ToggleAllPhotos)
            runCurrent()

            val state = viewModel.state.value
            assertTrue(state.isPhotoSheetVisible)
            assertEquals(21, state.availablePhotos.size)
            assertEquals(20, state.pendingPhotoIds.size)
            assertNull(sessionStore.preparation.value)
        }

    @Test
    fun `사진 권한을 거절하면 거부 상태로 시트를 열되 MediaStore를 조회하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 시트를 안 열면 초안 만들기를 눌렀는데 아무 일도 일어나지 않는 것으로 보인다.
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.ResolvePhotoAccess(granted = false))
            runCurrent()

            val state = viewModel.state.value
            assertTrue(state.isPhotoSheetVisible)
            assertTrue(state.isPhotoAccessDenied)
            assertFalse(state.isPhotoLoading)
            assertTrue(photoSource.requestedWindows.isEmpty())
        }

    @Test
    fun `설정에서 권한을 허용하고 돌아오면 거부 상태가 풀린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 복귀는 `ResolvePhotoAccess` 가 아니라 `RefreshPhotos` 로 들어온다. 여기서 풀지
            // 않으면 사진을 불러오고도 시트가 계속 거부 안내를 띄운다.
            photoSource.candidates = listOf(todayPhotoCandidate(1L))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.ResolvePhotoAccess(granted = false))
            runCurrent()
            assertTrue(viewModel.state.value.isPhotoAccessDenied)

            viewModel.sendIntent(HomeUiIntent.RefreshPhotos(hasAccess = true))
            runCurrent()

            assertFalse(viewModel.state.value.isPhotoAccessDenied)
            assertEquals(1, viewModel.state.value.availablePhotos.size)
        }

    @Test
    fun `저장된 날짜는 확정해도 기록 날짜로 반영되지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 조회가 끝나기 전에 고른 날짜가 뒤늦게 저장됨으로 판정될 수 있다. 화면 표시와
            // 별개로 경계에서 막지 않으면 서버 409 를 받는 날짜로 진행한다.
            val month = YearMonth.from(LocalDate.now(ZoneId.systemDefault()))
            val saved = month.atDay(3)
            recordRepository.monthlyRecords =
                mapOf(month to listOf(MonthlyDailyRecord(saved, DailyRecordStatus.SAVED, null)))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.LoadMonthlyRecords(month))
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.SelectDate(saved))
            runCurrent()

            assertEquals(LocalDate.now(ZoneId.systemDefault()), viewModel.state.value.selectedDate)
        }

    @Test
    fun `사진 없이 계속하면 선택을 비운 채 동의 화면으로 이동한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("calendar"))
            photoSource.candidates = listOf(todayPhotoCandidate(1L))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.ResolvePhotoAccess(granted = true))
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.TogglePhoto(mediaStoreId = 1L))
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.ContinueWithoutPhotos)
            runCurrent()

            val state = viewModel.state.value
            assertFalse(state.isPhotoSheetVisible)
            assertEquals(emptySet<Long>(), state.selectedPhotoIds)
            assertEquals(listOf<Page>(DraftConsentPage), navigationHelper.destinations)
        }

    @Test
    fun `사진 선택을 확정하면 곧바로 동의 화면으로 이어진다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 확정과 생성 사이에 홈으로 돌아가는 단계를 두지 않는다 — 만들기 흐름의 한 걸음이다.
            sourceRepository.items.value = listOf(todayItem("calendar"))
            photoSource.candidates = listOf(todayPhotoCandidate(1L))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.ResolvePhotoAccess(granted = true))
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.TogglePhoto(mediaStoreId = 1L))

            viewModel.sendIntent(HomeUiIntent.ConfirmPhotoSelection)
            runCurrent()

            assertEquals(setOf(1L), viewModel.state.value.selectedPhotoIds)
            assertEquals(listOf<Page>(DraftConsentPage), navigationHelper.destinations)
        }

    @Test
    fun `제한 접근 사진 추가 요청은 권한 런처를 강제로 다시 연다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            val effect = async { viewModel.sideEffect.first() }
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.RequestAdditionalPhotoAccess)
            runCurrent()

            assertEquals(HomeUiSideEffect.RequestPhotoAccess(force = true), effect.await())
        }

    @Test
    fun `사진 선택 취소는 기존 확정 선택을 변경하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            photoSource.candidates = listOf(todayPhotoCandidate(1L), todayPhotoCandidate(2L))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.ResolvePhotoAccess(granted = true))
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.TogglePhoto(mediaStoreId = 1L))
            viewModel.sendIntent(HomeUiIntent.ConfirmPhotoSelection)
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.ResolvePhotoAccess(granted = true))
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.TogglePhoto(mediaStoreId = 2L))
            viewModel.sendIntent(HomeUiIntent.DismissPhotoSheet)
            runCurrent()

            assertEquals(setOf(1L), viewModel.state.value.selectedPhotoIds)
            assertEquals(emptySet<Long>(), viewModel.state.value.pendingPhotoIds)
        }

    @Test
    fun `기록 창 변경과 foreground 복귀는 현재 범위로 사진을 다시 조회한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.RefreshPhotos(hasAccess = true, limited = true))
            runCurrent()
            viewModel.selectStartTime(LocalTime.of(9, 0))
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.RefreshPhotos(hasAccess = true, limited = true))
            runCurrent()

            assertEquals(3, photoSource.requestedWindows.size)
            assertEquals(today.atTime(9, 0).atZone(zone).toInstant(), photoSource.requestedWindows[1].start)
            assertEquals(today.plusDays(1).atStartOfDay(zone).toInstant(), photoSource.requestedWindows[1].end)
            assertTrue(viewModel.state.value.isPhotoAccessLimited)
        }

    @Test
    fun `연속된 기록 창 변경은 이전 사진 조회를 취소하고 마지막 결과만 반영한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val firstResult = CompletableDeferred<List<PhotoCandidate>>()
            val latestResult = CompletableDeferred<List<PhotoCandidate>>()
            photoSource.candidateGates += firstResult
            photoSource.candidateGates += latestResult
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.RefreshPhotos(hasAccess = true))
            runCurrent()
            viewModel.selectStartTime(LocalTime.of(9, 0))
            runCurrent()

            latestResult.complete(listOf(todayPhotoCandidate(2L)))
            runCurrent()
            firstResult.complete(listOf(todayPhotoCandidate(1L)))
            runCurrent()

            assertEquals(2, photoSource.requestedWindows.size)
            assertEquals(listOf(2L), viewModel.state.value.availablePhotos.map { it.mediaStoreId })
        }

    @Test
    fun `선택한 MediaStore 사진만 Room 저장 없이 전송 스냅샷에 합친다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value =
                listOf(
                    todayItem("calendar"),
                    todayPhotoItem(99L),
                )
            photoSource.candidates = listOf(todayPhotoCandidate(1L), todayPhotoCandidate(2L))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.ResolvePhotoAccess(granted = true))
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.TogglePhoto(mediaStoreId = 2L))
            viewModel.sendIntent(HomeUiIntent.ConfirmPhotoSelection)
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()

            val preparation = sessionStore.preparation.value
            assertNotNull(preparation)
            val items = preparation!!.selection.items
            assertEquals(
                setOf("calendar", "prepared-photo-2"),
                items.mapTo(mutableSetOf(), SourceItem::rawId),
            )
            assertFalse(items.any { it.sourceKey == "99" })
            assertEquals(listOf(2L), photoSource.collectedRequests.single())
        }

    @Test
    fun `선택 사진이 삭제되면 동의 화면으로 이동하지 않고 재선택을 유도한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("calendar"))
            photoSource.candidates = listOf(todayPhotoCandidate(1L), todayPhotoCandidate(2L))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.ResolvePhotoAccess(granted = true))
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.ToggleAllPhotos)
            runCurrent()
            photoSource.unavailableIds = setOf(1L)

            viewModel.sendIntent(HomeUiIntent.ConfirmPhotoSelection)
            runCurrent()

            assertNull(sessionStore.preparation.value)
            assertTrue(navigationHelper.destinations.isEmpty())
            assertEquals(DraftCreationStatus.FAILED, viewModel.state.value.draftStatus)
            assertTrue(viewModel.state.value.isPhotoSheetVisible)
            assertEquals(setOf(2L), viewModel.state.value.selectedPhotoIds)
            assertEquals(setOf(2L), viewModel.state.value.pendingPhotoIds)
            assertTrue(viewModel.state.value.draftMessage.orEmpty().contains("접근할 수 없어요"))
        }

    @Test
    fun `생성 추적 중 새 데이터만 수신해도 추적 상태를 유지한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("first"))
            draftTaskCoordinator.emitProcessing(LocalDate.now(ZoneId.systemDefault()))
            val viewModel = createViewModel()
            runCurrent()

            sourceRepository.items.value = listOf(todayItem("first"), todayItem("second"))
            runCurrent()

            assertEquals(DraftCreationStatus.PROCESSING, viewModel.state.value.draftStatus)
        }

    @Test
    fun `SUCCESS 초안 보기는 작업을 유지하고 타임라인 화면으로 이동한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val recordDate = LocalDate.now(ZoneId.systemDefault())
            sourceRepository.items.value = listOf(todayItem("first"))
            val viewModel = createViewModel()
            runCurrent()

            draftTaskCoordinator.emitSuccess(recordDate)
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.ViewDraft)
            runCurrent()

            assertEquals(0, draftTaskCoordinator.discardCount)
            assertEquals(listOf<Page>(TimelinePage(recordDate = recordDate)), navigationHelper.destinations)
        }

    @Test
    fun `SUCCESS 카드 본문에서 날짜 선택을 요청하면 이동하지 않고 모달을 연다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("first"))
            val viewModel = createViewModel()
            runCurrent()
            draftTaskCoordinator.emitSuccess(LocalDate.now(ZoneId.systemDefault()))
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.ShowDatePicker)
            runCurrent()

            assertTrue(viewModel.state.value.isDatePickerVisible)
            assertTrue(navigationHelper.destinations.isEmpty())
        }

    @Test
    fun `SUCCESS 후 다른 날짜를 선택하면 해당 날짜의 새 초안 상태로 전환한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val draftDate = LocalDate.now(ZoneId.systemDefault())
            val otherDate = draftDate.minusDays(1)
            sourceRepository.items.value = listOf(todayItem("first"))
            val viewModel = createViewModel()
            runCurrent()
            draftTaskCoordinator.emitSuccess(draftDate)
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.SelectDate(otherDate))
            runCurrent()

            assertEquals(otherDate, viewModel.state.value.selectedDate)
            assertEquals(DraftCreationStatus.IDLE, viewModel.state.value.draftStatus)
            assertTrue(navigationHelper.destinations.isEmpty())
        }

    @Test
    fun `polling 오류에서 다시 시도하면 동의 준비가 아니라 상태 조회를 재개한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("first"))
            draftTaskCoordinator.emitRetryableError(LocalDate.now(ZoneId.systemDefault()))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.RetryDraft)
            runCurrent()

            assertEquals(1, draftTaskCoordinator.retryCount)
            assertTrue(navigationHelper.destinations.isEmpty())
            assertNull(sessionStore.preparation.value)
        }

    @Test
    fun `과거 날짜 활성 작업이 있으면 홈 재진입 시 해당 날짜와 처리 상태를 복원한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val activeDate = LocalDate.now(ZoneId.systemDefault()).minusDays(12)
            draftTaskCoordinator.emitProcessing(activeDate)

            val viewModel = createViewModel()
            runCurrent()

            assertEquals(activeDate, viewModel.state.value.selectedDate)
            assertEquals(DraftCreationStatus.PROCESSING, viewModel.state.value.draftStatus)
        }

    @Test
    fun `사용자가 다른 날짜를 선택한 뒤 기존 작업이 재개돼도 선택 날짜를 되돌리지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val activeDate = LocalDate.now(ZoneId.systemDefault())
            val selectedDate = activeDate.minusDays(1)
            sourceRepository.items.value = listOf(todayItem("first"))
            draftTaskCoordinator.emitRetryableError(activeDate)
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.SelectDate(selectedDate))
            runCurrent()
            draftTaskCoordinator.emitProcessing(activeDate)
            runCurrent()

            assertEquals(selectedDate, viewModel.state.value.selectedDate)
            assertEquals(DraftCreationStatus.IDLE, viewModel.state.value.draftStatus)
        }

    @Test
    fun `지난 기록 동기화는 서버 정렬 그대로 카드 표시 데이터로 전달한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.dailyRecords =
                listOf(
                    pastTimeline(dailyRecordId = 32L, date = LocalDate.of(2026, 7, 27)),
                    pastTimeline(
                        dailyRecordId = 31L,
                        date = LocalDate.of(2026, 7, 26),
                        emotion = null,
                        events = emptyList(),
                    ),
                )
            val viewModel = createViewModel()

            viewModel.sendIntent(HomeUiIntent.SyncPastRecords)
            runCurrent()

            val content = viewModel.state.value.pastRecords as HomePastRecordsUiState.Content
            assertEquals(listOf(32L, 31L), content.records.map { it.dailyRecordId })
            val latest = content.records.first()
            assertEquals(Emotion.CALM, latest.emotion)
            assertEquals("점심 · 파스타", latest.summary)
            assertEquals("https://cdn/photo.jpg", latest.photoUrl)
            val emptyRecord = content.records.last()
            assertEquals(null, emptyRecord.emotion)
            assertEquals(null, emptyRecord.summary)
            assertEquals(null, emptyRecord.photoUrl)
        }

    @Test
    fun `대표 이미지는 전체 Event를 통틀어 가장 이른 PHOTO를 선택한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val earlierEventWithLatePhoto =
                TimelineEvent(
                    timelineEventId = 41L,
                    eventType = TimelineEventType.WAKE_UP,
                    startAt = LocalDateTime.of(2026, 7, 27, 9, 0),
                    endAt = null,
                    title = "기상",
                    subtitle = null,
                    memo = null,
                    question = null,
                    items =
                        listOf(
                            photoItem(
                                timelineItemId = 51L,
                                startAt = LocalDateTime.of(2026, 7, 27, 21, 0),
                                photoUrl = "https://cdn/late.jpg",
                            ),
                        ),
                )
            val laterEventWithEarlyPhotos =
                TimelineEvent(
                    timelineEventId = 42L,
                    eventType = TimelineEventType.MEAL,
                    startAt = LocalDateTime.of(2026, 7, 27, 12, 0),
                    endAt = null,
                    title = "점심",
                    subtitle = null,
                    memo = null,
                    question = null,
                    items =
                        listOf(
                            photoItem(
                                timelineItemId = 53L,
                                startAt = LocalDateTime.of(2026, 7, 27, 10, 0),
                                photoUrl = "https://cdn/early.jpg",
                            ),
                            photoItem(timelineItemId = 52L, startAt = null, photoUrl = "https://cdn/null-first.jpg"),
                        ),
                )
            recordRepository.dailyRecords =
                listOf(
                    pastTimeline(
                        dailyRecordId = 32L,
                        events = listOf(earlierEventWithLatePhoto, laterEventWithEarlyPhotos),
                    ),
                )
            val viewModel = createViewModel()

            viewModel.sendIntent(HomeUiIntent.SyncPastRecords)
            runCurrent()

            val content = viewModel.state.value.pastRecords as HomePastRecordsUiState.Content
            assertEquals("https://cdn/null-first.jpg", content.records.single().photoUrl)
        }

    @Test
    fun `지난 기록이 없으면 빈 상태를 표시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(HomeUiIntent.SyncPastRecords)
            runCurrent()

            assertEquals(HomePastRecordsUiState.Empty, viewModel.state.value.pastRecords)
        }

    @Test
    fun `지난 기록 조회 실패는 초안 생성을 차단하지 않고 재시도로 복구한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.dailyRecordsFailure = ApiException.NetworkException()
            val viewModel = createViewModel()

            viewModel.sendIntent(HomeUiIntent.SyncPastRecords)
            runCurrent()

            assertEquals(HomePastRecordsUiState.LoadFailed, viewModel.state.value.pastRecords)
            assertEquals(DraftCreationStatus.IDLE, viewModel.state.value.draftStatus)

            recordRepository.dailyRecordsFailure = null
            recordRepository.dailyRecords = listOf(pastTimeline(dailyRecordId = 32L))
            viewModel.sendIntent(HomeUiIntent.SyncPastRecords)
            runCurrent()

            assertTrue(viewModel.state.value.pastRecords is HomePastRecordsUiState.Content)
        }

    @Test
    fun `지난 기록 동기화 중 중복 요청을 보내지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            recordRepository.dailyRecordsGate = CompletableDeferred()
            val viewModel = createViewModel()

            viewModel.sendIntent(HomeUiIntent.SyncPastRecords)
            viewModel.sendIntent(HomeUiIntent.SyncPastRecords)
            runCurrent()

            assertEquals(1, recordRepository.dailyRecordsCallCount)

            recordRepository.dailyRecordsGate?.complete(listOf(pastTimeline(dailyRecordId = 32L)))
            runCurrent()

            assertTrue(viewModel.state.value.pastRecords is HomePastRecordsUiState.Content)
        }

    @Test
    fun `지난 기록 선택은 해당 기록의 타임라인 화면으로 이동한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val recordDate = LocalDate.of(2026, 7, 27)
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.SelectPastRecord(recordDate = recordDate))
            runCurrent()

            assertEquals(listOf<Page>(TimelinePage(recordDate = recordDate)), navigationHelper.destinations)
        }

    @Test
    fun `시각 시트는 확인 전까지 기록 범위를 바꾸지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.ShowTimePicker(HomeTimeField.START))
            viewModel.sendIntent(
                HomeUiIntent.ChangeSheetTime(HomeTimeField.START, LocalDate.now(ZoneId.systemDefault()), LocalTime.of(9, 0)),
            )
            runCurrent()

            assertEquals(LocalTime.of(9, 0), viewModel.state.value.timeSheet?.startTime)
            assertEquals(LocalTime.MIDNIGHT, viewModel.state.value.startTime)

            viewModel.sendIntent(HomeUiIntent.ConfirmTimeSheet)
            runCurrent()

            assertEquals(LocalTime.of(9, 0), viewModel.state.value.startTime)
            assertNull(viewModel.state.value.timeSheet)
        }

    @Test
    fun `시트를 닫으면 고르던 값을 버린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.ShowTimePicker(HomeTimeField.START))
            viewModel.sendIntent(
                HomeUiIntent.ChangeSheetTime(HomeTimeField.START, LocalDate.now(ZoneId.systemDefault()), LocalTime.of(9, 0)),
            )
            viewModel.sendIntent(HomeUiIntent.DismissTimePicker)
            runCurrent()

            assertNull(viewModel.state.value.timeSheet)
            assertEquals(LocalTime.MIDNIGHT, viewModel.state.value.startTime)
        }

    @Test
    fun `종료 날짜 롤러가 당일과 익일을 겸한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val today = LocalDate.now(ZoneId.systemDefault())
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.ShowTimePicker(HomeTimeField.END))
            viewModel.sendIntent(HomeUiIntent.ChangeSheetTime(HomeTimeField.END, today, LocalTime.of(23, 0)))
            viewModel.sendIntent(HomeUiIntent.ConfirmTimeSheet)
            runCurrent()

            assertEquals(DraftEndDay.SAME_DAY, viewModel.state.value.endDay)
            assertEquals(LocalTime.of(23, 0), viewModel.state.value.endTime)
            assertNotNull(viewModel.state.value.recordDateWindow(ZoneId.systemDefault()))

            viewModel.sendIntent(HomeUiIntent.ShowTimePicker(HomeTimeField.END))
            viewModel.sendIntent(HomeUiIntent.ChangeSheetTime(HomeTimeField.END, today.plusDays(1), LocalTime.of(2, 0)))
            viewModel.sendIntent(HomeUiIntent.ConfirmTimeSheet)
            runCurrent()

            assertEquals(DraftEndDay.NEXT_DAY, viewModel.state.value.endDay)
            assertEquals(LocalTime.of(2, 0), viewModel.state.value.endTime)
        }

    @Test
    fun `시작을 늦추면 종료가 최소 6시간 뒤로 따라 밀린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val today = LocalDate.now(ZoneId.systemDefault())
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.ShowTimePicker(HomeTimeField.START))
            viewModel.sendIntent(HomeUiIntent.ChangeSheetTime(HomeTimeField.START, today, LocalTime.of(23, 55)))
            runCurrent()

            // 기본 종료(익일 00:00)는 최소 길이를 못 채우므로 하한인 익일 05:55 로 붙는다.
            val sheet = viewModel.state.value.timeSheet
            assertEquals(DraftEndDay.NEXT_DAY, sheet?.endDay)
            assertEquals(LocalTime.of(5, 55), sheet?.endTime)
            assertEquals(true, sheet?.isConfirmEnabled)
        }

    @Test
    fun `6시간을 못 채우는 조합은 확인해도 반영되지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val today = LocalDate.now(ZoneId.systemDefault())
            val viewModel = createViewModel()
            runCurrent()
            viewModel.selectStartTime(LocalTime.of(9, 0))
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.ShowTimePicker(HomeTimeField.END))
            viewModel.sendIntent(HomeUiIntent.ChangeSheetTime(HomeTimeField.END, today, LocalTime.of(14, 0)))
            viewModel.sendIntent(HomeUiIntent.ConfirmTimeSheet)
            runCurrent()

            // 확인이 막히므로 시트는 열린 채 남고 기록 범위도 그대로다.
            assertEquals(DraftEndDay.NEXT_DAY, viewModel.state.value.endDay)
            assertEquals(LocalTime.MIDNIGHT, viewModel.state.value.endTime)
            assertEquals(false, viewModel.state.value.timeSheet?.isConfirmEnabled)
        }

    /** 시각 시트를 열어 시작 시각만 바꾸고 확정하는 흐름. */
    private fun HomeViewModel.selectStartTime(time: LocalTime) {
        sendIntent(HomeUiIntent.ShowTimePicker(HomeTimeField.START))
        sendIntent(HomeUiIntent.ChangeSheetTime(HomeTimeField.START, LocalDate.now(ZoneId.systemDefault()), time))
        sendIntent(HomeUiIntent.ConfirmTimeSheet)
    }

    @Test
    fun `닉네임을 받으면 인사말 상태에 반영한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            userProfileCoordinator.emit(UserProfile.of("김소마"))
            runCurrent()

            assertEquals("김소마", viewModel.state.value.nickname)
        }

    @Test
    fun `닉네임이 없어도 홈은 그대로 열린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            userProfileCoordinator.emit(UserProfile.of(null))
            runCurrent()

            // 조회 실패·미조회와 같은 상태로 두고 화면이 fallback 문구를 쓴다.
            assertNull(viewModel.state.value.nickname)
        }

    @Test
    fun `계정이 바뀌어 공용 프로필이 비면 인사말도 이름을 뗀다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()
            userProfileCoordinator.emit(UserProfile.of("김소마"))
            runCurrent()

            userProfileCoordinator.emit(null)
            runCurrent()

            assertNull(viewModel.state.value.nickname)
        }

    @Test
    fun `홈이 뜰 때마다 아직 못 받은 프로필을 다시 요청한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()
            // ViewModel 이 Activity 수명이라 재진입해도 같은 인스턴스다. init 에서만 부르면 첫 조회가
            // 실패한 세션 내내 닉네임이 fallback 으로 남는다.
            assertEquals(0, userProfileCoordinator.refreshCount)

            viewModel.sendIntent(HomeUiIntent.RefreshProfile)
            runCurrent()
            assertEquals(1, userProfileCoordinator.refreshCount)

            viewModel.sendIntent(HomeUiIntent.RefreshProfile)
            runCurrent()

            // 성공 뒤의 중복 요청은 coordinator 의 세션 캐시·single-flight 가 막는다.
            assertEquals(2, userProfileCoordinator.refreshCount)
        }

    // --- 자동 수집 연동 ---

    @Test
    fun `날짜를 확정하면 최종 생성 전에 미리 수집한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()
            val before = autoCollectionCoordinator.refreshCount

            viewModel.sendIntent(HomeUiIntent.SelectDate(LocalDate.of(2026, 8, 10)))
            runCurrent()

            assertTrue(autoCollectionCoordinator.refreshCount > before)
        }

    @Test
    fun `기본 날짜로 사진 선택을 열어도 미리 수집한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 오늘을 그대로 쓰면 날짜 확정을 거치지 않아 선행 수집 기회가 없다. 사진을 고르는
            // 동안 수집이 돌아야 확인 화면에서 기다리는 시간이 짧다.
            val viewModel = createViewModel()
            runCurrent()
            val before = autoCollectionCoordinator.refreshCount

            viewModel.sendIntent(HomeUiIntent.OpenPhotoSheet)
            runCurrent()

            assertTrue(autoCollectionCoordinator.refreshCount > before)
        }

    @Test
    fun `저장 데이터가 없어도 자동 수집 기회를 갖기 전에 생성을 끊지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()
            val before = autoCollectionCoordinator.refreshCount

            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()

            assertTrue(autoCollectionCoordinator.refreshCount > before)
        }

    @Test
    fun `최신 확보에 실패하면 알리되 기존 데이터로 생성을 이어 간다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceRepository.items.value = listOf(todayItem("cal-1"))
            autoCollectionCoordinator.result = AutoCollectionResult(timedOut = true)
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.CreateDraft)
            runCurrent()

            assertEquals(listOf(DraftConsentPage), navigationHelper.destinations)
        }

    @Test
    fun `수집 실험실을 열 수 없으면 진입 요청을 무시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            isCollectionLabAccessible = false
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.NavigateToCollection)
            runCurrent()

            assertTrue(navigationHelper.destinations.isEmpty())
        }

    private fun createViewModel(): HomeViewModel =
        HomeViewModel(
            observeSourceItemsUseCase = ObserveSourceItemsUseCase(sourceRepository),
            prepareTimelineDraftSelectionUseCase =
                PrepareTimelineDraftSelectionUseCase(
                    selectionPolicy = DraftSourceItemSelectionPolicy(),
                    privacyPolicy = NotificationPrivacyPolicy(),
                    selectionReporter = DraftSourceItemSelectionReporter.NONE,
                ),
            getDailyRecordsUseCase =
                GetDailyRecordsUseCase(
                    repository = recordRepository,
                    messageHelper = NoOpMessageHelper,
                ),
            getMonthlyDailyRecordsUseCase =
                GetMonthlyDailyRecordsUseCase(
                    repository = recordRepository,
                    messageHelper = NoOpMessageHelper,
                ),
            getPhotosInWindowUseCase = GetPhotosInWindowUseCase(photoSource),
            prepareSelectedPhotosUseCase = PrepareSelectedPhotosUseCase(photoSource),
            draftConsentSessionStore = sessionStore,
            draftTaskCoordinator = draftTaskCoordinator,
            observeUserProfileUseCase = ObserveUserProfileUseCase(userProfileCoordinator),
            refreshUserProfileUseCase = RefreshUserProfileUseCase(userProfileCoordinator),
            navigationHelper = navigationHelper,
            globalLoadingHelper = NoOpGlobalLoadingHelper,
            autoCollectionCoordinator = autoCollectionCoordinator,
            getSourceItemsInWindowUseCase = GetSourceItemsInWindowUseCase(sourceRepository),
            termsCoordinator = termsCoordinator,
            resolveStayAddress = ResolveStayAddressUseCase(addressResolver, NoOpStayAddressRepository),
            collectionLabAccessGate = { isCollectionLabAccessible },
        )

    private class FakeHomeTermsCoordinator : TermsAgreementCoordinator {
        var isLocationAgreed = false

        override val loginGate: StateFlow<TermsGateState> = MutableStateFlow(TermsGateState.Satisfied)

        override fun refresh() = Unit

        override suspend fun requirementOf(stage: TermStage): Result<TermStageRequirement> =
            Result.success(
                TermStageRequirement(
                    stage = stage,
                    items =
                        stage.requiredTypes.map { type ->
                            TermRequirement(document(type), isAgreed = isLocationAgreed)
                        },
                ),
            )

        override suspend fun documentOf(type: TermType): TermDocument = document(type)

        override suspend fun agree(documents: List<TermDocument>): Result<Unit> = Result.success(Unit)

        private fun document(type: TermType) =
            TermDocument(
                termType = type,
                version = "1.0",
                title = type.name,
                contentUrl = "https://laimory.app/terms/${type.name}/1.0",
            )
    }

    private class FakeHomeAddressResolver : LocationAddressResolver {
        var answer: ResolvedAddress? = null
        var resolveCount = 0

        override suspend fun resolve(
            latitude: Double,
            longitude: Double,
        ): ResolvedAddress? {
            resolveCount++
            return answer
        }
    }

    private object NoOpStayAddressRepository : StayAddressRepository {
        override suspend fun updateAddress(
            rawId: String,
            address: ResolvedAddress,
        ): Boolean = true
    }

    private fun pastTimeline(
        dailyRecordId: Long,
        date: LocalDate = LocalDate.of(2026, 7, 27),
        emotion: TimelineEmotion? = TimelineEmotion.HAPPY,
        events: List<TimelineEvent> =
            listOf(
                TimelineEvent(
                    timelineEventId = 41L,
                    eventType = TimelineEventType.MEAL,
                    startAt = LocalDateTime.of(2026, 7, 27, 12, 0),
                    endAt = null,
                    title = "점심",
                    subtitle = "파스타",
                    memo = null,
                    question = null,
                    items =
                        listOf(
                            TimelineItem(
                                timelineItemId = 51L,
                                itemType = TimelineItemType.PHOTO,
                                rawId = "photo-raw-1",
                                startAt = null,
                                endAt = null,
                                photoUrl = "https://cdn/photo.jpg",
                            ),
                        ),
                ),
            ),
    ) = DailyTimeline(
        dailyRecordId = dailyRecordId,
        recordDate = date,
        emotion = emotion,
        events = events,
    )

    private fun photoItem(
        timelineItemId: Long,
        startAt: LocalDateTime?,
        photoUrl: String,
    ) = TimelineItem(
        timelineItemId = timelineItemId,
        itemType = TimelineItemType.PHOTO,
        rawId = "photo-raw-$timelineItemId",
        startAt = startAt,
        endAt = null,
        photoUrl = photoUrl,
    )

    @Test
    fun `위치 약관 동의가 없으면 주소를 해석하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Geocoder 는 좌표를 기기 밖으로 보낸다. 동의 없이 부르지 않는다.
            addressResolver.answer = ResolvedAddress(line = "대한민국 경기도 오산시 원동 123", city = "오산시", district = "원동")
            sourceRepository.items.value = listOf(todayStay("stay-1"))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.RefreshLocationConsent)
            runCurrent()

            assertFalse(viewModel.state.value.isLocationConsentGranted)
            assertEquals(0, addressResolver.resolveCount)
            assertNull(viewModel.state.value.summary.stayPlace?.label)
        }

    @Test
    fun `동의가 있으면 가장 오래 머문 곳의 층위를 채우고 같은 항목을 다시 묻지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            termsCoordinator.isLocationAgreed = true
            addressResolver.answer = ResolvedAddress(line = "대한민국 경기도 오산시 원동 123", city = "오산시", district = "원동")
            sourceRepository.items.value = listOf(todayStay("stay-1"))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(HomeUiIntent.RefreshLocationConsent)
            runCurrent()

            assertTrue(viewModel.state.value.isLocationConsentGranted)
            assertEquals("오산시 원동", viewModel.state.value.summary.stayPlace?.label)

            // 복귀마다 재판정하므로 같은 항목을 반복해서 물으면 Geocoder 를 계속 때린다.
            repeat(3) { viewModel.sendIntent(HomeUiIntent.RefreshLocationConsent) }
            runCurrent()

            assertEquals(1, addressResolver.resolveCount)
        }

    @Test
    fun `동의 판정이 수집보다 먼저 와도 주소를 채운다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 복귀 시점에는 아직 수집이 안 실려 머문 곳이 없다. 그때 한 번만 묻고 말면
            // 뒤늦게 실려 들어온 체류의 주소가 영영 안 채워진다.
            termsCoordinator.isLocationAgreed = true
            addressResolver.answer = ResolvedAddress(line = "대한민국 경기도 오산시 원동 123", city = "오산시", district = "원동")
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.RefreshLocationConsent)
            runCurrent()
            assertEquals(0, addressResolver.resolveCount)

            sourceRepository.items.value = listOf(todayStay("stay-1"))
            runCurrent()

            assertEquals("오산시 원동", viewModel.state.value.summary.stayPlace?.label)
            assertEquals(1, addressResolver.resolveCount)
        }

    @Test
    fun `계정이 바뀌면 이전 계정의 위치 동의로 주소를 해석하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 이 ViewModel 은 Activity 범위라 계정 경계를 넘어 살아남는다. 판정값만 남으면
            // 새 계정이 동의하지 않았는데도 원천 갱신이 그 값을 보고 Geocoder 를 부른다.
            termsCoordinator.isLocationAgreed = true
            addressResolver.answer = ResolvedAddress(line = "대한민국 경기도 오산시 원동 123", city = "오산시", district = "원동")
            sourceRepository.items.value = listOf(todayStay("stay-1"))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(HomeUiIntent.RefreshLocationConsent)
            runCurrent()
            assertEquals(1, addressResolver.resolveCount)

            // 로그아웃·재로그인. 새 계정은 아직 동의하지 않았다.
            termsCoordinator.isLocationAgreed = false
            sessionStore.clearAll()
            runCurrent()

            assertFalse(viewModel.state.value.isLocationConsentGranted)

            // 새 계정의 판정이 끝나기 전에 들어온 원천 갱신이 이전 판정을 쓰면 안 된다.
            sourceRepository.items.value = listOf(todayStay("stay-1"), todayStay("stay-2"))
            runCurrent()

            assertEquals(1, addressResolver.resolveCount)
        }

    @Test
    fun `권한 도트는 화면이 넘긴 값을 그대로 담는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(
                HomeUiIntent.RefreshSourcePermissions(
                    photo = DataSourceStatus.LIMITED,
                    calendar = DataSourceStatus.DENIED,
                    location = DataSourceStatus.GRANTED,
                    notification = DataSourceStatus.UNSUPPORTED,
                ),
            )
            runCurrent()

            val permissions = viewModel.state.value.permissions
            // 일부 허용·미지원을 `꺼짐` 으로 뭉치지 않는다.
            assertEquals(DataSourceStatus.LIMITED, permissions.photo)
            assertEquals(DataSourceStatus.DENIED, permissions.calendar)
            assertEquals(DataSourceStatus.GRANTED, permissions.location)
            assertEquals(DataSourceStatus.UNSUPPORTED, permissions.notification)
        }

    private fun todayStay(id: String): SourceItem {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atTime(13, 0).atZone(zone).toInstant()
        return SourceItem(
            rawId = id,
            startAt = start,
            endAt = start.plusSeconds(3_600),
            timeZoneId = zone,
            payload = StayPayload(latitude = 37.5, longitude = 126.9),
            sourceName = SourceName.LOCATION_PROVIDER,
            sourceKey = id,
            collectedAt = start,
        )
    }

    private fun todayItem(id: String): SourceItem {
        val zone = ZoneId.systemDefault()
        val instant = LocalDate.now(zone).atTime(12, 0).atZone(zone).toInstant()
        return SourceItem(
            rawId = id,
            startAt = instant,
            endAt = null,
            timeZoneId = zone,
            payload = CalendarPayload("일정", null, null, false),
            sourceName = SourceName.CALENDAR_PROVIDER,
            sourceKey = id,
            collectedAt = instant,
        )
    }

    private fun todayPhotoItem(id: Long): SourceItem {
        val zone = ZoneId.systemDefault()
        val instant = LocalDate.now(zone).atTime(12, 0).atZone(zone).toInstant()
        return SourceItem(
            rawId = "staged-photo-$id",
            startAt = instant,
            endAt = null,
            timeZoneId = zone,
            payload =
                PhotoPayload(
                    fileName = "photo-$id.jpg",
                    clientPhotoUri = "content://photo/$id",
                    latitude = null,
                    longitude = null,
                    description = null,
                ),
            sourceName = SourceName.MEDIA_STORE,
            sourceKey = id.toString(),
            collectedAt = instant,
        )
    }

    private fun todayPhotoCandidate(id: Long): PhotoCandidate {
        val zone = ZoneId.systemDefault()
        return PhotoCandidate(
            id = id,
            contentUri = "content://photo/$id",
            takenAt = LocalDate.now(zone).atTime(12, 0).atZone(zone).toInstant().plusSeconds(id),
        )
    }

    /** 자동 수집은 홈 테스트의 관심사가 아니라 곧장 통과시킨다. 호출 여부만 센다. */
    private class FakeAutoCollectionCoordinator : AutoCollectionCoordinator {
        var refreshCount = 0
            private set
        var result = AutoCollectionResult()

        override suspend fun refresh(timeoutMillis: Long?): AutoCollectionResult {
            refreshCount++
            return result
        }

        override fun discard() = Unit
    }

    private data object NoOpGlobalLoadingHelper : GlobalLoadingHelper {
        override val activeKeys: StateFlow<Set<String>> = MutableStateFlow(emptySet())

        override suspend fun <T> withLoading(
            key: String,
            block: suspend () -> T,
        ): T = block()
    }

    private class FakeSourceItemRepository : SourceItemRepository {
        val items = MutableStateFlow<List<SourceItem>>(emptyList())

        override suspend fun addAll(items: List<SourceItem>): Int = 0

        override suspend fun upsertAll(items: List<SourceItem>): Int = 0

        override fun observeAll(): Flow<List<SourceItem>> = items

        /** 실제 구현과 같은 겹침 규칙으로 걸러 준다 — 스냅샷 확정이 이 조회 결과를 쓴다. */
        override suspend fun getInWindow(
            start: Instant,
            end: Instant,
        ): List<SourceItem> =
            items.value.filter { item ->
                val itemEnd = item.endAt
                if (itemEnd == null) {
                    item.startAt >= start && item.startAt < end
                } else {
                    item.startAt < end && itemEnd > start
                }
            }

        override suspend fun getLatestCollectedAt(itemType: ItemType): Instant? = null

        override suspend fun deleteExpired(cutoff: Instant): Int = 0

        override suspend fun clear(itemType: ItemType) = Unit
    }

    private class FakePhotoSource : PhotoSource {
        var candidates: List<PhotoCandidate> = emptyList()
        var unavailableIds: Set<Long> = emptySet()
        val candidateGates = ArrayDeque<CompletableDeferred<List<PhotoCandidate>>>()
        val requestedWindows = mutableListOf<RecordDateWindow>()
        val collectedRequests = mutableListOf<List<Long>>()

        override suspend fun photosIn(window: RecordDateWindow): List<PhotoCandidate> {
            requestedWindows += window
            val response = candidateGates.pollFirst()?.await() ?: candidates
            return response.filter { it.takenAt >= window.start && it.takenAt < window.end }
        }

        override suspend fun collect(ids: List<Long>): List<SourceItem> {
            collectedRequests += ids
            val zone = ZoneId.systemDefault()
            return ids
                .filterNot(unavailableIds::contains)
                .map { id ->
                    val candidate = candidates.first { it.id == id }
                    SourceItem(
                        rawId = "prepared-photo-$id",
                        startAt = candidate.takenAt,
                        endAt = null,
                        timeZoneId = zone,
                        payload = PhotoPayload("$id.jpg", candidate.contentUri, null, null, null),
                        sourceName = SourceName.MEDIA_STORE,
                        sourceKey = id.toString(),
                        collectedAt = candidate.takenAt,
                    )
                }
        }
    }

    private class FakeTimelineRecordRepository : TimelineRecordRepository {
        var dailyRecords: List<DailyTimeline> = emptyList()
        var dailyRecordsGate: CompletableDeferred<List<DailyTimeline>>? = null
        var dailyRecordsFailure: ApiException? = null
        var dailyRecordsCallCount = 0

        override suspend fun getDailyRecords(): List<DailyTimeline> {
            dailyRecordsCallCount++
            dailyRecordsFailure?.let { throw it }
            return dailyRecordsGate?.await() ?: dailyRecords
        }

        override suspend fun getDailyRecord(recordDate: LocalDate): DailyTimeline = error("사용하지 않음")

        override suspend fun createEvent(command: CreateTimelineEventCommand): TimelineEvent = error("사용하지 않음")

        override suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent = error("사용하지 않음")

        override suspend fun updateEventMemo(
            timelineEventId: Long,
            memo: String?,
        ): TimelineEvent = error("사용하지 않음")

        override suspend fun deleteEvent(timelineEventId: Long) = error("사용하지 않음")

        override suspend fun deleteEventPhoto(
            timelineEventId: Long,
            timelineItemId: Long,
        ) = error("사용하지 않음")

        override suspend fun updateDailyRecordEmotion(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ) = Unit

        override suspend fun saveDailyRecord(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ) = error("사용하지 않음")

        var monthlyRecords: Map<YearMonth, List<MonthlyDailyRecord>> = emptyMap()
        var monthlyCallCount = 0

        override suspend fun getMonthlyDailyRecords(month: YearMonth): List<MonthlyDailyRecord> {
            monthlyCallCount++
            return monthlyRecords[month].orEmpty()
        }

        override suspend fun deleteDailyRecord(recordDate: LocalDate) = error("사용하지 않음")
    }

    private data object NoOpMessageHelper : MessageHelper {
        override fun send(message: UserMessage) = Unit
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

    private class FakeDraftTaskCoordinator : DraftTaskCoordinator {
        private val mutableState = MutableStateFlow<DraftTaskTrackingState>(DraftTaskTrackingState.Idle)
        override val state: StateFlow<DraftTaskTrackingState> = mutableState
        override val pendingCompletion: StateFlow<DraftTaskCompletion?> = MutableStateFlow(null)

        override suspend fun consumeCompletion(taskId: String): Boolean = false

        var retryCount = 0
        var discardCount = 0

        override suspend fun start(
            taskId: String,
            recordDate: LocalDate,
        ) {
            mutableState.value =
                DraftTaskTrackingState.Processing(
                    ActiveDraftTask(taskId, recordDate, Instant.EPOCH),
                )
        }

        override suspend fun onForeground() = Unit

        override suspend fun onBackground() = Unit

        override fun refreshFromCompletionSignal(taskId: String) = Unit

        override fun retry() {
            retryCount++
        }

        override fun continueWaiting() = Unit

        override suspend fun discard() {
            discardCount++
            mutableState.value = DraftTaskTrackingState.Idle
        }

        fun emitSuccess(recordDate: LocalDate) {
            mutableState.value =
                DraftTaskTrackingState.Success(
                    task = ActiveDraftTask("task-1", recordDate, Instant.EPOCH),
                )
        }

        fun emitProcessing(recordDate: LocalDate) {
            mutableState.value =
                DraftTaskTrackingState.Processing(
                    ActiveDraftTask("task-1", recordDate, Instant.EPOCH),
                )
        }

        fun emitRetryableError(recordDate: LocalDate) {
            mutableState.value =
                DraftTaskTrackingState.RetryableError(
                    ActiveDraftTask("task-1", recordDate, Instant.EPOCH),
                )
        }
    }

    private class RecordingNavigationHelper : NavigationHelper {
        val destinations = mutableListOf<Page>()

        override fun navigateTo(page: Page) {
            destinations += page
        }

        override fun replaceRoot(page: Page) = Unit

        override fun navigateToBack() = Unit
    }
}
