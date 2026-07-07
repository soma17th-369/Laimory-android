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
import javax.inject.Inject

@HiltViewModel
class NotificationCollectionViewModel
    @Inject
    constructor(
        observeSourceItemsUseCase: ObserveSourceItemsUseCase,
        observeNotificationFilterUseCase: ObserveNotificationFilterUseCase,
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
                        copy(mode = filter.mode, keywords = filter.keywords, allowedPackages = filter.allowedPackages)
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
                is NotificationUiIntent.SetMode -> updateFilter { copy(mode = intent.mode) }
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

        /** 현재 상태의 필터에 [transform] 을 적용해 저장한다. 저장 결과는 관찰 flow 로 다시 상태에 반영된다. */
        private fun updateFilter(transform: NotificationFilter.() -> NotificationFilter) =
            safeLaunch {
                val current =
                    NotificationFilter(
                        mode = state.value.mode,
                        keywords = state.value.keywords,
                        allowedPackages = state.value.allowedPackages,
                    )
                updateNotificationFilterUseCase(current.transform())
            }

        private fun clearStaged() =
            safeLaunch {
                clearCollectedNotificationsUseCase()
                sendEffect(NotificationUiSideEffect.ShowMessage("스테이징 알림을 모두 비웠습니다."))
            }
    }
