package com.soma369.laimory.core.domain.model.user

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserProfileTest {
    @Test
    fun `닉네임이 없으면 표시할 값도 없다`() {
        assertNull(UserProfile.of(null).nickname)
    }

    @Test
    fun `공백뿐인 닉네임은 없는 것으로 본다`() {
        // 그대로 두면 화면에 `안녕하세요,  님` 처럼 샌다.
        assertNull(UserProfile.of("   ").nickname)
        assertNull(UserProfile.of("\n\t").nickname)
    }

    @Test
    fun `앞뒤 공백은 다듬고 값은 지킨다`() {
        assertEquals("김소마", UserProfile.of("  김소마 ").nickname)
    }
}
