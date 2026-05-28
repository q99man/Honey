param(
    [string]$DeviceId,
    [int]$BackendPort = 8080,
    [switch]$NoFix
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "mobile-dev-common.ps1")

Initialize-HoneyMobileDevEnvironment

$adb = Get-HoneyAdbCommand
$resolvedDeviceId = Resolve-HoneyAndroidDevice -AdbCommand $adb -DeviceId $DeviceId
$apiBaseUrl = "http://127.0.0.1:$BackendPort"
$reverseSpec = "tcp:$BackendPort tcp:$BackendPort"

Test-HoneyBackendHealth -BaseUrl $apiBaseUrl -Required | Out-Null

Write-Host "[adb] device $resolvedDeviceId" -ForegroundColor Cyan
$reverseRows = @(& $adb -s $resolvedDeviceId reverse --list)
$hasReverse = $reverseRows | Where-Object { $_ -like "*$reverseSpec*" }

if ($hasReverse) {
    Write-Host "[adb] OK reverse $reverseSpec" -ForegroundColor Green
} elseif ($NoFix) {
    throw "ADB reverse is missing for $reverseSpec. Rerun without -NoFix to restore it."
} else {
    Write-Host "[adb] restore reverse $reverseSpec" -ForegroundColor Yellow
    & $adb -s $resolvedDeviceId reverse "tcp:$BackendPort" "tcp:$BackendPort"
    if ($LASTEXITCODE -ne 0) {
        throw "ADB reverse failed with exit code $LASTEXITCODE."
    }
}

$finalReverseRows = @(& $adb -s $resolvedDeviceId reverse --list)
$finalReverse = $finalReverseRows | Where-Object { $_ -like "*$reverseSpec*" }
if (-not $finalReverse) {
    throw "ADB reverse is still missing for $reverseSpec."
}

Write-Host "[mobile-dev-check] OK. Backend and USB reverse are ready for login testing." -ForegroundColor Green
