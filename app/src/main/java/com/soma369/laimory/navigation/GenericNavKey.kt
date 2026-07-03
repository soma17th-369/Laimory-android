package com.soma369.laimory.navigation

import androidx.navigation3.runtime.NavKey
import com.soma369.laimory.core.domain.navigation.NavRoute
import kotlinx.serialization.Serializable

/**
 * Navigation3 의 단일 백스택 엔트리 타입.
 *
 * 모든 목적지가 본 타입 하나로 표현되고, 실제 화면 분기는 [path] 로 결정한다([appRouteByPath]).
 * [args] 는 [NavRoute.args] 와 동일한 String 맵이라 백스택 직렬화/복원이 그대로 동작한다.
 */
@Serializable
data class GenericNavKey(
    val path: String,
    val args: Map<String, String> = emptyMap(),
) : NavKey {
    fun toNavRoute(): NavRoute = NavRoute(path, args)

    companion object {
        fun of(route: NavRoute): GenericNavKey = GenericNavKey(route.path, route.args)
    }
}
