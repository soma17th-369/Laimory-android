# Ubiquitous Language

이 문서는 DDD를 엄격히 적용하기 위한 문서가 아니라, Laimory 팀이 제품 용어를 같은 의미로 사용하기 위한 팀 용어 사전입니다.

## 1. Accepted Terms

확정된 제품/도메인 용어를 기록합니다. 코드, 문서, 이슈, PR, 디자인 산출물에서는 이 표의 한국어와 English / Code 표현을 우선 사용합니다.

| 영역 | 한국어 | English / Code | 정의 | 쓰지 않을 표현 / 주의 |
|---|---|---|---|---|
| Timeline | 타임라인 | Timeline | 사용자의 기록을 시간순으로 탐색하는 화면 또는 흐름입니다. | 기록 목록과 혼용하지 않습니다. |
| Timeline | 순간 | Moment | 특정 시점에 기록되거나 수집된 개별 단위입니다. | Item, Record처럼 모호한 표현으로 대체하지 않습니다. |
| Timeline | 기록 창 | RecordDateWindow | 선택한 날짜를 기준으로 한 반열린 기록 구간 `[start, end)` 입니다. 기본값은 `[자정, 다음 날 자정)` 이며, 사용자가 시작 시각과 당일·익일 종료 시각을 지정할 수 있습니다. 경계를 걸친 수면·일정은 겹치면(overlap) 포함하고, 홈 요약과 초안 생성 입력이 같은 창을 공유합니다. | "수면 경계 창"으로 좁혀 부르지 않습니다. 화면이나 Data 계층에서 구간을 다시 계산하지 않고 `RecordDateWindow`를 전달합니다. 수집(collection)이 아니라 타임라인 도메인이 소유합니다. |
| Collection | 체류 | Stay / `ItemType.STAY` | 한 장소에 일정 시간 머문 이벤트입니다. 체류 시간은 아이템의 `startAt`/`endAt`에서 파생합니다. 서버 초안 계약(`sourceItems[].itemType`)과 같은 이름을 로컬 도메인·로컬 DB에서도 사용합니다. | 체류 의미로 LOCATION을 쓰지 않습니다. UI 상위 개념 "위치"(체류+이동 합산 라벨)와 혼동하지 않습니다. |

## 2. Proposed Terms

아직 합의되지 않은 후보 용어를 기록합니다. 확정 전까지는 코드나 API 이름에 반영하지 않습니다.

| 영역 | 한국어 후보 | English / Code 후보 | 제안 이유 | 결정 필요 사항 |
|---|---|---|---|---|

## 3. Deprecated Terms

더 이상 사용하지 않기로 한 용어와 대체 용어를 기록합니다.

| 사용 금지 용어 | 대체 용어 | 이유 | 적용 범위 |
|---|---|---|---|
| LOCATION (체류 아이템 타입) | STAY / `ItemType.STAY` | 서버 초안 계약이 체류 itemType을 STAY로 확정해 로컬 도메인·DB 용어를 통일했습니다. GPS 위치 수집(LocationCollectionService 등)의 "위치"는 그대로 둡니다. | `ItemType`, payload 클래스(`StayPayload`), 로컬 DB(v2 마이그레이션), 서버 전송·검증 JSON |
