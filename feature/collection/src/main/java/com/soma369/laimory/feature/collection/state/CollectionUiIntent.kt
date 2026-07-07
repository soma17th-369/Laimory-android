package com.soma369.laimory.feature.collection.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface CollectionUiIntent : UiIntent {
    /** 저장 파이프라인 검증용 테스트 아이템을 삽입한다. (수집기 연결 전 디버그 경로) */
    data object InsertTestItems : CollectionUiIntent
}
