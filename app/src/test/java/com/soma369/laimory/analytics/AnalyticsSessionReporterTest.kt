package com.soma369.laimory.analytics

import com.soma369.laimory.core.domain.model.auth.SignedInAccount
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.model.user.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalyticsSessionReporterTest {
    private val account = SignedInAccount(provider = SocialLoginProvider.GOOGLE)

    @Test
    fun `회원 식별자를 받으면 그 회원으로 건다`() {
        assertEquals(
            AnalyticsSessionChange.Assign(42L),
            analyticsSessionChange(account, UserProfile.of("김소마", userId = 42L)),
        )
    }

    @Test
    fun `세션이 없으면 사용자 구분을 풀고 편집 흔적을 비운다`() {
        assertEquals(AnalyticsSessionChange.Clear, analyticsSessionChange(account = null, profile = null))
    }

    @Test
    fun `로그아웃 직후 회원 정보가 남아 있어도 푼다`() {
        // 세션 전이와 회원 정보 비우기는 따로 방출된다. 세션이 먼저 사라진 순간에 옛 회원으로 다시 걸면 안 된다.
        assertEquals(
            AnalyticsSessionChange.Clear,
            analyticsSessionChange(account = null, profile = UserProfile.of("김소마", userId = 42L)),
        )
    }

    @Test
    fun `세션은 있는데 회원 정보를 아직 못 받았으면 그대로 둔다`() {
        // 앱 시작 직후다. 여기서 풀면 SDK 가 기억하던 값이 지워진다.
        assertNull(analyticsSessionChange(account, profile = null))
    }

    @Test
    fun `회원 식별자를 주지 않는 서버에서는 그대로 둔다`() {
        assertNull(analyticsSessionChange(account, UserProfile.of("김소마", userId = null)))
    }
}
