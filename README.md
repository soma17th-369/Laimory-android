# 369-mobile
AI·SW 마에스트로 17기 369팀의 모바일 저장소입니다.

<!-- MODULE_GRAPH_START -->
## 모듈 의존성 그래프

> 마지막 업데이트: 2026-08-26

![Module Graph](docs/dependency-graph/modules-graph.png)
<!-- MODULE_GRAPH_END -->

## 로컬 설정 — Firebase (`google-services.json`)

Firebase 초기 연동(google-services 플러그인)이 배선되어 있어, **빌드하려면 `app/google-services.json` 이 필요**합니다. 이 파일은 자격증명이라 **공개 레포에 커밋하지 않습니다**(`.gitignore` 등록됨). 각자 로컬에 배치하세요.

1. Firebase 콘솔 → 프로젝트 설정 → 내 앱에서 `google-services.json` 다운로드
2. `app/google-services.json` 위치에 둡니다

> ⚠️ debug 빌드는 `applicationIdSuffix = ".debug"` 때문에 패키지명이 `com.soma369.laimory.debug` 입니다.
> `google-services.json` 에 **`com.soma369.laimory` 와 `com.soma369.laimory.debug` 두 앱이 모두 등록**되어 있어야 debug 빌드가 통과합니다.
> (콘솔에서 `com.soma369.laimory.debug` 앱을 추가 등록한 뒤 파일을 다시 받으면 두 패키지가 함께 들어갑니다.)

## 로컬 설정 — Google Maps API 키

초안 동의 화면의 위치 상세 지도(`Maps SDK for Android`)에 API 키가 필요합니다. 키는 **저장소에 커밋하지 않고** gitignore 대상인 `local.properties` 에서 주입합니다.

```properties
laimory.mapsApiKey=AIza...
```

**키가 없어도 빌드는 성공합니다.** 빈 문자열로 떨어지고 지도 영역만 대체 안내로 바뀌므로, 지도를 볼 필요가 없다면 설정하지 않아도 됩니다.

### 키 발급

Firebase 와 같은 GCP 프로젝트(`laimory-dev`)를 씁니다.

1. Google Cloud 콘솔 → `laimory-dev` 프로젝트 → **API 및 서비스** 에서 `Maps SDK for Android` 사용 설정
2. **사용자 인증 정보** → API 키 만들기
3. 만든 키에 제한 두 가지를 겁니다.
   - **애플리케이션 제한** → Android 앱 → 패키지명과 SHA-1 등록
   - **API 제한** → `Maps SDK for Android` 만 허용

debug 빌드에 등록할 값입니다.

| 항목 | 값 |
| --- | --- |
| 패키지명 | `com.soma369.laimory.debug` |
| SHA-1 | 아래 명령으로 확인 |

```bash
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

> `Maps SDK for Android` 는 결제 계정 활성화가 필요하지만, Map ID 를 쓰지 않는 이 구성의 SKU 는 공식 가격표상 무료 사용량이 `Unlimited` 입니다.
>
> 운영 키는 Play App Signing 인증서 SHA-1 이 필요해 배포 환경이 준비된 뒤 별도로 발급합니다.

## 서버 환경

API 요청과 OAuth App Link가 build type에 맞는 서버를 사용하도록 공개 기본 URL을 `gradle.properties`에서 관리합니다.

```properties
laimory.debugBaseUrl=https://dev.laimory.app/
laimory.releaseBaseUrl=https://laimory.app/
```

임시 환경이 필요하면 `~/.gradle/gradle.properties`에 같은 키를 설정하거나 Gradle 실행 시 `-P`로 덮어쓸 수 있습니다.
