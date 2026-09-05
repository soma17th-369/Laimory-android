package com.soma369.laimory.core.domain.model.update

import com.soma369.laimory.core.domain.model.IntroInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class AppUpdateRequirementTest {
    @Test
    fun `하한선 미만이면 강제다`() {
        assertEquals(
            AppUpdateRequirement.Forced,
            AppUpdateRequirement.of(intro(min = 5, recommend = 5), installedVersion = 4),
        )
    }

    @Test
    fun `하한선과 같으면 막지 않는다`() {
        // 하한선은 "이 버전부터 쓸 수 있다" 는 뜻이다. 같은 값까지 막으면 방금 올린 버전이 잠긴다.
        assertEquals(
            AppUpdateRequirement.None,
            AppUpdateRequirement.of(intro(min = 5, recommend = 5), installedVersion = 5),
        )
    }

    @Test
    fun `권장선 미만이면 그 버전을 실어 권장한다`() {
        assertEquals(
            AppUpdateRequirement.Recommended(version = 7),
            AppUpdateRequirement.of(intro(min = 5, recommend = 7), installedVersion = 6),
        )
    }

    @Test
    fun `둘 다 넘어서면 아무것도 요구하지 않는다`() {
        assertEquals(
            AppUpdateRequirement.None,
            AppUpdateRequirement.of(intro(min = 5, recommend = 7), installedVersion = 8),
        )
    }

    @Test
    fun `서버 값이 어긋나도 강제가 이긴다`() {
        // minAppVersion > recommendAppVersion 은 서버 설정 실수지만, 두 선이 겹치면 더 강한 쪽을
        // 따르는 것이 안전하다.
        assertEquals(
            AppUpdateRequirement.Forced,
            AppUpdateRequirement.of(intro(min = 9, recommend = 3), installedVersion = 4),
        )
    }

    @Test
    fun `서버가 비운 값은 0 이라 아무도 막지 않는다`() {
        // DTO 변환이 null 을 0 으로 낮춘다. 필드가 비면 막지 않는 쪽이 안전하다.
        assertEquals(
            AppUpdateRequirement.None,
            AppUpdateRequirement.of(intro(min = 0, recommend = 0), installedVersion = 1),
        )
    }

    @Test
    fun `같은 권장 버전은 24시간 동안 숨는다`() {
        val dismissed = DismissedRecommendation(version = 7, at = NOW - Duration.ofHours(23))

        assertTrue(dismissed.hides(version = 7, now = NOW))
    }

    @Test
    fun `24시간이 지나면 다시 보인다`() {
        val dismissed = DismissedRecommendation(version = 7, at = NOW - Duration.ofHours(24))

        assertFalse(dismissed.hides(version = 7, now = NOW))
    }

    @Test
    fun `더 높은 권장 버전은 보류 기간 안이어도 보인다`() {
        // 미룬 것은 그 버전이지 앞으로의 모든 업데이트가 아니다.
        val dismissed = DismissedRecommendation(version = 7, at = NOW - Duration.ofMinutes(1))

        assertFalse(dismissed.hides(version = 8, now = NOW))
    }

    @Test
    fun `미룬 적이 없으면 숨기지 않는다`() {
        assertFalse(null.hides(version = 7, now = NOW))
    }

    @Test
    fun `보류 시각이 미래면 숨기지 않는다`() {
        // 기기 시계가 뒤로 돌아간 경우다. 얼마나 지났는지 모르는 기록으로 계속 감추지 않는다.
        val dismissed = DismissedRecommendation(version = 7, at = NOW + Duration.ofHours(1))

        assertFalse(dismissed.hides(version = 7, now = NOW))
    }

    private fun intro(
        min: Int,
        recommend: Int,
    ) = IntroInfo(minAppVersion = min, recommendAppVersion = recommend, debugTestMessage = "")

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-05T12:00:00Z")
    }
}
