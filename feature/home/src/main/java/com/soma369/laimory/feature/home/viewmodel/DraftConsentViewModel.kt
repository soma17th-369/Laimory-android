package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.coordinator.TermsAgreementCoordinator
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.DraftPhotoAccessException
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.StayPayload
import com.soma369.laimory.core.domain.model.terms.TermStage
import com.soma369.laimory.core.domain.model.timeline.LocationMapRenderGate
import com.soma369.laimory.core.domain.navigation.DraftConsentDetailPage
import com.soma369.laimory.core.domain.navigation.DraftLoadingPage
import com.soma369.laimory.core.domain.navigation.StageTermsPage
import com.soma369.laimory.core.domain.usecase.CreateTimelineDraftUseCase
import com.soma369.laimory.core.domain.usecase.ResolveMovementAddressesUseCase
import com.soma369.laimory.core.domain.usecase.ResolveStayAddressUseCase
import com.soma369.laimory.core.domain.usecase.terms.GetDisplayTermsUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.home.draft.DraftConsentPreparation
import com.soma369.laimory.feature.home.draft.DraftConsentSessionStore
import com.soma369.laimory.feature.home.draft.DraftLoadingSessionStore
import com.soma369.laimory.feature.home.draft.toLoadingSession
import com.soma369.laimory.feature.home.state.DraftConsentUiIntent
import com.soma369.laimory.feature.home.state.DraftConsentUiSideEffect
import com.soma369.laimory.feature.home.state.DraftConsentUiState
import com.soma369.laimory.feature.home.state.movementEndAddressKey
import com.soma369.laimory.feature.home.state.movementStartAddressKey
import com.soma369.laimory.feature.home.state.stayAddressKey
import com.soma369.laimory.feature.home.state.toConsentContent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 데이터 전송 확인·동의 화면의 ViewModel.
 *
 * 홈이 확정한 생성 시도 스냅샷([DraftConsentSessionStore])을 구독해 화면을 구성하고,
 * 필수 동의 완료 후에만 스냅샷 그대로 제출한다. 새 attemptId 가 들어올 때마다
 * 체크·오류 상태를 초기화하므로 재진입은 항상 새 생성 시도로 시작한다.
 */
@HiltViewModel
class DraftConsentViewModel
    @Inject
    constructor(
        private val sessionStore: DraftConsentSessionStore,
        private val loadingSessionStore: DraftLoadingSessionStore,
        private val createTimelineDraftUseCase: CreateTimelineDraftUseCase,
        private val draftTaskCoordinator: DraftTaskCoordinator,
        private val navigationHelper: NavigationHelper,
        private val termsCoordinator: TermsAgreementCoordinator,
        private val getDisplayTerms: GetDisplayTermsUseCase,
        private val resolveStayAddress: ResolveStayAddressUseCase,
        private val resolveMovementAddresses: ResolveMovementAddressesUseCase,
        private val mapRenderGate: LocationMapRenderGate,
    ) : BaseMviViewModel<DraftConsentUiState, DraftConsentUiIntent, DraftConsentUiSideEffect>(
            DraftConsentUiState(),
        ) {
        /** 지도 렌더 허용 여부. 약관 조회가 끝나기 전과 조회에 실패했을 때는 false 다. */
        private var isMapRenderAllowed = false
        private var activePreparation: DraftConsentPreparation? = null

        /**
         * 표시 시점에 해석한 주소. 키는 [stayAddressKey] 계열이다.
         *
         * 전송 스냅샷이 아니라 화면 모델에만 덧입힌다. 새 생성 시도가 오면 비운다 — 스냅샷이
         * 바뀌면 대응하는 항목도 달라진다.
         */
        private val resolvedAddresses = mutableMapOf<String, String>()

        init {
            // 조회가 끝나기 전에는 지도를 붙이지 않는다 — 그리는 순간 카메라 영역이 Google 로 나간다.
            safeLaunch(onError = {}) {
                isMapRenderAllowed = mapRenderGate.isMapRenderAllowed()
                updateState { copy(isMapRenderAllowed = isMapRenderAllowed) }
            }
            safeLaunch {
                sessionStore.preparation.collect { preparation ->
                    when {
                        // 폐기(null) 시 activity 범위 ViewModel 에 알림 본문·사진 URI 같은
                        // 민감 표시 모델과 체크 상태가 남지 않도록 즉시 초기화한다.
                        preparation == null -> {
                            activePreparation = null
                            clearResolvedAddresses()
                            updateState { initialUiState() }
                        }

                        preparation.attemptId != activePreparation?.attemptId -> {
                            activePreparation = preparation
                            clearResolvedAddresses()
                            updateState { initialUiState().copy(content = preparation.toConsentContent()) }
                            resolveMissingAddresses(preparation)
                        }
                    }
                }
            }
        }

        override suspend fun handleIntent(intent: DraftConsentUiIntent) {
            when (intent) {
                is DraftConsentUiIntent.ToggleItemInclusion -> toggleItemInclusion(intent)
                DraftConsentUiIntent.ToggleLocationInclusion -> toggleLocationInclusion()
                is DraftConsentUiIntent.OpenTypeDetail -> openTypeDetail(intent)
                DraftConsentUiIntent.CloseTypeDetail -> navigationHelper.navigateToBack()
                DraftConsentUiIntent.Submit -> submit()
                DraftConsentUiIntent.NavigateBack -> navigateBack()
            }
        }

        private fun toggleItemInclusion(intent: DraftConsentUiIntent.ToggleItemInclusion) {
            if (state.value.isSubmitting) return
            val preparation = activePreparation ?: return
            val item = preparation.selection.items.firstOrNull { it.rawId == intent.itemKey } ?: return
            // 사진은 홈 사진 시트 선택이 정본이므로 여기서 제외할 수 없다.
            if (item.itemType == ItemType.PHOTO) return
            updateState {
                copy(
                    excludedRawIds =
                        if (intent.itemKey in excludedRawIds) {
                            excludedRawIds - intent.itemKey
                        } else {
                            excludedRawIds + intent.itemKey
                        },
                )
            }
        }

        /**
         * 현재 생성 시도의 위치 항목 전체를 한 번에 포함·제외한다.
         *
         * 켤 때 최초 스냅샷의 위치 rawId 만 제외 집합에서 뺀다 — 다른 유형이 제외한 항목을 함께
         * 되살리지 않기 위해서다. 상한 여유 재충원은 [DraftSourceItemSelection.excluding] 정책 그대로
         * 하지 않는다.
         */
        private fun toggleLocationInclusion() {
            if (state.value.isSubmitting) return
            val locationRawIds = state.value.content?.locationRawIds.orEmpty()
            if (locationRawIds.isEmpty()) return
            updateState {
                copy(
                    excludedRawIds =
                        if (isLocationIncluded) excludedRawIds + locationRawIds else excludedRawIds - locationRawIds,
                )
            }
        }

        private fun openTypeDetail(intent: DraftConsentUiIntent.OpenTypeDetail) {
            val summary = state.value.content?.summaryOf(intent.group) ?: return
            if (!summary.isSent) return
            navigationHelper.navigateTo(DraftConsentDetailPage(intent.group.name))
        }

        private fun submit() {
            val preparation = activePreparation ?: return
            if (!state.value.canSubmit) return
            // 스냅샷에서 사용자 제외 항목만 뺀 결과를 전송한다. 제외로 생긴 상한 여유는 재충원하지 않는다.
            val submission = preparation.selection.excluding(state.value.excludedRawIds)
            updateState { copy(isSubmitting = true, submitError = null) }
            safeLaunch(onError = ::handleSubmitFailure) {
                if (preparation.discardActiveTask) draftTaskCoordinator.discard()
                val result =
                    createTimelineDraftUseCase(
                        preparation.recordDate,
                        preparation.zone,
                        preparation.window,
                        submission,
                    )
                val handle =
                    result.getOrElse {
                        handleSubmitFailure(it)
                        return@safeLaunch
                    }
                draftTaskCoordinator.start(handle.taskId, preparation.recordDate)
                // 준비 상태는 여기서 폐기되므로, 로딩 화면이 쓸 것만 먼저 옮겨 담는다.
                loadingSessionStore.start(submission.toLoadingSession(handle.taskId, preparation.recordDate))
                sessionStore.clearPreparation()
                activePreparation = null
                // 동의 화면을 백스택에서 빼고 로딩 화면을 올린다 — 로딩에서 뒤로가면 홈이다.
                navigationHelper.navigateToBack()
                navigationHelper.navigateTo(DraftLoadingPage)
            }
        }

        private fun handleSubmitFailure(error: Throwable) {
            // 서버가 이 단계 동의를 다시 요구한다 — 약관이 개정됐거나, 온보딩이 동의를 받기
            // 전 버전으로 온보딩을 마친 계정이다. 이 화면은 동의를 받지 않으므로 받는 자리로
            // 보낸다. **어느 단계가 비었는지는 오류가 알려 주지 않아** 후보를 모두 싣고,
            // 그 화면이 다시 조회해 실제로 남은 것만 받는다.
            //
            // 준비는 폐기하지 않는다 — 돌아오면 같은 스냅샷으로 다시 제출할 수 있고, 그래야
            // 사진을 다시 고르지 않는다. 생성을 자동으로 재개하지도 않는다.
            if (error is ApiException && error.errorCode == TERMS_AGREEMENT_REQUIRED) {
                updateState { copy(isSubmitting = false, submitError = AGREEMENT_REQUIRED_MESSAGE) }
                navigationHelper.navigateTo(StageTermsPage(DRAFT_CONSENT_STAGES.map(TermStage::name)))
                return
            }
            // 이미 그 날짜 기록에 들어간 항목만 다시 보낸 경우다. 서버는 초안이 있는 날짜의
            // 생성을 덮어쓰기가 아니라 **이어 붙이기**로 처리하므로, 새로 더할 것이 없으면
            // 409 `-1013` 으로 거절한다. 실패로만 보이면 사용자는 이유를 알 수 없다.
            if (error is ApiException && error.errorCode == APPEND_NO_NEW_ITEMS) {
                updateState { copy(isSubmitting = false, submitError = NO_NEW_ITEMS_MESSAGE) }
                return
            }
            // 스냅샷 확정 뒤 사진이 삭제되거나 권한이 바뀐 경우 — 같은 스냅샷 재시도로는 복구되지
            // 않으므로 준비를 폐기하고 홈의 사진 재선택 흐름으로 복귀시킨다.
            if (error is DraftPhotoAccessException) {
                sessionStore.clearPreparation()
                sessionStore.markPhotoReselectionNeeded()
                activePreparation = null
                navigationHelper.navigateToBack()
                return
            }
            // 그 외에는 같은 스냅샷으로 재시도할 수 있게 화면에 머물러 안내한다.
            updateState {
                copy(
                    isSubmitting = false,
                    submitError = "초안 생성 요청을 보내지 못했어요. 잠시 후 다시 시도해주세요.",
                )
            }
            handleFailure(error)
        }

        private fun navigateBack() {
            if (state.value.isSubmitting) return
            sessionStore.clearPreparation()
            activePreparation = null
            navigationHelper.navigateToBack()
        }

        /**
         * 주소가 없는 위치 항목을 화면에 보여줄 때 해석한다.
         *
         * 수집 시점에 붙이지 않는 이유는 두 가지다. 수집은 초안을 만들지 않을 날의 좌표까지
         * 해석하게 되고, 배경에서 도는 데다 `Geocoder` 는 네트워크가 필요해 조용히 실패한 뒤
         * **다시 시도할 계기가 없다.** 화면에서 부르면 열 때마다 재시도 기회가 생긴다.
         *
         * 해석 결과는 같은 로컬 SourceItem 에 저장되므로 다음 진입부터는 캐시처럼 붙어 있다.
         *
         * 좌표 하나를 한 번씩만 부른다 — 새 `attemptId` 마다 이 함수가 한 번 돌고 항목을 한 번씩
         * 지난다. 실패해도 이번 시도 안에서는 다시 부르지 않고 `주소 미확인` 으로 남긴다. 재시도
         * 기회는 화면을 다시 열어 새 시도가 시작될 때 생긴다.
         */
        private fun resolveMissingAddresses(preparation: DraftConsentPreparation) {
            val attemptId = preparation.attemptId
            preparation.selection.items.forEach { item ->
                when (val payload = item.payload) {
                    is StayPayload -> {
                        if (payload.address != null) return@forEach
                        launchAddressResolution(attemptId) {
                            resolveStayAddress(item.rawId, payload.latitude, payload.longitude)
                                ?.let { mapOf(stayAddressKey(item.rawId) to it) }
                                .orEmpty()
                        }
                    }

                    is MovementPayload -> {
                        if (payload.start.address != null && payload.end.address != null) return@forEach
                        launchAddressResolution(attemptId) {
                            val resolved = resolveMovementAddresses(item.rawId, payload.start, payload.end)
                            buildMap {
                                resolved.start?.let { put(movementStartAddressKey(item.rawId), it) }
                                resolved.end?.let { put(movementEndAddressKey(item.rawId), it) }
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }

        /**
         * 해석 한 건을 띄우고 결과를 화면에 얹는다.
         *
         * 항목마다 따로 띄운다 — 한 좌표가 늦거나 응답이 오지 않아도 나머지 주소는 먼저 뜬다.
         * 주소는 보조 표시라 실패는 삼키고 `주소 미확인` 으로 남긴다.
         */
        private fun launchAddressResolution(
            attemptId: Long,
            resolve: suspend () -> Map<String, String>,
        ) {
            safeLaunch(onError = {}) {
                val resolved = resolve()
                if (resolved.isEmpty()) return@safeLaunch
                // 늦게 도착한 이전 생성 시도의 결과는 버린다. 지금 화면의 스냅샷과 맞지 않는다.
                val preparation = activePreparation?.takeIf { it.attemptId == attemptId } ?: return@safeLaunch
                resolvedAddresses += resolved
                updateState { copy(content = preparation.toConsentContent(resolvedAddresses)) }
            }
        }

        private fun clearResolvedAddresses() = resolvedAddresses.clear()

        private fun initialUiState(): DraftConsentUiState = DraftConsentUiState(isMapRenderAllowed = isMapRenderAllowed)

        private companion object {
            /** 서버가 이 단계 동의를 요구할 때 주는 코드. */
            const val TERMS_AGREEMENT_REQUIRED = -3001

            /**
             * `-3001` 을 받았을 때 다시 조회할 후보 단계.
             *
             * 오류가 어느 단계인지 알려 주지 않으므로 온보딩이 받는 것과 **같은 집합**을 싣는다.
             * 실제로 남은 것만 받는 판단은 동의 화면이 다시 조회해서 한다.
             */
            val DRAFT_CONSENT_STAGES = listOf(TermStage.TIMELINE_FIRST_CREATE, TermStage.TIMELINE_LOCATION)

            /** 이어 붙일 새 항목이 없을 때 서버가 주는 코드. */
            const val APPEND_NO_NEW_ITEMS = -1013

            const val AGREEMENT_REQUIRED_MESSAGE = "동의가 다시 필요해요."
            const val NO_NEW_ITEMS_MESSAGE = "이미 이 날 기록에 담긴 항목이에요. 새로 추가할 것이 없어요."
        }
    }
