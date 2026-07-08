# Drive 테스트 내보내기 자격증명 (임시 테스트 전용)

이 폴더에 `drive_oauth.json` 을 두면 타임라인의 "Drive 테스트" 버튼이 활성화됩니다.
**debug 빌드에서만** 읽히고, release 빌드엔 이 파일이 없어 기능이 자연 비활성됩니다.
`drive_oauth.json` 은 `.gitignore` 로 커밋 제외되니 **각자 로컬에 직접 설정**하세요.

형식(수집 계정 OAuth, scope `drive.file` 권장):

```json
{
  "client_id": "...",
  "client_secret": "...",
  "refresh_token": "..."
}
```

관련 코드: `feature/timeline/src/main/java/.../testexport/` (통째로 삭제하면 기능 제거).
