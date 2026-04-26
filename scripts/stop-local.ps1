$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$pidFile = Join-Path $root "build\local-server.pid"

function Get-PortOwnerId {
    $lines = cmd /c "netstat -ano | findstr :8081"
    foreach ($line in $lines) {
        if ($line -match "LISTENING\s+(\d+)$") {
            return $matches[1]
        }
    }
    return $null
}

if (Test-Path $pidFile) {
    $pidValue = (Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1).Trim()
} else {
    $pidValue = $null
}

if (-not $pidValue) {
    $pidValue = Get-PortOwnerId
}

if (-not $pidValue) {
    Remove-Item $pidFile -ErrorAction SilentlyContinue
    Write-Output "No running local server found."
    exit 0
}

$process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
if (-not $process) {
    Remove-Item $pidFile -ErrorAction SilentlyContinue
    Write-Output "Process already stopped. PID file removed."
    exit 0
}

Stop-Process -Id $pidValue -Force
Remove-Item $pidFile -ErrorAction SilentlyContinue
Write-Output "Server stopped. PID=$pidValue"
