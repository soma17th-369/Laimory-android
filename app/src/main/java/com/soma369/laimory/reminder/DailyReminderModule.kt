package com.soma369.laimory.reminder

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DailyReminderModule {
    @Binds
    abstract fun bindDailyReminderAlarms(impl: DailyReminderAlarmScheduler): DailyReminderAlarms

    @Binds
    abstract fun bindDailyReminderNotifications(impl: DailyReminderNotifier): DailyReminderNotifications
}
