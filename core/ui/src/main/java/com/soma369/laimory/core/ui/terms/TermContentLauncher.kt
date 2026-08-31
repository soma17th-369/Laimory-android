package com.soma369.laimory.core.ui.terms

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

/**
 * 약관 원문을 여는 창구.
 *
 * 앱 안에 WebView 화면을 만들지 않는다 — 원문은 게시된 정적 HTML 이고, 확대 가능해야 한다는
 * 요구를 브라우저가 그대로 만족한다. 로그인 전에도 열리는 주소라 인증 상태를 따지지 않는다.
 *
 * 반환값은 **열렸는지**다. 브라우저가 없는 기기가 있어 호출부가 안내를 바꿀 수 있어야 한다.
 */
fun interface TermContentLauncher {
    fun open(url: String): Boolean
}

@Composable
fun rememberTermContentLauncher(): TermContentLauncher {
    val context = LocalContext.current
    return remember(context) { TermContentLauncher { url -> context.openTermContent(url) } }
}

private fun Context.openTermContent(url: String): Boolean =
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            // 약관 주소를 공유 메뉴로 흘릴 이유가 없다. 로그인 흐름과 같은 설정을 쓴다.
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .build()
            .launchUrl(this, url.toUri())
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
