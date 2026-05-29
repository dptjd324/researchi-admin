# 배포 가이드

이 문서는 Researchi Admin을 운영 환경에 배포할 때 필요한 설정과 검증 절차를 정리합니다.

이 프로젝트는 개인정보, 메일/SMS 발송, 대용량 운영 DB를 다루므로 배포 시 secret 분리와 실발송 제어가 중요합니다.

## 배포 방향

애플리케이션은 다음 설정을 사용합니다.

- `ADMIN_DB_URL`: 보조 admin 테이블 DB
- `OLD_ADMIN_DB_URL`: `TB_RESEARCH_MST`, `TB_RESEARCH_APP`, `TB_BLACKLIST_MST`가 포함된 기존 운영 DB 복사본 또는 운영 DB
- SMTP 설정: 메일 발송
- Naver SENS 설정: SMS 발송

클라우드 서버에서는 `prod` profile을 사용합니다. `local` profile은 개발자 PC 전용으로 유지합니다.

운영 secret은 JAR 내부에 포함하지 않습니다. 운영 서버 전용 설정 파일을 생성해 외부에서 주입합니다.

예시 위치:

```text
/etc/researchi-admin/application-prod.yml
```

템플릿:

```text
src/main/resources/application-prod.yml.example
```

## 저장소에 올리면 안 되는 파일

- `src/main/resources/application-local.yml`
- `.env`, `.env.*`
- `*.sql`, `*.dump`, `*.bak` 등 DB dump
- `*.pem`, `*.key`, `*.p12`, `*.jks` 등 private key/keystore
- local `uploads/`, `exports/`, `build/`, `.gradle/`, `.idea/`, `.vs/`

## 필수 환경 변수

- `ADMIN_DB_URL`
- `ADMIN_DB_USERNAME`
- `ADMIN_DB_PASSWORD`
- `OLD_ADMIN_DB_URL`
- `OLD_ADMIN_DB_USERNAME`
- `OLD_ADMIN_DB_PASSWORD`
- `SMTP_HOST`
- `SMTP_PORT`
- `SMTP_USERNAME`
- `SMTP_PASSWORD`
- `APP_BASE_URL`
- `APP_SMS_SIMULATE_SEND`
- `APP_SMS_PROVIDER`
- `APP_SMS_ACCESS_KEY`
- `APP_SMS_SECRET_KEY`
- `APP_SMS_SERVICE_ID`
- `APP_SMS_FROM_NUMBER`
- `ENCRYPTION_KEY`
- `PHONE_HASH_KEY`
- `APP_SCHEDULER_ENABLED`
- `APP_SCHEDULER_SCHEDULED_SEND_CRON`
- `APP_SCHEDULER_THRESHOLD_CRON`
- `APP_SCHEDULER_CLEANUP_CRON`
- `APP_EXPORT_PATH`
- `APP_UPLOAD_PATH`

전체 운영 설정 예시는 `src/main/resources/application-prod.yml.example`을 참고합니다.

## 배포 순서

1. admin DB와 old admin DB를 백업합니다.
2. 로컬에서 테스트를 실행하고 실패 항목이 있으면 기록합니다.
3. 애플리케이션을 build합니다.
4. 서버에 `/etc/researchi-admin/application-prod.yml`을 생성합니다.
5. `APP_EXPORT_PATH`, `APP_UPLOAD_PATH`를 만들고 애플리케이션 사용자에게 쓰기 권한을 부여합니다.
6. 필요한 보조 admin schema가 적용되었는지 확인합니다.
7. `spring.profiles.active=prod`와 외부 설정 파일을 지정해 애플리케이션을 실행합니다.
8. `/login` 접속을 확인합니다.
9. `/dashboard` 접속을 확인합니다.
10. `/research` 목록 조회를 확인합니다.
11. `/research/{researchNo}/apply` 공개 신청 링크를 1개 이상 확인합니다.
12. matching, blacklist 흐름을 확인합니다.
13. 첫 smoke test에서는 mail/SMS simulation을 유지합니다.
14. 통제된 수신자에게만 mail/SMS를 테스트한 뒤 실발송으로 전환합니다.

## 실행 명령 예시

```powershell
java -jar researchi-admin.jar --spring.profiles.active=prod --spring.config.additional-location=file:/etc/researchi-admin/application-prod.yml
```

이 명령으로 직접 검증한 뒤 서버의 process manager 또는 service runner에 등록합니다.

## 첫 배포 안전 설정

첫 배포는 실발송과 scheduler를 비활성화한 상태로 시작합니다.

```yaml
app:
  mail:
    simulate-send: true
  sms:
    simulate-send: true
  scheduler:
    enabled: false
```

로그인, 대시보드, 리서치 목록, 공개 신청 폼, export path, matching, blacklist 확인이 끝난 뒤 아래 순서로 전환합니다.

1. 통제된 수신자에게 메일 실발송을 테스트합니다.
2. 통제된 수신자에게 SMS 실발송을 테스트합니다.
3. 두 테스트가 성공한 뒤 scheduler를 활성화합니다.

## 운영 체크리스트

- HTTPS 강제 적용
- 관리자 접속 로그 확인
- `/dashboard`에서 월별 메일/SMS 건수 확인
- 주기적인 DB 백업 확인
- 메일/SMS 설정 검증 전 scheduler 비활성화 유지
- export 파일 저장 경로 권한 확인

## Rollback 준비

- 이전 버전 JAR 보관
- 배포 직전 DB 백업 보관
- 배포 당시 환경 변수 기록
- 메일/SMS 오발송 발생 시 우선 아래 설정으로 전환

```text
APP_SMS_SIMULATE_SEND=true
APP_MAIL_SIMULATE_SEND=true
APP_SCHEDULER_ENABLED=false
```

## 포트폴리오 관점의 품질 포인트

- 운영 secret을 코드와 분리했습니다.
- 실발송 기능은 simulation mode로 안전하게 검증할 수 있도록 설계했습니다.
- DB 백업, smoke test, rollback 절차를 문서화해 운영 안정성을 고려했습니다.
