# v1.0.0 — 카메라 무음 설정

## 다운로드 선택

- **AntiCameraOff-full-debug.apk**: Root(su), Shizuku(ADB/Shell), Root Shizuku 지원
- **AntiCameraOff-rootOnly-debug.apk**: Root 전용. Shizuku 설치 불필요
- **SHA256SUMS.txt**: APK 무결성 확인용 SHA-256

## 기능

- `csc_pref_camera_forced_shuttersound_key`를 0(무음 요청) / 1(셔터음 요청)로 변경
- 현재 값 조회, 최초 원래 값 백업, 원래 값 복원
- 쓰기 후 재조회 검증, 권한/UID 확인, 시간 제한 및 오류 표시
- 32·64비트 공용 Java/Dex APK, Android 9 이상 설치 대상으로 작성

## 확인 결과 및 제한

- 사용자 실기기 정상 작동 확인 (2026-10-04). 기기 모델/OS/권한 모드는 미제공
- 두 APK 빌드·서명 검증 성공, 호스트 로직 검사 34개 통과
- Android 9~17 전체 기기/권한 조합의 실기기 테스트 완료를 의미하지 않음
- API 36 타깃; Android 17은 호환 실행 대상으로 작성했으며 별도 검증 필요
- 카메라가 이 설정 키를 반영하는 펌웨어에서만 실제 무음 가능
- 촬영 앱이 아닌 시스템 설정 도구. 재부팅 자동 적용 및 볼륨 조작 없음
- **로컬 debug 키로 서명한 APK**. 업데이트 시 같은 서명 키 필요
- 통합/Root 전용 중 하나를 선택해서 사용 권장 (설정 키는 공용, 백업은 앱별)

사용법·소스 빌드 방법은 저장소 README를 참고하세요.
