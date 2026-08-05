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
