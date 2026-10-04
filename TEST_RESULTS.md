# 검증 결과

2026-10-04 (Asia/Seoul)

## 확인 완료

- Python 직접 빌드로 full / rootOnly Java 컴파일, AIDL 생성(full), Dex 생성, APK 패키징 성공.
- 두 APK 모두 apksigner 서명 검증 통과 (v3; minSdk 28은 v3 지원).
- aapt2 badging: 두 APK 모두 minSdk 28 / targetSdk 36.
- rootOnly 패키지 `kr.local.anticamera.root`, full 패키지 `kr.local.anticamera`.
- 두 APK에 native `lib/` 파일 없음; ABI 제한 없는 Dex 패키징.
- 호스트 로직 테스트 **34개 검사 통과**:
  최초 백업, 반복 적용 시 백업 유지, 원래 0/1 복원, 백업 없을 때 복원 거부,
  null/빈 값/비정상 값/오류 문자열 거부, 백업 저장 실패 시 쓰기 중단,
  쓰기 실패·쓰기 무시 시 백업 유지, 잘못된 백업 복원 거부,
  명령 허용 목록/사용자 번호 확인, nonzero exit/stderr 오류 처리.
- 빌드 출력: `build-output.log`.

## 사용자 실기기 확인

2026-10-04 사용자가 정상 작동한다고 확인했습니다. 기기 모델, Android 버전,
사용한 권한 모드는 제공되지 않았으므로 모든 지원 조합의 검증으로 확대 해석하지 않습니다.

## 개발 환경에서 미검증

`adb devices`에 연결된 기기가 없어 앱 설치·UI·Root 실행·Shizuku 바인딩·촬영음은 미검증입니다.
Android 9~17 / 32·64비트 실기기 전체 조합을 테스트한 결과가 아닙니다.
Gradle Wrapper 설정을 제공했으나 Gradle 빌드는 미실행입니다.

## 실기기 검사 체크리스트

각 OS/기기에서 다음을 독립적으로 확인하세요.

1. 두 APK의 설치/실행 및 화면 회전/시스템 바/한국어 UI.
2. Root 거부, Root 승인, su 없음, 승인 시간 초과.
3. Shizuku 미실행, 권한 거부/승인, ADB UID 2000 연결.
4. Root Shizuku UID 0 연결; UID와 선택 모드가 다르면 오류 표시.
5. Shizuku 서비스 종료/재시작 및 작업 중 앱 종료 후 재조회.
6. 변경 전 `settings --user USER_ID get system ...` 값 기록.
7. 무음 적용 뒤 재조회 0, 실제 카메라 촬영음 확인.
8. 셔터음 적용 뒤 재조회 1, 실제 카메라 촬영음 확인.
9. 원래 값 복원 뒤 최초 기록과 동일한 값인지 확인.
10. 설정 키 없는 기기 또는 키 값이 0/1이 아닌 기기에서 쓰기 거부.
11. Android 9/10의 PC ADB 시작 경로, 11+의 무선 디버깅 경로.
12. 32비트/64비트 실기기, 재부팅/OTA 후 설정 유지 여부.

설정 키 저장만 성공하고 카메라 소리가 계속 나는 기기는 이 방식의 실제 무음 지원 대상이 아닙니다.
