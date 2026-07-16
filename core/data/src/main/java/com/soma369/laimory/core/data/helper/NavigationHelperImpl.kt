package com.soma369.laimory.core.data.helper

import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.navigation.NavSignal
import com.soma369.laimory.core.domain.navigation.Page
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [NavigationHelper]의 구현체 겸 브릿지. (MessageHelperImpl과 동일 패턴)
 *
 * 도메인이 발행한 이동을 [NavSignal]로 Channel에 넣고, Compose 호스트(app `AppNavHost`)가
 * [navigationFlow]를 수집해 실제 backStack 조작으로 매핑한다.
 * (@Singleton impl은 Compose 상태를 직접 못 가지므로 브릿지)
 */
@Singleton
class NavigationHelperImpl
    @Inject
    constructor() : NavigationHelper {
        private val channel = Channel<NavSignal>(Channel.BUFFERED)
        val navigationFlow: Flow<NavSignal> = channel.receiveAsFlow()

        override fun navigateTo(page: Page) {
            channel.trySend(NavSignal.GoToDestPage(page.toRoute()))
        }

        override fun replaceRoot(page: Page) {
            channel.trySend(NavSignal.ReplaceRoot(page.toRoute()))
        }

        override fun navigateToBack() {
            channel.trySend(NavSignal.Back)
        }
    }
