param(
    [switch]$CheckOnly,
    [switch]$StartEmulator,
    [switch]$SkipBuild,
    [switch]$UseAdbReverse,
    [string]$AvdName,
    [string]$DeviceId,
    [int]$LogcatSeconds = 15
)

$ErrorActionPreference = "Stop"

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

. (Join-Path $PSScriptRoot "dev-env.ps1") -Quiet

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$MobileDir = Join-Path $RootDir "mobile"
$EnvFile = Join-Path $RootDir ".env"
$DevPackageName = "com.honeytong.app.dev"

function Import-EnvFileIfPresent {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        return
    }

    foreach ($rawLine in Get-Content -Encoding UTF8 $Path) {
        $line = $rawLine.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
            continue
        }

        $separatorIndex = $line.IndexOf("=")
        if ($separatorIndex -le 0) {
            continue
        }

        $key = $line.Substring(0, $separatorIndex).Trim()
        $value = $line.Substring($separatorIndex + 1).Trim()
        if ([string]::IsNullOrWhiteSpace($key)) {
            continue
        }

        if ([string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable($key, "Process"))) {
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
        }
    }
}

function Write-Status {
    param(
        [string]$Name,
        [bool]$Ok,
        [string]$Detail
    )

    $mark = if ($Ok) { "[OK]" } else { "[NEEDS]" }
    $color = if ($Ok) { "Green" } else { "Yellow" }
    Write-Host "$mark $Name - $Detail" -ForegroundColor $color
}

function Get-ConnectedDeviceIds {
    if ([string]::IsNullOrWhiteSpace($env:HONEY_ADB_EXE) -or -not (Test-Path $env:HONEY_ADB_EXE)) {
        return @()
    }

    $devices = @()
    foreach ($line in & $env:HONEY_ADB_EXE devices) {
        if ($line -match "^([^\s]+)\s+device$") {
            $devices += $matches[1]
        }
    }
    return $devices
}

function Test-DeviceIsConnected {
    param([string]$TargetDeviceId)

    if ([string]::IsNullOrWhiteSpace($TargetDeviceId)) {
        return $false
    }

    foreach ($line in & $env:HONEY_ADB_EXE devices) {
        if ($line -match "^$([regex]::Escape($TargetDeviceId))\s+device$") {
            return $true
        }
    }
    return $false
}

function Get-AvailableAvdNames {
    if ([string]::IsNullOrWhiteSpace($env:HONEY_EMULATOR_EXE) -or -not (Test-Path $env:HONEY_EMULATOR_EXE)) {
        return @()
    }

    $names = @()
    foreach ($line in & $env:HONEY_EMULATOR_EXE -list-avds) {
        $trimmed = $line.Trim()
        if (-not [string]::IsNullOrWhiteSpace($trimmed)) {
            $names += $trimmed
        }
    }
    return $names
}

function Wait-ForAndroidDevice {
    param(
        [string]$ExpectedDeviceId,
        [int]$TimeoutSeconds = 180
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $currentDevices = @(Get-ConnectedDeviceIds)
        $candidate = $ExpectedDeviceId
        if ([string]::IsNullOrWhiteSpace($candidate) -and $currentDevices.Count -gt 0) {
            $candidate = $currentDevices[0]
        }

        if (Test-DeviceIsConnected $candidate) {
            $bootCompleted = (& $env:HONEY_ADB_EXE -s $candidate shell getprop sys.boot_completed 2>$null | Select-Object -First 1).Trim()
            if ($bootCompleted -eq "1") {
                return $candidate
            }
        }

        Start-Sleep -Seconds 3
    }

    throw "Timed out waiting for an Android device to finish booting."
}

function New-DartDefineArgs {
    param([string[]]$Keys)

    $args = @()
    foreach ($key in $Keys) {
        $value = [Environment]::GetEnvironmentVariable($key, "Process")
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            $args += "--dart-define=$key=$value"
        }
    }
    return $args
}

function Get-DevicePrimaryAbi {
    param([string]$TargetDeviceId)

    return (& $env:HONEY_ADB_EXE -s $TargetDeviceId shell getprop ro.product.cpu.abi | Select-Object -First 1).Trim()
}

function Test-ApkHasNativeLibrary {
    param(
        [string]$ApkPath,
        [string]$Abi,
        [string]$LibraryName
    )

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($ApkPath)
    try {
        $entryPath = "lib/$Abi/$LibraryName"
        return [bool]($zip.Entries | Where-Object { $_.FullName -eq $entryPath } | Select-Object -First 1)
    }
    finally {
        $zip.Dispose()
    }
}

Import-EnvFileIfPresent $EnvFile

$nativeKey = [Environment]::GetEnvironmentVariable("KAKAO_NATIVE_APP_KEY", "Process")
$apiBaseUrl = [Environment]::GetEnvironmentVariable("HONEY_DEV_API_BASE_URL", "Process")
if ([string]::IsNullOrWhiteSpace($apiBaseUrl)) {
    $apiBaseUrl = [Environment]::GetEnvironmentVariable("HONEY_API_BASE_URL", "Process")
}
if ([string]::IsNullOrWhiteSpace($apiBaseUrl)) {
    $apiBaseUrl = "http://10.0.2.2:8080"
}
if ($UseAdbReverse) {
    $apiBaseUrl = "http://127.0.0.1:8080"
    [Environment]::SetEnvironmentVariable("HONEY_DEV_API_BASE_URL", $apiBaseUrl, "Process")
}

$devices = @(Get-ConnectedDeviceIds)
$avdNames = @(Get-AvailableAvdNames)
$selectedDevice = $DeviceId
if ([string]::IsNullOrWhiteSpace($selectedDevice) -and $devices.Count -gt 0) {
    $selectedDevice = $devices[0]
}

Write-Host "Honeytong mobile Kakao smoke" -ForegroundColor Cyan
Write-Host "Root: $RootDir"
Write-Host "Package: $DevPackageName"
Write-Host ""

Write-Status ".env file" (Test-Path $EnvFile) ".env is loaded when present."
Write-Status "KAKAO_NATIVE_APP_KEY" (-not [string]::IsNullOrWhiteSpace($nativeKey)) "Required for native Kakao map authentication."
Write-Status "HONEY_DEV_API_BASE_URL" (-not [string]::IsNullOrWhiteSpace($apiBaseUrl)) "Using $apiBaseUrl"
Write-Status "ADB" (-not [string]::IsNullOrWhiteSpace($env:HONEY_ADB_EXE) -and (Test-Path $env:HONEY_ADB_EXE)) "Required to install and launch the app."
Write-Status "Emulator" (-not [string]::IsNullOrWhiteSpace($env:HONEY_EMULATOR_EXE) -and (Test-Path $env:HONEY_EMULATOR_EXE)) "Available AVDs: $($avdNames -join ', ')"
Write-Status "Device" (-not [string]::IsNullOrWhiteSpace($selectedDevice)) "Selected device: $selectedDevice"

if ($CheckOnly) {
    Write-Host ""
    Write-Host "Check-only mode finished. No build, install, or app launch was attempted." -ForegroundColor Cyan
    exit 0
}

if ([string]::IsNullOrWhiteSpace($nativeKey)) {
    throw "KAKAO_NATIVE_APP_KEY is missing. Add it to the root .env before running this smoke test."
}

if ($StartEmulator -and [string]::IsNullOrWhiteSpace($selectedDevice)) {
    $targetAvd = $AvdName
    if ([string]::IsNullOrWhiteSpace($targetAvd) -and $avdNames.Count -gt 0) {
        $targetAvd = $avdNames[0]
    }
    if ([string]::IsNullOrWhiteSpace($targetAvd)) {
        throw "No Android Virtual Device was found. Create one in Android Studio Device Manager first."
    }

    Write-Host ""
    Write-Host "Starting emulator: $targetAvd" -ForegroundColor Cyan
    Start-Process -FilePath $env:HONEY_EMULATOR_EXE -ArgumentList @("-avd", $targetAvd, "-netdelay", "none", "-netspeed", "full")
    $selectedDevice = Wait-ForAndroidDevice -ExpectedDeviceId $DeviceId
    Write-Host "Emulator is ready: $selectedDevice" -ForegroundColor Green
}

if ([string]::IsNullOrWhiteSpace($selectedDevice)) {
    throw "No ADB device is connected. Start an emulator/connect a device, or rerun with -StartEmulator."
}
if (-not (Test-DeviceIsConnected $selectedDevice)) {
    throw "Selected ADB device is not connected: $selectedDevice"
}

if ($UseAdbReverse) {
    Write-Host ""
    Write-Host "Configuring ADB reverse: device tcp:8080 -> host tcp:8080" -ForegroundColor Cyan
    & $env:HONEY_ADB_EXE -s $selectedDevice reverse tcp:8080 tcp:8080
}

$selectedDeviceAbi = Get-DevicePrimaryAbi $selectedDevice
Write-Host "Device ABI: $selectedDeviceAbi"
if ($selectedDeviceAbi -eq "x86" -or $selectedDeviceAbi -eq "x86_64") {
    throw "Kakao Maps native SDK is not available for '$selectedDeviceAbi' in this project. Use an ARM64 Android device for the real Kakao map smoke test."
}

$dartDefines = New-DartDefineArgs @(
    "KAKAO_NATIVE_APP_KEY",
    "HONEY_DEV_API_BASE_URL",
    "HONEY_API_BASE_URL",
    "HONEY_ALLOW_MOCK_KAKAO_LOGIN"
)

Push-Location $MobileDir
try {
    $apkPath = Join-Path $MobileDir "build\app\outputs\flutter-apk\app-dev-debug.apk"

    Write-Host ""
    if ($SkipBuild) {
        Write-Host "[1/5] Skipping build and using existing dev debug APK..." -ForegroundColor Cyan
    } else {
        Write-Host "[1/5] Building dev debug APK..." -ForegroundColor Cyan
        flutter build apk --flavor dev --debug @dartDefines
    }

    if (-not (Test-Path $apkPath)) {
        throw "Expected APK was not created: $apkPath"
    }

    if (-not (Test-ApkHasNativeLibrary -ApkPath $apkPath -Abi $selectedDeviceAbi -LibraryName "libK3fAndroid.so")) {
        throw "Kakao Maps native library is not packaged for device ABI '$selectedDeviceAbi'. Use an ARM64 Android device, or an ARM/arm64 emulator image if your host supports it."
    }

    Write-Host ""
    Write-Host "[2/5] Installing APK on $selectedDevice..." -ForegroundColor Cyan
    & $env:HONEY_ADB_EXE -s $selectedDevice install -r $apkPath

    Write-Host ""
    Write-Host "[3/5] Clearing device logcat..." -ForegroundColor Cyan
    & $env:HONEY_ADB_EXE -s $selectedDevice logcat -c

    Write-Host ""
    Write-Host "[4/5] Launching app..." -ForegroundColor Cyan
    & $env:HONEY_ADB_EXE -s $selectedDevice shell monkey -p $DevPackageName -c android.intent.category.LAUNCHER 1

    Write-Host ""
    Write-Host "[5/5] Capturing Kakao/Honeytong/Flutter logs for $LogcatSeconds seconds..." -ForegroundColor Cyan
    Start-Sleep -Seconds $LogcatSeconds
    & $env:HONEY_ADB_EXE -s $selectedDevice logcat -d |
        Select-String -Pattern "Kakao|kakao|Honeytong|Flutter|Map|Auth|Exception|Error" |
        ForEach-Object { $_.Line }

    Write-Host ""
    Write-Host "Manual checks still required: map tiles visible, current marker visible, place markers visible, marker tap opens detail." -ForegroundColor Cyan
}
finally {
    Pop-Location
}
