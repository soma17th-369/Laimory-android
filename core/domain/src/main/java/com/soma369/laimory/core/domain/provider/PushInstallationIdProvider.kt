package com.soma369.laimory.core.domain.provider

/**
 * 현재 앱 설치를 식별하는 Firebase Installation ID(FID)를 제공한다.
 *
 * FID는 서버가 특정 앱 설치로 FCM 메시지를 전송할 때 사용하는 opaque 식별자다.
 * 인증정보는 아니지만 로그나 사용자 화면에 원문을 노출하지 않는다.
 */
fun interface PushInstallationIdProvider {
    /**
     * 현재 Firebase 앱 설치에 할당된 FID를 반환한다.
     *
     * @return Firebase가 발급한 현재 설치의 FID
     */
    suspend fun getCurrentId(): String
}
