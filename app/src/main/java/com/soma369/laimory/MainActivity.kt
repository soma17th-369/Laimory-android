package com.soma369.laimory

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.soma369.laimory.core.data.helper.MessageHelperImpl
import com.soma369.laimory.core.data.helper.NavigationHelperImpl
import com.soma369.laimory.core.domain.helper.GlobalLoadingHelper
import com.soma369.laimory.core.domain.helper.SocialLoginCallbackHandler
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.usecase.auth.ObserveAuthSessionUseCase
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.feature.home.draft.DraftConsentSessionStore
import com.soma369.laimory.navigation.LaimoryNavGraph
import com.soma369.laimory.push.DraftCompletionPushHandler
import com.soma369.laimory.push.DraftCompletionSignalParser
import com.soma369.laimory.ui.GlobalUiHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // 권한 거부도 초안 생성·polling·전경 복구를 막지 않는다.
        }

    @Inject
    lateinit var messageHelper: MessageHelperImpl

    @Inject
    lateinit var globalLoadingHelper: GlobalLoadingHelper

    @Inject
    lateinit var navigationHelper: NavigationHelperImpl

    @Inject
    lateinit var socialLoginCallbackHandler: SocialLoginCallbackHandler

    @Inject
    lateinit var observeAuthSession: ObserveAuthSessionUseCase

    @Inject
    lateinit var draftCompletionPushHandler: DraftCompletionPushHandler

    @Inject
    lateinit var draftConsentSessionStore: DraftConsentSessionStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeSocialLoginCallback(intent)
        consumeDraftCompletionNotification(intent)
        val authSessionStates = observeAuthSession()
        lifecycleScope.launch {
            observeAuthSession().first { it == AuthSessionState.Authenticated }
            requestNotificationPermissionIfNeeded()
        }
        setContent {
            LaimoryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LaimoryNavGraph(
                            messages = messageHelper.messages,
                            navigationFlow = navigationHelper.navigationFlow,
                            authSessionStates = authSessionStates,
                            onAuthRootReplaced = {
                                // 계정 경계 교체 시 이전 사용자의 대화 상자와 생성 시도 스냅샷을 함께 정리한다.
                                messageHelper.clearDialogs()
                                draftConsentSessionStore.clearAll()
                            },
                        )
                        GlobalUiHost(
                            messageHelper = messageHelper,
                            loadingHelper = globalLoadingHelper,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeSocialLoginCallback(intent)
        consumeDraftCompletionNotification(intent)
    }

    private fun consumeSocialLoginCallback(intent: Intent) {
        val callback = intent.dataString?.toSocialLoginCallbackOrNull() ?: return
        socialLoginCallbackHandler.handle(callback)
        // 구성 변경으로 Activity가 재생성돼도 같은 callback을 다시 제출하지 않는다.
        intent.data = null
        setIntent(intent)
    }

    private fun consumeDraftCompletionNotification(intent: Intent) {
        val taskId = intent.getStringExtra(DraftCompletionSignalParser.TASK_ID_KEY)
        val status = intent.getStringExtra(DraftCompletionSignalParser.STATUS_KEY)
        if (taskId == null && status == null) return
        intent.removeExtra(DraftCompletionSignalParser.TASK_ID_KEY)
        intent.removeExtra(DraftCompletionSignalParser.STATUS_KEY)
        setIntent(intent)
        lifecycleScope.launch {
            if (!draftCompletionPushHandler.onNotificationOpened(taskId, status)) return@launch
            navigationHelper.replaceRoot(HomePage)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
}
