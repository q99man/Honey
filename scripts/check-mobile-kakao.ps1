param(
    [switch]$ShowDevices
)

$ErrorActionPreference = "Stop"

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

. (Join-Path $PSScriptRoot "dev-env.ps1") -Quiet

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$EnvFile = Join-Path $RootDir ".env"
$AndroidDir = Join-Path $RootDir "mobile\android"
$LocalProperties = Join-Path $AndroidDir "local.properties"
$KeyProperties = Join-Path $AndroidDir "key.properties"

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

function Get-PropertyValue {
    param(
        [string]$Path,
        [string]$Name
    )

    if (-not (Test-Path $Path)) {
        return $null
    }

    $line = Get-Content -Encoding UTF8 $Path |
        Where-Object { $_ -match "^\s*$([regex]::Escape($Name))\s*=" } |
        Select-Object -First 1

    if (-not $line) {
        return $null
    }

    return ($line -split "=", 2)[1].Trim()
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

function Write-KeyHashes {
    param(
        [string]$Label,
        [string]$Keystore,
        [string]$Alias,
        [string]$StorePassword,
        [string]$KeyPassword
    )

    if (-not (Test-Path $Keystore)) {
        Write-Status "$Label keystore" $false "$Keystore does not exist."
        return
    }

    Write-Host ""
    Write-Host "[$Label key hashes]" -ForegroundColor Cyan
    $hashLines = & keytool -list -v -alias $Alias -keystore $Keystore -storepass $StorePassword -keypass $KeyPassword |
        Select-String -Pattern "SHA1|SHA-1|SHA256|SHA-256" |
        ForEach-Object { $_.Line.Trim() }

    $hashLines | ForEach-Object { $_ }

    $sha1Line = $hashLines | Where-Object { $_ -match "SHA1|SHA-1" } | Select-Object -First 1
    if ($sha1Line -match "([0-9A-Fa-f]{2}:){19}[0-9A-Fa-f]{2}") {
        $bytes = $matches[0].Split(":") | ForEach-Object { [Convert]::ToByte($_, 16) }
        $kakaoKeyHash = [Convert]::ToBase64String($bytes)
        Write-Host "Kakao key hash: $kakaoKeyHash"
    } else {
        Write-Status "$Label Kakao key hash" $false "Could not derive Kakao Base64 key hash from SHA1 fingerprint."
    }
}

Import-EnvFileIfPresent $EnvFile

$nativeKey = [Environment]::GetEnvironmentVariable("KAKAO_NATIVE_APP_KEY", "Process")
$restKey = [Environment]::GetEnvironmentVariable("KAKAO_REST_API_KEY", "Process")
$javascriptKey = [Environment]::GetEnvironmentVariable("KAKAO_JAVASCRIPT_KEY", "Process")

Write-Host "Honeytong mobile Kakao configuration check" -ForegroundColor Cyan
Write-Host "Root: $RootDir"
Write-Host ""

Write-Status ".env file" (Test-Path $EnvFile) ".env must not be committed to Git."
Write-Status "KAKAO_NATIVE_APP_KEY" (-not [string]::IsNullOrWhiteSpace($nativeKey)) "Required for Flutter native map/login."
Write-Status "KAKAO_REST_API_KEY" (-not [string]::IsNullOrWhiteSpace($restKey)) "Required for Spring Kakao Local API."
Write-Status "KAKAO_JAVASCRIPT_KEY" (-not [string]::IsNullOrWhiteSpace($javascriptKey)) "Required for web Kakao Maps JavaScript SDK."

$sdkDir = Get-PropertyValue $LocalProperties "sdk.dir"
Write-Status "Android SDK" (-not [string]::IsNullOrWhiteSpace($env:ANDROID_HOME) -or -not [string]::IsNullOrWhiteSpace($sdkDir)) "Used for adb/keytool checks."
Write-Status "ADB" (-not [string]::IsNullOrWhiteSpace($env:HONEY_ADB_EXE) -and (Test-Path $env:HONEY_ADB_EXE)) "Used for device/emulator checks."

Write-Host ""
Write-Host "[Kakao Console registration values]" -ForegroundColor Cyan
Write-Host "Dev package:  com.honeytong.app.dev"
Write-Host "Prod package: com.honeytong.app"

$debugKeystore = Join-Path $env:USERPROFILE ".android\debug.keystore"
Write-KeyHashes `
    -Label "Debug" `
    -Keystore $debugKeystore `
    -Alias "androiddebugkey" `
    -StorePassword "android" `
    -KeyPassword "android"

if (Test-Path $KeyProperties) {
    $storeFile = Get-PropertyValue $KeyProperties "storeFile"
    $storePassword = Get-PropertyValue $KeyProperties "storePassword"
    $keyPassword = Get-PropertyValue $KeyProperties "keyPassword"
    $keyAlias = Get-PropertyValue $KeyProperties "keyAlias"
    if ($storeFile -and $storePassword -and $keyPassword -and $keyAlias) {
        $releaseKeystore = Join-Path $AndroidDir $storeFile
        Write-KeyHashes `
            -Label "Release" `
            -Keystore $releaseKeystore `
            -Alias $keyAlias `
            -StorePassword $storePassword `
            -KeyPassword $keyPassword
    } else {
        Write-Status "Release key hash" $false "mobile/android/key.properties is incomplete."
    }
} else {
    Write-Status "Release key hash" $false "mobile/android/key.properties is missing."
}

if ($ShowDevices -and $env:HONEY_ADB_EXE -and (Test-Path $env:HONEY_ADB_EXE)) {
    Write-Host ""
    Write-Host "[ADB devices]" -ForegroundColor Cyan
    & $env:HONEY_ADB_EXE devices
}
