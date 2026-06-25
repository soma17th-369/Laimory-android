---
name: github-workflow-guide
description: GitHub 이슈, Pull Request, 리뷰 흐름을 정리하는 참조 문서입니다.
---

# GitHub 워크플로우 가이드

GitHub 워크플로우 지침을 이 문서에 추가합니다.
브랜치 네이밍과 커밋 규약은 Git 스킬 문서를 기준으로 합니다.

## 1. 세부 참조 문서

- [브랜치 네이밍 가이드](../../../git/branch-naming/references/branch-naming-guide.md)
- [커밋 가이드](../../../git/commit/references/commit-guide.md)
- [이슈 생성 가이드](issue-creation-guide.md)
- [PR 리뷰 코멘트 처리 가이드](pr-review-comment-guide.md)
- [PR 리뷰 코멘트 판단 기준](pr-review-disposition-guide.md)
- [gh 명령 가이드](gh-command-guide.md)

## 2. 이슈 템플릿

이슈는 `.github/ISSUE_TEMPLATE` 아래의 템플릿을 기준으로 작성합니다.

- 라벨 기준: [라벨 가이드](label-guide.md)
- 버그: `.github/ISSUE_TEMPLATE/bug_report.md`
- 기능: `.github/ISSUE_TEMPLATE/feature_request.md`
- 리팩터링: `.github/ISSUE_TEMPLATE/refactor.md`

템플릿의 빈 섹션은 삭제하지 않고 유지합니다. 아직 확정되지 않은 항목은 비워 두어 작성자가 보완할 수 있게 합니다.

## 3. GitHub Markdown의 `@` 표기

이슈, PR, 리뷰 코멘트에서 Kotlin/Android 어노테이션처럼 코드 심볼로 쓰는 `@` 표현은 코드블럭 밖의 inline 문맥에서 Markdown code span으로 감쌉니다.

권장:

```markdown
`@Composable`
`@Inject`
`@HiltViewModel`
`@Provides`
`@Binds`
`@Module`
`@InstallIn`
```

코드블럭 안에서는 그대로 작성할 수 있습니다.

````markdown
```kotlin
@Composable
fun TimelineScreen() {}
```
````

피함:

```markdown
@Composable
@Inject
@HiltViewModel
@Provides
@Binds
@Module
@InstallIn
```

- 실제 사람이나 팀을 멘션해야 하는 경우에는 백틱 없이 `@username`을 사용합니다.
- 예시 계정명처럼 멘션 의도가 없는 표현은 백틱으로 감싸거나 `username`처럼 멘션이 아닌 표현으로 바꿉니다.
- 커밋 메시지의 `@` 표기는 [커밋 가이드](../../../git/commit/references/commit-guide.md)를 따릅니다.
