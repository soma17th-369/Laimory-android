package com.soma369.laimory.feature.collection.viewmodel

import com.soma369.laimory.core.domain.model.collection.LocationPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.usecase.AddSourceItemsUseCase
import com.soma369.laimory.core.domain.usecase.CollectPhotosUseCase
import com.soma369.laimory.core.domain.usecase.ObserveSourceItemsUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.collection.state.CollectionUiIntent
import com.soma369.laimory.feature.collection.state.CollectionUiSideEffect
import com.soma369.laimory.feature.collection.state.CollectionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel
    @Inject
    constructor(
        observeSourceItemsUseCase: ObserveSourceItemsUseCase,
        private val addSourceItemsUseCase: AddSourceItemsUseCase,
        private val collectPhotosUseCase: CollectPhotosUseCase,
    ) : BaseMviViewModel<CollectionUiState, CollectionUiIntent, CollectionUiSideEffect>(CollectionUiState()) {
        init {
            safeLaunch {
                observeSourceItemsUseCase().collect { items ->
                    updateState { copy(isLoading = false, items = items) }
                }
            }
        }

        override suspend fun handleIntent(intent: CollectionUiIntent) {
            when (intent) {
                CollectionUiIntent.InsertTestItems -> insertTestItems()
                CollectionUiIntent.CollectPhotos -> collectPhotos()
            }
        }

        private fun collectPhotos() =
            safeLaunch {
                val inserted = collectPhotosUseCase()
                sendEffect(CollectionUiSideEffect.ShowMessage("사진 수집 완료 — 새로 저장 ${inserted}건"))
            }

        private fun insertTestItems() =
            safeLaunch {
                val items = buildTestItems()
                val inserted = addSourceItemsUseCase(items)
                sendEffect(
                    CollectionUiSideEffect.ShowMessage("저장 ${inserted}건 / 중복 무시 ${items.size - inserted}건"),
                )
            }

        /**
         * 저장 파이프라인 검증용 테스트 아이템 2건을 만든다.
         * - 고정 sourceKey 1건: 재삽입 시 무시되어 addAll 멱등성을 확인한다.
         * - 고유 sourceKey 1건: 클릭마다 rows 증가를 확인한다.
         */
        private fun buildTestItems(): List<SourceItem> {
            val now = Instant.now()
            return listOf(
                testItem(now = now, sourceKey = FIXED_SOURCE_KEY),
                testItem(now = now, sourceKey = "debug-${UUID.randomUUID()}"),
            )
        }

        private fun testItem(
            now: Instant,
            sourceKey: String,
        ): SourceItem =
            SourceItem(
                rawId = UUID.randomUUID().toString(),
                startAt = now,
                endAt = null,
                timeZoneId = ZoneId.systemDefault(),
                payload = LocationPayload(latitude = SEOUL_LATITUDE, longitude = SEOUL_LONGITUDE),
                sourceName = SourceName.LOCATION_PROVIDER,
                sourceKey = sourceKey,
                collectedAt = now,
            )

        private companion object {
            const val FIXED_SOURCE_KEY = "debug-fixed"
            const val SEOUL_LATITUDE = 37.5665
            const val SEOUL_LONGITUDE = 126.9780
        }
    }
