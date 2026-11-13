package com.soma369.laimory.apiorigin

import android.content.Context
import com.soma369.laimory.core.data.network.ApiOriginOverride
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/** debug 에서만 서버 주소를 덮는다. release·qa 에는 이 모듈이 없어 `core:data` 의 optional 바인딩이 비어 있다. */
@Module
@InstallIn(SingletonComponent::class)
object DebugApiOriginModule {
    @Provides
    fun provideApiOriginOverride(
        @ApplicationContext context: Context,
    ): ApiOriginOverride = DebugApiOriginStore(context)
}
