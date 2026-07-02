package com.soma369.laimory.notification

import com.soma369.laimory.core.domain.notification.MessageHelper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds
    @Singleton
    abstract fun bindMessageHelper(impl: MessageHelperImpl): MessageHelper
}
