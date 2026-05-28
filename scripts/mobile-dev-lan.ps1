param(
    [string]$DeviceId,
    [string]$HostIp,
    [int]$BackendPort = 8080,
    [switch]$SkipBuild,
    [switch]$SkipInstall,
    [switch]$SkipLaunch,
    [switch]$CheckOnly
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "mobile-dev-common.ps1")

function Resolve-HoneyLanHostIp {
    param([string]$PreferredHostIp)

    if (-not [string]::IsNullOrWhiteSpace($PreferredHostIp)) {
        return $PreferredHostIp
    }

    $addresses = Get-NetIPAddress -AddressFamily IPv4 |
        Where-Object {
            $_.IPAddress -notlike "127.*" -and
            $_.IPAddress -notlike "169.254.*" -and
            $_.InterfaceAlias -notlike "*WSL*" -and
            $_.InterfaceAlias -notlike "*Loopback*"
        } |
        Sort-Object InterfaceMetric |
        Select-Object -ExpandProperty IPAddress

    if ($addresses.Count -eq 0) {
        throw "Could not detect a LAN IPv4 address. Rerun with -HostIp <pc-ip>."
    }

    if ($addresses.Count -gt 1) {
        Write-Host "[lan] Multiple candidate IPs found: $($addresses -join ', '). Using $($addresses[0])." -ForegroundColor Yellow
    }

    return $addresses[0]
}

Initialize-HoneyMobileDevEnvironment

$adb = Get-HoneyAdbCommand
$resolvedDeviceId = Resolve-HoneyAndroidDevice -AdbCommand $adb -DeviceId $DeviceId
$resolvedHostIp = Resolve-HoneyLanHostIp -PreferredHostIp $HostIp
$apiBaseUrl = "http://$resolvedHostIp`:$BackendPort"

Test-HoneyBackendHealth -BaseUrl $apiBaseUrl -Required | Out-Null

Write-Host "[lan] API base URL: $apiBaseUrl" -ForegroundColor Cyan
Write-Host "[lan] Phone and PC must be on the same reachable network. If this fails at school/home, use scripts\mobile-dev-usb.ps1." -ForegroundColor Yellow

if ($CheckOnly) {
    Write-Host "[mobile-dev-lan] OK. Build URL would be $apiBaseUrl." -ForegroundColor Green
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

Write-Host "[mobile-dev-lan] Done. API base URL: $apiBaseUrl" -ForegroundColor Green
