package com.soma369.laimory.core.data.analytics

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.soma369.laimory.core.data.di.AnalyticsConsentDataStore
import com.soma369.laimory.core.domain.model.analytics.AnalyticsConsent
import com.soma369.laimory.core.domain.repository.AnalyticsConsentRepository
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 수집 동의를 설치 단위로 저장한다.
 *
 * 읽지 못하거나 모르는 값이면 **미결정**으로 본다 — 동의로 기울면 묻지도 않고 모으게 된다.
 */
@Singleton
internal class PreferencesAnalyticsConsentRepository
    @Inject
    constructor(
        @AnalyticsConsentDataStore private val dataStore: DataStore<Preferences>,
    ) : AnalyticsConsentRepository {
        override val consent: Flow<AnalyticsConsent> =
            dataStore.data
                .map { preferences -> preferences[KEY_CONSENT].toConsent() }
                .catch { error ->
                    Logger.w(LogDomain.ANALYTICS, "수집 동의를 읽지 못했다: ${error::class.simpleName}")
                    emit(AnalyticsConsent.UNDECIDED)
                }

        override suspend fun set(consent: AnalyticsConsent) {
            dataStore.edit { preferences -> preferences[KEY_CONSENT] = consent.storedValue }
        }

        private fun String?.toConsent(): AnalyticsConsent =
            AnalyticsConsent.entries.firstOrNull { consent -> consent.storedValue == this } ?: AnalyticsConsent.UNDECIDED

        /** 저장 값도 상수 이름에 묶지 않는다. 이름을 바꾸면 기존 사용자의 동의가 미결정으로 돌아간다. */
        private val AnalyticsConsent.storedValue: String
            get() =
                when (this) {
                    AnalyticsConsent.UNDECIDED -> "undecided"
                    AnalyticsConsent.GRANTED -> "granted"
                    AnalyticsConsent.DENIED -> "denied"
                }

        private companion object {
            val KEY_CONSENT = stringPreferencesKey("analytics_consent")
        }
    }
