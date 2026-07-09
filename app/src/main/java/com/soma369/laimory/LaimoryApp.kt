package com.soma369.laimory

import android.app.Application
import com.soma369.laimory.core.collection.health.SleepDetectionEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LaimoryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Sleep API 감지 구독. 권한/Play Services 가 있으면 구독하고, 시스템 구독이라 앱이 죽어도 유지된다.
        EntryPointAccessors
            .fromApplication(this, SleepDetectionEntryPoint::class.java)
            .sleepDetectionSubscriber()
            .start()
    }
}
