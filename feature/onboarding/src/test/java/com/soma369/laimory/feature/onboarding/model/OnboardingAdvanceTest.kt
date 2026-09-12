package com.soma369.laimory.feature.onboarding.model

import com.soma369.laimory.core.ui.permission.DataPermission
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 허용 뒤 다음 장으로 넘기는 조건을 고정한다.
 *
 * 넓으면 뒤로 돌아본 사용자가 곧장 앞으로 밀려나고, 좁으면 허용하고도 버튼을 한 번 더 눌러야 한다.
 */
class OnboardingAdvanceTest {
    @Test
    fun `요청을 보낸 장이 허용되면 넘긴다`() {
        assertTrue(
            advancesAfterGrant(
                permission = DataPermission.PHOTO,
                isPageDone = true,
                wasRequestedHere = true,
                isLastPage = false,
            ),
        )
    }

    @Test
    fun `이미 허용된 장에 도착했을 때는 넘기지 않는다`() {
        // 뒤로 넘겨 다시 보는 사용자를 앞으로 밀어내면 되돌아볼 수 없다.
        assertFalse(
            advancesAfterGrant(
                permission = DataPermission.PHOTO,
                isPageDone = true,
                wasRequestedHere = false,
                isLastPage = false,
            ),
        )
    }

    @Test
    fun `아직 받을 것이 남았으면 넘기지 않는다`() {
        // 위치의 `앱 사용 중에만` 처럼 요청은 보냈지만 장이 끝나지 않은 경우다.
        assertFalse(
            advancesAfterGrant(
                permission = DataPermission.LOCATION,
                isPageDone = false,
                wasRequestedHere = true,
                isLastPage = false,
            ),
        )
    }

    @Test
    fun `마지막 장에서는 넘기지 않는다`() {
        assertFalse(
            advancesAfterGrant(
                permission = DataPermission.APP_NOTIFICATION,
                isPageDone = true,
                wasRequestedHere = true,
                isLastPage = true,
            ),
        )
    }

    @Test
    fun `권한이 없는 안내 장은 넘길 판단을 하지 않는다`() {
        assertFalse(
            advancesAfterGrant(
                permission = null,
                isPageDone = true,
                wasRequestedHere = true,
                isLastPage = false,
            ),
        )
    }
}
