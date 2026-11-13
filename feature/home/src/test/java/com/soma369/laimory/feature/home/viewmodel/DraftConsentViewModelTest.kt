package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.coordinator.TermsAgreementCoordinator
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.DialogRequest
import com.soma369.laimory.core.domain.message.DialogResult
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.ResolvedAddress
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
import com.soma369.laimory.core.domain.model.timeline.LocationMapKeyGate
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.navigation.DraftConsentDetailPage
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.provider.LocationAddressResolver
import com.soma369.laimory.core.domain.repository.MovementAddressRepository
import com.soma369.laimory.core.domain.repository.SourceItemDeletionRepository
import com.soma369.laimory.core.domain.repository.SourceItemRepository
import com.soma369.laimory.core.domain.repository.StayAddressRepository
import com.soma369.laimory.core.domain.repository.TermsRepository
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import com.soma369.laimory.core.domain.usecase.CreateTimelineDraftUseCase
import com.soma369.laimory.core.domain.usecase.DeleteSourceItemsUseCase
import com.soma369.laimory.core.domain.usecase.GetSourceItemsInWindowUseCase
import com.soma369.laimory.core.domain.usecase.ResolveMovementAddressesUseCase
import com.soma369.laimory.core.domain.usecase.ResolveStayAddressUseCase
import com.soma369.laimory.core.domain.usecase.RestoreSourceItemsUseCase
import com.soma369.laimory.core.domain.usecase.terms.GetDisplayTermsUseCase
import com.soma369.laimory.feature.home.draft.DraftConsentSessionStore
import com.soma369.laimory.feature.home.draft.DraftLoadingSessionStore
import com.soma369.laimory.feature.home.state.DraftConsentTypeGroup
import com.soma369.laimory.feature.home.state.DraftConsentUiContent
import com.soma369.laimory.feature.home.state.DraftConsentUiIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
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
    private val addressResolver = RecordingLocationAddressResolver()
    private var mapKeyPresent = true
    private val sourceItemStore = FakeSourceItemStore()
    private val messageHelper = RecordingMessageHelper()

    @Test
    fun `카드를 밀면 기기에서 지우고 실행 취소 묶음을 연다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(notificationItem("n-1"), notificationItem("n-2")))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("n-1"))
            runCurrent()

            assertEquals(listOf("n-2"), sourceItemStore.items.map { it.rawId })
            assertEquals(1, viewModel.state.value.deletionUndo?.count)
        }

    @Test
    fun `지운 카드는 홈이 스냅샷을 다시 만들기 전에 목록과 건수에서 바로 빠진다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(notificationItem("n-1"), notificationItem("n-2"), stayItem("stay")))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("n-1"))
            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("stay"))
            runCurrent()

            val content = viewModel.state.value.content!!
            assertEquals(listOf("n-2"), content.itemKeysOf(DraftConsentTypeGroup.NOTIFICATION))
            assertEquals(1, content.summaryOf(DraftConsentTypeGroup.NOTIFICATION)?.sentCount)
            assertEquals(emptyList<String>(), content.itemKeysOf(DraftConsentTypeGroup.LOCATION))
            assertTrue(content.locationMarkers.isEmpty())
            assertEquals(1, content.sentTotal)
        }

    @Test
    fun `스냅샷이 아직 지우기 전 것이면 실행 취소로 바로 다시 보인다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(notificationItem("n-1"), notificationItem("n-2")))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("n-1"))
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.UndoDeletion(viewModel.state.value.deletionUndo!!.token))
            runCurrent()

            assertEquals(listOf("n-1", "n-2"), viewModel.state.value.content!!.itemKeysOf(DraftConsentTypeGroup.NOTIFICATION).sorted())
        }

    @Test
    fun `새 스냅샷에서 사라지면 숨김을 걷어 되돌린 항목이 다시 들어오면 보인다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(notificationItem("n-1"), notificationItem("n-2")))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("n-1"))
            runCurrent()
            // 홈이 지운 뒤의 저장소를 다시 읽었다.
            resnapshot()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.UndoDeletion(viewModel.state.value.deletionUndo!!.token))
            runCurrent()
            assertEquals(listOf("n-2"), viewModel.state.value.content!!.itemKeysOf(DraftConsentTypeGroup.NOTIFICATION))
            // 홈이 복원분을 다시 읽으면 돌아온다.
            resnapshot()
            runCurrent()

            assertEquals(listOf("n-1", "n-2"), viewModel.state.value.content!!.itemKeysOf(DraftConsentTypeGroup.NOTIFICATION).sorted())
        }

    @Test
    fun `연달아 지우면 스낵바 하나에 건수가 쌓이고 묶음 표시가 바뀐다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(notificationItem("n-1"), notificationItem("n-2"), stayItem("stay")))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("n-1"))
            runCurrent()
            val first = viewModel.state.value.deletionUndo!!
            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("stay"))
            runCurrent()
            val second = viewModel.state.value.deletionUndo!!

            assertEquals(2, second.count)
            // 화면은 표시가 바뀔 때마다 남은 시간을 처음부터 다시 센다.
            assertTrue(first.token != second.token)
        }

    @Test
    fun `실행 취소는 묶음 전체를 되돌리고 스낵바를 닫는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(notificationItem("n-1"), notificationItem("n-2"), notificationItem("n-3")))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("n-1"))
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("n-2"))
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.UndoDeletion(viewModel.state.value.deletionUndo!!.token))
            runCurrent()

            assertEquals(setOf("n-1", "n-2", "n-3"), sourceItemStore.items.mapTo(mutableSetOf()) { it.rawId })
            assertNull(viewModel.state.value.deletionUndo)
        }

    @Test
    fun `전송에서 뺀 항목은 되돌려도 빠진 채로 돌아온다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(notificationItem("n-1"), notificationItem("n-2")))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.ToggleItemInclusion("n-1"))
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("n-1"))
            runCurrent()
            // 홈이 지운 뒤의 목록으로 스냅샷을 다시 만들면 사라진 항목은 제외 집합에서 걷힌다.
            resnapshot()
            runCurrent()
            assertEquals(emptySet<String>(), viewModel.state.value.excludedRawIds)

            viewModel.sendIntent(DraftConsentUiIntent.UndoDeletion(viewModel.state.value.deletionUndo!!.token))
            runCurrent()
            resnapshot()
            runCurrent()

            assertEquals(setOf("n-1"), viewModel.state.value.excludedRawIds)
        }

    @Test
    fun `시간이 다 되면 묶음이 끝나 되돌릴 수 없다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(notificationItem("n-1")))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("n-1"))
            runCurrent()
            val token = viewModel.state.value.deletionUndo!!.token

            viewModel.sendIntent(DraftConsentUiIntent.ExpireDeletionUndo(token))
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.UndoDeletion(token))
            runCurrent()

            assertNull(viewModel.state.value.deletionUndo)
            assertTrue(sourceItemStore.items.isEmpty())
        }

    @Test
    fun `지난 묶음 표시로 온 만료와 실행 취소는 무시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(notificationItem("n-1"), notificationItem("n-2")))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("n-1"))
            runCurrent()
            val staleToken = viewModel.state.value.deletionUndo!!.token
            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("n-2"))
            runCurrent()

            // 두 번째 삭제가 시간을 다시 걸었다. 첫 번째 표시의 타이머가 늦게 와도 묶음은 살아 있다.
            viewModel.sendIntent(DraftConsentUiIntent.ExpireDeletionUndo(staleToken))
            viewModel.sendIntent(DraftConsentUiIntent.UndoDeletion(staleToken))
            runCurrent()

            assertEquals(2, viewModel.state.value.deletionUndo?.count)
            assertTrue(sourceItemStore.items.isEmpty())
        }

    @Test
    fun `화면을 떠나면 묶음을 끝낸다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(notificationItem("n-1")))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("n-1"))
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.EndDeletionUndo)
            runCurrent()

            assertNull(viewModel.state.value.deletionUndo)
        }

    @Test
    fun `알림과 위치만 지울 수 있다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(calendarItem("cal"), photoItem(1)))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("cal"))
            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("photo-1"))
            runCurrent()

            assertEquals(2, sourceItemStore.items.size)
            assertNull(viewModel.state.value.deletionUndo)
        }

    @Test
    fun `제출용 스냅샷이 확정된 동안에는 지우지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            sourceItemStore.items += notificationItem("n-1")
            prepare(listOf(notificationItem("n-1")))
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("n-1"))
            runCurrent()

            assertFalse(viewModel.state.value.canDelete)
            assertEquals(1, sourceItemStore.items.size)
        }

    @Test
    fun `완성된 날에도 지울 수 있다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(notificationItem("n-1")))
            sessionStore.setSelectionReadOnly(true)
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("n-1"))
            runCurrent()

            assertTrue(sourceItemStore.items.isEmpty())
        }

    @Test
    fun `앱 묶음 지우기는 확인하면 기록 범위 안 그 앱 알림을 모두 지운다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(notificationItem("kakao-1"), notificationItem("kakao-2"), notificationItem("slack-1", packageName = "slack")))
            // 전송 상한 때문에 화면 목록에 없는 같은 앱 알림도 지운다. 기기에서 다시 읽기 때문이다.
            sourceItemStore.items += notificationItem("kakao-hidden")
            messageHelper.twoButtonResult = DialogResult.Primary
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.DeleteNotificationApp("kakao"))
            runCurrent()

            assertEquals("카카오톡 알림 3건을 기기에서 지울까요?", messageHelper.twoButtonRequests.single().title)
            assertEquals(listOf("slack-1"), sourceItemStore.items.map { it.rawId })
            assertEquals(3, viewModel.state.value.deletionUndo?.count)
        }

    @Test
    fun `앱 묶음 지우기를 기다리는 사이 화면을 떠나면 끝낸 묶음을 다시 열지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(notificationItem("kakao-1"), notificationItem("kakao-2")))
            messageHelper.twoButtonResult = DialogResult.Primary
            val gate = CompletableDeferred<Unit>()
            sourceItemStore.deleteGate = gate
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.DeleteNotificationApp("kakao"))
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.EndDeletionUndo)
            runCurrent()
            gate.complete(Unit)
            runCurrent()

            // 기기에서는 지워졌지만 화면 밖에서 묶음이 되살아나지 않는다.
            assertTrue(sourceItemStore.items.isEmpty())
            assertNull(viewModel.state.value.deletionUndo)
        }

    @Test
    fun `지우기를 기다리는 사이 계정이 바뀌면 이전 계정의 묶음을 만들지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(notificationItem("n-1"), notificationItem("n-2")))
            val gate = CompletableDeferred<Unit>()
            sourceItemStore.deleteGate = gate
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("n-1"))
            runCurrent()

            sessionStore.clearAll()
            runCurrent()
            gate.complete(Unit)
            runCurrent()

            assertNull(viewModel.state.value.deletionUndo)
            assertNull(viewModel.state.value.content)
        }

    @Test
    fun `하단 모두 지우기는 확인하면 기록 범위 안 알림을 모두 지우고 다른 유형은 남긴다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(notificationItem("kakao-1"), notificationItem("slack-1", packageName = "slack"), stayItem("stay")))
            // 전송 상한 때문에 화면 목록에 없는 알림도 지운다.
            sourceItemStore.items += notificationItem("kakao-hidden")
            messageHelper.twoButtonResult = DialogResult.Primary
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.DeleteAllOfGroup(DraftConsentTypeGroup.NOTIFICATION))
            runCurrent()

            assertEquals("알림 3건을 모두 기기에서 지울까요?", messageHelper.twoButtonRequests.single().title)
            assertEquals(listOf("stay"), sourceItemStore.items.map { it.rawId })
            assertEquals(3, viewModel.state.value.deletionUndo?.count)
        }

    @Test
    fun `위치 모두 지우기는 체류와 이동을 함께 지우고 실행 취소로 전부 돌아온다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(stayItem("stay"), movementItem("move"), notificationItem("n-1")))
            messageHelper.twoButtonResult = DialogResult.Primary
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.DeleteAllOfGroup(DraftConsentTypeGroup.LOCATION))
            runCurrent()
            assertEquals("위치 2건을 모두 기기에서 지울까요?", messageHelper.twoButtonRequests.single().title)
            assertEquals(listOf("n-1"), sourceItemStore.items.map { it.rawId })

            viewModel.sendIntent(DraftConsentUiIntent.UndoDeletion(viewModel.state.value.deletionUndo!!.token))
            runCurrent()

            assertEquals(setOf("stay", "move", "n-1"), sourceItemStore.items.mapTo(mutableSetOf()) { it.rawId })
        }

    @Test
    fun `지울 수 없는 유형의 모두 지우기는 다이얼로그 없이 무시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(calendarItem("cal"), notificationItem("n-1")))
            messageHelper.twoButtonResult = DialogResult.Primary
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.DeleteAllOfGroup(DraftConsentTypeGroup.CALENDAR))
            runCurrent()

            assertTrue(messageHelper.twoButtonRequests.isEmpty())
            assertEquals(2, sourceItemStore.items.size)
        }

    @Test
    fun `앱 묶음 지우기를 취소하면 아무것도 지우지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(notificationItem("kakao-1")))
            messageHelper.twoButtonResult = DialogResult.Secondary
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.DeleteNotificationApp("kakao"))
            runCurrent()

            assertEquals(1, sourceItemStore.items.size)
            assertNull(viewModel.state.value.deletionUndo)
        }

    @Test
    fun `스냅샷이 사라지면 지운 내용을 메모리에 남기지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            browse(listOf(notificationItem("n-1")))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.DeleteItem("n-1"))
            runCurrent()
            val token = viewModel.state.value.deletionUndo!!.token

            sessionStore.clearAll()
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.UndoDeletion(token))
            runCurrent()

            assertNull(viewModel.state.value.deletionUndo)
            assertTrue(sourceItemStore.items.isEmpty())
        }

    @Test
    fun `새 스냅샷이 들어오면 내용을 다시 구성하고 고른 것은 남긴다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()
            prepare(listOf(calendarItem("cal")))
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.ToggleItemInclusion("cal"))
            runCurrent()
            assertEquals(setOf("cal"), viewModel.state.value.excludedRawIds)

            // 수집이 갱신돼도 사용자가 고른 것은 남는다 — 매번 비우면 카드에서 무엇을 뺄 수가 없다.
            prepare(listOf(calendarItem("cal")))
            runCurrent()

            val state = viewModel.state.value
            assertEquals(setOf("cal"), state.excludedRawIds)
            assertNotNull(state.content)
            assertEquals(1, state.content!!.sentTotal)
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

    @Test
    fun `홈이 선택을 잠그면 항목과 위치 전송을 바꾸지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 완성된 날은 상세를 보여 주기만 한다.
            prepare(listOf(stayItem("stay-1"), calendarItem("cal-1")))
            sessionStore.setSelectionReadOnly(true)
            val viewModel = createViewModel()
            runCurrent()
            assertTrue(viewModel.state.value.isSelectionReadOnly)

            viewModel.sendIntent(DraftConsentUiIntent.ToggleItemInclusion("cal-1"))
            viewModel.sendIntent(DraftConsentUiIntent.ToggleLocationInclusion)
            runCurrent()

            assertTrue(viewModel.state.value.excludedRawIds.isEmpty())
            assertTrue(viewModel.state.value.isLocationIncluded)
        }

    // --- 위치정보 전송 Switch ---

    @Test
    fun `위치정보 전송을 끄면 제외 집합을 건드리지 않고 전송에서만 뺀다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepare(listOf(stayItem("stay-1"), movementItem("move-1"), calendarItem("cal-1")))
            val viewModel = createViewModel()
            runCurrent()
            assertTrue(viewModel.state.value.isLocationIncluded)

            viewModel.sendIntent(DraftConsentUiIntent.ToggleLocationInclusion)
            runCurrent()

            // 그 시점 rawId 를 제외 집합에 넣지 않는다 — 그러면 뒤에 수집된 위치가 샌다.
            assertTrue(viewModel.state.value.excludedRawIds.isEmpty())
            assertFalse(viewModel.state.value.isLocationIncluded)
            // 전송 건수에서는 빠지고, 다른 유형은 건드리지 않는다.
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
    fun `주소 없는 위치는 진입할 때 해석해 목록과 지도에 함께 얹는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            agreeLocationTerms()
            addressResolver.answer = { latitude, _ -> if (latitude == 37.5665) "해석한 체류 주소" else "해석한 이동 주소" }
            prepare(listOf(stayItem("stay-1", address = null), movementItem("move-1", null, null)))
            val viewModel = createViewModel()
            runCurrent()

            val content = viewModel.state.value.content
            assertEquals(
                listOf("해석한 체류 주소", "해석한 이동 주소", "해석한 이동 주소"),
                content?.locationMarkers?.map { it.title },
            )
            // 지도 말풍선과 목록이 같은 맵을 본다.
            val titles = content?.summaryOf(DraftConsentTypeGroup.LOCATION)?.sections?.flatMap { it.items }?.map { it.title }
            assertEquals(listOf("해석한 체류 주소", "해석한 이동 주소 →\n해석한 이동 주소"), titles)
        }

    @Test
    fun `수집 당시 주소가 있으면 다시 해석하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            agreeLocationTerms()
            prepare(listOf(stayItem("stay-1"), movementItem("move-1")))
            val viewModel = createViewModel()
            runCurrent()

            assertTrue(addressResolver.requested.isEmpty())
            assertEquals("서울특별시 중구 세종대로 110", viewModel.state.value.content?.locationMarkers?.first()?.title)
        }

    @Test
    fun `해석에 실패하면 주소 미확인으로 두고 좌표마다 한 번씩만 부른다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            agreeLocationTerms()
            addressResolver.answer = { _, _ -> null }
            prepare(listOf(stayItem("stay-1", address = null), movementItem("move-1", null, null)))
            val viewModel = createViewModel()
            runCurrent()

            assertEquals(3, addressResolver.requested.size)
            assertEquals(3, addressResolver.requested.distinct().size)
            assertTrue(viewModel.state.value.content?.locationMarkers.orEmpty().all { it.title == "주소 미확인" })
        }

    @Test
    fun `위치 약관에 동의했을 때만 지도를 붙인다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            agreeLocationTerms()
            val viewModel = createViewModel()
            prepare(listOf(stayItem("stay-1")))
            runCurrent()

            assertTrue(viewModel.state.value.isMapRenderAllowed)
        }

    @Test
    fun `동의가 없으면 지도도 주소 해석도 시작하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 지도는 카메라 영역을, Geocoder 는 좌표 자체를 Google 로 보낸다. 지도만 막으면
            // 미동의 좌표가 주소 해석으로 그대로 나간다.
            addressResolver.answer = { _, _ -> "해석한 주소" }
            val viewModel = createViewModel()
            prepare(listOf(stayItem("stay-1", address = null), movementItem("move-1", null, null)))
            runCurrent()

            assertFalse(viewModel.state.value.isMapRenderAllowed)
            assertTrue(addressResolver.requested.isEmpty())
        }

    @Test
    fun `약관 조회에 실패하면 지도를 붙이지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            agreeLocationTerms()
            termsCoordinator.requirementFailure = IllegalStateException("network")
            val viewModel = createViewModel()
            prepare(listOf(stayItem("stay-1")))
            runCurrent()

            assertFalse(viewModel.state.value.isMapRenderAllowed)
        }

    @Test
    fun `키가 없으면 동의해도 지도를 붙이지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            agreeLocationTerms()
            mapKeyPresent = false
            val viewModel = createViewModel()
            prepare(listOf(stayItem("stay-1")))
            runCurrent()

            assertFalse(viewModel.state.value.isMapRenderAllowed)
        }

    @Test
    fun `동의 조회가 늦게 와도 현재 화면에 반영된다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            agreeLocationTerms()
            termsCoordinator.requirementGate = CompletableDeferred()
            val viewModel = createViewModel()
            prepare(listOf(stayItem("stay-1")))
            runCurrent()
            assertFalse(viewModel.state.value.isMapRenderAllowed)

            termsCoordinator.requirementGate?.complete(Unit)
            runCurrent()

            assertTrue(viewModel.state.value.isMapRenderAllowed)
        }

    @Test
    fun `계정이 바뀌면 이전 계정의 지도 허용을 넘겨주지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 이 ViewModel 은 Activity 범위라 로그아웃 뒤 다음 계정까지 살아 있다.
            agreeLocationTerms()
            val viewModel = createViewModel()
            prepare(listOf(stayItem("stay-1")))
            runCurrent()
            assertTrue(viewModel.state.value.isMapRenderAllowed)

            sessionStore.clearAll()
            termsCoordinator.revokeAll()
            runCurrent()
            prepare(listOf(stayItem("stay-2")))
            runCurrent()

            assertFalse(viewModel.state.value.isMapRenderAllowed)
        }

    @Test
    fun `뒤늦게 동의하면 다음 시도에서 지도가 열린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            prepare(listOf(stayItem("stay-1")))
            runCurrent()
            assertFalse(viewModel.state.value.isMapRenderAllowed)

            agreeLocationTerms()
            prepare(listOf(stayItem("stay-2")))
            runCurrent()

            assertTrue(viewModel.state.value.isMapRenderAllowed)
        }

    @Test
    fun `약관 화면에서 동의하고 복귀하면 같은 스냅샷에서 지도와 주소가 열린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 제출이 403 -3001 로 막히면 사진을 다시 고르지 않도록 준비를 폐기하지 않는다.
            // 새 attemptId 가 없으므로 스냅샷 수집만으로는 재판정 계기가 없다.
            addressResolver.answer = { _, _ -> "해석한 주소" }
            val viewModel = createViewModel()
            prepare(listOf(stayItem("stay-1", address = null), calendarItem("cal-1")))
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.ToggleItemInclusion("cal-1"))
            runCurrent()
            val attemptId = viewModel.state.value.content?.revision
            assertFalse(viewModel.state.value.isMapRenderAllowed)
            assertTrue(addressResolver.requested.isEmpty())

            agreeLocationTerms()
            viewModel.sendIntent(DraftConsentUiIntent.Sync)
            runCurrent()

            assertTrue(viewModel.state.value.isMapRenderAllowed)
            assertEquals("해석한 주소", viewModel.state.value.content?.locationMarkers?.single()?.title)
            // 스냅샷과 제외 선택은 그대로여야 한다.
            assertEquals(attemptId, viewModel.state.value.content?.revision)
            assertEquals(setOf("cal-1"), viewModel.state.value.excludedRawIds)
        }

    @Test
    fun `복귀를 반복해도 같은 좌표를 다시 부르지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            agreeLocationTerms()
            addressResolver.answer = { _, _ -> "해석한 주소" }
            val viewModel = createViewModel()
            prepare(listOf(stayItem("stay-1", address = null)))
            runCurrent()
            assertEquals(1, addressResolver.requested.size)

            repeat(3) { viewModel.sendIntent(DraftConsentUiIntent.Sync) }
            runCurrent()

            assertEquals(1, addressResolver.requested.size)
        }

    @Test
    fun `준비가 없으면 복귀 신호를 무시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            agreeLocationTerms()
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(DraftConsentUiIntent.Sync)
            runCurrent()

            assertFalse(viewModel.state.value.isMapRenderAllowed)
        }

    @Test
    fun `스냅샷이 갱신돼도 제외는 남고 사라진 항목만 걷힌다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 수집이 돌 때마다 사용자가 뺀 것이 되살아나면 카드에서 무엇을 뺄 수가 없다.
            prepare(listOf(calendarItem("cal-1"), calendarItem("cal-2")))
            val viewModel = createViewModel()
            runCurrent()
            viewModel.sendIntent(DraftConsentUiIntent.ToggleItemInclusion("cal-1"))
            viewModel.sendIntent(DraftConsentUiIntent.ToggleItemInclusion("cal-2"))
            runCurrent()
            assertEquals(setOf("cal-1", "cal-2"), viewModel.state.value.excludedRawIds)

            // cal-2 가 창 밖으로 나갔다.
            prepare(listOf(calendarItem("cal-1")))
            runCurrent()

            assertEquals(setOf("cal-1"), viewModel.state.value.excludedRawIds)
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
            resolveStayAddress = ResolveStayAddressUseCase(addressResolver, NoOpStayAddressRepository),
            resolveMovementAddresses = ResolveMovementAddressesUseCase(addressResolver, NoOpMovementAddressRepository),
            deleteSourceItems = DeleteSourceItemsUseCase(sourceItemStore),
            restoreSourceItems = RestoreSourceItemsUseCase(sourceItemStore),
            getSourceItemsInWindow = GetSourceItemsInWindowUseCase(sourceItemStore),
            messageHelper = messageHelper,
            mapKeyGate = LocationMapKeyGate { mapKeyPresent },
        )

    /** 홈이 상시로 유지하는 스냅샷만 둔다. CTA 를 누르기 전 카드에서 상세로 들어온 상태다. */
    private fun browse(items: List<SourceItem>) {
        sourceItemStore.items += items
        val window = RecordDateWindow.ofDate(date, zone)
        val selection = DraftSourceItemSelectionPolicy().select(window, items).getOrThrow()
        sessionStore.updateSelection(recordDate = date, zone = zone, window = window, selection = selection)
    }

    /** 상세 화면에 보이는 그 유형 카드의 rawId. */
    private fun DraftConsentUiContent.itemKeysOf(group: DraftConsentTypeGroup): List<String> =
        summaryOf(group)?.sections.orEmpty().flatMap { section -> section.items.map { it.key } }

    /** 홈이 수집 관찰로 저장소의 지금 목록을 다시 스냅샷으로 만든다. */
    private fun resnapshot() {
        val window = RecordDateWindow.ofDate(date, zone)
        val selection = DraftSourceItemSelectionPolicy().select(window, sourceItemStore.items.toList()).getOrThrow()
        sessionStore.updateSelection(recordDate = date, zone = zone, window = window, selection = selection)
    }

    private fun notificationItem(
        id: String,
        packageName: String = "kakao",
    ): SourceItem {
        val instant = date.atTime(15, 0).atZone(zone).toInstant()
        return SourceItem(
            rawId = id,
            startAt = instant,
            endAt = null,
            timeZoneId = zone,
            payload =
                NotificationPayload(
                    appName = if (packageName == "kakao") "카카오톡" else packageName,
                    packageName = packageName,
                    title = "제목",
                    text = "본문",
                    collectReason = NotificationPayload.CollectReason.APP,
                ),
            sourceName = SourceName.NOTIFICATION_LISTENER,
            sourceKey = "key:$id",
            collectedAt = instant,
        )
    }

    /** 기기 저장소 대역. 지우기·되돌리기·기록 범위 조회만 실제로 흉내 낸다. */
    private class FakeSourceItemStore :
        SourceItemRepository,
        SourceItemDeletionRepository {
        val items = mutableListOf<SourceItem>()

        /** 두면 지우기가 이것이 끝날 때까지 DB 응답을 기다린다. */
        var deleteGate: CompletableDeferred<Unit>? = null

        override suspend fun delete(rawIds: Collection<String>): List<SourceItem> {
            deleteGate?.await()
            val deleted = items.filter { it.rawId in rawIds }
            items.removeAll(deleted)
            return deleted
        }

        override suspend fun restore(items: List<SourceItem>) {
            this.items += items
        }

        override suspend fun getInWindow(
            start: Instant,
            end: Instant,
        ): List<SourceItem> = items.filter { it.startAt >= start && it.startAt < end }

        override suspend fun addAll(items: List<SourceItem>): Int = error("사용하지 않음")

        override suspend fun upsertAll(items: List<SourceItem>): Int = error("사용하지 않음")

        override fun observeAll(): Flow<List<SourceItem>> = flowOf(items.toList())

        override suspend fun getLatestCollectedAt(itemType: ItemType): Instant? = null

        override suspend fun deleteExpired(cutoff: Instant): Int = 0

        override suspend fun clear(itemType: ItemType) = Unit
    }

    private class RecordingMessageHelper : MessageHelper {
        var twoButtonResult: DialogResult = DialogResult.Dismissed
        val twoButtonRequests = mutableListOf<DialogRequest.TwoButton>()

        override fun send(message: UserMessage) = Unit

        override suspend fun showTwoButtonDialog(request: DialogRequest.TwoButton): DialogResult {
            twoButtonRequests += request
            return twoButtonResult
        }
    }

    private fun agreeLocationTerms() = termsCoordinator.markAgreed(TermType.LOCATION_BASED_SERVICE_TERMS)

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

    private class RecordingLocationAddressResolver : LocationAddressResolver {
        var answer: (Double, Double) -> String? = { _, _ -> null }
        val requested = mutableListOf<Pair<Double, Double>>()

        override suspend fun resolve(
            latitude: Double,
            longitude: Double,
        ): ResolvedAddress? {
            requested += latitude to longitude
            return answer(latitude, longitude)?.let { ResolvedAddress(line = it) }
        }
    }

    private object NoOpStayAddressRepository : StayAddressRepository {
        override suspend fun updateAddress(
            rawId: String,
            address: ResolvedAddress,
        ): Boolean = true
    }

    private object NoOpMovementAddressRepository : MovementAddressRepository {
        override suspend fun updateAddresses(
            rawId: String,
            startAddress: String?,
            endAddress: String?,
        ): Boolean = true
    }

    private fun stayItem(
        id: String,
        address: String? = "서울특별시 중구 세종대로 110",
    ): SourceItem {
        val instant = date.atTime(9, 0).atZone(zone).toInstant()
        return SourceItem(
            rawId = id,
            startAt = instant,
            endAt = instant.plusSeconds(3_600),
            timeZoneId = zone,
            payload = StayPayload(latitude = 37.5665, longitude = 126.9780, address = address),
            sourceName = SourceName.LOCATION_PROVIDER,
            sourceKey = "STAY:$id",
            collectedAt = instant,
        )
    }

    private fun movementItem(
        id: String,
        startAddress: String? = "서울특별시 종로구 종로 1",
        endAddress: String? = "서울특별시 중구 남대문로 81",
    ): SourceItem {
        val instant = date.atTime(10, 0).atZone(zone).toInstant()
        return SourceItem(
            rawId = id,
            startAt = instant,
            endAt = instant.plusSeconds(1_500),
            timeZoneId = zone,
            payload =
                MovementPayload(
                    start = GeoPoint(37.5701, 126.9820, startAddress),
                    end = GeoPoint(37.5512, 126.9882, endAddress),
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

        override suspend fun isCreatable(
            items: List<SourceItem>,
            zone: ZoneId,
        ): Boolean = error("사용하지 않음")

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

        /** 조회를 붙잡아 두는 문. 비동기 응답이 늦는 상황을 만든다. */
        var requirementGate: CompletableDeferred<Unit>? = null
        var requirementFailure: Throwable? = null
        private val agreed = mutableSetOf<TermType>()

        fun markAgreed(type: TermType) {
            agreed += type
        }

        /** 계정 경계 교체. 다음 계정에는 이전 계정의 동의가 없다. */
        fun revokeAll() {
            agreed.clear()
        }

        override val loginGate: StateFlow<TermsGateState> = MutableStateFlow(TermsGateState.Satisfied)

        override fun refresh() = Unit

        override suspend fun requirementOf(stage: TermStage): Result<TermStageRequirement> {
            requirementGate?.await()
            requirementFailure?.let { return Result.failure(it) }
            return Result.success(
                TermStageRequirement(
                    stage = stage,
                    items = stage.requiredTypes.map { TermRequirement(document(it), isAgreed = it in agreed) },
                ),
            )
        }

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
