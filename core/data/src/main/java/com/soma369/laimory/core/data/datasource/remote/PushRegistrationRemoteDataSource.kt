package com.soma369.laimory.core.data.datasource.remote

/** Push Registration API 호출을 캡슐화하는 원격 데이터 소스다. */
interface PushRegistrationRemoteDataSource {
    /**
     * 인증 사용자의 FCM 수신 대상으로 FID를 등록한다.
     *
     * @param firebaseInstallationId 등록할 앱 설치의 FID
     */
    suspend fun register(firebaseInstallationId: String)

    /**
     * 인증 사용자의 FCM 수신 대상에서 FID를 해제한다.
     *
     * @param firebaseInstallationId 해제할 앱 설치의 FID
     */
    suspend fun unregister(firebaseInstallationId: String)
}
