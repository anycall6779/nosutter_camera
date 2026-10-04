# 카메라 무음 설정 / Root 전용 버전

사용자가 지정한 `csc_pref_camera_forced_shuttersound_key` 시스템 설정을 0/1로 변경하는 Android 앱입니다.
**사진을 직접 촬영하는 카메라 앱이 아닙니다.** 볼륨 음소거나 카메라 코드 후킹을 하지 않습니다.

## APK

| 파일 | 패키지 | 모드 |
|---|---|---|
| `AntiCameraOff-full-debug.apk` | `kr.local.anticamera` | Root(su), Shizuku(ADB/Shell), Root Shizuku |
| `AntiCameraOff-rootOnly-debug.apk` | `kr.local.anticamera.root` | Root(su)만; Shizuku 의존성·Provider 없음 |

두 버전은 동시 설치할 수 있으나, **동일한 시스템 키를 수정**합니다. 원래 값 백업은 앱별로 분리되어
있으므로 두 앱을 번갈아 적용하면 각 백업의 기준이 달라집니다. 한 버전만 선택해서 사용하세요.
로컬 debug 키로 서명했으며 스토어 배포용 release 서명은 아닙니다.

[APK 다운로드 (v1.0.0)](https://github.com/anycall6779/nosutter_camera/releases/tag/v1.0.0)

2026-10-04 사용자 실기기에서 정상 작동 확인을 받았습니다. 기기/OS/권한 모드 정보는 제공되지 않았으며,
전체 Android 버전 및 기기 지원을 검증한 결과는 아닙니다.

## 지원 범위와 제한

- `minSdk=28` (Android 9), `compileSdk=36`, `targetSdk=36`, `maxSdk` 제한 없음.
- Android 9~17을 설치/실행 호환성 목표로 작성했습니다. Android 17(API 37)은 API 36 타깃 앱으로
  호환 실행하는 구조입니다. **전체 버전·제조사·권한 조합의 실기기 동작을 검증했다는 뜻은 아닙니다.**
- Java/Dex만 포함하고 native `.so`는 없습니다. APK를 ABI별로 분리하지 않으므로
  32비트/64비트 Android에서 공통으로 사용할 수 있는 구조입니다. Root/Shizuku 자체의 기기 지원은 별개입니다.
- 이 키를 사용하는 카메라/펌웨어에서만 효과가 있습니다. 키가 존재해도 카메라가 무시할 수 있습니다.
  Root 권한도 카메라가 더 이상 읽지 않는 키를 유효하게 만들지는 못합니다.
- 최초 조회 값이 `0` 또는 `1`이 아니면 지원을 확인할 수 없는 상태로 보고 변경하지 않습니다.
  키가 없는 기기에 임의로 만들어서 성공했다고 표시하지 않습니다.
- 쓰기 후 다시 읽어 저장 여부를 검증하지만, **설정 저장과 실제 촬영 무음은 다릅니다.**
- Samsung 펌웨어에서 Shell의 쓰기가 막히면 오류를 표시합니다. Root도 SELinux/Root 관리자 정책에
  따라 거부될 수 있습니다. 다른 권한 모드로 자동 우회하지 않습니다.
- OTA, 재부팅, 카메라 앱 업데이트 등으로 값이 바뀌면 다시 조회·적용하세요.
  부팅 시 자동 실행, 백그라운드 상주, 카메라 앱 강제 종료는 하지 않습니다.

## 사용

1. Root 버전이면 Root 관리자에서 이 앱의 `su` 요청을 허용합니다.
2. 통합 버전에서 Shizuku를 쓰려면 먼저 Shizuku를 설치하고 시작합니다.
   - Android 9/10: PC의 ADB로 Shizuku 시작.
   - Android 11 이상: 무선 디버깅 또는 PC ADB로 시작 가능.
   - Root Shizuku: Shizuku를 Root로 시작해야 합니다. 기기에 Root가 있다고 ADB Shizuku가 Root로 바뀌지 않습니다.
3. 앱에서 실제 실행 권한에 맞는 모드를 선택하고 **현재 설정 조회 / 권한 연결**을 누릅니다.
   Shizuku는 권한 요청을 승인합니다. Shizuku 12 이상이 필요합니다.
4. **무음 설정 (0)**을 누릅니다. 첫 변경 전에 원래 값을 동기 저장합니다.
5. 기본 카메라를 완전히 종료했다가 다시 열어 직접 촬영음을 확인합니다.
6. 해제는 **셔터음 설정 (1)** 또는 **처음 저장한 원래 값 복원**을 사용합니다.

원래 값이 0이었다면 복원도 0입니다. 복원 성공 후에만 백업을 지웁니다. 쓰기/검증 실패 시 백업은 유지합니다.
앱 삭제/데이터 초기화 시 백업도 없어지므로 먼저 복원하세요. 앱을 삭제한다고 시스템 키가 복원되지는 않습니다.

권한 구분: Root 직접 실행은 `su -c` 안에서 UID 0을 검사합니다. ADB Shizuku 모드는 UID 2000,
Root Shizuku 모드는 UID 0을 서버와 UserService 양쪽에서 검사합니다.
Shizuku의 deprecated `newProcess` 대신 AIDL UserService를 사용합니다.
서비스는 고정된 read/put(0)/put(1)만 받으며 임의 셸 명령 실행 인터페이스를 제공하지 않습니다.
명령 실행은 UI 스레드 밖에서 수행하고 프로세스에 30초 제한을 둡니다.

## 다시 빌드 (현재 PC에서 확인한 경로)

Python 3.11+, JDK 17+, Android SDK platform 36 / build-tools 36.0.0 필요.
SDK 위치를 바꾸었다면 `ANDROID_SDK_ROOT`를 설정하세요.

```powershell
cd C:\Users\USER\Desktop\suck_9.0\anti_camera_off
python build.py
# Root 전용만 빌드 (Shizuku 다운로드 불필요)
python build.py --flavor rootOnly
# 호스트 로직 테스트만
python build.py --test-only
```

`build.py`는 SDK의 aidl/javac/d8/aapt2/zipalign/apksigner를 직접 사용합니다.
Gradle을 설치하지 않아도 두 APK가 프로젝트 루트에 생성됩니다.
`build/debug.keystore`는 APK 업데이트를 위해 보관하세요. 삭제하면 새 키가 만들어져 기존 앱 위에 업데이트할 수 없습니다.

Android Studio에서는 이 폴더를 열고 SDK 경로를 지정한 후 `fullDebug` / `rootOnlyDebug`를 선택합니다.
Gradle Wrapper 8.13, AGP 8.13.2 설정도 포함했습니다.

```powershell
.\gradlew.bat :app:assembleFullDebug :app:assembleRootOnlyDebug
```

Gradle 산출물은 `app/build/outputs/apk/`에 생성됩니다.
이 작업에서 실제 확인한 빌드 경로는 **Python 직접 빌드**이며 Gradle 빌드는 별도로 실행하지 않았습니다.
배포하려면 본인 release keystore로 별도 서명하고 debug 키를 사용하지 마세요.

## 설치

```powershell
adb install -r .\AntiCameraOff-full-debug.apk
# 또는 Root 전용
adb install -r .\AntiCameraOff-rootOnly-debug.apk
```

실제 실행하는 명령은 앱을 실행한 Android 사용자의 번호를 지정합니다.
개인 프로필(사용자 0) 기준으로 사용자가 준 명령과 동일한 효과를 요청합니다.

```sh
settings --user 0 get system csc_pref_camera_forced_shuttersound_key
settings --user 0 put system csc_pref_camera_forced_shuttersound_key 0
settings --user 0 put system csc_pref_camera_forced_shuttersound_key 1
```

## 파일

- `app/src/main/.../Shell.java`: 고정 명령 생성, Root UID 검사, stdout/stderr 분리, 시간 제한
- `app/src/main/.../SettingsLogic.java`: 저장 전 백업, 재조회 검증, 복원
- `app/src/main/.../MainActivity.java`: 한국어 UI, 권한 모드 선택, 비동기 실행
- `app/src/full/.../Bridge.java`, `SettingsService.java`, `ISettingsService.aidl`: Shizuku 연결/권한/UID 검사
- `app/src/rootOnly/.../Bridge.java`: Shizuku 없는 Root 전용 구현
- `tests/LogicTest.java`: 호스트 테스트
- `TEST_RESULTS.md`: 실제 검증 범위와 실기기 확인 항목

## 공식 참고

- [Shizuku API / UserService / UID / 시작 방법](https://github.com/RikkaApps/Shizuku-API)
- [Android SDK 버전 및 minSdk](https://developer.android.com/guide/topics/manifest/uses-sdk-element)
- [AGP 8.13.2 / Gradle 8.13 / JDK 17](https://developer.android.com/build/releases/agp-8-13-0-release-notes)

이 자료들은 플랫폼/라이브러리 사용 방법의 근거이며 Samsung 설정 키의 모든 펌웨어 지원을 보장하지 않습니다.
Shizuku 라이선스는 `licenses/Shizuku-MIT.txt`에 포함했습니다.
