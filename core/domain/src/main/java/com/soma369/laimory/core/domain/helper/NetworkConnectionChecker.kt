package com.soma369.laimory.core.domain.helper

/**
 * 지금 인터넷에 나갈 수 있는 연결이 있는지 묻는 포트.
 *
 * 요청을 보내기 전에 오프라인을 미리 걸러 사용자에게 바로 알리는 용도다. 연결이 있다고 해서 요청이 성공한다는
 * 보장은 아니므로, 요청 실패 처리는 따로 둔다.
 */
fun interface NetworkConnectionChecker {
    fun isConnected(): Boolean
}
