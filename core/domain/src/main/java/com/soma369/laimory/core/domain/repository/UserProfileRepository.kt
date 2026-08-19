package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.user.UserProfile

/** 서버 회원 정보 조회 계약. */
interface UserProfileRepository {
    /** 현재 인증 세션 계정의 회원 정보를 조회한다. */
    suspend fun getMyProfile(): UserProfile
}
