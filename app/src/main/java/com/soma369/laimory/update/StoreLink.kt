package com.soma369.laimory.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.soma369.laimory.BuildConfig

/**
 * 스토어의 이 앱 페이지.
 *
 * 패키지명을 화면에서 하드코딩하지 않고 빌드 계약 한 곳([BuildConfig.STORE_APPLICATION_ID])에서
 * 가져온다. debug 는 `.debug`, qa 는 `.qa` 가 붙으므로 자기 applicationId 를 쓰면 스토어에 없는
 * 주소가 열린다.
 */
object StoreLink {
    /**
     * Play 앱이 여는 주소.
     *
     * 주소를 Intent 와 나눠 둔 것은 검증 때문이다. `Intent`·`Uri` 는 단위 테스트에 구현이 없어
     * 껍데기로 돌아오므로, 어떤 패키지를 가리키는지는 문자열로만 확인할 수 있다.
     */
    fun marketUri(applicationId: String = BuildConfig.STORE_APPLICATION_ID): String = "market://details?id=$applicationId"

    /** Play 앱이 없는 기기의 대체 주소. */
    fun webUri(applicationId: String = BuildConfig.STORE_APPLICATION_ID): String =
        "https://play.google.com/store/apps/details?id=$applicationId"

    private fun marketIntent(): Intent = Intent(Intent.ACTION_VIEW, marketUri().toUri())

    private fun webIntent(): Intent = Intent(Intent.ACTION_VIEW, webUri().toUri())

    /**
     * Play 앱을 먼저 시도하고 없으면 웹으로 연다.
     *
     * `resolveActivity()` 로 미리 확인하지 않는다 — Android 11+ 패키지 가시성 때문에 매니페스트에
     * `<queries>` 선언이 필요한데, 지금 그 선언이 없어 늘 `null` 로 보인다.
     */
    fun open(context: Context) {
        runCatching { context.startActivity(marketIntent()) }
            .onFailure { error ->
                if (error !is ActivityNotFoundException) throw error
                context.startActivity(webIntent())
            }
    }
}
