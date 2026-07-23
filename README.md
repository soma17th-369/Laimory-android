# 369-mobile
AI·SW 마에스트로 17기 369팀의 모바일 저장소입니다.

<!-- MODULE_GRAPH_START -->
## 모듈 의존성 그래프

> 마지막 업데이트: 2026-07-24

![Module Graph](docs/dependency-graph/modules-graph.png)
<!-- MODULE_GRAPH_END -->

## 로컬 설정 — Firebase (`google-services.json`)

Firebase 초기 연동(google-services 플러그인)이 배선되어 있어, **빌드하려면 `app/google-services.json` 이 필요**합니다. 이 파일은 자격증명이라 **공개 레포에 커밋하지 않습니다**(`.gitignore` 등록됨). 각자 로컬에 배치하세요.

1. Firebase 콘솔 → 프로젝트 설정 → 내 앱에서 `google-services.json` 다운로드
2. `app/google-services.json` 위치에 둡니다

> ⚠️ debug 빌드는 `applicationIdSuffix = ".debug"` 때문에 패키지명이 `com.soma369.laimory.debug` 입니다.
> `google-services.json` 에 **`com.soma369.laimory` 와 `com.soma369.laimory.debug` 두 앱이 모두 등록**되어 있어야 debug 빌드가 통과합니다.
> (콘솔에서 `com.soma369.laimory.debug` 앱을 추가 등록한 뒤 파일을 다시 받으면 두 패키지가 함께 들어갑니다.)

## 서버 환경

API 요청과 OAuth App Link가 build type에 맞는 서버를 사용하도록 공개 기본 URL을 `gradle.properties`에서 관리합니다.

```properties
laimory.debugBaseUrl=https://dev.laimory.app/
laimory.releaseBaseUrl=https://laimory.app/
```

임시 환경이 필요하면 `~/.gradle/gradle.properties`에 같은 키를 설정하거나 Gradle 실행 시 `-P`로 덮어쓸 수 있습니다.
