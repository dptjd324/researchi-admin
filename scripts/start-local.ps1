$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root "build"
$pidFile = Join-Path $logDir "local-server.pid"
$outLog = Join-Path $logDir "local-server.out.log"
$errLog = Join-Path $logDir "local-server.err.log"
$jarPath = Join-Path $root "build\libs\admin-0.0.1-SNAPSHOT.jar"
$loginUrl = "http://127.0.0.1:8081/login"

function Get-PortOwnerId {
    $lines = cmd /c "netstat -ano | findstr :8081"
    foreach ($line in $lines) {
        if ($line -match "LISTENING\s+(\d+)$") {
            return $matches[1]
        }
    }
    return $null
}

if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir | Out-Null
}

try {
    $existingResponse = Invoke-WebRequest -UseBasicParsing $loginUrl -TimeoutSec 5
    $portOwnerId = Get-PortOwnerId
    if ($portOwnerId) {
        $portOwnerId | Set-Content $pidFile
    }
    Write-Output "Server already running. PID=$portOwnerId URL=$loginUrl"
    Write-Output "HTTP: $($existingResponse.StatusCode) $($existingResponse.Headers['Content-Type'])"
    exit 0
} catch {
    if (Test-Path $pidFile) {
        $existingPid = (Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1).Trim()
        if ($existingPid) {
            $existing = Get-Process -Id $existingPid -ErrorAction SilentlyContinue
            if ($existing) {
                Stop-Process -Id $existingPid -Force -ErrorAction SilentlyContinue
                Start-Sleep -Seconds 2
            }
        }
        Remove-Item $pidFile -ErrorAction SilentlyContinue
    }
}

Push-Location $root
try {
    & ".\gradlew.bat" bootJar | Out-Host

    $process = Start-Process `
        -FilePath "java" `
        -ArgumentList "-jar", $jarPath `
        -WorkingDirectory $root `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog `
        -PassThru

    $process.Id | Set-Content $pidFile

    Start-Sleep -Seconds 14

    $alive = Get-Process -Id $process.Id -ErrorAction SilentlyContinue
    if (-not $alive) {
        Write-Output "Server process exited early. Check logs:"
        Write-Output "  $outLog"
        Write-Output "  $errLog"
        exit 1
    }

    try {
        $response = Invoke-WebRequest -UseBasicParsing $loginUrl -TimeoutSec 8
        $portOwnerId = Get-PortOwnerId
        if ($portOwnerId) {
            $portOwnerId | Set-Content $pidFile
        }
        Write-Output "Server started. PID=$portOwnerId"
        Write-Output "Login URL: $loginUrl"
        Write-Output "HTTP: $($response.StatusCode) $($response.Headers['Content-Type'])"
    } catch {
        Write-Output "Server started but login check failed. PID=$($process.Id)"
        Write-Output "Login URL: $loginUrl"
        Write-Output "Check logs:"
        Write-Output "  $outLog"
        Write-Output "  $errLog"
        exit 1
    }
}
finally {
    Pop-Location
}
