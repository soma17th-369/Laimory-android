package com.soma369.laimory.core.data.session

/** 인증 세션 원문을 저장 가능한 문자열 암호문으로 변환하고 복원하는 계약. */
internal interface SessionCipher {
    /** [plainText]를 암호화해 복호화에 필요한 메타데이터를 포함한 문자열로 반환한다. */
    fun encrypt(plainText: ByteArray): String

    /** [cipherText]를 복호화하며 유효하지 않거나 복호화할 수 없는 값이면 예외를 던진다. */
    fun decrypt(cipherText: String): ByteArray
}
