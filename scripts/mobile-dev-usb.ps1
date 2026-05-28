param(
    [string]$DeviceId,
    [int]$BackendPort = 8080,
    [switch]$SkipBuild,
    [switch]$SkipInstall,
    [switch]$SkipLaunch,
    [switch]$CheckOnly
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "mobile-dev-common.ps1")

Initialize-HoneyMobileDevEnvironment

$adb = Get-HoneyAdbCommand
$resolvedDeviceId = Resolve-HoneyAndroidDevice -AdbCommand $adb -DeviceId $DeviceId
$apiBaseUrl = "http://127.0.0.1:$BackendPort"

Test-HoneyBackendHealth -BaseUrl $apiBaseUrl -Required | Out-Null

Write-Host "[adb] reverse tcp:$BackendPort tcp:$BackendPort on $resolvedDeviceId" -ForegroundColor Cyan
& $adb -s $resolvedDeviceId reverse "tcp:$BackendPort" "tcp:$BackendPort"

if ($CheckOnly) {
    Write-Host "[mobile-dev-usb] OK. Device can use $apiBaseUrl through adb reverse." -ForegroundColor Green
    exit 0
}

if (-not $SkipBuild) {
    Build-HoneyDevApk -ApiBaseUrl $apiBaseUrl
}

if (-not $SkipInstall) {
    Install-HoneyDevApk -AdbCommand $adb -DeviceId $resolvedDeviceId
}

if (-not $SkipLaunch) {
    Start-HoneyDevApp -AdbCommand $adb -DeviceId $resolvedDeviceId
}

Write-Host "[mobile-dev-usb] Done. API base URL: $apiBaseUrl" -ForegroundColor Green
