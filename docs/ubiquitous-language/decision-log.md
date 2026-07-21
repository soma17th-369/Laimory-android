# Ubiquitous Language Decision Log

제품 용어를 새로 확정하거나 변경한 이유를 기록합니다.

## 1. Decisions

| 날짜 | 결정 | 영향받는 용어 | 이유 | 후속 작업 |
|---|---|---|---|---|
| 2026-07-20 | 기록 창의 달력 하루 정의를 기본값으로 유지하되, 사용자가 시작 시각과 당일·익일 종료 시각을 지정할 수 있는 `[start, end)` 구간으로 확장한다. `RecordDateWindow`가 요약 필터링과 서버 `timelineWindow`의 단일 정본이다. | 기록 창 (RecordDateWindow) | 초안 생성 서버 계약에 `recordDate`·`timelineWindow`가 추가됐고, 사용자가 익일 데이터까지 한 초안에 포함할 수 있어야 하기 때문이다. | #164, #165, #166: 서버 요청 계약과 홈 `DateHeaderCard`·범위 설정 UI에 반영. |
| 2026-07-08 | 기록 창을 달력 하루 `[자정, 다음 날 자정)` 로 정의하고, 경계를 걸친 수면·일정은 overlap으로 그날에 포함한다. 초기 검토안이던 "기상~취침 수면 경계 창"은 폐기. 창은 타임라인 도메인이 소유한다(collection은 수집만). | 기록 창 (RecordDateWindow) | 홈/타임라인 요약(#123)과 초안 생성 입력(#120)이 같은 창을 공유해야 하고, 수집과 타임라인 관심사를 분리하기 위함. | #120 초안 생성 입력에서 `RecordDateWindow.ofDate`/`contains` 재사용. |
| 2026-07-09 | 수면 데이터의 정본은 Health Connect 로 두고, Laimory 는 HC에 수면을 기록하는 프로듀서 역할만 한다. 폰 Sleep API 로 감지한 수면을 감지 신뢰도가 충분히 높을 때만 HC에 write 하고(신뢰도가 낮으면 기록하지 않고 사용자 입력으로 폴백), 수집은 기존 HC-read(#93) 경로로 통일한다. 앱 자체 로컬 수면 저장(별도 SLEEP SourceName 신설·직접 기록)은 하지 않는다. | 수면 (Sleep) — Health Connect 정본 / HC 프로듀서 | 삼성헬스·핏빗 등 HC에 수면을 써주는 앱이 없는 사용자도 수면이 채워지게 하되, 앱 간 중복·이중 계수를 피하고 수집 경로를 HC 하나로 단순화하기 위함. | 에픽 #142 (서브 #143 HC 쓰기 배관·`WRITE_SLEEP` / #144 Sleep API 감지→HC 기록 / #145 불확실한 밤 사용자 입력 폴백). |
| 2026-07-17 | 체류 아이템 타입을 서버 초안 계약과 동일한 `STAY` 로 확정하고, 로컬 도메인(`ItemType`·`StayPayload`)·로컬 DB(v2 migration)·Drive 검증 JSON까지 같은 이름으로 통일한다. 체류 의미의 `LOCATION` 은 폐기한다. 경계(projection)에서만 `LOCATION → STAY` 로 매핑하는 안은 기각. GPS 위치 수집 계열 명칭(`LocationCollectionService` 등)은 체류가 아니라 "위치 수집" 의미이므로 유지한다. | 체류 (Stay / `ItemType.STAY`) | 서버 계약 확인 결과 서버는 `STAY` 만 받아 초안 생성이 400 으로 거절됐고(#137), 배포 전이라 도메인·DB 전환 비용이 가장 낮은 시점이며, 용어를 통일하면 이후 타임라인 조회 역매핑과 경계별 예외 규칙이 사라지기 때문. | #137 (PR #160): 도메인 rename + 수집 DB v2 migration(itemType·`sourceKey` 접두사 전환) + glossary Accepted/Deprecated 반영. 타임라인 조회 구현 시 서버 `STAY` 를 추가 매핑 없이 그대로 사용. |
