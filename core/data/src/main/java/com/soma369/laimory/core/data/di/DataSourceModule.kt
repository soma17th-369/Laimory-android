package com.soma369.laimory.core.data.di

import com.soma369.laimory.core.data.datasource.remote.Feature1RemoteDataSource
import com.soma369.laimory.core.data.datasource.remote.Feature1RemoteDataSourceImpl
import com.soma369.laimory.core.data.datasource.remote.IntroRemoteDataSource
import com.soma369.laimory.core.data.datasource.remote.IntroRemoteDataSourceImpl
import com.soma369.laimory.core.data.datasource.remote.TimelineDraftRemoteDataSource
import com.soma369.laimory.core.data.datasource.remote.TimelineDraftRemoteDataSourceImpl
import com.soma369.laimory.core.data.network.s3.PhotoMetaResolver
import com.soma369.laimory.core.data.network.s3.PhotoMetaResolverImpl
import com.soma369.laimory.core.data.network.s3.S3PhotoUploader
import com.soma369.laimory.core.data.network.s3.S3PhotoUploaderImpl
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
    abstract fun bindFeature1RemoteDataSource(impl: Feature1RemoteDataSourceImpl): Feature1RemoteDataSource

    @Binds
    @Singleton
    abstract fun bindIntroRemoteDataSource(impl: IntroRemoteDataSourceImpl): IntroRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindTimelineDraftRemoteDataSource(impl: TimelineDraftRemoteDataSourceImpl): TimelineDraftRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindS3PhotoUploader(impl: S3PhotoUploaderImpl): S3PhotoUploader

    @Binds
    @Singleton
    abstract fun bindPhotoMetaResolver(impl: PhotoMetaResolverImpl): PhotoMetaResolver
}
