package com.soma369.laimory.apiorigin

import android.content.Context
import androidx.core.content.edit
import com.soma369.laimory.core.data.network.ApiOriginOverride

/**
 * 마지막으로 고른 서버. 백그라운드로 뜬 프로세스(알림·작업)도 다이얼로그 없이 이 값으로 붙는다.
 *
 * SharedPreferences 를 동기로 읽는다 — 첫 Retrofit 을 만들 때 값이 이미 있어야 하고, debug 전용이라
 * 작은 파일 하나를 읽는 비용은 문제 되지 않는다.
 */
class DebugApiOriginStore(
    context: Context,
) : ApiOriginOverride {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val selected: DebugApiOrigin
        get() = DebugApiOrigin.fromNameOrDefault(prefs.getString(KEY_ORIGIN, null))

    override val baseUrl: String
        get() = selected.baseUrl

    /** 바로 뒤에 프로세스를 끝내므로 디스크에 쓰일 때까지 기다린다(`commit`). */
    fun save(origin: DebugApiOrigin) {
        prefs.edit(commit = true) { putString(KEY_ORIGIN, origin.name) }
    }

    private companion object {
        const val PREFS_NAME = "debug_api_origin"
        const val KEY_ORIGIN = "origin"
    }
}
