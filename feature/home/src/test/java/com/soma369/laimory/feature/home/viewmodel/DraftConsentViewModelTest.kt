package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.coordinator.TermsAgreementCoordinator
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.DraftPhotoAccessException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.collection.StayPayload
import com.soma369.laimory.core.domain.model.terms.TermAgreement
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermRequirement
import com.soma369.laimory.core.domain.model.terms.TermStage
import com.soma369.laimory.core.domain.model.terms.TermStageRequirement
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.model.terms.TermsGateState
import com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionPolicy
import com.soma369.laimory.core.domain.model.timeline.DraftTaskCompletion
import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.model.timeline.LocationMapRenderGate
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.navigation.DraftConsentDetailPage
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.navigation.StageTermsPage
import com.soma369.laimory.core.domain.repository.TermsRepository
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import com.soma369.laimory.core.domain.usecase.CreateTimelineDraftUseCase
import com.soma369.laimory.core.domain.usecase.terms.GetDisplayTermsUseCase
import com.soma369.laimory.feature.home.draft.DraftConsentSessionStore
import com.soma369.laimory.feature.home.draft.DraftLoadingSessionStore
import com.soma369.laimory.feature.home.state.DraftConsentTypeGroup
import com.soma369.laimory.feature.home.state.DraftConsentUiIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DraftConsentViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone: ZoneId = ZoneId.systemDefault()
    private val date: LocalDate = LocalDate.now(zone)

    private val sessionStore = DraftConsentSessionStore()
    private val loadingSessionStore = DraftLoadingSessionStore()
    private val draftRepository = FakeTimelineDraftRepository()
    private val draftTaskCoordinator = FakeDraftTaskCoordinator()
    private val navigationHelper = RecordingNavigationHelper()
    private val termsCoordinator = FakeTermsAgreementCoordinator()
    private var mapRenderAllowed = false

    @Test
    fun `새 스냅샷이 들어오면 내용을 구성하고 체크 상태를 초기화한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()
            prepare(listOf(calendarItem("cal")))
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.ToggleItemInclusion("cal"))
            runCurrent()
            assertEquals(setOf("cal"), viewModel.state.value.excludedRawIds)

            // 뒤로가기 후 재진입 = 같은 데이터라도 새 attemptId → 새 생성 시도로 초기화
            prepare(listOf(calendarItem("cal")))
            runCurrent()

            val state = viewModel.state.value
            assertNotNull(state.content)
            assertEquals(1, state.content!!.sentTotal)
            assertTrue(state.excludedRawIds.isEmpty())
        }

    @Test
    fun `준비물이 없으면 내용 없이 홈 복귀 안내 상태를 유지한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            assertNull(viewModel.state.value.content)
            assertFalse(viewModel.state.value.canSubmit)
        }

    @Test
    fun `모든 동의 후 제출하면 스냅샷 그대로 전송하고 홈으로 복귀한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal"), photoItem(7L)))
            val snapshotItems = sessionStore.preparation.value!!.selection.items
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            assertEquals(1, draftRepository.createCount)
            assertEquals(snapshotItems, draftRepository.createdItems)
            assertEquals(listOf("content://photo/7"), draftRepository.uploadedUris)
            assertEquals(listOf("task-1"), draftTaskCoordinator.startedTaskIds)
            assertNull(sessionStore.preparation.value)
            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `제출 중 중복 요청과 체크 변경을 무시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal")))
            draftRepository.createGate = CompletableDeferred()
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            viewModel.sendIntent(DraftConsentUiIntent.ToggleItemInclusion("cal"))
            runCurrent()

            assertEquals(1, draftRepository.createCount)
            assertTrue(viewModel.state.value.isSubmitting)
            // 제출 중에는 전송 목록도 바뀌지 않는다.
            assertTrue(viewModel.state.value.excludedRawIds.isEmpty())

            draftRepository.createGate?.complete(DraftTaskHandle("task-1"))
            runCurrent()
            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `제출 실패 시 동의 화면에 머물러 같은 스냅샷으로 재시도할 수 있다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal")))
            draftRepository.createFailure = IllegalStateException("failed")
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            assertNotNull(viewModel.state.value.submitError)
            assertFalse(viewModel.state.value.isSubmitting)
            assertEquals(0, navigationHelper.backCount)
            assertNotNull(sessionStore.preparation.value)

            draftRepository.createFailure = null
            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            assertEquals(2, draftRepository.createCount)
            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `뒤로가기는 준비 상태를 폐기하고 네트워크를 호출하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal"), photoItem(7L)))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.NavigateBack)
            runCurrent()

            assertNull(sessionStore.preparation.value)
            assertEquals(1, navigationHelper.backCount)
            assertEquals(0, draftRepository.uploadCount)
            assertEquals(0, draftRepository.createCount)
        }

    @Test
    fun `제출 중에는 뒤로가기를 무시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal")))
            draftRepository.createGate = CompletableDeferred()
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.NavigateBack)
            runCurrent()

            assertEquals(0, navigationHelper.backCount)
            assertNotNull(sessionStore.preparation.value)

            draftRepository.createGate?.complete(DraftTaskHandle("task-1"))
            runCurrent()
            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `이전 작업 폐기 플래그가 있으면 제출 직전에 폐기한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal")), discardActiveTask = true)
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            assertEquals(1, draftTaskCoordinator.discardCount)
            assertEquals(listOf("task-1"), draftTaskCoordinator.startedTaskIds)
        }

    @Test
    fun `유형 상세는 전송 항목이 있을 때만 상세 화면으로 이동한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal")))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.OpenTypeDetail(DraftConsentTypeGroup.PHOTO))
            runCurrent()
            assertTrue(navigationHelper.destinations.isEmpty())

            viewModel.sendIntent(DraftConsentUiIntent.OpenTypeDetail(DraftConsentTypeGroup.CALENDAR))
            runCurrent()
            assertEquals(
                listOf<Page>(DraftConsentDetailPage(DraftConsentTypeGroup.CALENDAR.name)),
                navigationHelper.destinations,
            )
        }

    @Test
    fun `항목 토글은 제외 집합과 전송 예정 건수를 갱신한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal-1"), calendarItem("cal-2")))
            val viewModel = createViewModel()
            runCurrent()
            assertEquals(2, viewModel.state.value.includedTotal)

            viewModel.sendIntent(DraftConsentUiIntent.ToggleItemInclusion("cal-1"))
            runCurrent()

            assertEquals(setOf("cal-1"), viewModel.state.value.excludedRawIds)
            assertEquals(1, viewModel.state.value.includedTotal)
            assertEquals(1, viewModel.state.value.excludedCountOf(DraftConsentTypeGroup.CALENDAR))

            // 다시 누르면 포함으로 복귀한다.
            viewModel.sendIntent(DraftConsentUiIntent.ToggleItemInclusion("cal-1"))
            runCurrent()
            assertTrue(viewModel.state.value.excludedRawIds.isEmpty())
            assertEquals(2, viewModel.state.value.includedTotal)
        }

    @Test
    fun `사진 항목은 토글되지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(photoItem(7L)))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.ToggleItemInclusion("photo-7"))
            runCurrent()

            assertTrue(viewModel.state.value.excludedRawIds.isEmpty())
        }

    // --- 위치정보 전송 Switch ---

    @Test
    fun `위치정보 전송을 끄면 현재 시도의 체류와 이동을 모두 제외한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(stayItem("stay-1"), movementItem("move-1"), calendarItem("cal-1")))
            val viewModel = createViewModel()
            runCurrent()
            assertTrue(viewModel.state.value.isLocationIncluded)

            viewModel.sendIntent(DraftConsentUiIntent.ToggleLocationInclusion)
            runCurrent()

            assertEquals(setOf("stay-1", "move-1"), viewModel.state.value.excludedRawIds)
            assertFalse(viewModel.state.value.isLocationIncluded)
            // 다른 유형은 건드리지 않는다.
            assertEquals(1, viewModel.state.value.includedTotal)
        }

    @Test
    fun `다시 켜면 최초 스냅샷의 위치만 복원하고 다른 유형 제외는 유지한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(stayItem("stay-1"), movementItem("move-1"), calendarItem("cal-1")))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.ToggleItemInclusion("cal-1"))
            viewModel.sendIntent(DraftConsentUiIntent.ToggleLocationInclusion)
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.ToggleLocationInclusion)
            runCurrent()

            assertEquals(setOf("cal-1"), viewModel.state.value.excludedRawIds)
            assertTrue(viewModel.state.value.isLocationIncluded)
        }

    @Test
    fun `위치를 모두 꺼도 다른 유형이 남아 있으면 생성할 수 있다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(stayItem("stay-1"), calendarItem("cal-1")))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.ToggleLocationInclusion)
            runCurrent()

            assertTrue(viewModel.state.value.canSubmit)
        }

    @Test
    fun `전송할 항목이 위치뿐이면 모두 끈 뒤 생성 CTA 가 잠긴다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(stayItem("stay-1")))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.ToggleLocationInclusion)
            runCurrent()

            assertEquals(0, viewModel.state.value.includedTotal)
            assertFalse(viewModel.state.value.canSubmit)
        }

    @Test
    fun `위치를 끈 채 제출하면 위치가 빠진 목록만 전송한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(stayItem("stay-1"), movementItem("move-1"), calendarItem("cal-1")))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.ToggleLocationInclusion)
            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            assertEquals(listOf("cal-1"), draftRepository.createdItems.map(SourceItem::rawId))
        }

    @Test
    fun `지도 마커는 체류 한 개 이동 두 개로 만들어진다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(stayItem("stay-1"), movementItem("move-1")))
            val viewModel = createViewModel()
            runCurrent()

            val markers = viewModel.state.value.content?.locationMarkers.orEmpty()

            assertEquals(3, markers.size)
            assertEquals(setOf("stay-1", "move-1"), viewModel.state.value.content?.locationRawIds)
        }

    @Test
    fun `지도는 게이트가 허용할 때만 켜진다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 게이트는 저장된 위치정보 약관 동의를 본다. 조회 전·실패는 false 라 지도를 붙이지 않는다.
            mapRenderAllowed = true
            val allowed = createViewModel()
            runCurrent()
            assertTrue(allowed.state.value.isMapRenderAllowed)

            mapRenderAllowed = false
            val blocked = createViewModel()
            runCurrent()
            assertFalse(blocked.state.value.isMapRenderAllowed)
        }

    @Test
    fun `새 스냅샷이 들어와도 지도 허용 판정은 유지된다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            mapRenderAllowed = true
            val viewModel = createViewModel()
            runCurrent()

            prepare(listOf(stayItem("stay-1")))
            runCurrent()

            assertTrue(viewModel.state.value.isMapRenderAllowed)
        }

    @Test
    fun `제출 중에는 위치 Switch 를 바꿀 수 없다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(stayItem("stay-1"), calendarItem("cal-1")))
            draftRepository.createGate = CompletableDeferred()
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.ToggleLocationInclusion)
            runCurrent()

            assertTrue(viewModel.state.value.excludedRawIds.isEmpty())
        }

    @Test
    fun `제외 항목을 뺀 결과만 제출한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal-1"), calendarItem("cal-2"), photoItem(7L)))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.ToggleItemInclusion("cal-1"))
            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            assertEquals(1, draftRepository.createCount)
            assertEquals(listOf("cal-2", "photo-7"), draftRepository.createdItems.map(SourceItem::rawId))
            assertEquals(listOf("content://photo/7"), draftRepository.uploadedUris)
        }

    @Test
    fun `제출에 성공하면 로딩 화면이 쓸 스냅샷을 남긴다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal-1"), calendarItem("cal-2"), photoItem(7L)))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            // 동의 준비 상태는 폐기되지만 로딩 스냅샷은 남아야 한다.
            assertNull(sessionStore.preparation.value)
            val session = loadingSessionStore.session.value
            assertEquals("task-1", session?.taskId)
            assertEquals(date, session?.recordDate)
            assertEquals(listOf("content://photo/7"), session?.photoUris)
            assertEquals(1, session?.photoCount)
            assertEquals(2, session?.calendarCount)
            assertEquals(0, session?.stayCount)
        }

    @Test
    fun `제출 스냅샷의 건수는 사용자가 제외한 항목을 빼고 센다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal-1"), calendarItem("cal-2")))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.ToggleItemInclusion("cal-1"))
            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            assertEquals(1, loadingSessionStore.session.value?.calendarCount)
        }

    @Test
    fun `모든 항목을 제외하면 제출할 수 없다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal-1")))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.ToggleItemInclusion("cal-1"))
            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            assertFalse(viewModel.state.value.canSubmit)
            assertEquals(0, draftRepository.createCount)
            assertFalse(viewModel.state.value.isSubmitting)
        }

    @Test
    fun `새 스냅샷이 들어오면 제외 상태도 초기화된다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal-1")))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.ToggleItemInclusion("cal-1"))
            runCurrent()
            assertEquals(setOf("cal-1"), viewModel.state.value.excludedRawIds)

            prepare(listOf(calendarItem("cal-1")))
            runCurrent()

            assertTrue(viewModel.state.value.excludedRawIds.isEmpty())
            assertEquals(1, viewModel.state.value.includedTotal)
        }

    @Test
    fun `준비 상태가 폐기되면 표시 모델과 제외 상태를 즉시 비운다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal"), photoItem(7L)))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.ToggleItemInclusion("cal"))
            runCurrent()
            assertNotNull(viewModel.state.value.content)

            // 로그아웃·세션 만료로 인증 경계가 교체되면 store 전체가 초기화되는 경로
            sessionStore.clearAll()
            runCurrent()

            assertNull(viewModel.state.value.content)
            assertTrue(viewModel.state.value.excludedRawIds.isEmpty())
            assertFalse(viewModel.state.value.canSubmit)
        }

    @Test
    fun `제출 중 사진 접근 실패는 준비를 폐기하고 사진 재선택 복귀를 기록한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal"), photoItem(7L)))
            draftRepository.uploadFailure = DraftPhotoAccessException("사진에 접근할 수 없습니다")
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            assertNull(sessionStore.preparation.value)
            assertTrue(sessionStore.consumePhotoReselectionNeeded())
            assertEquals(1, navigationHelper.backCount)
            assertEquals(0, draftRepository.createCount)
            // 폐기와 함께 민감 표시 모델도 남지 않는다.
            assertNull(viewModel.state.value.content)
        }

    @Test
    fun `유형 상세에서의 복귀는 준비 상태를 유지한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(calendarItem("cal")))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.CloseTypeDetail)
            runCurrent()

            assertEquals(1, navigationHelper.backCount)
            assertNotNull(sessionStore.preparation.value)
        }

    @Test
    fun `새로 더할 항목이 없으면 이유를 알려 준다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 서버는 초안이 있는 날짜의 생성을 덮어쓰기가 아니라 이어 붙이기로 처리한다. 이미
            // 들어간 항목만 다시 보내면 409 -1013 이고, 실패로만 보이면 이유를 알 수 없다.
            draftRepository.createFailure = ApiException.ConflictException(errorCode = -1013, rawCode = 409)
            prepare(listOf(calendarItem("cal")))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            assertTrue(viewModel.state.value.submitError.orEmpty().contains("새로 추가할 것이 없어요"))
            assertFalse(viewModel.state.value.isSubmitting)
            // 같은 스냅샷으로 다시 제출할 수 있게 준비는 남긴다.
            assertNotNull(sessionStore.preparation.value)
        }

    @Test
    fun `서버가 동의를 다시 요구하면 단계 동의 화면으로 보내고 준비는 지킨다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 약관 개정이나 구버전 온보딩으로 남은 동의가 있는 경우다. 이 화면은 동의를 받지
            // 않으므로 받는 자리로 보내고, 돌아와 같은 스냅샷으로 다시 제출할 수 있어야 한다.
            draftRepository.createFailure = ApiException.UnauthorizedException(errorCode = -3001, rawCode = 403)
            prepare(listOf(calendarItem("cal")))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.Submit)
            runCurrent()

            val destination = navigationHelper.destinations.filterIsInstance<StageTermsPage>().single()
            // 어느 단계가 비었는지 오류가 알려 주지 않으므로 후보를 모두 싣는다.
            assertEquals(
                listOf(TermStage.TIMELINE_FIRST_CREATE.name, TermStage.TIMELINE_LOCATION.name),
                destination.stages,
            )
            assertNotNull(sessionStore.preparation.value)
            assertFalse(viewModel.state.value.isSubmitting)
        }

    private fun createViewModel(): DraftConsentViewModel =
        DraftConsentViewModel(
            sessionStore = sessionStore,
            loadingSessionStore = loadingSessionStore,
            createTimelineDraftUseCase =
                CreateTimelineDraftUseCase(
                    repository = draftRepository,
                    messageHelper = NoOpMessageHelper,
                ),
            draftTaskCoordinator = draftTaskCoordinator,
            navigationHelper = navigationHelper,
            termsCoordinator = termsCoordinator,
            getDisplayTerms = GetDisplayTermsUseCase(EmptyTermsRepository),
            mapRenderGate = LocationMapRenderGate { mapRenderAllowed },
        )

    private fun prepare(
        items: List<SourceItem>,
        discardActiveTask: Boolean = false,
    ) {
        val window = RecordDateWindow.ofDate(date, zone)
        val selection = DraftSourceItemSelectionPolicy().select(window, items).getOrThrow()
        sessionStore.prepare(
            recordDate = date,
            zone = zone,
            window = window,
            selection = selection,
            discardActiveTask = discardActiveTask,
        )
    }

    private fun calendarItem(id: String): SourceItem {
        val instant = date.atTime(12, 0).atZone(zone).toInstant()
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

    private fun stayItem(id: String): SourceItem {
        val instant = date.atTime(9, 0).atZone(zone).toInstant()
        return SourceItem(
            rawId = id,
            startAt = instant,
            endAt = instant.plusSeconds(3_600),
            timeZoneId = zone,
            payload = StayPayload(latitude = 37.5665, longitude = 126.9780, address = "서울특별시 중구 세종대로 110"),
            sourceName = SourceName.LOCATION_PROVIDER,
            sourceKey = "STAY:$id",
            collectedAt = instant,
        )
    }

    private fun movementItem(id: String): SourceItem {
        val instant = date.atTime(10, 0).atZone(zone).toInstant()
        return SourceItem(
            rawId = id,
            startAt = instant,
            endAt = instant.plusSeconds(1_500),
            timeZoneId = zone,
            payload =
                MovementPayload(
                    start = GeoPoint(37.5701, 126.9820, "서울특별시 종로구 종로 1"),
                    end = GeoPoint(37.5512, 126.9882, "서울특별시 중구 남대문로 81"),
                    distanceMeters = 2_400.0,
                    transports = MovementPayload.Transport.WALKING,
                ),
            sourceName = SourceName.LOCATION_PROVIDER,
            sourceKey = "MOVEMENT:$id",
            collectedAt = instant,
        )
    }

    private fun photoItem(id: Long): SourceItem {
        val instant = date.atTime(13, 0).atZone(zone).toInstant()
        return SourceItem(
            rawId = "photo-$id",
            startAt = instant,
            endAt = null,
            timeZoneId = zone,
            payload = PhotoPayload("$id.jpg", "content://photo/$id", null, null, null),
            sourceName = SourceName.MEDIA_STORE,
            sourceKey = id.toString(),
            collectedAt = instant,
        )
    }

    /** 좌표가 붙은 사진. 서버는 이것도 위치정보로 본다. */
    private fun geoPhotoItem(id: Long): SourceItem {
        val instant = date.atTime(13, 0).atZone(zone).toInstant()
        return SourceItem(
            rawId = "photo-$id",
            startAt = instant,
            endAt = null,
            timeZoneId = zone,
            payload = PhotoPayload("$id.jpg", "content://photo/$id", 37.5665, 126.9780, null),
            sourceName = SourceName.MEDIA_STORE,
            sourceKey = id.toString(),
            collectedAt = instant,
        )
    }

    private data object NoOpMessageHelper : MessageHelper {
        override fun send(message: UserMessage) = Unit
    }

    private class FakeTimelineDraftRepository : TimelineDraftRepository {
        var uploadCount = 0
        var createCount = 0
        var createGate: CompletableDeferred<DraftTaskHandle>? = null
        var createFailure: Throwable? = null
        var uploadFailure: Throwable? = null
        var createdItems: List<SourceItem> = emptyList()
        var uploadedUris: List<String>? = null

        override suspend fun uploadPhotos(clientPhotoUris: List<String>): List<String> {
            uploadCount++
            uploadedUris = clientPhotoUris
            uploadFailure?.let { throw it }
            return clientPhotoUris.mapIndexed { index, _ -> "uploaded-$index.jpg" }
        }

        override suspend fun createDraft(
            recordDate: LocalDate,
            zone: ZoneId,
            window: RecordDateWindow,
            items: List<SourceItem>,
            uploadedPhotoFilenames: Map<String, String>,
        ): DraftTaskHandle {
            createCount++
            createdItems = items
            createFailure?.let { throw it }
            return createGate?.await() ?: DraftTaskHandle("task-$createCount")
        }

        override suspend fun getDraftStatus(taskId: String): DraftTaskSnapshot = throw UnsupportedOperationException()
    }

    private class FakeDraftTaskCoordinator : DraftTaskCoordinator {
        private val mutableState = MutableStateFlow<DraftTaskTrackingState>(DraftTaskTrackingState.Idle)
        override val state: StateFlow<DraftTaskTrackingState> = mutableState
        override val pendingCompletion: StateFlow<DraftTaskCompletion?> = MutableStateFlow(null)

        override suspend fun consumeCompletion(taskId: String): Boolean = false

        val startedTaskIds = mutableListOf<String>()
        var discardCount = 0

        override suspend fun start(
            taskId: String,
            recordDate: LocalDate,
        ) {
            startedTaskIds += taskId
            mutableState.value =
                DraftTaskTrackingState.Processing(
                    ActiveDraftTask(taskId, recordDate, Instant.EPOCH),
                )
        }

        override suspend fun onForeground() = Unit

        override suspend fun onBackground() = Unit

        override fun refreshFromCompletionSignal(taskId: String) = Unit

        override fun retry() = Unit

        override fun continueWaiting() = Unit

        override suspend fun discard() {
            discardCount++
            mutableState.value = DraftTaskTrackingState.Idle
        }
    }

    /** catalog 가 있는 환경을 가정한다 — 대체 조회는 돌지 않는다. */
    private object EmptyTermsRepository : TermsRepository {
        override suspend fun getCurrentTerms(types: List<TermType>) = emptyList<TermDocument>()

        override suspend fun getPublishedTerms(types: List<TermType>) = emptyList<TermDocument>()

        override suspend fun getMyAgreements() = emptyList<TermAgreement>()

        override suspend fun agree(documents: List<TermDocument>) = Unit
    }

    /** 세 종류를 모두 받아야 하는 상태로 시작한다. 서버 판정과 같은 집합이다. */
    private class FakeTermsAgreementCoordinator : TermsAgreementCoordinator {
        var agreeFailure: Throwable? = null
        var agreeCount = 0
        private val agreed = mutableSetOf<TermType>()

        fun markAgreed(type: TermType) {
            agreed += type
        }

        override val loginGate: StateFlow<TermsGateState> = MutableStateFlow(TermsGateState.Satisfied)

        override fun refresh() = Unit

        override suspend fun requirementOf(stage: TermStage): Result<TermStageRequirement> =
            Result.success(
                TermStageRequirement(
                    stage = stage,
                    items = stage.requiredTypes.map { TermRequirement(document(it), isAgreed = it in agreed) },
                ),
            )

        override suspend fun documentOf(type: TermType): TermDocument = document(type)

        override suspend fun agree(documents: List<TermDocument>): Result<Unit> {
            agreeCount++
            agreeFailure?.let { return Result.failure(it) }
            agreed += documents.map { it.termType }
            return Result.success(Unit)
        }

        private fun document(type: TermType) =
            TermDocument(
                termType = type,
                version = "1.0",
                title = type.name,
                contentUrl = "https://laimory.app/terms/${type.name}/1.0",
            )
    }

    private class RecordingNavigationHelper : NavigationHelper {
        val destinations = mutableListOf<Page>()
        var backCount = 0

        override fun navigateTo(page: Page) {
            destinations += page
        }

        override fun replaceRoot(page: Page) = Unit

        override fun navigateToBack() {
            backCount++
        }
    }
}
