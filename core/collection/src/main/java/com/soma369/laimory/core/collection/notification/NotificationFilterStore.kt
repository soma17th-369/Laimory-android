package com.soma369.laimory.core.collection.notification

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.soma369.laimory.core.domain.model.collection.NotificationCollectMode
import com.soma369.laimory.core.domain.model.collection.NotificationFilter
import com.soma369.laimory.core.domain.repository.NotificationFilterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** 알림 수집 필터를 Preferences DataStore 에 저장하는 [NotificationFilterRepository] 구현. */
internal class NotificationFilterStore
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : NotificationFilterRepository {
        override fun observe(): Flow<NotificationFilter> =
            dataStore.data.map { prefs ->
                NotificationFilter(
                    mode =
                        prefs[KEY_MODE]
                            ?.let { runCatching { NotificationCollectMode.valueOf(it) }.getOrNull() }
                            ?: NotificationCollectMode.SELECTIVE,
                    keywords = prefs[KEY_KEYWORDS].orEmpty(),
                    allowedPackages = prefs[KEY_PACKAGES].orEmpty(),
                )
            }

        override suspend fun update(filter: NotificationFilter) {
            dataStore.edit { prefs ->
                prefs[KEY_MODE] = filter.mode.name
                prefs[KEY_KEYWORDS] = filter.keywords
                prefs[KEY_PACKAGES] = filter.allowedPackages
            }
        }

        private companion object {
            val KEY_MODE = stringPreferencesKey("mode")
            val KEY_KEYWORDS = stringSetPreferencesKey("keywords")
            val KEY_PACKAGES = stringSetPreferencesKey("allowed_packages")
        }
    }
