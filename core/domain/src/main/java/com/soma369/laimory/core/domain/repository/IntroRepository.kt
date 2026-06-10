package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.IntroInfo

interface IntroRepository {
    suspend fun getIntroInfo(): IntroInfo
}
