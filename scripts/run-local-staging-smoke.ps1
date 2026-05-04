param(
    [switch]$Stop,
    [string]$Schema = "",
    [int]$BackendPort = 18083,
    [int]$FrontendPort = 5174
)

$ErrorActionPreference = "Stop"

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$BackendDir = Join-Path $RootDir "backend"
$FrontendDir = Join-Path $RootDir "frontend"
$StateDir = Join-Path $RootDir ".tmp\local-staging-smoke"
$StateFile = Join-Path $StateDir "state.json"

function Import-DotEnv {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        return
    }

    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) {
            continue
        }

        $separatorIndex = $trimmed.IndexOf("=")
        if ($separatorIndex -le 0) {
            continue
        }

        $name = $trimmed.Substring(0, $separatorIndex).Trim()
        $value = $trimmed.Substring($separatorIndex + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

function Find-MySqlClient {
    $candidates = @(
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe",
        "mysql"
    )

    foreach ($candidate in $candidates) {
        if ($candidate -eq "mysql") {
            $command = Get-Command mysql -ErrorAction SilentlyContinue
            if ($command) {
                return $command.Source
            }
            continue
        }

        if (Test-Path $candidate) {
            return $candidate
        }
    }

    throw "[staging-smoke] mysql client not found. Add MySQL bin to Path or install MySQL client tools."
}

function Get-ListeningProcessIds {
    param([int]$Port)

    $connections = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue |
        Where-Object { $_.State -eq "Listen" }

    return @($connections | Select-Object -ExpandProperty OwningProcess -Unique)
}

function Assert-PortFree {
    param(
        [int]$Port,
        [string]$Purpose
    )

    $processIds = Get-ListeningProcessIds -Port $Port
    if ($processIds.Count -gt 0) {
        $joined = $processIds -join ", "
        throw "[staging-smoke] Port $Port is already in use for $Purpose. Existing PID(s): $joined. Choose another port."
    }
}

function Wait-HttpOk {
    param(
        [string]$Url,
        [int]$TimeoutSeconds,
        [System.Diagnostics.Process]$Process,
        [string]$Name
    )

    for ($i = 0; $i -lt $TimeoutSeconds; $i++) {
        Start-Sleep -Seconds 1
        if ($null -ne $Process -and $Process.HasExited) {
            $Process.Refresh()
            throw "[staging-smoke] $Name exited early with code $($Process.ExitCode). Check logs under $StateDir."
        }

        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                return
            }
        } catch {
            continue
        }
    }

    throw "[staging-smoke] $Name did not become reachable in ${TimeoutSeconds}s: $Url"
}

function Wait-HealthUp {
    param(
        [string]$Url,
        [int]$TimeoutSeconds,
        [System.Diagnostics.Process]$Process
    )

    for ($i = 0; $i -lt $TimeoutSeconds; $i++) {
        Start-Sleep -Seconds 1
        if ($null -ne $Process -and $Process.HasExited) {
            $Process.Refresh()
            throw "[staging-smoke] backend exited early with code $($Process.ExitCode). Check logs under $StateDir."
        }

        try {
            $health = Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec 2
            if ($health.status -eq "UP") {
                return
            }
        } catch {
            continue
        }
    }

    throw "[staging-smoke] backend health did not become UP in ${TimeoutSeconds}s: $Url"
}

function Stop-SmokeProcessTree {
    param([int]$ProcessId)

    try {
        $process = Get-Process -Id $ProcessId -ErrorAction Stop
        $processName = $process.ProcessName
        Stop-Process -Id $ProcessId -Force -ErrorAction Stop
        return "${ProcessId}:$processName"
    } catch {
        return $null
    }
}

function Stop-RecordedProcesses {
    if (-not (Test-Path $StateFile)) {
        Write-Host "[staging-smoke] No state file found: $StateFile"
        return
    }

    $state = Get-Content -LiteralPath $StateFile -Raw -Encoding UTF8 | ConvertFrom-Json
    $stopped = @()
    foreach ($processInfo in @($state.processes)) {
        $processId = [int]$processInfo.pid
        $stoppedProcess = Stop-SmokeProcessTree -ProcessId $processId
        if (-not [string]::IsNullOrWhiteSpace($stoppedProcess)) {
            $stopped += $stoppedProcess
        } else {
            Write-Host "[staging-smoke] Already stopped or unavailable: PID $processId"
        }
    }

    Remove-Item -LiteralPath $StateFile -Force -ErrorAction SilentlyContinue
    if ($stopped.Count -gt 0) {
        Write-Host "[staging-smoke] Stopped: $($stopped -join ', ')"
    }
}

if ($Stop) {
    Stop-RecordedProcesses
    exit 0
}

. (Join-Path $PSScriptRoot "dev-env.ps1") -Quiet
Import-DotEnv (Join-Path $RootDir ".env")
Import-DotEnv (Join-Path $BackendDir ".env")

if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME) -or [string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    throw "[staging-smoke] DB_USERNAME and DB_PASSWORD must be set in .env or the process environment."
}

$mysql = Find-MySqlClient
if ([string]::IsNullOrWhiteSpace($Schema)) {
    if (-not [string]::IsNullOrWhiteSpace($env:HONEY_STAGING_SMOKE_SCHEMA)) {
        $Schema = $env:HONEY_STAGING_SMOKE_SCHEMA
    } else {
        $env:MYSQL_PWD = $env:DB_PASSWORD
        $schemaRows = & $mysql -h localhost -P 3306 -u $env:DB_USERNAME -N -e "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME LIKE 'honey_stage_rehearsal_%' ORDER BY SCHEMA_NAME DESC LIMIT 1;"
        $Schema = @($schemaRows | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -First 1)
    }
}

if ([string]::IsNullOrWhiteSpace($Schema)) {
    throw "[staging-smoke] No staging rehearsal schema found. Pass -Schema or run the seed/bootstrap rehearsal first."
}
if ($Schema -notmatch '^[A-Za-z0-9_]+$') {
    throw "[staging-smoke] Schema contains unsupported characters: $Schema"
}

$env:MYSQL_PWD = $env:DB_PASSWORD
$schemaExists = & $mysql -h localhost -P 3306 -u $env:DB_USERNAME -N -e "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '$Schema';"
if ([int]$schemaExists -ne 1) {
    throw "[staging-smoke] Schema does not exist: $Schema"
}

Assert-PortFree -Port $BackendPort -Purpose "backend"
Assert-PortFree -Port $FrontendPort -Purpose "frontend"

$jar = Join-Path $BackendDir "build\libs\honeytong-backend-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $jar)) {
    throw "[staging-smoke] Backend jar not found: $jar. Build it first with scripts/run-backend-gradle.ps1 bootJar"
}

$java = $env:HONEY_JAVA_EXE
if ([string]::IsNullOrWhiteSpace($java) -or -not (Test-Path $java)) {
    $java = Join-Path $env:JAVA_HOME "bin\java.exe"
}
if (-not (Test-Path $java)) {
    throw "[staging-smoke] Java executable not found. Run scripts/check-dev-env.ps1."
}

if ([string]::IsNullOrWhiteSpace($env:HONEY_NPM_CMD) -or -not (Test-Path $env:HONEY_NPM_CMD)) {
    throw "[staging-smoke] npm.cmd not found. Run scripts/check-dev-env.ps1."
}

New-Item -ItemType Directory -Force -Path $StateDir | Out-Null

$backendUrl = "http://127.0.0.1:$BackendPort"
$frontendUrl = "http://127.0.0.1:$FrontendPort"
$healthUrl = "$backendUrl/actuator/health"
$backendOutLog = Join-Path $StateDir "backend.out.log"
$backendErrLog = Join-Path $StateDir "backend.err.log"
$backendAppLog = Join-Path $StateDir "backend.app.log"
$frontendOutLog = Join-Path $StateDir "frontend.out.log"
$frontendErrLog = Join-Path $StateDir "frontend.err.log"

$originalEnv = @{}
$envKeys = @(
    "SPRING_PROFILES_ACTIVE",
    "DB_URL",
    "SERVER_PORT",
    "JWT_SECRET",
    "CORS_ALLOWED_ORIGINS",
    "FLYWAY_ENABLED",
    "JPA_DDL_AUTO",
    "REGION_SEED_ENABLED",
    "POLICY_SEED_ENABLED",
    "ADMIN_BOOTSTRAP_ENABLED",
    "PHONE_VERIFICATION_SENDER_PROVIDER",
    "CACHE_TYPE",
    "APP_REDIS_ENABLED",
    "REDIS_HEALTH_ENABLED",
    "SPRING_JPA_SHOW_SQL",
    "LOGGING_LEVEL_ORG_HIBERNATE_SQL",
    "LOGGING_LEVEL_ORG_HIBERNATE_ORM_JDBC_BIND",
    "LOG_FILE",
    "VITE_API_BASE_URL"
)
foreach ($key in $envKeys) {
    $originalEnv[$key] = [Environment]::GetEnvironmentVariable($key, "Process")
}

$backendProcess = $null
$frontendProcess = $null
try {
    $env:SPRING_PROFILES_ACTIVE = "prod"
    $env:DB_URL = "jdbc:mysql://localhost:3306/$Schema`?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true"
    $env:SERVER_PORT = [string]$BackendPort
    $env:JWT_SECRET = "local-staging-smoke-jwt-secret-20260504-long-enough-for-hmac"
    $env:CORS_ALLOWED_ORIGINS = "http://localhost:$FrontendPort,http://127.0.0.1:$FrontendPort"
    $env:FLYWAY_ENABLED = "true"
    $env:JPA_DDL_AUTO = "validate"
    $env:REGION_SEED_ENABLED = "false"
    $env:POLICY_SEED_ENABLED = "false"
    $env:ADMIN_BOOTSTRAP_ENABLED = "false"
    $env:PHONE_VERIFICATION_SENDER_PROVIDER = "solapi"
    $env:CACHE_TYPE = "none"
    $env:APP_REDIS_ENABLED = "false"
    $env:REDIS_HEALTH_ENABLED = "false"
    $env:SPRING_JPA_SHOW_SQL = "false"
    $env:LOGGING_LEVEL_ORG_HIBERNATE_SQL = "WARN"
    $env:LOGGING_LEVEL_ORG_HIBERNATE_ORM_JDBC_BIND = "WARN"
    $env:LOG_FILE = $backendAppLog

    $backendProcess = Start-Process `
        -FilePath $java `
        -ArgumentList @("-jar", $jar) `
        -WorkingDirectory $BackendDir `
        -WindowStyle Hidden `
        -PassThru `
        -RedirectStandardOutput $backendOutLog `
        -RedirectStandardError $backendErrLog

    Wait-HealthUp -Url $healthUrl -TimeoutSeconds 120 -Process $backendProcess

    $frontendCommand = "`$ErrorActionPreference = 'Stop'; Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force; Set-Location '$RootDir'; . .\scripts\dev-env.ps1 -Quiet; `$env:VITE_API_BASE_URL = '$backendUrl'; Set-Location '$FrontendDir'; & `$env:HONEY_NPM_CMD run dev -- --host 127.0.0.1 --port $FrontendPort"

    $frontendProcess = Start-Process `
        -FilePath "powershell.exe" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $frontendCommand) `
        -WorkingDirectory $FrontendDir `
        -WindowStyle Hidden `
        -PassThru `
        -RedirectStandardOutput $frontendOutLog `
        -RedirectStandardError $frontendErrLog

    Wait-HttpOk -Url $frontendUrl -TimeoutSeconds 60 -Process $frontendProcess -Name "frontend"

    $recordedProcesses = @(
        [ordered]@{ name = "backend"; pid = $backendProcess.Id },
        [ordered]@{ name = "frontend"; pid = $frontendProcess.Id }
    )
    foreach ($listenerPid in (Get-ListeningProcessIds -Port $FrontendPort)) {
        if ($listenerPid -ne $frontendProcess.Id) {
            $recordedProcesses += [ordered]@{ name = "frontend-listener"; pid = $listenerPid }
        }
    }

    $state = [ordered]@{
        schema = $Schema
        backendUrl = $backendUrl
        frontendUrl = $frontendUrl
        healthUrl = $healthUrl
        logs = [ordered]@{
            backendOut = $backendOutLog
            backendErr = $backendErrLog
            backendApp = $backendAppLog
            frontendOut = $frontendOutLog
            frontendErr = $frontendErrLog
        }
        processes = $recordedProcesses
        startedAt = (Get-Date).ToString("o")
    }
    $state | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $StateFile -Encoding UTF8

    Write-Host "[staging-smoke] Started."
    Write-Host "[staging-smoke] Schema:       $Schema"
    Write-Host "[staging-smoke] Frontend URL: $frontendUrl"
    Write-Host "[staging-smoke] Backend URL:  $backendUrl"
    Write-Host "[staging-smoke] Health URL:   $healthUrl"
    Write-Host "[staging-smoke] Logs:         $StateDir"
    Write-Host "[staging-smoke] Stop command: .\scripts\run-local-staging-smoke.ps1 -Stop"
} catch {
    foreach ($listenerPid in (Get-ListeningProcessIds -Port $FrontendPort)) {
        Stop-SmokeProcessTree -ProcessId $listenerPid | Out-Null
    }
    if ($null -ne $frontendProcess -and -not $frontendProcess.HasExited) {
        Stop-SmokeProcessTree -ProcessId $frontendProcess.Id | Out-Null
    }
    if ($null -ne $backendProcess -and -not $backendProcess.HasExited) {
        Stop-SmokeProcessTree -ProcessId $backendProcess.Id | Out-Null
    }
    throw
} finally {
    foreach ($key in $envKeys) {
        [Environment]::SetEnvironmentVariable($key, $originalEnv[$key], "Process")
    }
}
