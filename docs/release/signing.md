# 서명 키

`qa` 와 `release` 를 **업로드 키 하나**로 서명합니다. 키 자체와 비밀번호는 저장소에 없고, 빌드는
`local.properties` 에서 읽습니다(README 의 "로컬 설정 — 서명").

## 왜 하나인가

QA 전용 키를 따로 두는 이유는 "개발자마다 키를 만들면 인증서 지문이 제각각이 되어, 지도 API 키
제한과 `assetlinks.json` 에 등록한 지문과 맞는 빌드만 동작한다" 는 것입니다. 이 저장소는 Android
개발자가 한 명이라 지문이 이미 하나로 고정됩니다. 목적이 이미 충족되는데 키를 나누면 만들고
보관하고 회전시킬 비밀만 하나 늘어납니다.

서명 키와 패키지명은 묶여 있지 않으므로 `.qa` 빌드를 업로드 키로 서명해도 문제가 없습니다. QA 는
Play 를 거치지 않아 그 지문이 그대로 최종 서명이 됩니다.

**언제 다시 나누나:** Android 개발자가 늘어 여러 사람이 로컬에서 QA 빌드를 만들게 되면 나눕니다.
업로드 키를 여러 명에게 배포하는 상태가 되기 때문입니다. 그 시점에 QA 지문이 바뀌므로 지도 키
제한과 `assetlinks.json` 을 함께 갱신합니다.

## 업로드 키

| 항목 | 값 |
| --- | --- |
| alias | `laimory-upload` |
| 형식 | PKCS12, RSA 2048, 유효기간 10,000일 |
| DN | `CN=Laimory, OU=SOMA, O=369, L=Osan, ST=Gyeonggi, C=KR` |
| SHA-1 | `08:B3:34:39:5C:24:27:26:30:5D:FD:86:77:CC:90:C2:50:6F:AE:A7` |
| SHA-256 | `30:56:73:00:FA:02:B7:04:CA:89:37:86:AA:D7:78:8B:EF:C6:89:EE:C5:A5:20:B4:3A:18:70:6F:F4:AE:A4:10` |

**인증서 지문은 비밀이 아닙니다.** `assetlinks.json` 으로 공개 서빙되고 배포된 APK 에서 누구나
추출할 수 있습니다. 비밀은 keystore 파일과 비밀번호뿐입니다.

만든 명령입니다.

```bash
keytool -genkeypair -v -keystore laimory-upload.jks -alias laimory-upload \
  -keyalg RSA -keysize 2048 -validity 10000 -storetype PKCS12
```

지문을 다시 확인할 때는 이렇게 봅니다.

```bash
keytool -list -v -keystore <경로> -alias laimory-upload | grep -E "SHA1|SHA256"
```

## 이 지문을 쓰는 곳

| 대상 | 지문 |
| --- | --- |
| 지도 API 키 제한 — `com.soma369.laimory.qa` | 업로드 인증서 **SHA-1** |
| `assetlinks.json` — `.qa` 패키지 (#207) | 업로드 인증서 **SHA-256** |
| 지도 API 키 제한 — `com.soma369.laimory` | **Play App Signing 인증서** SHA-1 |
| 운영 `assetlinks.json` — release 패키지 (#207) | **Play App Signing 인증서** SHA-256 |

운영 패키지에 업로드 인증서 지문을 넣지 않습니다. Play 가 배포본을 앱 서명 키로 재서명하므로
기기에 깔리는 APK 의 지문이 다릅니다. Play App Signing 인증서는 Play Console 에 첫 AAB 를 올린
뒤 확인할 수 있습니다.

## 보관

- 원본과 별개로 **최소 한 벌을 다른 매체에** 둡니다. 파일·비밀번호·alias 셋이 모두 있어야 쓸 수
  있으므로 함께 기록합니다.
- 비밀번호는 비밀번호 관리자에 둡니다. 저장소·이슈·PR·채팅에 남기지 않습니다.
- CI 는 keystore 를 base64 로 인코딩해 Secret 으로 주입하고 빌드 시 복원합니다. 자세한 배선은 CI
  이슈에서 정합니다.

## 잃어버렸을 때

업로드 키를 잃으면 Play Console 에서 **업로드 키 재설정**을 요청합니다. 앱 서명 키는 Google 이
보관하므로 앱 자체는 살아 있고, 재설정이 끝날 때까지 새 버전을 올릴 수 없을 뿐입니다.

키가 바뀌면 지문도 바뀌므로 다음을 함께 갱신합니다.

- 지도 API 키 제한의 `com.soma369.laimory.qa` 항목
- `.qa` 패키지가 등록된 `assetlinks.json`

## 앱을 다른 계정으로 넘길 때

앱 서명 키는 Google 이 보관하므로 Play Console 의 앱 전송으로 함께 넘어갑니다. 업로드 키는 따라
가지 않으므로, keystore 를 넘기거나 **새 소유자가 자기 업로드 키를 만들고 Play 에 교체를 요청**
합니다. 키를 넘기지 않아도 되는 후자가 낫습니다.
