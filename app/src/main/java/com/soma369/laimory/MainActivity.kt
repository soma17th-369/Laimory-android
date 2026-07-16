package com.soma369.laimory

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.soma369.laimory.core.data.helper.MessageHelperImpl
import com.soma369.laimory.core.data.helper.NavigationHelperImpl
import com.soma369.laimory.core.domain.helper.SocialLoginCallbackHandler
import com.soma369.laimory.core.domain.usecase.auth.ObserveAuthSessionUseCase
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.navigation.LaimoryNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var messageHelper: MessageHelperImpl

    @Inject
    lateinit var navigationHelper: NavigationHelperImpl

    @Inject
    lateinit var socialLoginCallbackHandler: SocialLoginCallbackHandler

    @Inject
    lateinit var observeAuthSession: ObserveAuthSessionUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeSocialLoginCallback(intent)
        val authSessionStates = observeAuthSession()
        setContent {
            LaimoryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    LaimoryNavGraph(
                        messages = messageHelper.messages,
                        navigationFlow = navigationHelper.navigationFlow,
                        authSessionStates = authSessionStates,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeSocialLoginCallback(intent)
    }

    private fun consumeSocialLoginCallback(intent: Intent) {
        val callback = intent.dataString?.toSocialLoginCallbackOrNull() ?: return
        socialLoginCallbackHandler.handle(callback)
        // 구성 변경으로 Activity가 재생성돼도 같은 callback을 다시 제출하지 않는다.
        intent.data = null
        setIntent(intent)
    }
}
