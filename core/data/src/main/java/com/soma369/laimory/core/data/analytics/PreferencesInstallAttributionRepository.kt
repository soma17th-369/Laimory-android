package com.soma369.laimory.core.data.analytics

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.soma369.laimory.core.data.di.AnalyticsInstallAttributionDataStore
import com.soma369.laimory.core.domain.model.analytics.InstallAttribution
import com.soma369.laimory.core.domain.model.analytics.InstallAttributionRecord
import com.soma369.laimory.core.domain.model.analytics.InstallCampaign
import com.soma369.laimory.core.domain.model.analytics.InstallReferrerStatus
import com.soma369.laimory.core.domain.repository.InstallAttributionRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 설치 귀속을 DataStore 에 남긴다. 정제값·상태·시각·실패한 실행 수만 두고 원문 referrer 는 두지 않는다.
 *
 * 상태는 저장용 문자열로 적는다 — enum 이름을 그대로 쓰면 이름을 바꾸는 순간 이미 저장한 결과를 못 읽는다.
 */
@Singleton
internal class PreferencesInstallAttributionRepository
    @Inject
    constructor(
        @AnalyticsInstallAttributionDataStore private val dataStore: DataStore<Preferences>,
    ) : InstallAttributionRepository {
        override suspend fun load(): InstallAttributionRecord {
            var record: InstallAttributionRecord? = null
            // 설치 구분 값 만들기와 읽기를 한 번의 edit 안에서 한다 — 동시에 불려도 값이 둘로 갈리지 않는다.
            dataStore.edit { preferences ->
                val installId = preferences[INSTALL_ID] ?: UUID.randomUUID().toString().also { preferences[INSTALL_ID] = it }
                record =
                    InstallAttributionRecord(
                        installId = installId,
                        attribution = preferences.readAttribution(),
                        failedLaunchCount = preferences[FAILED_LAUNCH_COUNT] ?: 0,
                    )
            }
            return checkNotNull(record)
        }

        override suspend fun saveResolved(attribution: InstallAttribution) {
            dataStore.edit { preferences ->
                preferences[STATUS] = attribution.status.storedValue
                preferences.putOrRemove(SOURCE, attribution.campaign?.source)
                preferences.putOrRemove(MEDIUM, attribution.campaign?.medium)
                preferences.putOrRemove(CAMPAIGN, attribution.campaign?.campaign)
                preferences.putOrRemove(CONTENT, attribution.campaign?.content)
                preferences.putOrRemove(CAMPAIGN_ID, attribution.campaign?.campaignId)
                preferences.putOrRemove(CLICK_AT_MILLIS, attribution.clickAtMillis)
                preferences.putOrRemove(INSTALL_BEGIN_AT_MILLIS, attribution.installBeginAtMillis)
            }
        }

        override suspend fun countFailedLaunch(): Int {
            var count = 0
            dataStore.edit { preferences ->
                count = (preferences[FAILED_LAUNCH_COUNT] ?: 0) + 1
                preferences[FAILED_LAUNCH_COUNT] = count
            }
            return count
        }

        /** 알 수 없는 상태나 상태와 맞지 않는 값이 남아 있으면 결과가 없는 것으로 보고 다시 조회한다. */
        private fun Preferences.readAttribution(): InstallAttribution? {
            val status = this[STATUS]?.let(::statusOf) ?: return null
            val source = this[SOURCE]
            if (status.carriesCampaign && source == null) return null
            return InstallAttribution(
                status = status,
                campaign =
                    source?.takeIf { status.carriesCampaign }?.let {
                        InstallCampaign(
                            source = it,
                            medium = this[MEDIUM],
                            campaign = this[CAMPAIGN],
                            content = this[CONTENT],
                            campaignId = this[CAMPAIGN_ID],
                        )
                    },
                clickAtMillis = this[CLICK_AT_MILLIS],
                installBeginAtMillis = this[INSTALL_BEGIN_AT_MILLIS],
            )
        }

        private fun <T> MutablePreferences.putOrRemove(
            key: Preferences.Key<T>,
            value: T?,
        ) {
            if (value == null) remove(key) else this[key] = value
        }

        private companion object {
            val INSTALL_ID = stringPreferencesKey("install_id")
            val STATUS = stringPreferencesKey("referrer_status")
            val SOURCE = stringPreferencesKey("utm_source")
            val MEDIUM = stringPreferencesKey("utm_medium")
            val CAMPAIGN = stringPreferencesKey("utm_campaign")
            val CONTENT = stringPreferencesKey("utm_content")
            val CAMPAIGN_ID = stringPreferencesKey("utm_id")
            val CLICK_AT_MILLIS = longPreferencesKey("referrer_click_at_ms")
            val INSTALL_BEGIN_AT_MILLIS = longPreferencesKey("install_begin_at_ms")
            val FAILED_LAUNCH_COUNT = intPreferencesKey("failed_launch_count")

            fun statusOf(stored: String): InstallReferrerStatus? = InstallReferrerStatus.entries.firstOrNull { it.storedValue == stored }
        }
    }

private val InstallReferrerStatus.storedValue: String
    get() =
        when (this) {
            InstallReferrerStatus.PARSED -> "parsed"
            InstallReferrerStatus.PROVIDER -> "provider"
            InstallReferrerStatus.NO_CAMPAIGN -> "no_campaign"
            InstallReferrerStatus.INVALID -> "invalid"
            InstallReferrerStatus.UNAVAILABLE -> "unavailable"
        }
