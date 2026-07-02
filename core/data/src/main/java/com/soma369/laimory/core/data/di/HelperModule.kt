package com.soma369.laimory.core.data.di

import com.soma369.laimory.core.data.helper.MessageHelperImpl
import com.soma369.laimory.core.domain.helper.MessageHelper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HelperModule {
    @Binds
    @Singleton
    abstract fun bindMessageHelper(impl: MessageHelperImpl): MessageHelper
}
