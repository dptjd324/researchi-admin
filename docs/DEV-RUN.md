# 개발 서버 실행/재시작 가이드

## 권장 방식

개발 중에는 IntelliJ `Application` 실행 구성을 쓰는 것이 가장 안정적입니다.

- 설정 이름: `Researchi Admin Local Server (8081)`
- 장점:
  - 템플릿/자바 수정 후 IDE의 재실행 버튼으로 바로 반영 가능
  - 별도 `bootJar` 생성 없이 클래스 기준으로 실행되어 반복 개발이 빠름
  - `local` 프로필과 `8081` 포트가 고정되어 접속 주소가 일관적임

## 언제 `BootRun`을 쓰면 좋은가

`Researchi Admin BootRun Local (8081)`은 Gradle 실행 흐름을 확인할 때 적합합니다.

- Gradle 기반 실행 문제를 같이 점검할 수 있음
- 다만 일반적인 UI 수정 확인에는 `Application` 실행보다 느림

## 언제 스크립트를 쓰면 좋은가

`scripts/start-local.ps1`, `scripts/stop-local.ps1`는 IDE 밖에서 JAR 실행 상태를 확인할 때 사용합니다.

- JAR 기준 실행 동작 점검
- 로그 파일:
  - `build/local-server.out.log`
  - `build/local-server.err.log`
- PID 파일:
  - `build/local-server.pid`

## 추천 재시작 순서

1. 평소 개발:
   - IntelliJ에서 `Researchi Admin Local Server (8081)` 실행
   - 변경 후 `Rerun` 사용
2. Gradle 실행 검증 필요:
   - `Researchi Admin BootRun Local (8081)` 실행
3. IDE 없이 JAR 검증 필요:
   - `powershell -ExecutionPolicy Bypass -File scripts\stop-local.ps1`
   - `powershell -ExecutionPolicy Bypass -File scripts\start-local.ps1`

## 접속 주소

- 로그인: `http://127.0.0.1:8081/login`
