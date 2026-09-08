package com.soma369.laimory.core.util.logging

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoggerTest {
    private val reporter = RecordingCrashReporter()

    @Before
    fun setUp() {
        Logger.crashReporter = reporter
        Logger.minLevel = Logger.Level.VERBOSE
        Logger.remoteMinLevel = Logger.Level.INFO
    }

    @After
    fun tearDown() {
        Logger.crashReporter = null
        Logger.minLevel = Logger.Level.VERBOSE
        Logger.remoteMinLevel = Logger.Level.INFO
    }

    @Test
    fun `Logcat 하한이 높아도 원격 브레드크럼은 남는다`() {
        // 출시 빌드의 조합이다. 두 하한을 하나로 묶으면 크래시 직전 맥락이 통째로 사라진다.
        Logger.minLevel = Logger.Level.WARN

        Logger.i(LogDomain.NAVIGATION, "홈으로 이동")

        assertEquals(listOf("INFO/Navigation: 홈으로 이동"), reporter.messages)
    }

    @Test
    fun `원격 하한 미만은 브레드크럼으로 보내지 않는다`() {
        Logger.d(LogDomain.MVI, "상태 갱신")
        Logger.v(LogDomain.MVI, "진입")

        assertTrue(reporter.messages.isEmpty())
    }

    @Test
    fun `throwable 을 든 ERROR 는 non-fatal 로도 보고한다`() {
        val failure = IllegalStateException("boom")

        Logger.e(LogDomain.DRAFT_TASK, "초안 작업 실패", failure)

        assertEquals(listOf<Throwable>(failure), reporter.exceptions)
    }

    @Test
    fun `WARN 은 throwable 이 있어도 non-fatal 로 보고하지 않는다`() {
        // 이미 다룬 실패다. 보고하면 노이즈가 되어 진짜 신호를 덮는다.
        Logger.w(LogDomain.PUSH, "등록 재시도", IllegalStateException("boom"))

        assertTrue(reporter.exceptions.isEmpty())
        assertEquals(listOf("WARN/Push: 등록 재시도"), reporter.messages)
    }

    @Test
    fun `throwable 이 없는 ERROR 는 브레드크럼만 남긴다`() {
        Logger.e(LogDomain.NAVIGATION, "등록되지 않은 경로")

        assertTrue(reporter.exceptions.isEmpty())
        assertEquals(listOf("ERROR/Navigation: 등록되지 않은 경로"), reporter.messages)
    }

    @Test
    fun `대상이 없으면 아무 일도 일어나지 않는다`() {
        Logger.crashReporter = null

        Logger.e(LogDomain.MVI, "실패", IllegalStateException("boom"))
        Logger.setCrashKey("route", "home")

        assertTrue(reporter.messages.isEmpty())
        assertTrue(reporter.exceptions.isEmpty())
        assertTrue(reporter.keys.isEmpty())
    }

    @Test
    fun `꼬리표는 대상에 그대로 전달된다`() {
        Logger.setCrashKey("route", "home")
        Logger.setCrashKey("route", "timeline")

        assertEquals(listOf("route" to "home", "route" to "timeline"), reporter.keys)
    }

    private class RecordingCrashReporter : CrashReporter {
        val messages = mutableListOf<String>()
        val exceptions = mutableListOf<Throwable>()
        val keys = mutableListOf<Pair<String, String>>()

        override fun log(message: String) {
            messages += message
        }

        override fun recordException(throwable: Throwable) {
            exceptions += throwable
        }

        override fun setKey(
            key: String,
            value: String,
        ) {
            keys += key to value
        }
    }
}
