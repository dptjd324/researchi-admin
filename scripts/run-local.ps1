$ErrorActionPreference = "Stop"

$preferredPort = 8081

function Get-ListeningPids([int]$port) {
    $lines = cmd /c "netstat -ano | findstr :$port"
    $result = @()
    foreach ($line in $lines) {
        if ($line -match "LISTENING\s+(\d+)$") {
            $result += $matches[1]
        }
    }
    return ($result | Sort-Object -Unique)
}

function Resolve-Port([int]$port) {
    if (-not (Get-ListeningPids $port)) {
        return $port
    }

    foreach ($processId in (Get-ListeningPids $port)) {
        Write-Host "Stopping existing process on port $port (PID $processId)..."
        cmd /c "taskkill /PID $processId /F" | Out-Host
    }

    Start-Sleep -Seconds 2

    if (-not (Get-ListeningPids $port)) {
        return $port
    }

    Write-Host "Port $port is still busy. Searching for a fallback port..."
    foreach ($candidate in 8082..8090) {
        if (-not (Get-ListeningPids $candidate)) {
            return $candidate
        }
    }

    throw "No free fallback port found in 8082-8090."
}

Write-Host "Stopping Gradle daemons..."
& .\gradlew.bat --stop

$port = Resolve-Port $preferredPort

Write-Host "Starting application on port $port..."
& .\gradlew.bat clean bootRun --no-daemon --args="--server.port=$port"
