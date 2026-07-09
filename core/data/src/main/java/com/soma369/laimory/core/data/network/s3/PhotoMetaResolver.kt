package com.soma369.laimory.core.data.network.s3

/** `content://` 사진 URI 에서 업로드에 필요한 MIME/바이트 크기를 산출한다. */
interface PhotoMetaResolver {
    /**
     * MIME/크기를 산출한다. `ContentResolver` IO 이므로 구현은 IO dispatcher 에서 실행한다.
     * MIME/크기 미확보·URI open/권한 실패는 `ApiException` 으로 정규화해 던진다(size 미확보 시 업로드 중단).
     */
    suspend fun resolve(clientPhotoUri: String): PhotoMeta
}
