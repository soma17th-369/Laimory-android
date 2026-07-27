package com.soma369.laimory.core.domain.repository

/**
 * 인증 사용자와 현재 앱 설치의 FCM 수신 대상을 연결하거나 해제한다.
 *
 * 같은 FID의 반복 등록과 해제는 서버에서 멱등하게 처리되는 것을 전제로 한다.
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
