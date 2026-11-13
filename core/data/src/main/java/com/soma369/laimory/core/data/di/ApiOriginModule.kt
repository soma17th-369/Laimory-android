package com.soma369.laimory.core.data.di

import com.soma369.laimory.core.data.BuildConfig
import com.soma369.laimory.core.data.network.ApiOriginOverride
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Optional
import javax.inject.Singleton

/** 앱이 붙는 서버 origin 을 한 곳에서 정한다. 덮어쓸 값이 없으면(release·qa) 빌드 값이다. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ApiOriginModule {
    @BindsOptionalOf
    abstract fun optionalApiOriginOverride(): ApiOriginOverride

    companion object {
        @Provides
        @Singleton
        @ApiBaseUrl
        fun provideApiBaseUrl(override: Optional<ApiOriginOverride>): String = override.map { it.baseUrl }.orElse(BuildConfig.BASE_URL)
    }
}
