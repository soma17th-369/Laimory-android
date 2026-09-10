package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.coordinator.TermsAgreementCoordinator
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.StayPayload
import com.soma369.laimory.core.domain.model.terms.TermStage
import com.soma369.laimory.core.domain.model.timeline.LocationMapKeyGate
import com.soma369.laimory.core.domain.navigation.DraftConsentDetailPage
import com.soma369.laimory.core.domain.usecase.CreateTimelineDraftUseCase
import com.soma369.laimory.core.domain.usecase.ResolveMovementAddressesUseCase
import com.soma369.laimory.core.domain.usecase.ResolveStayAddressUseCase
import com.soma369.laimory.core.domain.usecase.terms.GetDisplayTermsUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.home.draft.DraftConsentPreparation
import com.soma369.laimory.feature.home.draft.DraftConsentSelectionSnapshot
import com.soma369.laimory.feature.home.draft.DraftConsentSessionStore
import com.soma369.laimory.feature.home.draft.DraftLoadingSessionStore
import com.soma369.laimory.feature.home.state.DraftConsentUiIntent
import com.soma369.laimory.feature.home.state.DraftConsentUiSideEffect
import com.soma369.laimory.feature.home.state.DraftConsentUiState
import com.soma369.laimory.feature.home.state.movementEndAddressKey
import com.soma369.laimory.feature.home.state.movementStartAddressKey
import com.soma369.laimory.feature.home.state.stayAddressKey
import com.soma369.laimory.feature.home.state.toConsentContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
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
        mapKeyGate: LocationMapKeyGate,
    ) : BaseMviViewModel<DraftConsentUiState, DraftConsentUiIntent, DraftConsentUiSideEffect>(
            DraftConsentUiState(),
        ) {
        /** 지도 SDK 키 준비 여부. 빌드 상수라 세션 동안 바뀌지 않는다. */
        private val isMapKeyPresent = mapKeyGate.isMapKeyPresent()
        private var activePreparation: DraftConsentPreparation? = null

        /**
         * 상세가 지금 보고 있는 스냅샷.
         *
         * 제출용이 확정돼 있으면 그것이고, 아니면 홈이 상시로 유지하는 것이다 — 카드에서 CTA 전에
         * 들어오는 경로가 후자다.
         */
        private var activeSnapshot: DraftConsentSelectionSnapshot? = null

        /**
         * 표시 시점에 해석한 주소. 키는 [stayAddressKey] 계열이다.
         *
         * 전송 스냅샷이 아니라 화면 모델에만 덧입힌다. 새 생성 시도가 오면 비운다 — 스냅샷이
         * 바뀌면 대응하는 항목도 달라진다.
         */
        private val resolvedAddresses = mutableMapOf<String, String>()

        /**
         * 해석을 이미 시도한 위치 항목의 rawId. 같은 생성 시도 안에서만 유효하다.
         *
         * 화면이 복귀할 때마다 재판정하므로 이 표시가 없으면 같은 좌표를 반복해서 묻게 된다.
         * 실패한 좌표도 이번 시도 안에서는 다시 부르지 않는다 — 주소는 보조 표시라 재시도 기회는
         * 홈에서 새 시도를 시작할 때 준다.
         */
        private val attemptedAddressRawIds = mutableSetOf<String>()

        init {
            safeLaunch {
                // 제출용이 확정돼 있으면 그것을 본다 — 확정 뒤에는 수집이 갱신돼도 보낼 것이
                // 바뀌지 않아야 한다. 확정 전에는 홈이 상시로 유지하는 것을 본다.
                combine(sessionStore.preparation, sessionStore.selection) { preparation, browsing ->
                    activePreparation = preparation
                    preparation?.snapshot ?: browsing
                }.collect { snapshot ->
                    when {
                        // 폐기(null) 시 activity 범위 ViewModel 에 알림 본문·사진 URI 같은
                        // 민감 표시 모델이 남지 않도록 즉시 초기화한다.
                        snapshot == null -> {
                            activeSnapshot = null
                            clearResolvedAddresses()
                            updateState { DraftConsentUiState() }
                        }

                        // 갱신 번호가 오르면 **표시 모델만** 다시 만든다. 제외·주소·동의 판정은
                        // 그대로다 — 수집이 돌 때마다 사용자가 고른 것이 사라지면 안 된다.
                        snapshot.revision != activeSnapshot?.revision -> {
                            activeSnapshot = snapshot
                            pruneResolvedAddresses(snapshot)
                            updateState { copy(content = snapshot.toConsentContent(resolvedAddresses)) }
                            applyLocationConsent(snapshot)
                        }
                    }
                }
            }
            // 제외 집합과 위치 전송 여부는 스토어가 소유한다 — 홈이 본문 건수를 세고 상세가
            // 토글하므로 한쪽이 가지면 다른 쪽이 못 본다.
            safeLaunch { sessionStore.excludedRawIds.collect { excluded -> updateState { copy(excludedRawIds = excluded) } } }
            safeLaunch {
                sessionStore.isLocationSendEnabled.collect { enabled ->
                    updateState { copy(isLocationSendEnabled = enabled) }
                }
            }
        }

        override suspend fun handleIntent(intent: DraftConsentUiIntent) {
            when (intent) {
                is DraftConsentUiIntent.ToggleItemInclusion -> toggleItemInclusion(intent)
                DraftConsentUiIntent.Sync -> syncLocationConsent()
                DraftConsentUiIntent.ToggleLocationInclusion -> toggleLocationInclusion()
                is DraftConsentUiIntent.OpenTypeDetail -> openTypeDetail(intent)
                DraftConsentUiIntent.CloseTypeDetail -> navigationHelper.navigateToBack()
            }
        }

        private fun toggleItemInclusion(intent: DraftConsentUiIntent.ToggleItemInclusion) {
            if (state.value.isSubmitting) return
            val snapshot = activeSnapshot ?: return
            val item = snapshot.selection.items.firstOrNull { it.rawId == intent.itemKey } ?: return
            // 사진은 홈 사진 시트 선택이 정본이므로 여기서 제외할 수 없다.
            if (item.itemType == ItemType.PHOTO) return
            sessionStore.toggleExcluded(intent.itemKey)
        }

        /**
         * 위치 전송을 한 번에 켜고 끈다.
         *
         * 끌 때 **그 시점 rawId 를 제외 집합에 넣지 않는다.** 그렇게 하면 그 뒤 수집된
         * STAY·MOVEMENT 가 제외 집합에 없어, 스위치는 OFF 인데 위치가 나간다. 무엇을 뺄지는
         * 제출 직전에 그때의 스냅샷으로 판단한다.
         */
        private fun toggleLocationInclusion() {
            if (state.value.isSubmitting) return
            sessionStore.setLocationSendEnabled(!state.value.isLocationSendEnabled)
        }

        private fun openTypeDetail(intent: DraftConsentUiIntent.OpenTypeDetail) {
            val summary = state.value.content?.summaryOf(intent.group) ?: return
            if (!summary.isSent) return
            navigationHelper.navigateTo(DraftConsentDetailPage(intent.group.name))
        }

        /** 화면 복귀 신호. 진행 중인 시도가 없으면 판정할 대상도 없다. */
        private fun syncLocationConsent() {
            applyLocationConsent(activeSnapshot ?: return)
        }

        /**
         * 저장된 위치정보 약관 동의를 확인하고, 그 결과로 지도와 주소 해석을 **함께** 연다.
         *
         * 둘 다 사용자의 좌표를 Google 로 내보내는 일이다 — 지도는 카메라 영역을, `Geocoder` 는
         * 좌표 자체를 보낸다. 지도만 막고 주소 해석을 열어 두면 미동의 좌표가 그대로 나간다.
         *
         * **시도마다, 그리고 화면에 돌아올 때마다 다시 판정한다.** 이 ViewModel 은 Activity 범위라
         * 로그아웃 뒤 다음 계정까지 살아 있다. 한 번 받은 값을 재사용하면 동의한 계정의 판정이 다음
         * 계정으로 넘어가고, 반대로 뒤늦게 동의한 사용자는 계속 막힌 채로 남는다. 제출이 막혀 약관
         * 화면에 다녀오는 경로는 **같은 스냅샷으로 돌아오므로** 새 시도가 생기지 않는다 — 그 복귀는
         * [DraftConsentUiIntent.Sync] 가 알려 준다. 이전 시도의 늦은 응답은 버린다.
         *
         * 알아내기 전과 조회 실패는 모두 "허용되지 않음"이다. 모르는 상태에서 좌표를 내보내지 않는다.
         * catalog 가 비면 요구가 없어 만족으로 보는데, 이는 서버의 fail-open 과 같은 판정이다.
         */
        private fun applyLocationConsent(snapshot: DraftConsentSelectionSnapshot) {
            val revision = snapshot.revision
            safeLaunch(onError = {}) {
                val isGranted =
                    termsCoordinator
                        .requirementOf(TermStage.TIMELINE_LOCATION)
                        .getOrNull()
                        ?.isSatisfied == true
                if (activeSnapshot?.revision != revision) return@safeLaunch
                val isMapAllowed = isGranted && isMapKeyPresent
                updateState { copy(isMapRenderAllowed = isMapAllowed) }
                if (isGranted) resolveMissingAddresses(snapshot)
            }
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
         * 좌표 하나를 한 번씩만 부른다([attemptedAddressRawIds]). 이 함수는 복귀할 때마다 다시
         * 도는데 그때 이미 물어본 좌표를 또 물으면 같은 요청이 쌓인다. 실패해도 이번 시도 안에서는
         * 다시 부르지 않고 `주소 미확인` 으로 남긴다 — 재시도 기회는 홈에서 새 시도를 시작할 때 생긴다.
         */
        private fun resolveMissingAddresses(snapshot: DraftConsentSelectionSnapshot) {
            val revision = snapshot.revision
            snapshot.selection.items.forEach { item ->
                when (val payload = item.payload) {
                    is StayPayload -> {
                        if (payload.address != null) return@forEach
                        if (!attemptedAddressRawIds.add(item.rawId)) return@forEach
                        launchAddressResolution(revision) {
                            // 목록·말풍선은 한 줄 주소만 쓴다. 시·동 층위는 홈 위치 카드가 쓴다.
                            resolveStayAddress(item.rawId, payload.latitude, payload.longitude)
                                ?.let { mapOf(stayAddressKey(item.rawId) to it.line) }
                                .orEmpty()
                        }
                    }

                    is MovementPayload -> {
                        if (payload.start.address != null && payload.end.address != null) return@forEach
                        if (!attemptedAddressRawIds.add(item.rawId)) return@forEach
                        launchAddressResolution(revision) {
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
            revision: Long,
            resolve: suspend () -> Map<String, String>,
        ) {
            safeLaunch(onError = {}) {
                val resolved = resolve()
                if (resolved.isEmpty()) return@safeLaunch
                // 늦게 도착한 이전 판의 결과는 버린다. 지금 화면의 스냅샷과 맞지 않는다.
                val snapshot = activeSnapshot?.takeIf { it.revision == revision } ?: return@safeLaunch
                resolvedAddresses += resolved
                updateState { copy(content = snapshot.toConsentContent(resolvedAddresses)) }
            }
        }

        /** 사라진 항목의 주소만 걷어 낸다. 갱신마다 비우면 수집이 돌 때마다 `Geocoder` 를 때린다. */
        private fun pruneResolvedAddresses(snapshot: DraftConsentSelectionSnapshot) {
            val alive = snapshot.selection.items.mapTo(mutableSetOf()) { it.rawId }
            resolvedAddresses.keys.retainAll { key -> key.substringBefore(':') in alive }
            attemptedAddressRawIds.retainAll(alive)
        }

        private fun clearResolvedAddresses() {
            resolvedAddresses.clear()
            attemptedAddressRawIds.clear()
        }

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
