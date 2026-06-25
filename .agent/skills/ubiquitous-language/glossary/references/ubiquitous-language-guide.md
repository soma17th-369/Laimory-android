---
name: ubiquitous-language-guide
description: Laimory 제품 용어 사전을 작성, 검토, 변경하는 기준입니다.
---

# Ubiquitous Language 가이드

이 가이드는 Laimory 팀이 제품 용어를 같은 의미로 사용하도록 돕는 운영 기준입니다.

## 0. 기본 전제

- `docs/ubiquitous-language/glossary.md`를 용어의 원천 문서로 사용합니다.
- `.agent/skills/ubiquitous-language`는 용어 사전을 어떻게 읽고 검증할지 정하는 에이전트용 규칙입니다.
- 이 문서는 DDD를 엄격히 적용하기 위한 문서가 아닙니다.
- `영역`은 Bounded Context가 아니라 제품 기능이나 도메인 묶음을 가볍게 표현한 것입니다.

## 1. Glossary 표준 형식

Accepted Terms는 아래 컬럼을 사용합니다.

| 컬럼 | 의미 |
|---|---|
| 영역 | 용어가 속한 제품 기능 또는 도메인 묶음 |
| 한국어 | 기획, 디자인, 회의, 사용자-facing 문서에서 사용하는 용어 |
| English / Code | Kotlin 클래스명, 함수명, 패키지명, API 필드명 등에 사용할 기준 표현 |
| 정의 | 팀이 같은 의미로 이해하기 위한 한 문장 정의 |
| 쓰지 않을 표현 / 주의 | 혼동되는 용어, 배제할 표현, 구분해야 하는 개념 |

## 2. 새 용어 검토 기준

1. 기존 Accepted Terms에 같은 개념이 있는지 확인합니다.
2. 같은 개념이 있다면 새 용어를 만들지 않고 기존 용어를 사용합니다.
3. 비슷하지만 다른 개념이라면 정의와 차이를 명확히 씁니다.
4. 한국어 용어와 English / Code 표현을 함께 정합니다.
5. English / Code 표현이 Kotlin 네이밍에 자연스러운지 확인합니다.
6. 확정되지 않은 용어는 Proposed Terms에 기록합니다.
7. 팀 합의 후 Accepted Terms로 옮기고 decision-log에 결정 이유를 남깁니다.

## 3. 코드 네이밍 적용 기준

- 도메인 모델, UseCase, Repository, DTO, 화면명에 도메인 용어가 들어가면 glossary의 English / Code 표현을 따릅니다.
- 새로운 코드 이름이 glossary에 없는 제품 용어를 포함하면 먼저 Proposed Terms에 후보로 기록합니다.
- 금지 또는 대체 용어가 Deprecated Terms에 있으면 새 코드에 사용하지 않습니다.
- 기존 코드의 용어 변경이 필요하면 변경 범위와 migration 필요성을 먼저 정리합니다.

## 4. 리뷰 기준

- PR이나 문서 변경에서 새로운 제품 용어가 생겼는지 확인합니다.
- 같은 개념을 여러 이름으로 부르고 있지 않은지 확인합니다.
- 한국어 용어와 English / Code 표현이 서로 대응되는지 확인합니다.
- 용어 변경이 코드, 이슈, 디자인 문서, API 문서에 영향을 주는지 확인합니다.

## 5. 피해야 할 패턴

- 합의되지 않은 용어를 바로 클래스명이나 API 필드명에 사용하는 것
- `Log`, `Record`, `Entry`처럼 넓은 단어를 정의 없이 사용하는 것
- 한국어 문서와 코드 이름이 서로 다른 개념처럼 갈라지는 것
- 임시 표현을 Accepted Terms에 바로 넣는 것
