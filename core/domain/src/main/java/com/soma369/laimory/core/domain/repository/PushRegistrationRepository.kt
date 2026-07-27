package com.soma369.laimory.core.domain.repository

/**
 * 인증 사용자와 현재 앱 설치의 FCM 수신 대상을 연결하거나 해제한다.
 *
 * 서버는 같은 FID의 반복 등록과 해제를 멱등하게 처리하고, 다른 사용자가 같은 FID를 등록하면 현재 사용자로
 * 소유권을 원자적으로 재결합한다. 강제 세션 만료처럼 인증된 해제 요청을 보낼 수 없는 경로의 등록 정리는
 * 서버 정책의 책임이며 클라이언트에서 보장할 수 없다.
 */
interface PushRegistrationRepository {
    /**
     * 현재 인증 사용자에게 앱 설치를 FCM 수신 대상으로 연결한다.
     *
     * @param firebaseInstallationId Firebase가 현재 앱 설치에 발급한 FID
     */
    suspend fun register(firebaseInstallationId: String)

    /**
     * 현재 인증 사용자와 앱 설치의 FCM 수신 연결을 해제한다.
     *
     * Firebase 설치 자체나 FID를 삭제하지는 않는다.
     *
     * @param firebaseInstallationId 연결을 해제할 현재 앱 설치의 FID
     */
    suspend fun unregister(firebaseInstallationId: String)
}
