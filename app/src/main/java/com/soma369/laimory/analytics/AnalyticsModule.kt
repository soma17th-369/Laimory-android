package com.soma369.laimory.analytics

import android.content.Context
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
    fun provideFirebaseAnalyticsBucket(firebaseAnalytics: FirebaseAnalytics): AnalyticsBucket = FirebaseAnalyticsBucket(firebaseAnalytics)
}
