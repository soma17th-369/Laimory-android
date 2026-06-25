---
name: branch-naming-guide
description: Laimory Android 저장소의 Git 브랜치 타입, 이슈 번호, 작업 요약, 예외 브랜치 기준입니다.
---

# 브랜치 네이밍 가이드

브랜치 이름은 작업의 성격, 추적 이슈, 변경 요약을 한눈에 알 수 있게 작성합니다.

## 0. 기본 전제

- 작업 브랜치는 원칙적으로 이슈를 기준으로 생성합니다.
- 이슈가 없는 작업은 먼저 이슈 생성이 필요한지 확인합니다.
- 사용자가 명시적으로 예외를 요청하지 않았다면 `develop`에서 작업 브랜치를 분기합니다.
- `main`, `develop`, `release/*`에는 직접 push하지 않습니다.

## 1. 기본 형식

```text
<type>/#<issue-number>-<summary>
```

- `<type>`은 작업 성격을 나타내는 소문자 영문 타입입니다.
- `#<issue-number>`는 GitHub 이슈 번호입니다.
- `<summary>`는 작업 내용을 짧은 kebab-case 영어로 작성합니다.

## 2. 브랜치 타입

| Type | 기준 | 예시 |
|---|---|---|
| `feat` | 사용자에게 보이는 기능 추가 또는 기능 단위 화면 구현 | `feat/#12-timeline-location` |
| `fix` | 버그 수정, 비정상 동작 수정 | `fix/#34-date-sort-bug` |
| `refactor` | 동작 변경 없이 구조 개선 | `refactor/#18-repository-layer` |
| `chore` | 빌드, 설정, 문서, 의존성, 운영성 작업 | `chore/#5-ktlint-setup` |
| `hotfix` | 배포 버전의 긴급 수정 | `hotfix/#99-crash-fix` |

## 3. 작업 요약 규칙

- 요약은 2-5개 단어 수준으로 작성합니다.
- 공백 대신 `-`를 사용합니다.
- 한국어 발음 표기보다 도메인 의미가 드러나는 영어를 우선합니다.
- 너무 넓은 표현은 피합니다. `home-update`보다 `home-empty-state`처럼 구체화합니다.

## 4. Release 브랜치

배포 준비 브랜치는 이슈 번호 대신 버전을 사용합니다.

```text
release/<version>
```

예시:

```text
release/1.0.0
release/1.1.0
```

## 5. 예외 처리

- 이슈 번호 없이 임시 확인이 필요하면 사용자에게 먼저 확인합니다.
- 이슈 생성이 과한 매우 작은 문서/설정 변경은 `chore/no-issue-<summary>`를 사용할 수 있습니다.
- 여러 이슈를 한 브랜치에서 처리해야 한다면 대표 이슈 번호를 사용하고 PR 본문에 관련 이슈를 명시합니다.
- 브랜치 이름을 잘못 만들었다면 새 브랜치를 만들기보다, 아직 원격 공유 전인지 확인하고 rename 여부를 판단합니다.
