package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.settings.AppThemeMode
import kotlinx.coroutines.flow.Flow

/** 기기에 저장하는 화면 모드. 서버와 동기화하지 않는다. */
interface AppThemeRepository {
    val themeMode: Flow<AppThemeMode>

    suspend fun setThemeMode(mode: AppThemeMode)
}
