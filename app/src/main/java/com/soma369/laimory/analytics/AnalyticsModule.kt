package com.soma369.laimory.analytics

import android.content.Context
import android.content.pm.PackageManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.soma369.laimory.core.data.analytics.AnalyticsBucket
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * 전송 대상을 꽂는 자리. Firebase 의존성은 app 모듈에만 둔다.
 *
 * 버킷을 Set 으로 모으므로 대상을 옮길 때 두 곳으로 함께 보내 비교한 뒤 옛 버킷을 뺄 수 있다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AnalyticsModule {
    @Provides
    @Singleton
    fun provideFirebaseAnalytics(
        @ApplicationContext context: Context,
    ): FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    @Provides
    @Singleton
    @IntoSet
    fun provideFirebaseAnalyticsBucket(
        @ApplicationContext context: Context,
        firebaseAnalytics: FirebaseAnalytics,
    ): AnalyticsBucket =
        FirebaseAnalyticsBucket(
            firebaseAnalytics = firebaseAnalytics,
            initiallyEnabled = context.manifestAnalyticsCollectionEnabled(),
        )

    /**
     * 매니페스트가 정한 수집 시작 상태를 읽는다.
     *
     * 같은 값을 코드에 또 적으면 빌드 타입별 설정과 조용히 어긋난다. SDK 도 이 값을 읽어 시작하므로
     * 출처를 하나로 둔다. 읽지 못하면 켜짐으로 보지 않는다 — 모르는 상태에서 수집을 켜는 쪽으로
     * 기울면 동의 없이 모으게 된다.
     */
    private fun Context.manifestAnalyticsCollectionEnabled(): Boolean =
        runCatching {
            packageManager
                .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                .metaData
                ?.getBoolean(COLLECTION_ENABLED_META_DATA, false) == true
        }.getOrDefault(false)

    private const val COLLECTION_ENABLED_META_DATA = "firebase_analytics_collection_enabled"
}
