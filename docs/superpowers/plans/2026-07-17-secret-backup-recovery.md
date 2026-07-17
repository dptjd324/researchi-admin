# Secret Backup and Recovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 노트북 분실 후에도 private GitHub, SOPS/age, Bitwarden, 종이 복구 키트 및 별도 암호화 백업을 이용해 Researchi Admin을 안전하게 복구할 수 있는 실행 가능한 체계를 구축한다.

**Architecture:** GitHub에는 소스와 SOPS 암호화 설정만 저장하고, age 개인 identity는 Bitwarden과 비밀번호 기반 외부 복구 번들로 분리한다. PowerShell 검증·암호화·복구 스크립트가 평문 추적을 차단하고 암복호화 왕복 및 Git bundle 복원을 자동 검증하며, 전체 Git 이력의 Gitleaks 검사가 통과한 후에만 private 원격 저장소에 푸시한다.

**Tech Stack:** PowerShell 5.1+, Git, SOPS, age, Gitleaks, Spring Boot/Gradle, GitHub private repository, Bitwarden

## Global Constraints

- 실제 토큰, 비밀번호, age 개인 identity, SSH 개인키 및 DB 덤프를 코드, 커밋, 터미널 출력 또는 작업 로그에 노출하지 않는다.
- 평문 `application-local.yml`, `application-prod.yml`, `.env`, `*.pem`, `*.key`, `*.sql`, `*.dump`, `*.bak`은 Git에서 계속 제외한다.
- `LightsailDefaultKey-<region>.pem`은 저장하거나 암호화 커밋하지 않고 AWS에서 재다운로드한다.
- SOPS 암호화 파일은 `secrets/application-local.sops.yml`과 `secrets/application-prod.sops.yml`만 추적한다.
- age 공개 recipient만 `.sops.yaml`에 기록하고, 개인 identity는 `%APPDATA%\sops\age\keys.txt`와 Bitwarden에만 둔다.
- DB와 대용량 운영 파일은 GitHub가 아닌 별도 암호화 백업에 포함한다.
- private 저장소 확인과 전체 이력 Gitleaks 검사가 끝나기 전에는 원격 저장소로 푸시하지 않는다.
- 실행은 `superpowers:using-git-worktrees`로 만든 `codex/secret-backup-recovery` 격리 브랜치에서 수행한다. 평문 설정 원본은 기존 `C:\admin` 작업 공간에서 읽기 전용 입력으로만 사용한다.
- 기존 미커밋 파일 `src/test/java/com/researchi/admin/legacy/application/service/LegacyPublicApplicationServiceTest.java`와 `.superpowers/`는 수정, 이동, 스테이징 또는 커밋하지 않는다.

---

## File Map

- Create: `scripts/secrets/Test-SecretPolicy.ps1` — 추적 파일명과 필수 암호화 파일 정책 검사
- Create: `scripts/secrets/Test-SecretPolicy.Tests.ps1` — 임시 Git 저장소를 이용한 정책 스크립트 회귀 테스트
- Create: `scripts/secrets/Initialize-SopsAge.ps1` — age 키 위치와 `.sops.yaml` 초기화
- Create: `scripts/secrets/Protect-ApplicationConfigs.ps1` — 평문 설정을 SOPS 파일로 암호화하고 왕복 검증
- Create: `scripts/secrets/Restore-ApplicationConfigs.ps1` — SOPS 파일을 지정된 평문 경로에 안전하게 복원
- Create: `scripts/secrets/ApplicationConfigCrypto.Tests.ps1` — 임시 age 키와 가상 설정을 이용한 암복호화 테스트
- Create: `.sops.yaml` — SOPS 대상 경로와 age 공개 recipient
- Create: `secrets/application-local.sops.yml` — 실제 로컬 설정의 암호화본
- Create: `secrets/application-prod.sops.yml` — 실제 운영 설정의 암호화본
- Create: `scripts/recovery/New-RecoveryBundle.ps1` — Git bundle, SOPS 파일, Bitwarden export 및 선택 파일을 하나의 암호화 번들로 생성
- Create: `scripts/recovery/Test-RecoveryBundle.ps1` — 번들 복호화, 체크섬 및 Git bundle 유효성 검사
- Create: `scripts/recovery/RecoveryBundle.Tests.ps1` — 임시 저장소와 임시 age 키를 사용한 번들 왕복 테스트
- Create: `docs/RECOVERY.md` — 노트북 분실 대응과 새 장비 복구 절차
- Create: `docs/BACKUP-INVENTORY.md` — 백업 대상, 주기, 보존 및 검증 기록 양식
- Modify: `.gitignore` — 평문 복원물, 키, Gitleaks 보고서 및 복구 작업 디렉터리 제외
- Modify: `docs/DEPLOYMENT.md` — SOPS 복원과 분실 시 자격 증명 교체 절차 연결

---

### Task 1: Secret Policy Guard

**Files:**
- Create: `scripts/secrets/Test-SecretPolicy.Tests.ps1`
- Create: `scripts/secrets/Test-SecretPolicy.ps1`
- Modify: `.gitignore`

**Interfaces:**
- Consumes: Git 저장소 루트 경로
- Produces: `Test-SecretPolicy.ps1 -RepositoryRoot (Get-Location).Path [-RequireEncryptedConfigs]`, 성공 시 exit code `0`, 위반 시 exit code `1`

- [ ] **Step 1: Write the failing policy test**

`scripts/secrets/Test-SecretPolicy.Tests.ps1`을 다음 동작으로 작성한다.

```powershell
$ErrorActionPreference = "Stop"
$scriptUnderTest = Join-Path $PSScriptRoot "Test-SecretPolicy.ps1"
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("secret-policy-" + [guid]::NewGuid())

try {
    New-Item -ItemType Directory -Path $tempRoot | Out-Null
    git -C $tempRoot init --quiet
    git -C $tempRoot config user.email "test@example.com"
    git -C $tempRoot config user.name "Secret Policy Test"

    New-Item -ItemType Directory -Path (Join-Path $tempRoot "src/main/resources") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $tempRoot "secrets") -Force | Out-Null
    Set-Content -LiteralPath (Join-Path $tempRoot "src/main/resources/application-example.yml") -Value "app: example"
    Set-Content -LiteralPath (Join-Path $tempRoot "secrets/application-local.sops.yml") -Value "sops: encrypted"
    Set-Content -LiteralPath (Join-Path $tempRoot "secrets/application-prod.sops.yml") -Value "sops: encrypted"
    git -C $tempRoot add .

    & $scriptUnderTest -RepositoryRoot $tempRoot -RequireEncryptedConfigs
    if ($LASTEXITCODE -ne 0) {
        throw "Expected safe tracked files to pass."
    }

    Set-Content -LiteralPath (Join-Path $tempRoot "src/main/resources/application-prod.yml") -Value "password: exposed"
    git -C $tempRoot add -f src/main/resources/application-prod.yml
    & $scriptUnderTest -RepositoryRoot $tempRoot -RequireEncryptedConfigs
    if ($LASTEXITCODE -eq 0) {
        throw "Expected tracked plaintext production config to fail."
    }

    Write-Output "PASS: secret policy rejects tracked plaintext files."
}
finally {
    if (Test-Path -LiteralPath $tempRoot) {
        $resolvedTempRoot = [System.IO.Path]::GetFullPath($tempRoot)
        $resolvedSystemTemp = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
        if (-not $resolvedTempRoot.StartsWith($resolvedSystemTemp, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove a test directory outside the system temp directory."
        }
        Remove-Item -LiteralPath $resolvedTempRoot -Recurse -Force
    }
}
```

- [ ] **Step 2: Run the policy test and verify RED**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/secrets/Test-SecretPolicy.Tests.ps1
```

Expected: FAIL because `Test-SecretPolicy.ps1` does not exist.

- [ ] **Step 3: Implement the policy guard**

`scripts/secrets/Test-SecretPolicy.ps1`을 다음 정책으로 구현한다.

```powershell
param(
    [Parameter(Mandatory = $true)]
    [string]$RepositoryRoot,
    [switch]$RequireEncryptedConfigs
)

$ErrorActionPreference = "Stop"
$resolvedRoot = (Resolve-Path -LiteralPath $RepositoryRoot).Path
$trackedFiles = @(git -C $resolvedRoot ls-files)
if ($LASTEXITCODE -ne 0) {
    Write-Error "RepositoryRoot is not a readable Git repository."
    exit 1
}

$forbiddenPatterns = @(
    '(^|/)application-(local|prod|secrets)\.ya?ml$',
    '(^|/)\.env(\..+)?$',
    '\.(pem|key|p12|jks|sql|dump|bak)$',
    '(^|/)keys\.txt$',
    '(^|/)gitleaks-report\.(json|sarif|csv)$'
)

$violations = foreach ($file in $trackedFiles) {
    foreach ($pattern in $forbiddenPatterns) {
        if ($file -match $pattern) {
            $file
            break
        }
    }
}

if ($RequireEncryptedConfigs) {
    foreach ($required in @(
        'secrets/application-local.sops.yml',
        'secrets/application-prod.sops.yml'
    )) {
        if ($trackedFiles -notcontains $required) {
            $violations += "MISSING:$required"
        }
    }
}

$violations = @($violations | Sort-Object -Unique)
if ($violations.Count -gt 0) {
    Write-Error ("Secret policy violations:`n" + ($violations -join "`n"))
    exit 1
}

Write-Output "PASS: tracked files comply with the secret policy."
exit 0
```

`.gitignore`에 다음 항목을 추가한다.

```gitignore
# SOPS / recovery working files
secrets/*.plain.yml
secrets/*.dec.yml
.recovery-work/
gitleaks-report.json
gitleaks-report.sarif
*.age
*.sha256
```

- [ ] **Step 4: Run the policy test and repository check**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/secrets/Test-SecretPolicy.Tests.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/secrets/Test-SecretPolicy.ps1 -RepositoryRoot .
```

Expected:

```text
PASS: secret policy rejects tracked plaintext files.
PASS: tracked files comply with the secret policy.
```

- [ ] **Step 5: Commit the guard**

```powershell
git add .gitignore scripts/secrets/Test-SecretPolicy.ps1 scripts/secrets/Test-SecretPolicy.Tests.ps1
git commit -m "chore: add tracked secret policy guard"
```

---

### Task 2: SOPS and age Bootstrap

**Files:**
- Create: `scripts/secrets/Initialize-SopsAge.ps1`
- Create: `.sops.yaml` by running the initializer

**Interfaces:**
- Consumes: optional `-KeyFile`; default `%APPDATA%\sops\age\keys.txt`
- Produces: age identity outside the repository, public recipient in `.sops.yaml`

- [ ] **Step 1: Install and verify required local tools**

Run with explicit user approval because package installation writes outside the workspace and uses the network:

```powershell
winget install --id FiloSottile.age --exact --accept-package-agreements --accept-source-agreements
winget install --id Mozilla.SOPS --exact --accept-package-agreements --accept-source-agreements
winget install --id Gitleaks.Gitleaks --exact --accept-package-agreements --accept-source-agreements
```

Verify:

```powershell
age --version
age-keygen --version
sops --version
gitleaks version
```

Expected: all four commands exit `0` and print versions without displaying any secret.

- [ ] **Step 2: Write the initializer**

Create `scripts/secrets/Initialize-SopsAge.ps1`:

```powershell
param(
    [string]$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path,
    [string]$KeyFile = (Join-Path $env:APPDATA "sops/age/keys.txt")
)

$ErrorActionPreference = "Stop"
$keyDirectory = Split-Path -Parent $KeyFile
New-Item -ItemType Directory -Path $keyDirectory -Force | Out-Null

if (-not (Test-Path -LiteralPath $KeyFile)) {
    & age-keygen -o $KeyFile
    if ($LASTEXITCODE -ne 0) {
        throw "age-keygen failed."
    }
}

$recipient = (& age-keygen -y $KeyFile).Trim()
if ($LASTEXITCODE -ne 0 -or $recipient -notmatch '^age1[0-9a-z]+$') {
    throw "Unable to derive a valid public age recipient."
}

$configPath = Join-Path $RepositoryRoot ".sops.yaml"
$config = @(
    "creation_rules:",
    "  - path_regex: ^secrets/application-(local|prod)\.sops\.ya?ml$",
    "    age: $recipient"
) -join [Environment]::NewLine
[System.IO.File]::WriteAllText($configPath, $config + [Environment]::NewLine, [System.Text.UTF8Encoding]::new($false))

Write-Output "PASS: age identity is stored outside the repository."
Write-Output "PASS: .sops.yaml contains public recipient $recipient."
```

- [ ] **Step 3: Run the initializer without exposing the private identity**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/secrets/Initialize-SopsAge.ps1
```

Expected:

```text
PASS: age identity is stored outside the repository.
PASS: .sops.yaml contains public recipient age1...
```

Verify only locations and the public recipient:

```powershell
Test-Path (Join-Path $env:APPDATA "sops/age/keys.txt")
Get-Content .sops.yaml
```

Expected: key file exists outside the repository; `.sops.yaml` contains only the public `age1...` recipient.

- [ ] **Step 4: User gate — store the identity and recovery codes**

Pause automated work. The user performs these actions without pasting secret material into chat:

1. Create a Bitwarden account with a unique master passphrase and MFA.
2. Add `%APPDATA%\sops\age\keys.txt` to a Bitwarden secure note named `Researchi SOPS age identity`.
3. Save GitHub, Bitwarden and primary email recovery codes in a sealed paper recovery kit.
4. Record the Bitwarden master passphrase and future external-backup passphrase in the sealed paper kit.

Resume only after the user confirms the Bitwarden entry and paper recovery kit exist.

- [ ] **Step 5: Verify the Bitwarden copy through a recovery drill**

1. Copy the local key file to a temporary backup path outside the repository.
2. Remove the working key file only after verifying the absolute target path is `%APPDATA%\sops\age\keys.txt`.
3. Restore the key file manually from Bitwarden without exposing it in terminal output.
4. Compare only the public recipients:

```powershell
$restoredRecipient = (& age-keygen -y (Join-Path $env:APPDATA "sops/age/keys.txt")).Trim()
$configuredRecipient = (Select-String -Path .sops.yaml -Pattern 'age:\s+(age1[0-9a-z]+)').Matches[0].Groups[1].Value
if ($restoredRecipient -ne $configuredRecipient) { throw "Bitwarden-restored key does not match .sops.yaml." }
Write-Output "PASS: Bitwarden-restored age identity matches the repository recipient."
```

Expected:

```text
PASS: Bitwarden-restored age identity matches the repository recipient.
```

- [ ] **Step 6: Commit bootstrap files**

```powershell
git add .sops.yaml scripts/secrets/Initialize-SopsAge.ps1
git commit -m "chore: bootstrap sops age encryption"
```

---

### Task 3: Application Configuration Encryption and Restore

**Files:**
- Create: `scripts/secrets/ApplicationConfigCrypto.Tests.ps1`
- Create: `scripts/secrets/Protect-ApplicationConfigs.ps1`
- Create: `scripts/secrets/Restore-ApplicationConfigs.ps1`
- Create: `secrets/application-local.sops.yml`
- Create: `secrets/application-prod.sops.yml`

**Interfaces:**
- Consumes: two existing plaintext YAML source paths and `%APPDATA%\sops\age\keys.txt`
- Produces: two tracked SOPS files; restores plaintext only to explicitly supplied paths

- [ ] **Step 1: Write the failing crypto round-trip test**

Create `scripts/secrets/ApplicationConfigCrypto.Tests.ps1` so it:

1. Creates a temporary repository root and temporary age key.
2. Writes two YAML files containing unique synthetic values.
3. Calls `Protect-ApplicationConfigs.ps1`.
4. Calls `Restore-ApplicationConfigs.ps1`.
5. Compares SHA-256 hashes of original and restored files.
6. Searches the encrypted files and fails if either synthetic value is visible.
7. Deletes the temporary directory in `finally`.

Use these synthetic values, which are not real credentials:

```powershell
$localFixture = "spring:`n  datasource:`n    password: TEST_LOCAL_ONLY_8f1b2e"
$prodFixture = "spring:`n  datasource:`n    password: TEST_PROD_ONLY_4c9d7a"
```

Expected final output:

```text
PASS: local and prod configs round-trip without plaintext leakage.
```

- [ ] **Step 2: Run the crypto test and verify RED**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/secrets/ApplicationConfigCrypto.Tests.ps1
```

Expected: FAIL because the protect and restore scripts do not exist.

- [ ] **Step 3: Implement `Protect-ApplicationConfigs.ps1`**

Implement these exact parameters:

```powershell
param(
    [Parameter(Mandatory = $true)][string]$RepositoryRoot,
    [Parameter(Mandatory = $true)][string]$LocalConfigSource,
    [Parameter(Mandatory = $true)][string]$ProdConfigSource,
    [string]$AgeKeyFile = (Join-Path $env:APPDATA "sops/age/keys.txt")
)
```

For each source/destination pair:

```powershell
$pairs = @(
    @{ Source = $LocalConfigSource; Destination = "secrets/application-local.sops.yml" },
    @{ Source = $ProdConfigSource; Destination = "secrets/application-prod.sops.yml" }
)

$env:SOPS_AGE_KEY_FILE = (Resolve-Path -LiteralPath $AgeKeyFile).Path
New-Item -ItemType Directory -Path (Join-Path $RepositoryRoot "secrets") -Force | Out-Null

foreach ($pair in $pairs) {
    $source = (Resolve-Path -LiteralPath $pair.Source).Path
    $destination = Join-Path $RepositoryRoot $pair.Destination
    & sops encrypt --config (Join-Path $RepositoryRoot ".sops.yaml") `
        --filename-override $pair.Destination `
        --output $destination `
        $source
    if ($LASTEXITCODE -ne 0) {
        throw "SOPS encryption failed for $($pair.Destination)."
    }

    $roundTrip = Join-Path ([System.IO.Path]::GetTempPath()) ([guid]::NewGuid().ToString() + ".yml")
    try {
        & sops decrypt --output $roundTrip $destination
        if ($LASTEXITCODE -ne 0) {
            throw "SOPS round-trip decryption failed for $($pair.Destination)."
        }
        if ((Get-FileHash $source -Algorithm SHA256).Hash -ne (Get-FileHash $roundTrip -Algorithm SHA256).Hash) {
            throw "SOPS round-trip hash mismatch for $($pair.Destination)."
        }
    }
    finally {
        if (Test-Path -LiteralPath $roundTrip) {
            Remove-Item -LiteralPath $roundTrip -Force
        }
    }
}

Write-Output "PASS: application configs encrypted and round-trip verified."
```

The script must never call `Get-Content` on plaintext source files or write their contents to stdout.

- [ ] **Step 4: Implement `Restore-ApplicationConfigs.ps1`**

Implement exact parameters and overwrite protection:

```powershell
param(
    [Parameter(Mandatory = $true)][string]$RepositoryRoot,
    [Parameter(Mandatory = $true)][string]$LocalConfigDestination,
    [Parameter(Mandatory = $true)][string]$ProdConfigDestination,
    [string]$AgeKeyFile = (Join-Path $env:APPDATA "sops/age/keys.txt"),
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$env:SOPS_AGE_KEY_FILE = (Resolve-Path -LiteralPath $AgeKeyFile).Path
$pairs = @(
    @{ Source = "secrets/application-local.sops.yml"; Destination = $LocalConfigDestination },
    @{ Source = "secrets/application-prod.sops.yml"; Destination = $ProdConfigDestination }
)

foreach ($pair in $pairs) {
    $source = Join-Path $RepositoryRoot $pair.Source
    $destination = [System.IO.Path]::GetFullPath($pair.Destination)
    if ((Test-Path -LiteralPath $destination) -and -not $Force) {
        throw "Refusing to overwrite existing file: $destination"
    }
    New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
    & sops decrypt --output $destination $source
    if ($LASTEXITCODE -ne 0) {
        throw "SOPS decryption failed for $($pair.Source)."
    }
}

Write-Output "PASS: application configs restored to explicit destinations."
```

- [ ] **Step 5: Run the crypto test and verify GREEN**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/secrets/ApplicationConfigCrypto.Tests.ps1
```

Expected:

```text
PASS: local and prod configs round-trip without plaintext leakage.
```

- [ ] **Step 6: Encrypt the real ignored configuration files**

From the isolated worktree, pass the existing workspace files as read-only sources:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/secrets/Protect-ApplicationConfigs.ps1 `
  -RepositoryRoot . `
  -LocalConfigSource C:\admin\src\main\resources\application-local.yml `
  -ProdConfigSource C:\admin\src\main\resources\application-prod.yml
```

Expected:

```text
PASS: application configs encrypted and round-trip verified.
```

Do not display or diff decrypted values. Review only SOPS metadata, YAML key names, file sizes and Git status.

- [ ] **Step 7: Verify encrypted files and policy**

Run:

```powershell
$encryptedFiles = @(
  "scripts/secrets/ApplicationConfigCrypto.Tests.ps1",
  "scripts/secrets/Protect-ApplicationConfigs.ps1",
  "scripts/secrets/Restore-ApplicationConfigs.ps1",
  "secrets/application-local.sops.yml",
  "secrets/application-prod.sops.yml"
)
git add -- $encryptedFiles
$env:SOPS_AGE_KEY_FILE = Join-Path $env:APPDATA "sops/age/keys.txt"
sops filestatus secrets/application-local.sops.yml
sops filestatus secrets/application-prod.sops.yml
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/secrets/Test-SecretPolicy.ps1 -RepositoryRoot . -RequireEncryptedConfigs
git diff --cached | gitleaks stdin --redact --no-banner
```

Expected:

- both `sops filestatus` commands report encrypted files;
- policy guard reports PASS;
- the staged-diff Gitleaks scan exits `0` with no findings.

- [ ] **Step 8: Commit encrypted configuration support**

```powershell
git commit -m "feat: manage application configs with sops"
```

---

### Task 4: External Recovery Bundle

**Files:**
- Create: `scripts/recovery/RecoveryBundle.Tests.ps1`
- Create: `scripts/recovery/New-RecoveryBundle.ps1`
- Create: `scripts/recovery/Test-RecoveryBundle.ps1`

**Interfaces:**
- Consumes: repository root, output directory, optional Bitwarden encrypted export, DB dumps and operating files
- Produces: `researchi-recovery-YYYYMMDD-HHmmss.tar.age` and ciphertext SHA-256 file outside the repository

- [ ] **Step 1: Write the failing recovery bundle test**

Create `scripts/recovery/RecoveryBundle.Tests.ps1` with this test flow:

1. Create a temporary Git repository containing one commit and two synthetic SOPS fixture files.
2. Generate a temporary age identity and public recipient.
3. Call `New-RecoveryBundle.ps1 -Recipient $recipient`.
4. Call `Test-RecoveryBundle.ps1 -IdentityFile $keyFile`.
5. Assert the recovered `repository.bundle` passes `git bundle verify`.
6. Assert the synthetic additional file hash matches.
7. Delete the temporary root in `finally`.

The fixtures must contain only:

```text
synthetic encrypted config
synthetic external backup file
```

Expected final output:

```text
PASS: recovery bundle encrypts, verifies and restores all fixtures.
```

- [ ] **Step 2: Run the recovery test and verify RED**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/recovery/RecoveryBundle.Tests.ps1
```

Expected: FAIL because bundle scripts do not exist.

- [ ] **Step 3: Implement `New-RecoveryBundle.ps1`**

Implement two encryption parameter sets so automated tests use a recipient and real disaster-recovery copies use an interactive passphrase:

```powershell
[CmdletBinding(DefaultParameterSetName = "Passphrase")]
param(
    [Parameter(Mandatory = $true)][string]$RepositoryRoot,
    [Parameter(Mandatory = $true)][string]$OutputDirectory,
    [string[]]$AdditionalPath = @(),
    [Parameter(Mandatory = $true, ParameterSetName = "Recipient")][string]$Recipient,
    [Parameter(ParameterSetName = "Passphrase")][switch]$Passphrase
)
```

The script must:

1. Resolve repository and output paths.
2. Reject an output directory inside the repository.
3. Create a unique temporary directory with `New-Item`.
4. Run `git -C $RepositoryRoot bundle create <temp>\repository.bundle --all`.
5. Copy `.sops.yaml`, both SOPS files and each explicitly supplied additional path into the temporary directory.
6. Write `manifest.sha256` containing SHA-256 and relative path for every payload file.
7. Create an unencrypted tar only inside the unique temporary directory.
8. Encrypt it with one of:

```powershell
& age -r $Recipient -o $encryptedOutput $tarPath
```

or:

```powershell
& age -p -o $encryptedOutput $tarPath
```

9. Write the ciphertext hash to `<bundle>.sha256`.
10. Delete the temporary directory in `finally`.
11. Print only output paths, counts and PASS status, with `BUNDLE_PATH=<absolute path>` as the final output line.

Passphrase mode must not accept the passphrase as a command-line parameter or environment variable; `age -p` must prompt interactively.

- [ ] **Step 4: Implement `Test-RecoveryBundle.ps1`**

Implement parameter sets:

```powershell
[CmdletBinding(DefaultParameterSetName = "Passphrase")]
param(
    [Parameter(Mandatory = $true)][string]$BundlePath,
    [Parameter(Mandatory = $true)][string]$RestoreDirectory,
    [Parameter(Mandatory = $true, ParameterSetName = "Identity")][string]$IdentityFile,
    [Parameter(ParameterSetName = "Passphrase")][switch]$Passphrase
)
```

The script must:

1. Reject a non-empty restore directory.
2. Verify the adjacent ciphertext `.sha256` before decryption.
3. Decrypt with `age -d -i $IdentityFile` or interactive `age -d`.
4. Extract the tar into the restore directory.
5. Recompute every entry in `manifest.sha256` and fail on any mismatch.
6. Run `git bundle verify repository.bundle`.
7. Print only PASS status and restored relative paths.

- [ ] **Step 5: Run the recovery test and verify GREEN**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/recovery/RecoveryBundle.Tests.ps1
```

Expected:

```text
PASS: recovery bundle encrypts, verifies and restores all fixtures.
```

- [ ] **Step 6: Commit recovery bundle scripts**

```powershell
git add scripts/recovery/New-RecoveryBundle.ps1 scripts/recovery/Test-RecoveryBundle.ps1 scripts/recovery/RecoveryBundle.Tests.ps1
git commit -m "feat: add encrypted recovery bundle workflow"
```

---

### Task 5: Recovery Runbook and Backup Inventory

**Files:**
- Create: `docs/RECOVERY.md`
- Create: `docs/BACKUP-INVENTORY.md`
- Modify: `docs/DEPLOYMENT.md`

**Interfaces:**
- Consumes: scripts and file locations from Tasks 1–4
- Produces: operator-facing backup, loss response and restore instructions

- [ ] **Step 1: Write `docs/RECOVERY.md`**

Document these exact phases:

1. **Immediate loss response:** revoke GitHub, Bitwarden, AWS and email sessions; rotate GitHub tokens, AWS access keys, DB passwords, SMTP/SMS credentials; inspect and replace SSH authorized keys.
2. **New device hardening:** OS updates, full-disk encryption and MFA-capable password manager.
3. **Repository restore:** clone private GitHub or clone from `repository.bundle`.
4. **age restore:** recover `%APPDATA%\sops\age\keys.txt` from Bitwarden and compare only public recipients.
5. **configuration restore:** exact `Restore-ApplicationConfigs.ps1` command with explicit local and `/etc/researchi-admin` staging destinations.
6. **Lightsail:** re-download `LightsailDefaultKey-ap-northeast-2.pem` from AWS; never restore it from Git.
7. **DB and files:** decrypt external bundle, verify hashes, restore only into an isolated test DB first.
8. **application verification:** `gradlew.bat test`, simulation-mode startup, DB read check, public form check, controlled mail/SMS check.
9. **production enablement:** enable real sends and scheduler only after the checklist passes.

The runbook must warn not to paste credentials or private keys into terminal logs, issue trackers, chat or screenshots.

- [ ] **Step 2: Write `docs/BACKUP-INVENTORY.md`**

Include this inventory table:

| Asset | Authoritative location | Backup location | Frequency | Retention | Recovery check |
|---|---|---|---|---|---|
| Source and history | private GitHub | encrypted Git bundle | on change/monthly bundle | latest + monthly 12 | `git bundle verify` |
| Local/prod config | SOPS files in Git | encrypted recovery bundle | on change/monthly bundle | Git history + monthly 12 | hash round-trip |
| age identity | Bitwarden | password-protected Bitwarden export | monthly/on change | latest + monthly 12 | public recipient match |
| DB dumps | external encrypted storage | second encrypted copy | daily | daily 14, weekly 8, monthly 12 | isolated DB restore |
| Operational files | server storage | external encrypted storage | daily/change | daily 14, monthly 12 | checksum/sample open |
| Lightsail default key | AWS Lightsail | no file backup | AWS-managed | current | re-download path check |

Add a quarterly drill record table with date, operator, Git restore, SOPS restore, DB restore, application test, failures and corrective actions.

- [ ] **Step 3: Update deployment documentation**

In `docs/DEPLOYMENT.md`:

- replace any implication that `application-prod.yml` is manually reconstructed with a link to `docs/RECOVERY.md`;
- state that the deployment file is restored from `secrets/application-prod.sops.yml`;
- preserve simulation-first startup and scheduler-disabled requirements;
- link `docs/BACKUP-INVENTORY.md` for backup retention.

- [ ] **Step 4: Verify documentation and policy**

Run:

```powershell
$documentation = @("docs/RECOVERY.md", "docs/BACKUP-INVENTORY.md", "docs/DEPLOYMENT.md")
$unfinishedMarkers = @(("T" + "BD"), ("T" + "ODO"), ("FIX" + "ME"))
foreach ($marker in $unfinishedMarkers) {
  if (Select-String -Path $documentation -Pattern $marker -SimpleMatch) {
    throw "Unfinished documentation marker found: $marker"
  }
}
rg -n "password:\\s+[^$<{]" $documentation
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/secrets/Test-SecretPolicy.ps1 -RepositoryRoot . -RequireEncryptedConfigs
git diff --check
```

Expected:

- `rg` prints no matches;
- policy guard reports PASS;
- `git diff --check` exits `0`.

- [ ] **Step 5: Commit runbooks**

```powershell
git add docs/RECOVERY.md docs/BACKUP-INVENTORY.md docs/DEPLOYMENT.md
git commit -m "docs: add secret recovery runbooks"
```

---

### Task 6: Full Security Scan and Recovery Drill

**Files:**
- No new tracked files
- Temporary files only under an explicitly created temporary directory

**Interfaces:**
- Consumes: completed repository, local age identity and synthetic/real encrypted configuration
- Produces: evidence that history is clean and recovery succeeds without tracking plaintext

- [ ] **Step 1: Run all script tests**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/secrets/Test-SecretPolicy.Tests.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/secrets/ApplicationConfigCrypto.Tests.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/recovery/RecoveryBundle.Tests.ps1
```

Expected: all three scripts print PASS and exit `0`.

- [ ] **Step 2: Run repository and Git-history secret scans**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/secrets/Test-SecretPolicy.ps1 -RepositoryRoot . -RequireEncryptedConfigs
gitleaks git --redact --no-banner --log-opts="--all" .
```

Expected: all commands exit `0` with no findings.

If Gitleaks finds a secret, stop before any push. Record only rule ID, file and commit without copying the secret. Rotate the affected live credential first. History rewriting requires a separate explicit approval because it changes published commit identities.

- [ ] **Step 3: Run Gradle verification**

```powershell
.\gradlew.bat test
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 4: Create a real passphrase-protected recovery bundle**

The user first exports Bitwarden using its password-protected encrypted JSON format to a path outside the repository. Resolve all inputs interactively so no example path can accidentally target the repository:

```powershell
$backupRoot = (Resolve-Path -LiteralPath (Read-Host "Existing external backup directory")).Path
$bitwardenExport = (Resolve-Path -LiteralPath (Read-Host "Bitwarden encrypted export file")).Path
$databaseBackup = (Resolve-Path -LiteralPath (Read-Host "Encrypted database backup file")).Path

if ($backupRoot.StartsWith((Resolve-Path .).Path, [System.StringComparison]::OrdinalIgnoreCase)) {
  throw "External backup directory must be outside the repository."
}

$bundleOutput = powershell -NoProfile -ExecutionPolicy Bypass -File scripts/recovery/New-RecoveryBundle.ps1 `
  -RepositoryRoot . `
  -OutputDirectory $backupRoot `
  -AdditionalPath $bitwardenExport,$databaseBackup `
  -Passphrase
$bundlePath = ($bundleOutput | Where-Object { $_ -like "BUNDLE_PATH=*" } | Select-Object -Last 1).Substring(12)
```

`New-RecoveryBundle.ps1` must emit `BUNDLE_PATH=<absolute path>` as its final machine-readable output line. Before deletion or movement, resolve every target and confirm it is outside `C:\admin`.

Expected: one `.tar.age` and adjacent `.sha256` are created outside the repository without printing the passphrase.

- [ ] **Step 5: Restore the real recovery bundle to an empty temporary directory**

```powershell
$restoreRoot = Join-Path $backupRoot ("restore-test-" + [guid]::NewGuid())
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/recovery/Test-RecoveryBundle.ps1 `
  -BundlePath $bundlePath `
  -RestoreDirectory $restoreRoot `
  -Passphrase
```

Expected: ciphertext hash, all manifest hashes and `git bundle verify` pass.

- [ ] **Step 6: Verify clean Git state**

```powershell
git status --short
git log -6 --oneline
```

Expected: only intentionally ignored plaintext files remain outside Git; no plaintext secret, private key, DB dump, recovery archive or Gitleaks report is staged or tracked.

---

### Task 7: Confirm Private GitHub and Publish

**Files:**
- No new local files
- External state: existing `origin` repository

**Interfaces:**
- Consumes: clean branch and successful Task 6 evidence
- Produces: private GitHub backup of source and encrypted configuration

- [ ] **Step 1: Confirm exact origin and repository visibility**

Run:

```powershell
git remote get-url origin
```

Expected:

```text
https://github.com/dptjd324/researchi-admin.git
```

Use the connected GitHub app or authenticated GitHub UI to confirm `dptjd324/researchi-admin` visibility is `PRIVATE`. If it is public, change it to private before continuing. Do not infer visibility from the clone URL.

- [ ] **Step 2: Re-run the final push gate**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/secrets/Test-SecretPolicy.ps1 -RepositoryRoot . -RequireEncryptedConfigs
gitleaks git --redact --no-banner --log-opts="--all" .
git status --short
```

Expected: policy and both scans exit `0`; no unexpected change is present.

- [ ] **Step 3: Push the implementation branch**

```powershell
git push -u origin codex/secret-backup-recovery
```

Expected: push succeeds and the remote branch is visible only in the private repository.

- [ ] **Step 4: Open a draft pull request**

Create a draft PR from `codex/secret-backup-recovery` to `master` with:

```text
Title: Add encrypted secret backup and recovery workflow

Summary:
- enforce tracked-secret policy
- encrypt local and production configs with SOPS/age
- add encrypted recovery bundle scripts
- document loss response and quarterly restore drills

Verification:
- PowerShell secret policy tests
- SOPS round-trip tests
- recovery bundle round-trip tests
- Gitleaks working-tree and full-history scans
- Gradle test suite
```

Do not attach decrypted configs, Gitleaks reports, DB dumps, private keys or screenshots containing secret values.

---

## Final Acceptance Criteria

- [ ] GitHub repository visibility is confirmed `PRIVATE`.
- [ ] Real local and production settings exist in Git only as SOPS encrypted files.
- [ ] The age private identity is recoverable from Bitwarden and matches `.sops.yaml`.
- [ ] The sealed paper kit contains Bitwarden/GitHub recovery codes and the external backup passphrase.
- [ ] Lightsail default private key remains untracked and is documented as AWS-redownloadable.
- [ ] External recovery bundle contains a verified Git bundle, encrypted configs, Bitwarden encrypted export and selected DB/operational backups.
- [ ] Secret-policy tests, encryption round-trip tests, recovery-bundle tests and Gradle tests pass.
- [ ] Gitleaks working-tree and full-history scans return no findings.
- [ ] A real restore drill completes before the implementation branch is pushed.
