---
name: gh-command-guide
description: GitHub 이슈 생성과 PR 리뷰 코멘트 처리에 사용하는 gh 명령과 GraphQL 쿼리 모음입니다.
---

# gh 명령 가이드

GitHub 작업은 가능한 한 `gh` 명령으로 수행합니다.
외부 상태를 바꾸는 명령은 실행 전 사용자 확인을 받습니다.

## 1. 저장소 확인

```bash
gh repo view --json owner,name -q '"\(.owner.login)/\(.name)"'
```

## 2. 인증 확인

```bash
gh auth status
```

## 3. 이슈 생성

```bash
gh issue create \
  --repo <owner>/<repo> \
  --title "<title>" \
  --body-file <body-file>
```

## 4. PR 확인

```bash
gh pr status
gh pr view <pr-number> --json number,title,url,headRefName,baseRefName
```

## 5. unresolved 리뷰 thread 조회

```bash
gh api graphql -f query='
  query($owner: String!, $repo: String!, $pr: Int!) {
    repository(owner: $owner, name: $repo) {
      pullRequest(number: $pr) {
        reviewThreads(first: 100) {
          nodes {
            id
            isResolved
            isOutdated
            comments(first: 50) {
              nodes {
                id
                databaseId
                author { login }
                body
                path
                line
                originalLine
                diffHunk
                createdAt
              }
            }
          }
        }
      }
    }
  }
' -F owner=<owner> -F repo=<repo> -F pr=<pr>
```

응답에서 `isResolved == false`인 thread만 처리 대상으로 봅니다.

## 6. 리뷰 thread 답변

```bash
gh api repos/<owner>/<repo>/pulls/<pr>/comments \
  -f body="<reply text>" \
  -F in_reply_to=<comment-database-id>
```

`in_reply_to`에는 thread의 첫 번째 댓글 `databaseId`를 사용합니다. GraphQL node id나 thread id를 넣으면 실패합니다.

## 7. 리뷰 thread resolve

```bash
gh api graphql -f query='
  mutation($threadId: ID!) {
    resolveReviewThread(input: {threadId: $threadId}) {
      thread { isResolved }
    }
  }
' -F threadId=<thread-id>
```

리뷰어가 다시 확인해야 할 실질적 변경이나 논쟁 여지가 있는 코멘트는 임의로 resolve하지 않습니다.

## 8. 자주 나는 오류

- `404`: PR 번호, repo 경로, 인증 계정을 확인합니다.
- `422`: `in_reply_to`에 잘못된 ID를 넣었을 가능성이 큽니다.
- `403`: 댓글 작성 또는 resolve 권한이 부족할 수 있습니다.
