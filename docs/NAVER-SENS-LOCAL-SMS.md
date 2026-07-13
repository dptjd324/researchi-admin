# Naver Cloud SENS SMS 로컬 테스트

이 문서는 로컬 환경에서 Naver Cloud SENS를 사용해 SMS 발송을 테스트하는 방법을 정리합니다.

SMS는 실제 비용과 운영 리스크가 발생할 수 있으므로 기본값은 simulation mode로 유지합니다. 실제 발송 테스트가 필요할 때만 `APP_SMS_SIMULATE_SEND=false`로 실행합니다.

## 1. Naver Cloud에서 준비할 값

- Access Key ID
- Secret Key
- SENS SMS Service ID
- SENS에 등록 및 인증된 발신 번호

## 2. PowerShell 실행 예시

```powershell
$env:APP_SMS_SIMULATE_SEND="false"
$env:APP_SMS_PROVIDER="naver-sens"
$env:APP_SMS_ACCESS_KEY="YOUR_ACCESS_KEY_ID"
$env:APP_SMS_SECRET_KEY="YOUR_SECRET_KEY"
$env:APP_SMS_SERVICE_ID="ncp:sms:kr:000000000000:your-service"
$env:APP_SMS_FROM_NUMBER="01012345678"
$env:APP_SMS_MESSAGE_TYPE="SMS"

.\gradlew.bat bootRun
```

긴 문구를 발송해야 할 경우 `APP_SMS_MESSAGE_TYPE=LMS`를 사용합니다.

## 2-1. `application-local.yml` 방식

`src/main/resources/application-local.yml`은 Git에서 제외되어 있으므로 로컬 테스트 값을 둘 수 있습니다.

```yaml
app:
  sms:
    simulate-send: false
    provider: naver-sens
    access-key: "YOUR_ACCESS_KEY_ID"
    secret-key: "YOUR_SECRET_KEY"
    service-id: "ncp:sms:kr:000000000000:your-service"
    from-number: "01012345678"
    base-url: "https://sens.apigw.ntruss.com"
    message-type: "SMS"
```

테스트가 끝나면 반드시 `simulate-send: true`로 되돌립니다.

## 3. 로컬 테스트 순서

1. 위 환경 변수로 서버를 실행합니다.
2. 매칭 화면에서 SMS 발송 대상 결과를 만듭니다.
3. 수신 번호가 실제 테스트 가능한 번호인지 확인합니다.
4. `SMS 발송` 버튼을 누릅니다.
5. 실패하면 서버 로그와 Naver SENS 응답 메시지를 확인합니다.

## 4. 자주 발생하는 오류

- `401` 또는 signature 오류:
  - Access Key, Secret Key, Service ID, 서버 시간 차이를 확인합니다.
- 발신 번호 오류:
  - `APP_SMS_FROM_NUMBER`가 SENS에 등록된 발신 번호와 같은지 확인합니다.
- 수신 번호 오류:
  - 하이픈 없이 숫자만 전송되도록 정규화 로직을 확인합니다.
- 비용 발생:
  - `APP_SMS_SIMULATE_SEND=false`는 실제 발송이며 과금될 수 있습니다.

## 품질 포인트

- SMS 실발송은 운영 리스크가 크므로 simulation mode를 기본값으로 유지합니다.
- secret은 Git에 올리지 않고 환경 변수 또는 gitignore된 local 설정에만 둡니다.
- 발송 결과는 `admin_notification_log`에 기록해 추적 가능하게 관리합니다.
