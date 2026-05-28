$ErrorActionPreference = "Stop"

function Initialize-HoneyMobileDevEnvironment {
    $script:ProjectRootDir = Split-Path -Parent $PSScriptRoot
    $script:MobileDir = Join-Path $script:ProjectRootDir "mobile"
    $devEnvScript = Join-Path $script:ProjectRootDir "scripts\dev-env.ps1"

    if (Test-Path $devEnvScript) {
        . $devEnvScript -Quiet
    }

    Import-HoneyDotEnv -Path (Join-Path $script:ProjectRootDir ".env")

    if (-not (Test-Path $script:MobileDir)) {
        throw "Mobile directory not found: $script:MobileDir"
    }
}

function Import-HoneyDotEnv {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        return
    }

    foreach ($line in Get-Content $Path) {
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
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name, "Process"))) {
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
}

function Get-HoneyAdbCommand {
    if (-not [string]::IsNullOrWhiteSpace($env:HONEY_ADB_EXE) -and (Test-Path $env:HONEY_ADB_EXE)) {
        return $env:HONEY_ADB_EXE
    }

    return "adb"
}

function Get-HoneyFlutterCommand {
    if (-not [string]::IsNullOrWhiteSpace($env:HONEY_FLUTTER_CMD) -and (Test-Path $env:HONEY_FLUTTER_CMD)) {
        return $env:HONEY_FLUTTER_CMD
    }

    return "flutter"
}

function Resolve-HoneyAndroidDevice {
    param(
        [string]$AdbCommand,
        [string]$DeviceId
    )

    if (-not [string]::IsNullOrWhiteSpace($DeviceId)) {
        return $DeviceId
    }

    $deviceRows = @(& $AdbCommand devices |
        Where-Object { $_ -match "\tdevice$" } |
        ForEach-Object { ($_ -split "\s+")[0] })

    if ($deviceRows.Count -eq 0) {
        throw "No Android device is connected. Connect a phone with USB debugging enabled, then rerun the script."
    }

    if ($deviceRows.Count -gt 1) {
        throw "Multiple Android devices are connected: $($deviceRows -join ', '). Rerun with -DeviceId <id>."
    }

    return $deviceRows[0]
}

function Test-HoneyBackendHealth {
    param(
        [string]$BaseUrl,
        [switch]$Required
    )

    $healthUrl = "$($BaseUrl.TrimEnd('/'))/actuator/health"
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $healthUrl -TimeoutSec 3
        if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
            Write-Host "[backend] OK $healthUrl" -ForegroundColor Green
            return $true
        }
    } catch {
        if ($Required) {
            throw "Backend health check failed at $healthUrl. Start the backend first."
        }

        Write-Host "[backend] WARN health check failed at $healthUrl" -ForegroundColor Yellow
        return $false
    }

    if ($Required) {
        throw "Backend health check failed at $healthUrl."
    }

    return $false
}

function Get-HoneyKakaoNativeAppKey {
    if ([string]::IsNullOrWhiteSpace($env:KAKAO_NATIVE_APP_KEY) -or $env:KAKAO_NATIVE_APP_KEY -eq "your-kakao-native-app-key") {
        Write-Host "[kakao] WARN KAKAO_NATIVE_APP_KEY is empty. Kakao native map will show the configured fallback state." -ForegroundColor Yellow
        return ""
    }

    return $env:KAKAO_NATIVE_APP_KEY
}

function Build-HoneyDevApk {
    param([string]$ApiBaseUrl)

    $flutter = Get-HoneyFlutterCommand
    $kakaoNativeAppKey = Get-HoneyKakaoNativeAppKey

    Push-Location $script:MobileDir
    try {
        Write-Host "[flutter] pub get" -ForegroundColor Cyan
        & $flutter pub get
        if ($LASTEXITCODE -ne 0) {
            throw "flutter pub get failed with exit code $LASTEXITCODE."
        }

        Write-Host "[flutter] build dev debug APK ($ApiBaseUrl)" -ForegroundColor Cyan
        & $flutter build apk --debug --flavor dev "--dart-define=KAKAO_NATIVE_APP_KEY=$kakaoNativeAppKey" "--dart-define=HONEY_DEV_API_BASE_URL=$ApiBaseUrl"
        if ($LASTEXITCODE -ne 0) {
            throw "flutter build failed with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

function Install-HoneyDevApk {
    param(
        [string]$AdbCommand,
        [string]$DeviceId
    )

    $apkPath = Join-Path $script:MobileDir "build\app\outputs\flutter-apk\app-dev-debug.apk"
    if (-not (Test-Path $apkPath)) {
        throw "Dev APK not found: $apkPath"
    }

    Write-Host "[adb] install $apkPath" -ForegroundColor Cyan
    & $AdbCommand -s $DeviceId install -r $apkPath
    if ($LASTEXITCODE -ne 0) {
        throw "ADB install failed with exit code $LASTEXITCODE. If signatures differ, uninstall com.honeytong.app.dev and install again."
    }
}

function Start-HoneyDevApp {
    param(
        [string]$AdbCommand,
        [string]$DeviceId
    )

    Write-Host "[adb] launch com.honeytong.app.dev" -ForegroundColor Cyan
    & $AdbCommand -s $DeviceId shell monkey -p com.honeytong.app.dev 1
    if ($LASTEXITCODE -ne 0) {
        throw "ADB launch failed with exit code $LASTEXITCODE."
    }
}
