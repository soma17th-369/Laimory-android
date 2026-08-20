package com.soma369.laimory.core.collection.notification

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.soma369.laimory.core.domain.model.collection.NotificationFilter
import com.soma369.laimory.core.domain.repository.NotificationFilterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val KEY_COLLECT_ON_CLICK = booleanPreferencesKey("collect_on_click")
private val KEY_USE_DEFAULT_KEYWORDS = booleanPreferencesKey("use_default_keywords")
private val KEY_KEYWORDS = stringSetPreferencesKey("keywords")
private val KEY_PACKAGES = stringSetPreferencesKey("allowed_packages")

/** 알림 수집 필터를 Preferences DataStore 에 저장하는 [NotificationFilterRepository] 구현. */
internal class NotificationFilterStore
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : NotificationFilterRepository {
        override fun observe(): Flow<NotificationFilter> = dataStore.data.map(Preferences::toNotificationFilter)

        override suspend fun update(filter: NotificationFilter) {
            dataStore.edit { prefs ->
                prefs[KEY_COLLECT_ON_CLICK] = filter.collectOnClick
                prefs[KEY_USE_DEFAULT_KEYWORDS] = filter.useDefaultKeywords
                prefs[KEY_KEYWORDS] = filter.keywords
                prefs[KEY_PACKAGES] = filter.allowedPackages
            }
        }
    }

/**
 * 과거 DataStore의 `mode=ALL` 값은 의도적으로 읽지 않는다.
 *
 * 단일 선택 수집 정책에는 모드가 없으므로 기존 키가 파일에 남아 있어도 필터 결과에 영향을 주지 않는다.
 */
internal fun Preferences.toNotificationFilter(): NotificationFilter =
    NotificationFilter(
        collectOnClick = this[KEY_COLLECT_ON_CLICK] ?: true,
        // 기존 사용자에게도 기본 사전을 켜서 적용한다 — 저장된 값이 없으면 활성으로 읽는다.
        useDefaultKeywords = this[KEY_USE_DEFAULT_KEYWORDS] ?: true,
        keywords = this[KEY_KEYWORDS].orEmpty(),
        allowedPackages = this[KEY_PACKAGES].orEmpty(),
    )
