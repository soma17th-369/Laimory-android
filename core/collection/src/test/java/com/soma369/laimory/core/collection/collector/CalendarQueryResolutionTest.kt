package com.soma369.laimory.core.collection.collector

import com.soma369.laimory.core.util.logging.Logger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class CalendarQueryResolutionTest {
    private var previousLogLevel = Logger.minLevel

    @Before
    fun setUp() {
        // android.util.Log 는 JVM 테스트에서 쓸 수 없다. 레벨을 올려 실제 출력 경로를 타지 않게 한다.
        previousLogLevel = Logger.minLevel
        Logger.minLevel = Logger.Level.ERROR
    }

    @After
    fun tearDown() {
        Logger.minLevel = previousLogLevel
    }

    @Test
    fun `조회에 성공하면 값을 그대로 돌려준다`() {
        assertEquals("cursor", resolveCalendarQuery("일정", Result.success("cursor")))
    }

    @Test
    fun `권한이 없으면 빈 결과로 돌아간다`() {
        // 수집기 계약이 "권한 없으면 예외 대신 빈 목록" 이라 여기서는 null 로 내려보낸다.
        assertNull(resolveCalendarQuery<String>("일정", Result.failure(SecurityException())))
    }

    @Test
    fun `권한을 통과한 뒤의 null 결과는 실제 실패로 올린다`() {
        // ContentResolver.query 는 provider 가 죽었을 때도 null 을 낸다. 이걸 빈 목록으로 바꾸면
        // 자동 수집이 "성공 0건" 으로 확정하고 최신성까지 갱신해 옛 데이터를 계속 쓴다.
        assertThrows(IllegalStateException::class.java) {
            resolveCalendarQuery<String>("일정", Result.success(null))
        }
    }

    @Test
    fun `권한 외 예외는 그대로 올린다`() {
        assertThrows(IllegalArgumentException::class.java) {
            resolveCalendarQuery<String>("일정", Result.failure(IllegalArgumentException("boom")))
        }
    }
}
