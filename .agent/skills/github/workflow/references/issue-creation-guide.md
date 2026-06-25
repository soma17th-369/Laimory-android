---
name: issue-creation-guide
description: GitHub 이슈를 생성할 때 Type, Priority, Size, 본문 템플릿, Epic 분해 여부를 결정하는 기준입니다.
---

# 이슈 생성 가이드

사용자가 할 일, 기능, 버그, 리팩터링 작업을 말하면 GitHub 이슈로 추적할 수 있도록 정리합니다.
단순히 제목만 만드는 것이 아니라 본문, Type, 우선순위, 작업 크기, 분해 여부까지 검토합니다.

## 1. 원칙

- 이슈 본문은 한국어로 작성합니다.
- Type, Priority, Size, Epic 분해 여부가 모호하면 추측하지 않고 사용자에게 확인합니다.
- 실제 이슈 생성 전에는 제목, 본문 요약, Type, Priority, Size, 대상 레포/프로젝트를 확인받습니다.
- 이슈 템플릿의 빈 섹션은 삭제하지 않고 유지합니다.
- 라벨은 Issue Type을 대체하지 않고 작업 영향 영역을 보조 분류하는 용도로만 사용합니다.
- 지금 바로 코드로 해결할 작업이면 이슈 생성보다 구현 요청인지 먼저 확인합니다.

## 2. 사전 확인

- `gh auth status`로 GitHub 인증 상태를 확인합니다.
- 이슈 생성 대상 레포를 확인합니다. 현재 저장소가 대상이면 `gh repo view`로 확인합니다.
- 프로젝트 보드나 Size 필드를 설정해야 한다면 필요한 권한과 프로젝트 정보를 먼저 확인합니다.

## 3. Type 기준

| Type | 기준 |
|---|---|
| Bug | 예상과 다른 오류 또는 비정상 동작 |
| Feature | 사용자가 직접 경험하는 새 기능 또는 화면 |
| Task | 기능 외 기술 작업, 운영 작업, 문서 작업 |
| Refactor | 동작 변경 없이 구조를 개선하는 작업 |
| Epic | 여러 Feature/Task로 나눠야 하는 큰 작업 |

## 4. Priority 기준

| Priority | 기준 |
|---|---|
| Critical | 즉시 처리해야 하는 장애 또는 긴급 이슈 |
| High | 이번 주기 안에 반드시 처리해야 하는 작업 |
| Medium | 일반 우선순위 작업 |
| Low | 여유가 있을 때 처리해도 되는 작업 |
| Backlog | 당장 착수하지 않고 보관할 작업 |

## 5. Size 기준

| Size | 기준 |
|---|---|
| XS | 매우 작은 수정 또는 문서 보완 |
| S | 한두 파일 수준의 작은 작업 |
| M | 일반적인 기능/수정 작업 |
| L | 여러 모듈 또는 흐름에 걸친 큰 작업 |
| XL | Epic 분해를 고려해야 하는 큰 작업 |

## 6. 본문 템플릿

Bug, Feature, Refactor 이슈는 `.github/ISSUE_TEMPLATE`의 템플릿을 우선 사용합니다.

- Bug: `.github/ISSUE_TEMPLATE/bug_report.md`
- Feature: `.github/ISSUE_TEMPLATE/feature_request.md`
- Refactor: `.github/ISSUE_TEMPLATE/refactor.md`

그 외 Task 또는 Epic은 아래 일반 골격을 사용합니다.

```markdown
## 배경 / 목적
왜 필요한지, 어떤 문제를 푸는지 작성합니다.

## 상세 내용
무엇을 어떻게 할지 작성합니다.
- [ ] 필요한 작업을 체크리스트로 정리합니다.

## 완료 조건
이 조건이 충족되면 완료로 볼 수 있는 기준을 작성합니다.
```

## 7. Epic 분해

작업이 크고 독립적인 단계가 여러 개라면 Epic으로 만들고 하위 Feature/Task로 나눕니다.
분해가 필요해 보이면 먼저 하위 이슈 후보를 제안하고 사용자 확인을 받은 뒤 생성합니다.

## 8. 생성 명령

단일 이슈는 `gh issue create`를 사용합니다.

```bash
gh issue create \
  --repo <owner>/<repo> \
  --title "<title>" \
  --body-file <body-file>
```

라벨은 [라벨 가이드](label-guide.md)를 기준으로 영역 라벨을 필요한 만큼 추가합니다.
담당자, 마일스톤이 필요하면 사용자 확인 후 옵션을 추가합니다.
