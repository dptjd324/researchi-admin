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

    $safeOutput = & powershell -NoProfile -ExecutionPolicy Bypass -File $scriptUnderTest -RepositoryRoot $tempRoot -RequireEncryptedConfigs 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Expected safe tracked files to pass."
    }
    if (($safeOutput | Out-String) -notmatch "PASS: tracked files comply with the secret policy\.") {
        throw "Expected safe tracked files to produce the policy pass message."
    }

    Set-Content -LiteralPath (Join-Path $tempRoot "src/main/resources/application-prod.yml") -Value "password: exposed"
    git -C $tempRoot add -f src/main/resources/application-prod.yml
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $violationOutput = & powershell -NoProfile -ExecutionPolicy Bypass -File $scriptUnderTest -RepositoryRoot $tempRoot -RequireEncryptedConfigs 2>&1
        $violationExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($violationExitCode -eq 0) {
        throw "Expected tracked plaintext production config to fail."
    }
    $renderedViolation = $violationOutput | Out-String
    if ($renderedViolation -notmatch "Secret policy violations") {
        throw "Expected forbidden tracked file to report a secret policy violation."
    }
    if ($renderedViolation -notmatch "src/main/resources/application-prod\.yml") {
        throw "Expected forbidden tracked file to be identified in the policy violation."
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
