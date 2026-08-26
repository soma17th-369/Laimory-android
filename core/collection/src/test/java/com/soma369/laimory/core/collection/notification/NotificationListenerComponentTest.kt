package com.soma369.laimory.core.collection.notification

import com.soma369.laimory.core.util.permission.NotificationListenerAccess
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationListenerComponentTest {
    @Test
    fun `권한 판정이 가리키는 클래스 이름이 실제 리스너와 같다`() {
        // core:permission 은 이 모듈을 의존하지 않아 문자열로 서비스를 가리킨다. 이름이 어긋나면
        // 예외 없이 조용히 항상 미허용이 되고, 알림 수집이 켜지지 않는 이유를 화면에서 알 수 없다.
        assertEquals(
            LaimoryNotificationListenerService::class.java.name,
            NotificationListenerAccess.LISTENER_SERVICE_CLASS,
        )
    }
}
