# 369-mobile
AI·SW 마에스트로 17기 369팀의 모바일 저장소입니다.

<!-- MODULE_GRAPH_START -->
## 모듈 의존성 그래프

> 마지막 업데이트: 2026-09-04

![Module Graph](docs/dependency-graph/modules-graph.png)
<!-- MODULE_GRAPH_END -->

## 로컬 설정 — Firebase (`google-services.json`)

Firebase 초기 연동(google-services 플러그인)이 배선되어 있어, **빌드하려면 `app/google-services.json` 이 필요**합니다. 이 파일은 자격증명이라 **공개 레포에 커밋하지 않습니다**(`.gitignore` 등록됨). 각자 로컬에 배치하세요.

1. Firebase 콘솔 → 프로젝트 설정 → 내 앱에서 `google-services.json` 다운로드
2. `app/google-services.json` 위치에 둡니다

> ⚠️ 빌드 타입마다 패키지명이 다릅니다(`applicationIdSuffix`). `google-services.json` 에 **세 앱이 모두 등록**되어
> 있어야 각 빌드가 통과합니다 — 등록되지 않은 패키지로 빌드하면 `google-services` 플러그인이 빌드를 멈춥니다.
>
> | 빌드 타입 | 패키지명 |
> | --- | --- |
> | `debug` | `com.soma369.laimory.debug` |
> | `qa` | `com.soma369.laimory.qa` |
> | `release` | `com.soma369.laimory` |
>
> (콘솔에서 앱을 추가 등록한 뒤 파일을 다시 받으면 세 패키지가 함께 들어갑니다.)

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

키 하나에 **빌드 타입마다 제한 항목을 하나씩** 더합니다. 제한은 패키지명 + 서명 인증서 SHA-1 쌍이므로,
같은 키라도 등록되지 않은 조합으로 빌드하면 그 빌드에서만 지도가 대체 안내로 떨어집니다.

| 패키지명 | SHA-1 | 등록 시점 |
| --- | --- | --- |
| `com.soma369.laimory.debug` | debug keystore (아래 명령) | 지금 |
| `com.soma369.laimory.qa` | 업로드 인증서 (`docs/release/signing.md`) | 지금 |
| `com.soma369.laimory` | **Play App Signing 인증서** | Play 첫 AAB 업로드 이후 (#207) |

```bash
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

> 운영 패키지에는 업로드 인증서 지문을 넣지 않습니다. Play 가 배포본을 **앱 서명 키로 재서명**하므로
> 기기에 깔리는 APK 의 지문은 우리가 만든 키와 다릅니다.
>
> `Maps SDK for Android` 는 결제 계정 활성화가 필요하지만, Map ID 를 쓰지 않는 이 구성의 SKU 는 공식 가격표상 무료 사용량이 `Unlimited` 입니다.

## 빌드 타입

| 빌드 타입 | 서버 | R8 | applicationId | 런처 라벨 (ko / 기본) | 쓰임 |
| --- | --- | --- | --- | --- | --- |
| `debug` | dev | ✗ | `com.soma369.laimory.debug` | 라이모리-debug / Laimory-debug | 일상 개발 |
| `qa` | prod | ✓ | `com.soma369.laimory.qa` | 라이모리-qa / Laimory-qa | QA 검증 (Firebase App Distribution) |
| `release` | prod | ✓ | `com.soma369.laimory` | 라이모리 / Laimory | 스토어 |

라벨은 buildType 소스셋의 `strings.xml` 로 덮습니다. `buildTypes` 의 `resValue` 로 넣으면 main 의 `app_name`
과 중복 리소스 오류가 납니다. **qualifier 마다 덮어야 합니다** — main 에 `values-ko` 가 있으므로 기본
`values` 만 덮으면 한국어 기기에서는 세 빌드가 모두 `라이모리` 로 보입니다.

`qa` 는 `initWith(release)` 로 릴리즈 설정을 상속받고 **다른 것만** 덮습니다. 난독화를 끄지 않고 디버그 가능
플래그도 켜지 않습니다 — R8 은 런타임에 터지므로, 조건이 다르면 QA 를 통과한 빌드가 출시본에서만 터집니다.

세 번째 이름을 `prod` 가 아니라 `release` 로 둔 것은 AGP 예약 buildType 이라 지울 수 없고 서명·`lintVital`·
번들 기본값이 그 이름에 물려 있기 때문입니다. 서버 용어와의 통일은 설정 이름(`prodBaseUrl`)으로 맞춥니다.

### 서버 환경

API 요청과 OAuth App Link가 보는 공개 기본 URL을 `gradle.properties`에서 관리합니다. 이름은 buildType 이 아니라
**환경**을 가리킵니다 — `qa` 와 `release` 가 같은 서버를 봅니다.

```properties
laimory.devBaseUrl=https://dev.laimory.app/
laimory.prodBaseUrl=https://laimory.app/
```

임시 환경이 필요하면 `~/.gradle/gradle.properties`에 같은 키를 설정하거나 Gradle 실행 시 `-P`로 덮어쓸 수 있습니다.

```bash
# 난독화된 빌드를 dev 서버로 겨눠 R8 을 확인할 때
./gradlew :app:assembleQa -Plaimory.prodBaseUrl=https://dev.laimory.app/
```

> 이 명령의 검증 범위는 API 통신과 R8 smoke test 까지입니다. dev 도메인의 `assetlinks.json` 에 `.qa` 패키지가
> 없으면 OAuth App Link 는 완주되지 않습니다.

### 로그

| 빌드 타입 | 앱 로그(`Logger.minLevel`) | HTTP 로그 | `MockInterceptor` |
| --- | --- | --- | --- |
| `debug` | `VERBOSE` | `BODY` | 붙음 |
| `qa` | `DEBUG` | `BASIC` | 안 붙음 |
| `release` | `WARN` | 없음 | 안 붙음 |

QA 의 HTTP 로그가 `BASIC` 인 것은 QA 가 **운영 서버**를 보기 때문입니다. `BODY` 는 실사용자의 사진·위치·건강
데이터를 logcat 에 통째로 남깁니다. 본문까지 봐야 하면 dev 를 겨눈 빌드에서 봅니다.

## 로컬 설정 — 서명

`qa` 와 `release` 는 같은 업로드 키로 서명합니다. 키 자체와 비밀번호는 저장소에 넣지 않고 gitignore 대상인
`local.properties` 에서 주입합니다.

```properties
laimory.uploadStoreFile=~/keys/laimory-upload.jks
laimory.uploadStorePassword=...
laimory.uploadKeyAlias=laimory-upload
laimory.uploadKeyPassword=...
```

**값이 없어도 빌드는 성공합니다.** 서명 설정을 만들지 않고 unsigned 로 떨어지므로, 키가 없는 개발 환경이나
PR CI 에서 `assembleQa`·`bundleRelease` 가 그대로 통과합니다. 스토어에 올릴 AAB 를 만들 때만 값이 필요합니다.

키스토어 생성·보관·지문은 [`docs/release/signing.md`](docs/release/signing.md) 를 참고하세요.
