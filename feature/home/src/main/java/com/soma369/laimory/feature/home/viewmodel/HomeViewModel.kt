package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.coordinator.AutoCollectionCoordinator
import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.coordinator.TermsAgreementCoordinator
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.DraftPhotoAccessException
import com.soma369.laimory.core.domain.helper.GlobalLoadingHelper
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.DialogRequest
import com.soma369.laimory.core.domain.message.DialogResult
import com.soma369.laimory.core.domain.model.collection.CollectionLabAccessGate
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.PhotoCandidate
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceItemRetentionConfig
import com.soma369.laimory.core.domain.model.terms.TermStage
import com.soma369.laimory.core.domain.model.timeline.DailyRecordReadOutcome
import com.soma369.laimory.core.domain.model.timeline.DailyRecordStatus
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.DraftPhotoLimitExceededException
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelection
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.model.timeline.DraftTaskUnavailableReason
import com.soma369.laimory.core.domain.model.timeline.MonthlyDailyRecord
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.navigation.CollectionPage
import com.soma369.laimory.core.domain.navigation.DraftConsentDetailPage
import com.soma369.laimory.core.domain.navigation.DraftLoadingPage
import com.soma369.laimory.core.domain.navigation.PastRecordsPage
import com.soma369.laimory.core.domain.navigation.StageTermsPage
import com.soma369.laimory.core.domain.navigation.TimelinePage
import com.soma369.laimory.core.domain.usecase.CreateTimelineDraftUseCase
import com.soma369.laimory.core.domain.usecase.GetDailyRecordUseCase
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
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import com.soma369.laimory.feature.home.draft.DraftConsentPreparation
import com.soma369.laimory.feature.home.draft.DraftConsentSessionStore
import com.soma369.laimory.feature.home.draft.DraftLoadingSessionStore
import com.soma369.laimory.feature.home.draft.toLoadingSession
import com.soma369.laimory.feature.home.state.DraftConsentTypeGroup
import com.soma369.laimory.feature.home.state.DraftCreationStatus
import com.soma369.laimory.feature.home.state.DraftRetryMode
import com.soma369.laimory.feature.home.state.HomeDefaultDate
import com.soma369.laimory.feature.home.state.HomePhotoItem
import com.soma369.laimory.feature.home.state.HomeRecordState
import com.soma369.laimory.feature.home.state.HomeSourceKind
import com.soma369.laimory.feature.home.state.HomeSourcePermissions
import com.soma369.laimory.feature.home.state.HomeTimeField
import com.soma369.laimory.feature.home.state.HomeTimeSheetState
import com.soma369.laimory.feature.home.state.HomeUiIntent
import com.soma369.laimory.feature.home.state.HomeUiSideEffect
import com.soma369.laimory.feature.home.state.HomeUiState
import com.soma369.laimory.feature.home.state.MAX_PHOTO_SELECTION
import com.soma369.laimory.feature.home.state.confirmDialogBody
import com.soma369.laimory.feature.home.state.isDateLocked
import com.soma369.laimory.feature.home.state.isInputLocked
import com.soma369.laimory.feature.home.state.isSelectableRecordDate
import com.soma369.laimory.feature.home.state.isSourceViewLocked
import com.soma369.laimory.feature.home.state.locationRawIds
import com.soma369.laimory.feature.home.state.refreshSourceSummary
import com.soma369.laimory.feature.home.state.timelineButtonStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val observeSourceItemsUseCase: ObserveSourceItemsUseCase,
        private val prepareTimelineDraftSelectionUseCase: PrepareTimelineDraftSelectionUseCase,
        private val getDailyRecordsUseCase: GetDailyRecordsUseCase,
        private val getMonthlyDailyRecordsUseCase: GetMonthlyDailyRecordsUseCase,
        private val getDailyRecordUseCase: GetDailyRecordUseCase,
        private val getPhotosInWindowUseCase: GetPhotosInWindowUseCase,
        private val prepareSelectedPhotosUseCase: PrepareSelectedPhotosUseCase,
        private val draftConsentSessionStore: DraftConsentSessionStore,
        private val draftTaskCoordinator: DraftTaskCoordinator,
        private val observeUserProfileUseCase: ObserveUserProfileUseCase,
        private val refreshUserProfileUseCase: RefreshUserProfileUseCase,
        private val navigationHelper: NavigationHelper,
        private val globalLoadingHelper: GlobalLoadingHelper,
        private val autoCollectionCoordinator: AutoCollectionCoordinator,
        private val getSourceItemsInWindowUseCase: GetSourceItemsInWindowUseCase,
        private val createTimelineDraftUseCase: CreateTimelineDraftUseCase,
        private val loadingSessionStore: DraftLoadingSessionStore,
        private val messageHelper: MessageHelper,
        private val termsCoordinator: TermsAgreementCoordinator,
        private val resolveStayAddress: ResolveStayAddressUseCase,
        /** UTC 기준이다. 현지 날짜는 [zone] 으로 옮겨서 얻는다. */
        private val clock: Clock,
        retentionConfig: SourceItemRetentionConfig,
        collectionLabAccessGate: CollectionLabAccessGate,
    ) : BaseMviViewModel<HomeUiState, HomeUiIntent, HomeUiSideEffect>(
            LocalDateTime.now(clock.withZone(ZoneId.systemDefault())).let { now ->
                HomeUiState(
                    selectedDate = HomeDefaultDate.of(now),
                    today = now.toLocalDate(),
                    isCollectionLabAccessible = collectionLabAccessGate.isCollectionLabAccessible(),
                    retentionDays = retentionConfig.retentionDays,
                )
            },
        ) {
        /** 기록 창을 자를 시간대. 기기 시간대가 바뀔 수 있어 [HomeUiIntent.RefreshToday] 마다 다시 읽는다. */
        private var zone: ZoneId = ZoneId.systemDefault()
        private var sourceItems: List<SourceItem> = emptyList()

        /** 날짜 피커가 이미 받아 온 달. 같은 달을 두 번 부르지 않는다. */
        private val loadedRecordMonths = mutableSetOf<YearMonth>()

        private var photoCandidates: List<PhotoCandidate> = emptyList()
        private var photoAccessGranted = false
        private var photoCandidatesJob: Job? = null

        /** 층위를 채우러 이미 물어본 체류. 같은 항목을 반복해서 묻지 않는다. */
        private val attemptedStayRawIds = mutableSetOf<String>()
        private var lastLoadedPhotoWindow: RecordDateWindow? = null
        private var requestedPhotoWindow: RecordDateWindow? = null
        private var preparedPhotoCache: PreparedPhotoCache? = null
        private var dateSource = DateSource.DEFAULT
        private var consentPreparationJob: Job? = null
        private var locationConsentJob: Job? = null

        /** 고른 날짜의 서버 기록 판정. 날짜가 바뀌면 이전 판정을 끊는다. */
        private var recordStateJob: Job? = null

        init {
            observeSummary()
            observeDraftTask()
            observeUserProfile()
            observeAccountSession()
            observeSubmissionExclusions()
            observeSelectionLock()
        }

        /** 전송 선택을 바꿀 수 없는 구간을 상세에 알린다. 상세는 이 값으로 토글을 막는다. */
        private fun observeSelectionLock() =
            safeLaunch {
                state
                    .map { it.isInputLocked }
                    .distinctUntilChanged()
                    .collect(draftConsentSessionStore::setSelectionReadOnly)
            }

        /**
         * 상세에서 뺀 항목·위치 스위치가 바뀌면 홈 건수를 다시 센다.
         *
         * 상세는 스토어만 바꾸고 돌아온다. 홈이 수집 갱신을 기다리면, 위치를 끄고 돌아온 화면이
         * 한동안 끄기 전 건수를 그대로 적고 있는다.
         */
        private fun observeSubmissionExclusions() =
            safeLaunch {
                combine(
                    draftConsentSessionStore.excludedRawIds,
                    draftConsentSessionStore.isLocationSendEnabled,
                ) { excluded, isLocationEnabled -> excluded to isLocationEnabled }
                    .drop(1)
                    .collect { updateState { withSourceSummary(sourceItems, photoCandidates) } }
            }

        /**
         * 계정이 바뀌면 위치 동의 판정을 버린다.
         *
         * 이 ViewModel 은 Activity 범위라 계정 경계를 넘어 살아남는데, 판정값만 남으면 새 계정이
         * 동의하지 않았는데도 원천 갱신이 그 값을 보고 `Geocoder` 를 부른다. 진행 중이던 조회도
         * 끊는다 — 이전 계정의 응답이 늦게 도착해 새 계정의 판정으로 앉는다.
         */
        private fun observeAccountSession() =
            safeLaunch {
                draftConsentSessionStore.accountSession.drop(1).collect {
                    locationConsentJob?.cancel()
                    attemptedStayRawIds.clear()
                    updateState { copy(isLocationConsentGranted = false) }
                }
            }

        /**
         * 공용 회원 정보를 인사말에 반영한다.
         *
         * 조회 자체는 coordinator 가 세션당 한 번만 하므로 여기서 서버를 부르지 않는다.
         * 재시도는 [HomeUiIntent.RefreshProfile] 이 맡는다.
         */
        private fun observeUserProfile() {
            safeLaunch {
                observeUserProfileUseCase().collect { profile ->
                    updateState { copy(nickname = profile?.nickname) }
                }
            }
        }

        override suspend fun handleIntent(intent: HomeUiIntent) {
            when (intent) {
                // 화면이 뜰 때마다 부른다. ViewModel 이 Activity 수명이라 init 에서 한 번만 부르면
                // 첫 조회가 실패한 세션 내내 닉네임이 fallback 으로 남는다. 성공한 뒤의 중복 요청은
                // coordinator 의 세션 캐시·single-flight 가 막는다.
                HomeUiIntent.RefreshProfile -> refreshUserProfileUseCase()
                HomeUiIntent.RefreshToday -> refreshToday()
                // 버튼만 숨기지 않고 호출 경계에서도 막는다 — release 에는 라우트 자체가 없다.
                HomeUiIntent.NavigateToCollection ->
                    if (state.value.isCollectionLabAccessible) navigationHelper.navigateTo(CollectionPage) else Unit
                HomeUiIntent.OpenPhotoSheet -> startPhotoSelection()
                HomeUiIntent.RequestAdditionalPhotoAccess ->
                    sendEffect(HomeUiSideEffect.RequestPhotoAccess(force = true))
                is HomeUiIntent.ResolvePhotoAccess -> resolvePhotoAccess(intent.granted, intent.limited)
                is HomeUiIntent.RefreshPhotos -> refreshPhotos(intent.hasAccess, intent.limited)
                HomeUiIntent.DismissPhotoSheet ->
                    updateState {
                        copy(isPhotoSheetVisible = false, isPhotoAccessDenied = false, pendingPhotoIds = emptySet())
                    }
                is HomeUiIntent.TogglePhoto -> togglePhoto(intent.mediaStoreId)
                is HomeUiIntent.TogglePhotoDate -> togglePhotoDate(intent.date)
                HomeUiIntent.ToggleAllPhotos -> toggleAllPhotos()
                HomeUiIntent.ConfirmPhotoSelection -> confirmPhotoSelection()
                HomeUiIntent.ContinueWithoutPhotos -> continueWithoutPhotos()
                HomeUiIntent.ShowDatePicker -> showDatePicker()
                HomeUiIntent.DismissDatePicker -> updateState { copy(isDatePickerVisible = false) }
                is HomeUiIntent.LoadMonthlyRecords -> loadMonthlyRecords(intent.month)
                HomeUiIntent.RefreshRecordState -> refreshSelectedRecord()
                is HomeUiIntent.SelectDate -> selectDate(intent.date)
                is HomeUiIntent.ShowTimePicker -> showTimeSheet(intent.field)
                is HomeUiIntent.ExpandTimeField ->
                    updateState { copy(timeSheet = timeSheet?.copy(expandedField = intent.field)) }
                is HomeUiIntent.ChangeSheetTime -> changeSheetTime(intent.field, intent.date, intent.time)
                HomeUiIntent.ConfirmTimeSheet -> confirmTimeSheet()
                HomeUiIntent.DismissTimePicker -> updateState { copy(timeSheet = null) }
                HomeUiIntent.CreateDraft -> prepareDraftConsent()
                HomeUiIntent.RetryDraft -> retryDraft()
                HomeUiIntent.ContinueWaiting -> draftTaskCoordinator.continueWaiting()
                HomeUiIntent.StartNewDraft -> startNewDraft()
                HomeUiIntent.ViewDraft -> viewDraft()
                HomeUiIntent.OpenDraftLoading -> navigationHelper.navigateTo(DraftLoadingPage)
                is HomeUiIntent.RefreshSourcePermissions -> refreshSourcePermissions(intent)
                HomeUiIntent.RefreshLocationConsent -> refreshLocationConsent()
                HomeUiIntent.OpenPastRecords -> navigationHelper.navigateTo(PastRecordsPage)
                is HomeUiIntent.OpenSourceDetail -> openSourceDetail(intent.kind)
                // 건강 상세도 개발 도구다. 화면에서 숨기는 것과 별개로 호출 경계에서 막는다.
                HomeUiIntent.OpenHealthDetail ->
                    if (state.value.isCollectionLabAccessible) {
                        navigationHelper.navigateTo(DraftConsentDetailPage(DraftConsentTypeGroup.HEALTH.name))
                    } else {
                        Unit
                    }
            }
        }

        private fun observeSummary() =
            safeLaunch {
                observeSourceItemsUseCase().collect { items ->
                    sourceItems = items
                    updateState { withSourceSummary(items, photoCandidates) }
                    // 복귀 시점의 동의 판정만으로는 늦다 — 그때는 수집이 아직 안 실려 머문 곳이
                    // 없고, 실려 들어온 뒤에는 다시 물을 계기가 없어 주소가 영영 안 채워진다.
                    if (state.value.isLocationConsentGranted) resolveStayPlaceAddress()
                }
            }

        private fun observeDraftTask() =
            safeLaunch {
                draftTaskCoordinator.state.collect { trackingState ->
                    val previousWindow = state.value.recordDateWindow(zone)
                    val previousDate = state.value.selectedDate
                    val wasInProgress = state.value.draftStatus.isDateLocked
                    updateState {
                        if (dateSource == DateSource.USER) {
                            withDraftTrackingForSelectedDate(trackingState)
                        } else {
                            withDraftTracking(trackingState)
                        }
                    }
                    if (state.value.recordDateWindow(zone) != previousWindow) {
                        onRecordWindowChanged()
                    }
                    // 작업 날짜로 맞춰졌으면 이전 날짜의 판정을 버린다.
                    val isDateAligned = state.value.selectedDate != previousDate
                    if (isDateAligned) {
                        // 결과를 기다리는 날이라 기본 날짜가 바뀌어도 옮기지 않는다.
                        dateSource = DateSource.TASK
                        updateState { copy(selectedRecord = cachedRecordState(selectedDate)) }
                    }
                    // 생성 중에는 단건 조회를 건너뛰므로 추적이 끝나면(완료·실패·만료) 다시 봐야 한다. 앱을 다시 켜면
                    // 복원한 작업이 생성 중으로 시작했다가 서버에서 이미 만료돼 `Unavailable` 로 끝나는데, 그때도
                    // 서버에는 내용 있는 초안이 남아 있다.
                    val hasLeftProgress = wasInProgress && !state.value.draftStatus.isDateLocked
                    if (isDateAligned || hasLeftProgress || trackingState is DraftTaskTrackingState.Success) {
                        refreshSelectedRecord()
                    }
                }
            }

        /**
         * 최종 생성 전에 미리 수집을 시작한다. 결과를 기다리지 않는다.
         *
         * 조율자가 유형별 최신성으로 중복을 막으므로 여러 진입점에서 불려도 작업은 하나다.
         * 저장 결과는 [ObserveSourceItemsUseCase] 흐름으로 홈 요약에 반영된다.
         */
        private fun startAutoCollectionAhead() {
            safeLaunch(onError = { }) { autoCollectionCoordinator.refresh() }
        }

        /**
         * 초안 만들기의 시작. 사진 선택 시트를 연다.
         *
         * 기본 날짜(오늘)를 그대로 쓰면 날짜 확정을 거치지 않으므로 여기서 선행 수집을 건다 —
         * 사진을 고르는 동안 수집이 돌아, 확인 화면에서 기다리는 시간이 짧아진다.
         */
        private fun startPhotoSelection() {
            // 완성된 날도 연다. 시트가 읽기 전용으로 그려지고 선택 변경은 아래 토글들이 막는다.
            if (state.value.isSourceViewLocked) return
            startAutoCollectionAhead()
            sendEffect(HomeUiSideEffect.RequestPhotoAccess())
        }

        private fun resolvePhotoAccess(
            granted: Boolean,
            limited: Boolean,
        ) {
            photoAccessGranted = granted
            if (!granted) {
                // 거부됐다고 시트를 안 열면 초안 만들기를 눌렀는데 아무 일도 일어나지 않는다.
                // 열어서 왜 비었는지 알리고 설정으로 나가거나 사진 없이 이어 가게 둔다.
                updateState {
                    copy(
                        isPhotoSheetVisible = true,
                        isPhotoAccessDenied = true,
                        isPhotoLoading = false,
                        pendingPhotoIds = emptySet(),
                    )
                }
                return
            }
            updateState {
                copy(
                    isPhotoSheetVisible = true,
                    isPhotoAccessDenied = false,
                    pendingPhotoIds = selectedPhotoIds,
                    isPhotoLoading = true,
                    isPhotoAccessLimited = limited,
                )
            }
            loadPhotoCandidates(force = true)
        }

        private fun refreshPhotos(
            hasAccess: Boolean,
            limited: Boolean,
        ) {
            photoAccessGranted = hasAccess
            if (!hasAccess) {
                clearPhotoCandidates()
                return
            }
            // 거부 안내의 `설정 열기` 로 나갔다 허용하고 돌아오는 경로다. 복귀는
            // `ResolvePhotoAccess` 가 아니라 이 갱신으로 들어오므로, 여기서 풀지 않으면 사진을
            // 불러오고도 시트가 계속 거부 안내를 띄운다.
            updateState { copy(isPhotoAccessLimited = limited, isPhotoAccessDenied = false) }
            loadPhotoCandidates(force = true)
        }

        private fun togglePhoto(mediaStoreId: Long) {
            val current = state.value
            if (current.isInputLocked) return
            if (current.availablePhotos.none { it.mediaStoreId == mediaStoreId }) return
            if (mediaStoreId !in current.pendingPhotoIds && current.pendingPhotoIds.size >= MAX_PHOTO_SELECTION) {
                showPhotoLimitMessage()
                return
            }
            updateState {
                copy(
                    pendingPhotoIds =
                        if (mediaStoreId in pendingPhotoIds) {
                            pendingPhotoIds - mediaStoreId
                        } else {
                            pendingPhotoIds + mediaStoreId
                        },
                )
            }
        }

        private fun toggleAllPhotos() {
            val current = state.value
            if (current.isInputLocked) return
            val selectableIds =
                current.availablePhotos
                    .take(MAX_PHOTO_SELECTION)
                    .mapTo(linkedSetOf(), HomePhotoItem::mediaStoreId)
            val shouldClear =
                current.pendingPhotoIds.size == selectableIds.size &&
                    current.pendingPhotoIds.containsAll(selectableIds)
            updateState {
                copy(pendingPhotoIds = if (shouldClear) emptySet() else selectableIds)
            }
            if (!shouldClear && current.availablePhotos.size > MAX_PHOTO_SELECTION) showPhotoLimitMessage()
        }

        private fun togglePhotoDate(date: LocalDate) {
            val current = state.value
            if (current.isInputLocked) return
            val datePhotoIds =
                current.availablePhotos
                    .filter { it.capturedAt.atZone(zone).toLocalDate() == date }
                    .map(HomePhotoItem::mediaStoreId)
            if (datePhotoIds.isEmpty()) return
            val isDateSelected = current.pendingPhotoIds.containsAll(datePhotoIds)
            if (isDateSelected) {
                updateState { copy(pendingPhotoIds = pendingPhotoIds - datePhotoIds.toSet()) }
                return
            }

            val availableSlots = MAX_PHOTO_SELECTION - current.pendingPhotoIds.size
            val idsToAdd = datePhotoIds.filterNot(current.pendingPhotoIds::contains).take(availableSlots)
            updateState { copy(pendingPhotoIds = pendingPhotoIds + idsToAdd) }
            if (idsToAdd.size < datePhotoIds.count { it !in current.pendingPhotoIds }) showPhotoLimitMessage()
        }

        /** 고른 사진을 홈에 돌려주고 닫는다. */
        private fun confirmPhotoSelection() = closePhotoSheet { pendingPhotoIds }

        /** 고르지 않고 닫는다. 이전에 고른 것도 비운다. */
        private fun continueWithoutPhotos() = closePhotoSheet { emptySet() }

        /**
         * 사진 시트를 닫는다. **생성으로 넘어가지 않는다.**
         *
         * 종전에는 시트가 만들기 흐름의 첫 단계라 확정하면 곧장 다음 화면으로 갔다. 이제 사진은
         * 홈 카드에서 고르는 것이라, 확정은 고른 결과를 홈에 돌려주고 닫는 일까지다 — 만들기는
         * 사용자가 CTA 를 눌러 시작한다.
         */
        private fun closePhotoSheet(selected: HomeUiState.() -> Set<Long>) {
            if (state.value.isInputLocked) return
            preparedPhotoCache = null
            updateState {
                copy(
                    selectedPhotoIds = selected(),
                    pendingPhotoIds = emptySet(),
                    isPhotoSheetVisible = false,
                    isPhotoAccessDenied = false,
                    draftStatus = DraftCreationStatus.IDLE,
                    draftRetryMode = null,
                    draftMessage = null,
                ).withSourceSummary(sourceItems, photoCandidates)
            }
        }

        /**
         * 날짜 피커를 연다.
         *
         * 받아 둔 달을 비워 다시 조회하게 한다 — 이 화면에서 초안을 만들어 저장하고 돌아오면
         * 그 날짜가 저장됨으로 바뀌는데, 한 번 받은 값을 계속 쓰면 초안 도트로 남는다.
         * 표시하던 날짜는 지우지 않는다(다시 받는 사이 비었다 차면 격자가 깜빡인다).
         */
        private fun showDatePicker() {
            loadedRecordMonths.clear()
            updateState { copy(isDatePickerVisible = true) }
        }

        /**
         * 피커가 보여 주는 달의 기록 상태를 받는다.
         *
         * 실패는 조용히 넘긴다 — 못 받으면 그 달은 도트 없이 남는다. 여기서 오류를 띄우면 날짜를
         * 고르려던 흐름만 끊긴다.
         */
        private fun loadMonthlyRecords(month: YearMonth) {
            // 실행 전에 표시한다. 같은 달 요청이 연달아 오면 앞의 조회가 시작되기 전이라도 한 번만 부른다.
            if (!loadedRecordMonths.add(month)) return
            safeLaunch(onError = { loadedRecordMonths.remove(month) }) { fetchMonthlyRecords(month) }
        }

        /**
         * 한 달의 기록을 받아 초안·저장 날짜로 나눠 담는다.
         *
         * 그 달의 이전 결과를 걷어내고 다시 채운다 — 다른 화면에서 저장하거나 지웠을 수 있다. 상태를
         * 모르는 기록(필드 누락·미지 literal)은 어느 쪽에도 넣지 않는다 — 모르는 값을 초안으로 읽으면
         * 열어 볼 것이 없는 날을 열게 한다.
         */
        private suspend fun fetchMonthlyRecords(month: YearMonth) {
            loadedRecordMonths.add(month)
            val records =
                try {
                    getMonthlyDailyRecordsUseCase(month).getOrThrow()
                } catch (error: Throwable) {
                    // 못 받은 달을 받은 것으로 남기면 피커가 그 달을 다시 부르지 않는다.
                    loadedRecordMonths.remove(month)
                    throw error
                }
            val saved = records.datesWith(DailyRecordStatus.SAVED)
            val drafts = records.datesWith(DailyRecordStatus.DRAFT)
            updateState {
                copy(
                    savedRecordDates = savedRecordDates.filterNotTo(mutableSetOf()) { YearMonth.from(it) == month } + saved,
                    draftRecordDates = draftRecordDates.filterNotTo(mutableSetOf()) { YearMonth.from(it) == month } + drafts,
                )
            }
        }

        private fun List<MonthlyDailyRecord>.datesWith(status: DailyRecordStatus): Set<LocalDate> =
            filter { it.status == status }.mapTo(mutableSetOf(), MonthlyDailyRecord::recordDate)

        /**
         * 고른 날짜의 서버 기록을 다시 판정한다. CTA `타임라인 확인하기` 의 근거다.
         *
         * 판정은 **고른 날짜의 단건 조회 하나**로 한다. 응답에 저장 상태와 이벤트가 함께 오므로 월별 조회를
         * 기다리지 않는다 — 기다리면 왕복이 둘이라 버튼이 한 박자 늦게 바뀐다. 이벤트까지 보는 이유는
         * 서버가 생성 요청을 받으면 AI 로 보내기 전에 기록부터 만들고, 실패한 작업이 남긴 빈 기록도 지우지
         * 않기 때문이다. 월별 조회는 달력 도트를 위해 따로 새로 받는다.
         *
         * 판정이 끝나기 전에 날짜가 바뀌면 결과를 버린다. 실패하면 받아 둔 판정을 그대로 둔다.
         */
        private fun refreshSelectedRecord() {
            val date = state.value.selectedDate
            safeLaunch(onError = {}) { fetchMonthlyRecords(YearMonth.from(date)) }
            recordStateJob?.cancel()
            // 생성 중인 날짜는 CTA 가 제작중으로 먼저 잡는다. 아직 이벤트가 없을 기록을 묻지 않는다.
            if (state.value.draftStatus.isDateLocked) return
            recordStateJob = safeLaunch(onError = {}) { applySelectedRecord(date, recordStateOf(date)) }
        }

        private suspend fun recordStateOf(date: LocalDate): HomeRecordState =
            when (val outcome = getDailyRecordUseCase(date).getOrThrow()) {
                is DailyRecordReadOutcome.Record -> outcome.value.toHomeRecordState()
                DailyRecordReadOutcome.Unavailable -> HomeRecordState.NONE
            }

        /** 상태를 모르는 기록은 저장으로 확인되지 않았으므로 초안으로 본다([DailyTimeline] 계약). */
        private fun DailyTimeline.toHomeRecordState(): HomeRecordState =
            when {
                status == DailyRecordStatus.SAVED -> HomeRecordState.SAVED
                events.isEmpty() -> HomeRecordState.EMPTY_DRAFT
                else -> HomeRecordState.DRAFT
            }

        private fun applySelectedRecord(
            date: LocalDate,
            record: HomeRecordState,
        ) {
            updateState { if (selectedDate == date) copy(selectedRecord = record) else this }
        }

        /**
         * 받아 둔 달로 곧바로 판정한다. 날짜를 옮기는 순간에 쓰고, 단건 조회가 뒤이어 바로잡는다.
         *
         * 초안은 이벤트를 확인하기 전에도 **먼저 연다.** 확인까지 기다리면 달력에서 초안 날짜를 골랐는데
         * `만들기` 가 한 박자 머물다 바뀐다(실기기 확인). 빈 초안은 생성이 실패해 남은 것이라 드물고,
         * 그때만 `확인하기` 가 잠깐 보였다가 `만들기` 로 돌아간다.
         */
        private fun HomeUiState.cachedRecordState(date: LocalDate): HomeRecordState =
            when (date) {
                in savedRecordDates -> HomeRecordState.SAVED
                in draftRecordDates -> HomeRecordState.DRAFT
                else -> HomeRecordState.NONE
            }

        /**
         * 카드 건수를 다시 센다.
         *
         * 전송 예정 수(N)는 후보 수만으로 알 수 없다 — 타입별 상한과 정제를 거친 뒤의 값이라
         * 선택 정책을 돌려야 나온다. **측정 리포트는 발행하지 않는다**(제출 시점만 발행).
         *
         * 제외 집합은 세션 스토어가 소유한다 — 홈이 본문 건수를 세고 상세가 토글하므로 한쪽이
         * 가지면 다른 쪽이 못 본다.
         */
        private fun HomeUiState.withSourceSummary(
            items: List<SourceItem>,
            photoCandidates: List<PhotoCandidate>,
        ): HomeUiState {
            val window = recordDateWindow(zone)
            val selection =
                window?.let { prepareTimelineDraftSelectionUseCase(it, items, reportsMeasurement = false).getOrNull() }
            // 카드가 CTA 전에도 상세를 열 수 있도록 스냅샷을 상시로 유지한다. 같은 내용이면
            // 스토어가 갱신 번호를 올리지 않으므로 여기서 몇 번 불러도 상세가 흔들리지 않는다.
            if (window != null && selection != null) {
                draftConsentSessionStore.updateSelection(selectedDate, zone, window, selection)
            } else {
                draftConsentSessionStore.clearSelection()
            }
            return refreshSourceSummary(
                items = items,
                photoCandidates = photoCandidates,
                zone = zone,
                selection = selection,
                // 카드의 `M개 중 N개` 는 실제 제출과 같은 규칙으로 세야 한다. 위치를 통째로
                // 끈 경우가 제외 집합에 들어 있지 않아, 집합만 보면 0건을 보내면서 카드는
                // `1개 중 1개` 라고 적는다.
                excludedRawIds = selection?.let(draftConsentSessionStore::excludedRawIdsFor).orEmpty(),
            )
        }

        private fun selectDate(date: LocalDate) {
            if (state.value.isDateLocked) return
            // 피커가 막는 날짜를 경계에서 한 번 더 막는다 — 보존 기간 밖은 기기의 재료가 이미 지워졌다.
            // 저장된 날짜는 막지 않는다. 고르면 CTA 가 `타임라인 확인하기` 로 그 기록을 연다.
            if (!isSelectableRecordDate(date, LocalDate.now(clock.withZone(zone)), state.value.retentionDays)) return
            // 피커로 고른 날짜는 사용자가 범위를 지정한 것이라 기본 날짜가 바뀌어도 옮기지 않는다.
            // 지금 날짜를 그대로 다시 골라도 마찬가지다.
            dateSource = DateSource.USER
            // 날짜를 확정한 시점부터 미리 긁어 둬야 최종 생성에서 기다리는 시간이 짧다.
            startAutoCollectionAhead()
            updateState { copy(isDatePickerVisible = false) }
            moveToDate(date)
        }

        private fun moveToDate(date: LocalDate) {
            updateState {
                if (date == selectedDate) return@updateState this
                // 시간 범위는 그대로 둔다. 06:00~익일 06:00 으로 맞춰 둔 사람이 날짜만 옮길
                // 때마다 자정으로 되돌아가면, 고쳐 둔 것이 날짜를 고른 대가로 사라진다.
                val next =
                    copy(
                        selectedDate = date,
                        draftStatus = DraftCreationStatus.IDLE,
                        draftRetryMode = null,
                        draftMessage = null,
                        selectedRecord = cachedRecordState(date),
                    )
                next
                    .withSourceSummary(sourceItems, photoCandidates)
                    .withDraftTrackingForSelectedDate(draftTaskCoordinator.state.value)
            }
            onRecordWindowChanged()
            refreshSelectedRecord()
        }

        /**
         * 달력상 오늘과 기본 날짜를 다시 계산한다.
         *
         * 이 ViewModel 은 Activity 범위라 생성 때 정한 날짜가 앱이 살아 있는 내내 남는다. 그러면 다음 날
         * 돌아와도 어제 창으로 카드를 세고, 모르는 채 어제 초안을 만든다. 그래서 복귀와 날짜가 바뀌는
         * 시각마다 부르고, 날짜를 고르지 않은 사람만 기본 날짜로 옮긴다.
         *
         * 옮기지 않는 경우:
         * - 피커로 고른 날짜, 진행 중인 작업에 맞춘 날짜 — 사용자가 지정했거나 결과를 기다리는 날이다.
         * - 제출·생성 중 — 확정한 스냅샷의 날짜와 화면이 어긋난다.
         * - 날짜에 묶인 시트(사진·시간·날짜 피커)가 열려 있을 때 — 고르는 도중에 재료가 바뀐다. 다음 복귀에서 옮긴다.
         */
        private fun refreshToday() {
            val currentZone = ZoneId.systemDefault()
            val isZoneChanged = currentZone != zone
            zone = currentZone
            val now = LocalDateTime.now(clock.withZone(zone))
            updateState { if (today == now.toLocalDate()) this else copy(today = now.toLocalDate()) }

            val current = state.value
            val defaultDate = HomeDefaultDate.of(now)
            val canMove =
                dateSource == DateSource.DEFAULT &&
                    !current.isDateLocked &&
                    !current.isPhotoSheetVisible &&
                    current.timeSheet == null &&
                    !current.isDatePickerVisible
            when {
                canMove && current.selectedDate != defaultDate -> moveToDate(defaultDate)
                // 같은 날짜라도 시간대가 바뀌면 기록 창의 시각이 달라진다.
                isZoneChanged -> onRecordWindowChanged()
            }
        }

        private fun showTimeSheet(field: HomeTimeField) {
            if (state.value.isInputLocked) return
            updateState {
                copy(
                    timeSheet =
                        HomeTimeSheetState(
                            recordDate = selectedDate,
                            startTime = startTime,
                            endDay = endDay,
                            endTime = endTime,
                            expandedField = field,
                        ),
                )
            }
        }

        /**
         * 종료 줄의 날짜 롤러가 당일·익일 선택을 겸하므로 고른 날짜를 그대로 종료 일시로 받는다.
         *
         * 시작을 늦추면 최소 길이 때문에 종료 하한이 밀리므로, 이미 고른 종료가 범위 밖이 되면
         * 경계로 붙여 둔다 — 그대로 두면 롤러에 없는 값이 남아 확인만 막힌다.
         */
        private fun changeSheetTime(
            field: HomeTimeField,
            date: LocalDate,
            time: LocalTime,
        ) {
            updateState {
                val sheet = timeSheet ?: return@updateState this
                val next =
                    when (field) {
                        HomeTimeField.START -> sheet.withStartTime(time)
                        HomeTimeField.END -> sheet.withEnd(date.atTime(time))
                    }
                copy(timeSheet = next)
            }
        }

        private fun confirmTimeSheet() {
            val sheet = state.value.timeSheet ?: return
            if (state.value.isInputLocked || !sheet.isConfirmEnabled) return
            updateState {
                val next =
                    copy(
                        startTime = sheet.startTime,
                        endDay = sheet.endDay,
                        endTime = sheet.endTime,
                        timeSheet = null,
                        draftStatus = DraftCreationStatus.IDLE,
                        draftRetryMode = null,
                        draftMessage = null,
                    )
                next.withSourceSummary(sourceItems, photoCandidates)
            }
            onRecordWindowChanged()
        }

        /**
         * 전송 스냅샷을 확정하고 동의 화면으로 이동한다.
         *
         * 사진 업로드·초안 생성 API 는 여기서 호출하지 않는다 — 동의 화면에서 필수 동의를 모두
         * 완료하고 CTA 를 선택한 경우에만 시작된다. 사진 상한 초과·접근 불가 사진 같은 입력
         * 오류는 동의 화면으로 이동하지 않고 홈에서 바로 수정하도록 안내한다.
         */
        private fun prepareDraftConsent() {
            if (state.value.isInputLocked) return
            if (consentPreparationJob?.isActive == true) return
            // 제출이 이미 시작됐으면 중복 진입하지 않는다. **상시 스냅샷이 아니라 제출용
            // 스냅샷을 본다** — 상시 쪽은 늘 들어 있으므로 그것을 보면 CTA 가 영구히 막힌다.
            if (draftConsentSessionStore.preparation.value != null) return
            val current = state.value
            val window = current.recordDateWindow(zone)
            if (window == null) {
                sendEffect(HomeUiSideEffect.ShowSnackbar("종료 시각은 시작 시각보다 뒤로 설정해주세요."))
                return
            }
            // `데이터 0건` 판정은 자동 수집과 최신 조회 뒤로 미룬다. 여기서 끊으면 아직 한 번도
            // 수집하지 않은 사용자가 수집 기회를 갖기 전에 생성이 막힌다.
            val shouldDiscardPreviousTask = current.draftRetryMode == DraftRetryMode.NEW_DRAFT
            consentPreparationJob =
                safeLaunch(
                    onError = ::handleDraftCreationFailure,
                ) {
                    awaitAutoCollection()
                    val selectedPhotoItems = prepareSelectedPhotos(current) ?: return@safeLaunch
                    // 화면이 들고 있던 관찰 결과 대신 저장소를 다시 읽어 수집분이 반영된 값을 쓴다.
                    val collected = getSourceItemsInWindowUseCase(window).filter { it.payload !is PhotoPayload }
                    if (collected.isEmpty() && selectedPhotoItems.isEmpty()) {
                        sendEffect(HomeUiSideEffect.ShowSnackbar("선택한 범위에 모인 데이터가 없어요."))
                        return@safeLaunch
                    }
                    val selection =
                        prepareTimelineDraftSelectionUseCase(
                            window,
                            collected + selectedPhotoItems,
                        ).getOrElse {
                            handleDraftCreationFailure(it)
                            return@safeLaunch
                        }
                    draftConsentSessionStore.prepare(
                        recordDate = current.selectedDate,
                        zone = zone,
                        window = window,
                        selection = selection,
                        discardActiveTask = shouldDiscardPreviousTask,
                    )
                    confirmAndSubmit()
                }
        }

        /**
         * 확인 다이얼로그를 띄우고, 만들기를 고르면 그대로 제출한다.
         *
         * 화면을 한 장 더 두지 않는다 — 보낼 데이터를 보여 주고 유형 상세로 들어가는 일은 이미
         * 홈 카드가 하므로, 남는 것은 "이 건수로 만들겠습니까" 라는 마지막 확인뿐이다.
         */
        private suspend fun confirmAndSubmit() {
            val preparation = draftConsentSessionStore.preparation.value ?: return
            // 제출 목록은 **소유자인 스토어**를 읽어 만든다. 위치가 꺼져 있으면 이 시점 스냅샷의
            // 위치 항목 전체를 함께 뺀다 — 스위치를 끈 뒤 수집된 것까지 덮어야 어긋나지 않는다.
            val excluded =
                draftConsentSessionStore.excludedRawIds.value +
                    if (draftConsentSessionStore.isLocationSendEnabled.value) {
                        emptySet()
                    } else {
                        preparation.selection.locationRawIds()
                    }
            val submission = preparation.selection.excluding(excluded)
            if (submission.items.isEmpty()) {
                draftConsentSessionStore.clearPreparation()
                sendEffect(HomeUiSideEffect.ShowSnackbar("보낼 데이터를 모두 제외했어요."))
                return
            }
            val result =
                messageHelper.showTwoButtonDialog(
                    DialogRequest.TwoButton(
                        title = "타임라인을 만들까요?",
                        body = submission.confirmDialogBody(),
                        primaryLabel = "만들기",
                        secondaryLabel = "취소",
                    ),
                )
            // 취소·바깥 탭·뒤로가기는 모두 만들지 않는다. **제출용 스냅샷만 버리고** 홈 선택은
            // 남긴다 — 취소 한 번에 빼려던 일정·알림이 되살아나면 안 된다.
            if (result != DialogResult.Primary) {
                draftConsentSessionStore.clearPreparation()
                return
            }
            submitDraft(preparation, submission)
        }

        private suspend fun submitDraft(
            preparation: DraftConsentPreparation,
            submission: DraftSourceItemSelection,
        ) {
            val photoCount = submission.items.count { it.itemType == ItemType.PHOTO }
            Logger.i(
                LogDomain.USER_ACTION,
                "초안 생성 요청: 항목 ${submission.items.size}건(사진 ${photoCount}건), 기존 작업 폐기=${preparation.discardActiveTask}",
            )
            if (preparation.discardActiveTask) draftTaskCoordinator.discard()
            // 여기부터 응답까지 입력을 잠근다. `draftStatus` 는 서버가 작업을 받아야 움직이므로
            // 이 구간에는 아직 IDLE 이고, 그동안 날짜를 바꾸면 요청은 이전 스냅샷으로 진행된다.
            updateState { copy(isSubmitting = true) }
            createTimelineDraftUseCase(
                preparation.recordDate,
                preparation.zone,
                preparation.window,
                submission,
            ).onSuccess { handle ->
                draftTaskCoordinator.start(handle.taskId, preparation.recordDate)
                // 준비 상태는 여기서 폐기되므로, 로딩 화면이 쓸 것만 먼저 옮겨 담는다.
                loadingSessionStore.start(submission.toLoadingSession(handle.taskId, preparation.recordDate))
                draftConsentSessionStore.clearAfterSubmission()
                updateState { copy(isSubmitting = false) }
                navigationHelper.navigateTo(DraftLoadingPage)
            }.onFailure(::handleDraftSubmitFailure)
        }

        /**
         * 확인 화면이 받던 제출 실패를 홈이 받는다.
         *
         * 어느 경우든 **제출용 스냅샷만 버리고** 홈 선택 상태는 남긴다. 복귀가 홈이라 사진을 다시
         * 고를 필요도 없다.
         */
        private fun handleDraftSubmitFailure(error: Throwable) {
            draftConsentSessionStore.clearPreparation()
            // 실패하면 잠금을 푼다. 남겨 두면 다시 시도할 수도, 날짜를 바꿀 수도 없다.
            updateState { copy(isSubmitting = false) }
            when {
                // 서버가 단계 동의를 다시 요구한다 — 약관이 개정됐거나 구버전으로 온보딩을 마친
                // 계정이다. 받는 자리로 보내되 **자동으로 재개하지 않는다.**
                error is ApiException && error.errorCode == TERMS_AGREEMENT_REQUIRED -> {
                    navigationHelper.navigateTo(StageTermsPage(DRAFT_CONSENT_STAGES.map(TermStage::name)))
                }

                // 이미 그 날짜 기록에 들어간 항목만 다시 보낸 경우다. 실패로만 보이면 이유를 알 수 없다.
                error is ApiException && error.errorCode == APPEND_NO_NEW_ITEMS -> {
                    sendEffect(HomeUiSideEffect.ShowSnackbar("이미 기록에 들어간 것뿐이라 새로 더할 게 없어요."))
                }

                // 스냅샷 확정 뒤 사진이 삭제되거나 권한이 바뀐 경우 — 같은 사진으로는 복구되지
                // 않으므로 고르는 자리를 다시 연다.
                error is DraftPhotoAccessException -> {
                    handleUnavailablePhotos(emptySet())
                    startPhotoSelection()
                }

                else -> handleDraftCreationFailure(error)
            }
        }

        /**
         * 최종 생성 직전 자동 수집의 최신성을 확인한다.
         *
         * 날짜 확정·시트 진입에서 미리 시작했으므로 정상 경로에서는 대부분 즉시 통과한다.
         * 그래도 첫 실행처럼 아직 한 번도 못 긁은 경우가 있어 상한을 두고 기다린다.
         *
         * 상한을 넘기거나 일부 유형이 실패하면 **기존 저장 데이터로 계속한다.** 수집 때문에 생성
         * 자체가 막히면 안 된다. 다만 최신이 아닐 수 있다는 것은 알려 준다.
         */
        private suspend fun awaitAutoCollection() {
            val result =
                globalLoadingHelper.withLoading(AUTO_COLLECTION_LOADING_KEY) {
                    autoCollectionCoordinator.refresh(AUTO_COLLECTION_TIMEOUT_MILLIS)
                }
            if (result.isIncomplete) {
                sendEffect(HomeUiSideEffect.ShowSnackbar("일부 데이터를 최신 상태로 불러오지 못했어요."))
            }
        }

        private suspend fun prepareSelectedPhotos(current: HomeUiState): List<SourceItem>? {
            val selectedIds =
                current.availablePhotos
                    .asSequence()
                    .filter { it.mediaStoreId in current.selectedPhotoIds }
                    .map(HomePhotoItem::mediaStoreId)
                    .toList()
            if (selectedIds.isEmpty()) return emptyList()

            preparedPhotoCache
                ?.takeIf { it.ids == selectedIds.toSet() }
                ?.let { return it.items }

            val prepared = prepareSelectedPhotosUseCase(selectedIds)
            if (prepared.unavailableIds.isNotEmpty()) {
                handleUnavailablePhotos(prepared.unavailableIds)
                return null
            }
            preparedPhotoCache =
                PreparedPhotoCache(
                    ids = selectedIds.toSet(),
                    items = prepared.items,
                )
            return prepared.items
        }

        private fun handleUnavailablePhotos(unavailableIds: Set<Long>) {
            photoCandidates = photoCandidates.filterNot { it.id in unavailableIds }
            preparedPhotoCache = null
            val message = "선택한 사진 ${unavailableIds.size}장에 접근할 수 없어요. 사진을 다시 선택해주세요."
            updateState {
                val remainingSelectedIds = selectedPhotoIds - unavailableIds
                copy(
                    selectedPhotoIds = remainingSelectedIds,
                    pendingPhotoIds = remainingSelectedIds,
                    draftStatus = DraftCreationStatus.FAILED,
                    draftRetryMode = DraftRetryMode.NEW_DRAFT,
                    draftMessage = message,
                    isPhotoSheetVisible = true,
                ).withSourceSummary(sourceItems, photoCandidates)
            }
            sendEffect(HomeUiSideEffect.ShowSnackbar(message))
        }

        private fun handleDraftCreationFailure(error: Throwable) {
            // 준비·제출 어느 쪽에서 튀어나왔든 잠금을 푼다. 남겨 두면 다시 시도할 길이 없다.
            updateState { copy(isSubmitting = false) }
            if (error is DraftPhotoLimitExceededException) {
                val message = "${error.message}\n사진 선택에서 개수를 줄여주세요."
                updateState {
                    copy(
                        draftStatus = DraftCreationStatus.FAILED,
                        draftRetryMode = DraftRetryMode.NEW_DRAFT,
                        draftMessage = message,
                        isPhotoSheetVisible = true,
                        pendingPhotoIds = selectedPhotoIds,
                    )
                }
                sendEffect(HomeUiSideEffect.ShowSnackbar(message))
                return
            }

            updateState {
                copy(
                    draftStatus = DraftCreationStatus.FAILED,
                    draftRetryMode = DraftRetryMode.NEW_DRAFT,
                    draftMessage = "초안 생성 요청을 보내지 못했어요.",
                )
            }
            handleFailure(error)
        }

        private fun loadPhotoCandidates(force: Boolean) {
            if (!photoAccessGranted) return
            val window = state.value.recordDateWindow(zone) ?: return
            if (!force && window == requestedPhotoWindow) return
            if (!force && window == lastLoadedPhotoWindow) {
                photoCandidatesJob?.cancel()
                photoCandidatesJob = null
                requestedPhotoWindow = null
                updateState {
                    withSourceSummary(sourceItems, photoCandidates)
                        .copy(isPhotoLoading = false, hasLoadedPhotoCandidates = true)
                }
                return
            }

            photoCandidatesJob?.cancel()
            requestedPhotoWindow = window
            photoCandidatesJob =
                safeLaunch(
                    onError = { error ->
                        if (error !is CancellationException && requestedPhotoWindow == window) {
                            requestedPhotoWindow = null
                            updateState { copy(isPhotoLoading = false) }
                            handleFailure(error)
                        }
                    },
                ) {
                    updateState { copy(isPhotoLoading = true) }
                    val candidates = getPhotosInWindowUseCase(window)
                    photoCandidates = candidates
                    lastLoadedPhotoWindow = window
                    requestedPhotoWindow = null
                    preparedPhotoCache =
                        preparedPhotoCache?.takeIf { cache ->
                            val availableIds = candidates.mapTo(mutableSetOf(), PhotoCandidate::id)
                            cache.ids.all(availableIds::contains)
                        }
                    updateState {
                        withSourceSummary(sourceItems, candidates)
                            .copy(isPhotoLoading = false, hasLoadedPhotoCandidates = true)
                    }
                }
        }

        private fun onRecordWindowChanged() {
            preparedPhotoCache = null
            // 새 창의 후보를 받기 전이다. 받아 둔 게 없다고 "사진이 없다" 고 말하지 않게 한다.
            updateState { withSourceSummary(sourceItems, photoCandidates).copy(hasLoadedPhotoCandidates = false) }
            loadPhotoCandidates(force = false)
        }

        private fun clearPhotoCandidates() {
            photoCandidatesJob?.cancel()
            photoCandidatesJob = null
            photoCandidates = emptyList()
            lastLoadedPhotoWindow = null
            requestedPhotoWindow = null
            preparedPhotoCache = null
            updateState {
                copy(
                    isPhotoLoading = false,
                    hasLoadedPhotoCandidates = false,
                    isPhotoSheetVisible = false,
                    isPhotoAccessLimited = false,
                ).withSourceSummary(sourceItems, emptyList())
            }
        }

        private fun showPhotoLimitMessage() {
            sendEffect(HomeUiSideEffect.ShowSnackbar("사진은 최대 ${MAX_PHOTO_SELECTION}장까지 선택할 수 있어요."))
        }

        private fun retryDraft() {
            when (state.value.draftRetryMode) {
                DraftRetryMode.POLLING -> draftTaskCoordinator.retry()
                DraftRetryMode.NEW_DRAFT -> prepareDraftConsent()
                null -> Unit
            }
        }

        private fun startNewDraft() =
            safeLaunch {
                draftTaskCoordinator.discard()
            }

        /**
         * 고른 날짜의 타임라인을 연다.
         *
         * 추적 중인 작업의 완료에 기대지 않는다 — 앱을 다시 켜면 완료 표시가 없고, 서버 기록만으로도
         * `타임라인 확인하기` 가 뜬다. 완료 표시가 있을 때도 고른 날짜가 곧 작업 날짜다(홈이 맞춘다).
         */
        private fun viewDraft() {
            val current = state.value
            if (current.timelineButtonStatus != DraftCreationStatus.SUCCESS) return
            navigationHelper.navigateTo(TimelinePage(current.selectedDate))
        }

        /**
         * 원천 카드에서 상세로 들어간다.
         *
         * 사진만 시트로 간다 — 고른 사진이 정본이라 목록에서 빼는 것이 아니라 다시 고르는 일이다.
         * 나머지는 유형 상세로 가고, 상세는 홈이 상시로 유지하는 스냅샷을 읽는다.
         *
         * 완성된 날도 연다. 그날 무엇이 모였는지는 볼 수 있어야 한다 — 상세와 시트가 읽기 전용으로 뜬다.
         */
        private fun openSourceDetail(kind: HomeSourceKind) {
            if (state.value.isSourceViewLocked) return
            if (kind == HomeSourceKind.PHOTO) {
                startPhotoSelection()
                return
            }
            navigationHelper.navigateTo(DraftConsentDetailPage(kind.detailGroup().name))
        }

        private fun HomeSourceKind.detailGroup(): DraftConsentTypeGroup =
            when (this) {
                HomeSourceKind.PHOTO -> DraftConsentTypeGroup.PHOTO
                HomeSourceKind.CALENDAR -> DraftConsentTypeGroup.CALENDAR
                HomeSourceKind.LOCATION -> DraftConsentTypeGroup.LOCATION
                HomeSourceKind.NOTIFICATION -> DraftConsentTypeGroup.NOTIFICATION
            }

        private fun refreshSourcePermissions(intent: HomeUiIntent.RefreshSourcePermissions) {
            updateState {
                copy(
                    permissions =
                        HomeSourcePermissions(
                            photo = intent.photo,
                            calendar = intent.calendar,
                            location = intent.location,
                            notification = intent.notification,
                        ),
                )
            }
        }

        /**
         * 저장된 위치정보 약관 동의를 판정하고, 허용되면 가장 오래 머문 곳의 주소를 채운다.
         *
         * 알아내기 전과 조회 실패는 모두 "허용되지 않음"이다 — `Geocoder` 는 좌표를 기기 밖으로
         * 보내므로 모르는 상태에서 부르지 않는다(#330).
         */
        private fun refreshLocationConsent() {
            // 앞선 조회가 남아 있으면 끊는다. 늦게 도착한 이전 판정이 최신 판정을 덮어쓴다.
            locationConsentJob?.cancel()
            locationConsentJob =
                safeLaunch(onError = {}) {
                    val isGranted =
                        termsCoordinator
                            .requirementOf(TermStage.TIMELINE_LOCATION)
                            .getOrNull()
                            ?.isSatisfied == true
                    updateState { copy(isLocationConsentGranted = isGranted) }
                    if (isGranted) resolveStayPlaceAddress()
                }
        }

        /**
         * 위치 카드가 `오산시 부산동` 두 층위를 쓰는데 층위가 비어 있으면 채운다.
         *
         * 한 줄 주소가 있어도 층위가 없으면 다시 묻는다 — 예전에 한 줄만 저장된 항목을 해석
         * 완료로 보면 새 체류와 기존 저장분이 서로 다른 모양으로 뜬다. 해석 결과는 저장되므로
         * 항목당 한 번이고, 같은 항목을 반복해서 묻지 않는다.
         *
         * 부르는 자리가 둘이다 — 동의를 판정한 직후와 수집이 실려 요약이 바뀔 때. 둘 중 어느
         * 쪽이 먼저인지 정해져 있지 않아서다. [attemptedStayRawIds] 가 겹치는 호출을 막는다.
         */
        private suspend fun resolveStayPlaceAddress() {
            val place = state.value.summary.stayPlace ?: return
            if (!place.needsResolution) return
            if (!attemptedStayRawIds.add(place.rawId)) return
            val resolved = resolveStayAddress(place.rawId, place.latitude, place.longitude) ?: return
            updateState {
                // 해석하는 사이 창이나 수집이 바뀌었으면 지금 화면의 장소가 아니다.
                if (summary.stayPlace?.rawId != place.rawId) {
                    this
                } else {
                    copy(
                        summary =
                            summary.copy(
                                stayPlace =
                                    summary.stayPlace.copy(
                                        city = resolved.city,
                                        district = resolved.district,
                                        line = resolved.line,
                                    ),
                            ),
                    )
                }
            }
        }

        private fun HomeUiState.withDraftTracking(trackingState: DraftTaskTrackingState): HomeUiState {
            val trackingTask = (trackingState as? DraftTaskTrackingState.WithTask)?.task
            val alignedState =
                if (trackingTask != null && trackingTask.recordDate != selectedDate) {
                    copy(selectedDate = trackingTask.recordDate)
                        .withSourceSummary(sourceItems, photoCandidates)
                } else {
                    this
                }
            return when (trackingState) {
                DraftTaskTrackingState.Idle -> alignedState.resetDraftPresentation()
                is DraftTaskTrackingState.Processing ->
                    alignedState.copy(
                        draftStatus = DraftCreationStatus.PROCESSING,
                        draftRetryMode = null,
                        draftMessage = processingMessage(trackingState.elapsedSeconds),
                    )

                is DraftTaskTrackingState.LongRunning ->
                    alignedState.copy(
                        draftStatus = DraftCreationStatus.LONG_RUNNING,
                        draftRetryMode = null,
                        draftMessage =
                            "초안 생성 시작 후 ${trackingState.elapsedSeconds / 60}분이 지났어요. " +
                                "계속 기다리거나 다시 만들 수 있어요.",
                    )

                is DraftTaskTrackingState.Success ->
                    alignedState.copy(
                        draftStatus = DraftCreationStatus.SUCCESS,
                        draftRetryMode = null,
                        draftMessage = "초안이 준비됐어요.",
                    )

                is DraftTaskTrackingState.Failed ->
                    alignedState.copy(
                        draftStatus = DraftCreationStatus.FAILED,
                        draftRetryMode = DraftRetryMode.NEW_DRAFT,
                        draftMessage = "초안을 만들지 못했어요. 다시 시도해주세요.",
                    )

                is DraftTaskTrackingState.RetryableError ->
                    alignedState.copy(
                        draftStatus = DraftCreationStatus.FAILED,
                        draftRetryMode = DraftRetryMode.POLLING,
                        draftMessage = "네트워크 상태를 확인한 뒤 상태를 다시 확인해주세요.",
                    )

                is DraftTaskTrackingState.Unavailable ->
                    alignedState.copy(
                        draftStatus = DraftCreationStatus.FAILED,
                        draftRetryMode = DraftRetryMode.NEW_DRAFT,
                        draftMessage =
                            when (trackingState.reason) {
                                DraftTaskUnavailableReason.TASK -> "초안 작업 정보를 찾을 수 없어요. 다시 만들어주세요."
                                DraftTaskUnavailableReason.RESULT -> "완료된 초안 결과를 찾을 수 없어요. 다시 만들어주세요."
                            },
                    )
            }
        }

        private fun HomeUiState.withDraftTrackingForSelectedDate(trackingState: DraftTaskTrackingState): HomeUiState {
            val trackingTask = (trackingState as? DraftTaskTrackingState.WithTask)?.task
            return if (trackingTask?.recordDate == selectedDate) {
                withDraftTracking(trackingState)
            } else {
                resetDraftPresentation()
            }
        }

        private fun HomeUiState.resetDraftPresentation(): HomeUiState =
            copy(
                draftStatus = DraftCreationStatus.IDLE,
                draftRetryMode = null,
                draftMessage = null,
            )

        private fun processingMessage(elapsedSeconds: Long?): String =
            when {
                elapsedSeconds == null -> "초안을 만들고 있어요."
                elapsedSeconds < 60L -> "초안을 만들고 있어요. ${elapsedSeconds}초 지났어요."
                else -> "초안을 만들고 있어요. ${elapsedSeconds / 60}분 지났어요."
            }

        private data class PreparedPhotoCache(
            val ids: Set<Long>,
            val items: List<SourceItem>,
        )

        /** 지금 고른 날짜가 어디서 왔는지. 기본 날짜만 [refreshToday] 가 옮긴다. */
        private enum class DateSource {
            /** 아무도 고르지 않아 [HomeDefaultDate] 를 따른다. */
            DEFAULT,

            /** 사용자가 피커로 골랐다. */
            USER,

            /** 진행 중인 초안 작업의 날짜에 맞췄다. */
            TASK,
        }

        private companion object {
            /** 서버가 단계 동의를 요구할 때 주는 코드. */
            const val TERMS_AGREEMENT_REQUIRED = -3001

            /** 이어 붙일 새 항목이 없을 때 서버가 주는 코드. */
            const val APPEND_NO_NEW_ITEMS = -1013

            /**
             * `-3001` 을 받았을 때 다시 받아야 할 후보 단계.
             *
             * 오류가 어느 단계인지 알려 주지 않으므로 온보딩이 받는 것과 **같은 집합**을 싣고,
             * 실제로 남은 것만 받는 판단은 약관 화면이 다시 조회해서 한다.
             */
            val DRAFT_CONSENT_STAGES = listOf(TermStage.TIMELINE_FIRST_CREATE, TermStage.TIMELINE_LOCATION)
            const val AUTO_COLLECTION_LOADING_KEY = "home-auto-collection"

            /**
             * 최종 생성에서 자동 수집을 기다리는 상한.
             *
             * 날짜 확정·시트 진입에서 미리 시작하므로 정상 경로에서는 거의 걸리지 않는다.
             * 상한을 넘기면 기존 저장 데이터로 생성을 이어 간다 — 수집이 느리다고 생성이 막히면 안 된다.
             */
            const val AUTO_COLLECTION_TIMEOUT_MILLIS = 10_000L
        }
    }
