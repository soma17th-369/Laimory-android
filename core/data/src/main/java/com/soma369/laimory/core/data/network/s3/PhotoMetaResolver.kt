package com.soma369.laimory.core.data.network.s3

/** `content://` 사진 URI 에서 업로드에 필요한 MIME/바이트 크기를 산출한다. */
interface PhotoMetaResolver {
    /** MIME/크기를 못 구하면 `ApiException` 으로 던진다(size 미확보 시 업로드 중단). */
    fun resolve(clientPhotoUri: String): PhotoMeta
}
