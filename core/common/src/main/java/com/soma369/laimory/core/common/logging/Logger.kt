package com.soma369.laimory.core.common.logging

import android.util.Log
import com.soma369.laimory.core.common.BuildConfig

/**
 * 로컬 Logcat 전용 로거 래퍼. `android.util.Log`를 직접 쓰는 대신 이 래퍼를 쓴다.
 *
 * - domain: 로그 영역([LogDomain]). Logcat tag로 쓴다.
 * - 빌드 타입별 최소 레벨을 [minLevel]로 제어한다. 기본값은 debug=VERBOSE / 그 외=WARN이며,
 *   QA/Market 빌드 타입이 도입되면 앱 시작 시 [minLevel]을 재설정한다.
 * - remote 전송 / 파일 저장 / analytics는 이 로거의 책임이 아니다(후속 이슈).
 *
 * 민감정보 정책: 메시지에 객체 전체를 dump 하지 않는다. 식별자·상태 요약만 남긴다.
 * (attributes 맵을 원격으로 보내는 구조도 두지 않는다.)
 */
object Logger {
    enum class Level { VERBOSE, DEBUG, INFO, WARN, ERROR }

    /** 이 레벨 미만은 출력하지 않는다. 앱 시작 시 빌드 타입에 맞게 재설정 가능. */
    @Volatile
    var minLevel: Level = if (BuildConfig.DEBUG) Level.VERBOSE else Level.WARN

    fun v(
        domain: String,
        msg: String,
    ) = log(Level.VERBOSE, domain, msg, null)

    fun d(
        domain: String,
        msg: String,
    ) = log(Level.DEBUG, domain, msg, null)

    fun i(
        domain: String,
        msg: String,
    ) = log(Level.INFO, domain, msg, null)

    fun w(
        domain: String,
        msg: String,
        throwable: Throwable? = null,
    ) = log(Level.WARN, domain, msg, throwable)

    fun e(
        domain: String,
        msg: String,
        throwable: Throwable? = null,
    ) = log(Level.ERROR, domain, msg, throwable)

    private fun log(
        level: Level,
        domain: String,
        msg: String,
        throwable: Throwable?,
    ) {
        if (level.ordinal < minLevel.ordinal) return
        when (level) {
            Level.VERBOSE -> Log.v(domain, msg)
            Level.DEBUG -> Log.d(domain, msg)
            Level.INFO -> Log.i(domain, msg)
            Level.WARN -> if (throwable != null) Log.w(domain, msg, throwable) else Log.w(domain, msg)
            Level.ERROR -> if (throwable != null) Log.e(domain, msg, throwable) else Log.e(domain, msg)
        }
    }
}
