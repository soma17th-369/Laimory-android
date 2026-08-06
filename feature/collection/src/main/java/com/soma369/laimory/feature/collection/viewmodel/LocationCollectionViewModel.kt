package com.soma369.laimory.feature.collection.viewmodel

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.usecase.ClearCollectedLocationsUseCase
import com.soma369.laimory.core.domain.usecase.ObserveLocationTrackingStatusUseCase
import com.soma369.laimory.core.domain.usecase.ObserveLocationTrackingUseCase
import com.soma369.laimory.core.domain.usecase.ObserveSourceItemsUseCase
import com.soma369.laimory.core.domain.usecase.ResolveMovementAddressesUseCase
import com.soma369.laimory.core.domain.usecase.ResolveStayAddressUseCase
import com.soma369.laimory.core.domain.usecase.SetLocationTrackingUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.collection.state.LocationUiIntent
import com.soma369.laimory.feature.collection.state.LocationUiSideEffect
import com.soma369.laimory.feature.collection.state.LocationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LocationCollectionViewModel
    @Inject
    constructor(
        observeSourceItemsUseCase: ObserveSourceItemsUseCase,
        observeLocationTrackingUseCase: ObserveLocationTrackingUseCase,
        observeLocationTrackingStatusUseCase: ObserveLocationTrackingStatusUseCase,
        private val setLocationTrackingUseCase: SetLocationTrackingUseCase,
        private val clearCollectedLocationsUseCase: ClearCollectedLocationsUseCase,
        private val resolveMovementAddressesUseCase: ResolveMovementAddressesUseCase,
        private val resolveStayAddressUseCase: ResolveStayAddressUseCase,
    ) : BaseMviViewModel<LocationUiState, LocationUiIntent, LocationUiSideEffect>(LocationUiState()) {
        /** 같은 화면 생명주기에서 재구성·스크롤로 동일 rawId를 중복 조회하지 않는다. */
        private val attemptedLocationAddressRawIds = mutableSetOf<String>()

        init {
            safeLaunch {
                observeSourceItemsUseCase().collect { items ->
                    updateState {
                        copy(
                            isLoading = false,
                            stagedItems =
                                items.filter {
                                    it.itemType == ItemType.STAY || it.itemType == ItemType.MOVEMENT
                                },
                        )
                    }
                }
            }
            safeLaunch {
                observeLocationTrackingUseCase().collect { enabled -> updateState { copy(isTracking = enabled) } }
            }
            safeLaunch {
                observeLocationTrackingStatusUseCase().collect { status -> updateState { copy(trackingStatus = status) } }
            }
        }

        override suspend fun handleIntent(intent: LocationUiIntent) {
            when (intent) {
                is LocationUiIntent.SetTracking -> setTracking(intent.enabled)
                LocationUiIntent.ClearStaged -> clearStaged()
                is LocationUiIntent.ResolveMovementAddresses -> resolveMovementAddresses(intent)
                is LocationUiIntent.ResolveStayAddress -> resolveStayAddress(intent)
            }
        }

        private fun setTracking(enabled: Boolean) =
            safeLaunch {
                setLocationTrackingUseCase(enabled)
                sendEffect(
                    LocationUiSideEffect.ShowMessage(
                        if (enabled) "위치 자동 수집을 켰습니다." else "위치 자동 수집을 껐습니다.",
                    ),
                )
            }

        private fun clearStaged() =
            safeLaunch {
                clearCollectedLocationsUseCase()
                sendEffect(LocationUiSideEffect.ShowMessage("스테이징 위치를 모두 비웠습니다."))
            }

        private fun resolveStayAddress(intent: LocationUiIntent.ResolveStayAddress) {
            if (!attemptedLocationAddressRawIds.add(intent.rawId)) return
            // 주소는 보조 표시 정보이므로 실패 시 좌표 fallback을 유지하고 반복 요청하지 않는다.
            safeLaunch(onError = {}) {
                resolveStayAddressUseCase(
                    rawId = intent.rawId,
                    latitude = intent.latitude,
                    longitude = intent.longitude,
                )
            }
        }

        private fun resolveMovementAddresses(intent: LocationUiIntent.ResolveMovementAddresses) {
            if (!attemptedLocationAddressRawIds.add(intent.rawId)) return
            safeLaunch(onError = {}) {
                resolveMovementAddressesUseCase(
                    rawId = intent.rawId,
                    start = intent.start,
                    end = intent.end,
                )
            }
        }
    }
