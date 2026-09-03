package com.soma369.laimory.core.data.di

import com.soma369.laimory.core.data.datasource.remote.AuthRemoteDataSource
import com.soma369.laimory.core.data.datasource.remote.AuthRemoteDataSourceImpl
import com.soma369.laimory.core.data.datasource.remote.Feature1RemoteDataSource
import com.soma369.laimory.core.data.datasource.remote.Feature1RemoteDataSourceImpl
import com.soma369.laimory.core.data.datasource.remote.IntroRemoteDataSource
import com.soma369.laimory.core.data.datasource.remote.IntroRemoteDataSourceImpl
import com.soma369.laimory.core.data.datasource.remote.OnboardingRemoteDataSource
import com.soma369.laimory.core.data.datasource.remote.OnboardingRemoteDataSourceImpl
import com.soma369.laimory.core.data.datasource.remote.PushRegistrationRemoteDataSource
import com.soma369.laimory.core.data.datasource.remote.PushRegistrationRemoteDataSourceImpl
import com.soma369.laimory.core.data.datasource.remote.PushSettingsRemoteDataSource
import com.soma369.laimory.core.data.datasource.remote.PushSettingsRemoteDataSourceImpl
import com.soma369.laimory.core.data.datasource.remote.TermsRemoteDataSource
import com.soma369.laimory.core.data.datasource.remote.TermsRemoteDataSourceImpl
import com.soma369.laimory.core.data.datasource.remote.TimelineDraftRemoteDataSource
import com.soma369.laimory.core.data.datasource.remote.TimelineDraftRemoteDataSourceImpl
import com.soma369.laimory.core.data.datasource.remote.TimelineRecordRemoteDataSource
import com.soma369.laimory.core.data.datasource.remote.TimelineRecordRemoteDataSourceImpl
import com.soma369.laimory.core.data.datasource.remote.UserRemoteDataSource
import com.soma369.laimory.core.data.datasource.remote.UserRemoteDataSourceImpl
import com.soma369.laimory.core.data.network.s3.PhotoMetaResolver
import com.soma369.laimory.core.data.network.s3.PhotoMetaResolverImpl
import com.soma369.laimory.core.data.network.s3.S3PhotoUploader
import com.soma369.laimory.core.data.network.s3.S3PhotoUploaderImpl
import com.soma369.laimory.core.data.session.AndroidKeystoreSessionCipher
import com.soma369.laimory.core.data.session.EncryptedTokenSessionStore
import com.soma369.laimory.core.data.session.SessionCipher
import com.soma369.laimory.core.data.session.TokenSessionStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {
    @Binds
    @Singleton
    internal abstract fun bindAuthRemoteDataSource(impl: AuthRemoteDataSourceImpl): AuthRemoteDataSource

    @Binds
    @Singleton
    internal abstract fun bindTokenSessionStore(impl: EncryptedTokenSessionStore): TokenSessionStore

    @Binds
    @Singleton
    internal abstract fun bindSessionCipher(impl: AndroidKeystoreSessionCipher): SessionCipher

    @Binds
    @Singleton
    abstract fun bindFeature1RemoteDataSource(impl: Feature1RemoteDataSourceImpl): Feature1RemoteDataSource

    @Binds
    @Singleton
    abstract fun bindIntroRemoteDataSource(impl: IntroRemoteDataSourceImpl): IntroRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindOnboardingRemoteDataSource(impl: OnboardingRemoteDataSourceImpl): OnboardingRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindPushRegistrationRemoteDataSource(impl: PushRegistrationRemoteDataSourceImpl): PushRegistrationRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindPushSettingsRemoteDataSource(impl: PushSettingsRemoteDataSourceImpl): PushSettingsRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindTimelineDraftRemoteDataSource(impl: TimelineDraftRemoteDataSourceImpl): TimelineDraftRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindTimelineRecordRemoteDataSource(impl: TimelineRecordRemoteDataSourceImpl): TimelineRecordRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindS3PhotoUploader(impl: S3PhotoUploaderImpl): S3PhotoUploader

    @Binds
    @Singleton
    abstract fun bindPhotoMetaResolver(impl: PhotoMetaResolverImpl): PhotoMetaResolver

    @Binds
    @Singleton
    abstract fun bindUserRemoteDataSource(impl: UserRemoteDataSourceImpl): UserRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindTermsRemoteDataSource(impl: TermsRemoteDataSourceImpl): TermsRemoteDataSource
}
