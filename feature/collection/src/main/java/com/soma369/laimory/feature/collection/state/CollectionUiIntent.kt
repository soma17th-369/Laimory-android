package com.soma369.laimory.feature.collection.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface CollectionUiIntent : UiIntent {
    /** 저장 파이프라인 검증용 테스트 아이템을 삽입한다. (수집기 연결 전 디버그 경로) */
    data object InsertTestItems : CollectionUiIntent

    /** 사진 수집기를 실행해 MediaStore 사진 메타데이터를 저장한다. (권한 허용 후 호출) */
    data object CollectPhotos : CollectionUiIntent
}
