package com.soma369.laimory.crash

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.soma369.laimory.core.util.logging.CrashReporter

/**
 * [CrashReporter] 의 Firebase Crashlytics 구현. **Firebase 타입은 이 파일 밖으로 나가지 않는다.**
 *
 * 수집 여부를 여기서 분기하지 않는다. 빌드 타입별 정책은 매니페스트의
 * `firebase_crashlytics_collection_enabled` 가 소유한다 — SDK 가 프로세스 시작 시점에 그 값을 읽으므로
 * 코드가 돌기 전에 난 크래시까지 그 판정을 따르고, 런타임 분기를 겹치면 두 곳이 어긋날 수 있다.
 * 수집이 꺼진 빌드에서는 아래 호출이 그대로 무시된다.
 */
internal class CrashlyticsCrashReporter(
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance(),
) : CrashReporter {
    override fun log(message: String) = crashlytics.log(message)

    override fun recordException(throwable: Throwable) = crashlytics.recordException(throwable)

    override fun setKey(
        key: String,
        value: String,
    ) = crashlytics.setCustomKey(key, value)
}
