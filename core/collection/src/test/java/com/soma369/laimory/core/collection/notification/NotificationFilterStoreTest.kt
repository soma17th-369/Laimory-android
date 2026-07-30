package com.soma369.laimory.core.collection.notification

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.soma369.laimory.core.domain.model.collection.NotificationFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationFilterStoreTest {
    @Test
    fun `기존 ALL 모드 값은 무시하고 앱과 키워드만 복원한다`() {
        val preferences =
            preferencesOf(
                stringPreferencesKey("mode") to "ALL",
                stringSetPreferencesKey("keywords") to setOf("회의"),
                stringSetPreferencesKey("allowed_packages") to setOf("com.example.allowed"),
            )

        assertEquals(
            NotificationFilter(
                collectOnClick = true,
                keywords = setOf("회의"),
                allowedPackages = setOf("com.example.allowed"),
            ),
            preferences.toNotificationFilter(),
        )
    }

    @Test
    fun `클릭 수집 설정은 앱과 키워드 설정과 독립적으로 복원한다`() {
        val preferences =
            preferencesOf(
                booleanPreferencesKey("collect_on_click") to false,
                stringSetPreferencesKey("keywords") to setOf("예약"),
                stringSetPreferencesKey("allowed_packages") to setOf("com.example.calendar"),
            )

        assertEquals(
            NotificationFilter(
                collectOnClick = false,
                keywords = setOf("예약"),
                allowedPackages = setOf("com.example.calendar"),
            ),
            preferences.toNotificationFilter(),
        )
    }
}
