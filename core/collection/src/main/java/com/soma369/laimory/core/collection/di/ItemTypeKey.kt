package com.soma369.laimory.core.collection.di

import com.soma369.laimory.core.domain.model.collection.ItemType
import dagger.MapKey

/**
 * Collector 멀티바인딩의 맵 키. `Map<ItemType, Collector>` 레지스트리를 구성해
 * 각 UseCase 가 담당 카테고리 수집기를 [ItemType] 으로 찾도록 한다.
 */
@MapKey
annotation class ItemTypeKey(val value: ItemType)
