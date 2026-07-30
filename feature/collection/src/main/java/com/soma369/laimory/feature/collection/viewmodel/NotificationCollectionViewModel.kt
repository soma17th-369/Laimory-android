package com.soma369.laimory.feature.collection.viewmodel

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.NotificationFilter
import com.soma369.laimory.core.domain.source.InstalledAppsProvider
import com.soma369.laimory.core.domain.usecase.ClearCollectedNotificationsUseCase
import com.soma369.laimory.core.domain.usecase.ObserveNotificationFilterUseCase
import com.soma369.laimory.core.domain.usecase.ObserveSourceItemsUseCase
import com.soma369.laimory.core.domain.usecase.UpdateNotificationFilterUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.collection.state.NotificationUiIntent
import com.soma369.laimory.feature.collection.state.NotificationUiSideEffect
import com.soma369.laimory.feature.collection.state.NotificationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class NotificationCollectionViewModel
    @Inject
    constructor(
        observeSourceItemsUseCase: ObserveSourceItemsUseCase,
        private val observeNotificationFilterUseCase: ObserveNotificationFilterUseCase,
        private val updateNotificationFilterUseCase: UpdateNotificationFilterUseCase,
        private val clearCollectedNotificationsUseCase: ClearCollectedNotificationsUseCase,
        private val installedAppsProvider: InstalledAppsProvider,
    ) : BaseMviViewModel<NotificationUiState, NotificationUiIntent, NotificationUiSideEffect>(NotificationUiState()) {
        init {
            safeLaunch {
                observeSourceItemsUseCase().collect { items ->
                    updateState {
                        copy(
                            isLoading = false,
                            stagedNotifications = items.filter { it.itemType == ItemType.NOTIFICATION },
                        )
                    }
                }
            }
            safeLaunch {
                observeNotificationFilterUseCase().collect { filter ->
                    updateState {
                        copy(
                            collectOnClick = filter.collectOnClick,
                            keywords = filter.keywords,
                            allowedPackages = filter.allowedPackages,
                        )
                    }
                }
            }
            safeLaunch {
                val apps = installedAppsProvider.launchableApps()
                updateState { copy(installedApps = apps) }
            }
        }

        override suspend fun handleIntent(intent: NotificationUiIntent) {
            when (intent) {
                is NotificationUiIntent.AddKeyword -> {
                    val keyword = intent.keyword.trim()
                    if (keyword.isNotEmpty()) updateFilter { copy(keywords = keywords + keyword) }
                }
                is NotificationUiIntent.RemoveKeyword -> updateFilter { copy(keywords = keywords - intent.keyword) }
                is NotificationUiIntent.ToggleApp ->
                    updateFilter {
                        copy(
                            allowedPackages =
                                if (intent.packageName in allowedPackages) {
                                    allowedPackages - intent.packageName
                                } else {
                                    allowedPackages + intent.packageName
                                },
                        )
                    }
                NotificationUiIntent.ClearStaged -> clearStaged()
            }
        }

        /**
         * 저장된 필터를 소스에서 읽어 [transform] 적용 후 저장한다.
         *
         * handleIntent 의 순차 처리 안에서 suspend 로 직접 실행하고(별도 코루틴 X), state.value(관찰 flow 로
         * 지연 반영) 대신 저장소 최신값을 읽어 변형한다. 앱 토글/키워드를 빠르게 연속 입력해도 이전 스냅샷을
         * 덮어쓰지 않는다. 실패는 intent 루프를 죽이지 않도록 잡아 공통 실패 처리로 넘긴다.
         */
        private suspend fun updateFilter(transform: NotificationFilter.() -> NotificationFilter) {
            runCatching {
                val current = observeNotificationFilterUseCase().first()
                updateNotificationFilterUseCase(current.transform())
            }.onFailure(::handleFailure)
        }

        private fun clearStaged() =
            safeLaunch {
                clearCollectedNotificationsUseCase()
                sendEffect(NotificationUiSideEffect.ShowMessage("스테이징 알림을 모두 비웠습니다."))
            }
    }
