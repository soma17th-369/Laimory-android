package com.soma369.laimory.feature.feature1.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface Feature1UiIntent : UiIntent {
    data object LoadItems : Feature1UiIntent

    data object TriggerServerError : Feature1UiIntent

    data object TriggerUnauthorizedError : Feature1UiIntent

    data object TriggerNetworkError : Feature1UiIntent
}
