# 개발 서버 실행 가이드

이 문서는 로컬 개발 환경에서 Researchi Admin을 실행하고 검증하는 방법을 정리합니다.

## 권장 방식

일반 개발 중에는 IntelliJ의 `Application` 실행 구성을 사용하는 것이 가장 안정적입니다.

- 실행 구성 이름: `Researchi Admin Local Server (8081)`
- 권장 이유:
  - Java/Thymeleaf 수정 후 IDE에서 빠르게 재실행 가능
  - 별도 `bootJar` 생성 없이 classpath 기준으로 실행 가능
  - `local` profile과 `8081` port를 고정해 접속 주소가 일관됨

## Gradle BootRun을 사용하는 경우

`Researchi Admin BootRun Local (8081)`은 Gradle 실행 흐름을 검증할 때 사용합니다.

- Gradle 기반 실행 문제를 함께 확인할 수 있음
- 일반적인 UI 수정 확인에는 IntelliJ Application 실행보다 느릴 수 있음

## 스크립트 실행을 사용하는 경우

IDE 밖에서 JAR 실행 상태를 확인해야 할 때 아래 스크립트를 사용합니다.

```powershell
powershell -ExecutionPolicy Bypass -File scripts\stop-local.ps1
powershell -ExecutionPolicy Bypass -File scripts\start-local.ps1
```

스크립트 실행 시 생성되는 파일:

- 로그 파일:
  - `build/local-server.out.log`
  - `build/local-server.err.log`
- PID 파일:
  - `build/local-server.pid`

## 추천 개발 흐름

1. 평소 개발:
   - IntelliJ에서 `Researchi Admin Local Server (8081)` 실행
   - 변경 후 `Rerun`으로 빠르게 확인
2. Gradle 실행 검증:
   - `Researchi Admin BootRun Local (8081)` 실행
3. IDE 없이 JAR 검증:
   - `scripts/stop-local.ps1`
   - `scripts/start-local.ps1`

## 접속 주소

- 로그인: `http://127.0.0.1:8081/login`
- 대시보드: `http://127.0.0.1:8081/dashboard`
- 리서치 목록: `http://127.0.0.1:8081/research`

## 로컬 검증 체크리스트

- 로그인 성공/실패 확인
- `/research` 목록 조회 확인
- 특정 공고 상세 화면 확인
- 공개 신청 폼 `/research/{researchNo}/apply` 확인
- 신청자 목록과 제공 처리 확인
- export 파일 생성 확인
- mail/SMS는 기본적으로 simulation mode에서 확인

## 품질 포인트

로컬 실행 문서를 별도로 둔 이유는 개발 중 발생하는 실행 환경 차이를 줄이고, 기능 수정 후 재현 가능한 방식으로 검증하기 위해서입니다.
