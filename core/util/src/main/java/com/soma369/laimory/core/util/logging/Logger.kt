package com.soma369.laimory.core.util.logging

import android.util.Log
import com.soma369.laimory.core.util.BuildConfig

/**
 * 로그 래퍼. `android.util.Log`를 직접 쓰는 대신 이 래퍼를 쓴다.
 *
 * - domain: 로그 영역([LogDomain]). Logcat tag로 쓴다.
 * - 로그 한 줄이 가는 곳이 둘이다. **Logcat 출력**은 [minLevel]이, **원격 브레드크럼**은
 *   [remoteMinLevel]이 가른다. 앱 시작 시 빌드 타입에 맞게 재설정한다.
 * - [crashReporter]가 꽂혀 있으면 [remoteMinLevel] 이상의 로그가 크래시 리포트의 맥락으로 함께
 *   실리고, throwable 을 든 [e] 는 non-fatal 로도 보고된다.
 *
 * 민감정보 정책: 메시지에 객체 전체를 dump 하지 않는다. 식별자·상태 요약만 남긴다.
 * (attributes 맵을 원격으로 보내는 구조도 두지 않는다.) 원격으로 나가는 문장에는 이 정책이 더
 * 엄하게 걸린다 — [CrashReporter] 참고.
 */
object Logger {
    enum class Level { VERBOSE, DEBUG, INFO, WARN, ERROR }

    /** 이 레벨 미만은 Logcat에 출력하지 않는다. 앱 시작 시 빌드 타입에 맞게 재설정 가능. */
    @Volatile
    var minLevel: Level = if (BuildConfig.DEBUG) Level.VERBOSE else Level.WARN

    /**
     * 이 레벨 미만은 원격 브레드크럼으로 보내지 않는다.
     *
     * **[minLevel]을 재사용하지 않는다.** 출시 빌드의 Logcat 하한은 `WARN`이라, 그 값을 그대로
     * 쓰면 크래시 직전의 `INFO` 맥락이 통째로 사라진다. 그러면 매핑은 풀렸는데 왜 터졌는지는
     * 모르는 리포트가 남는다. 원격 전송 여부는 빌드 타입별 수집 설정이 이미 가르므로, 여기서는
     * 세 빌드 모두 같은 하한을 쓴다.
     */
    @Volatile
    var remoteMinLevel: Level = Level.INFO

    /** 원격 보고 대상. 꽂히기 전에는 로그가 Logcat 에만 남는다. */
    @Volatile
    var crashReporter: CrashReporter? = null

    /** 크래시 리포트를 가를 꼬리표를 남긴다. 대상이 없으면 조용히 지나간다. */
    fun setCrashKey(
        key: String,
        value: String,
    ) {
        crashReporter?.setKey(key, value)
    }

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
        // Logcat 게이트보다 먼저 부른다. 나중에 두면 minLevel 이 원격 전송까지 막는다.
        report(level, domain, msg, throwable)
        if (level.ordinal < minLevel.ordinal) return
        when (level) {
            Level.VERBOSE -> Log.v(domain, msg)
            Level.DEBUG -> Log.d(domain, msg)
            Level.INFO -> Log.i(domain, msg)
            Level.WARN -> if (throwable != null) Log.w(domain, msg, throwable) else Log.w(domain, msg)
            Level.ERROR -> if (throwable != null) Log.e(domain, msg, throwable) else Log.e(domain, msg)
        }
    }

    /**
     * 원격 대상에 맥락을 남긴다.
     *
     * throwable 을 든 [Level.ERROR]만 non-fatal 로 보고한다. 그보다 낮은 레벨은 "이미 다룬 실패"를
     * 뜻하므로 보고하면 노이즈가 되고, throwable 이 없으면 보고할 스택 자체가 없다.
     */
    private fun report(
        level: Level,
        domain: String,
        msg: String,
        throwable: Throwable?,
    ) {
        val reporter = crashReporter ?: return
        if (level.ordinal < remoteMinLevel.ordinal) return
        reporter.log("$level/$domain: $msg")
        if (level == Level.ERROR && throwable != null) reporter.recordException(throwable)
    }
}
