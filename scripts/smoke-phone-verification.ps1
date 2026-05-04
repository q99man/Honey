[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = "High")]
param(
    [string]$BaseUrl = "http://localhost:8080",

    [Parameter(Mandatory = $true)]
    [string]$Phone,

    [string]$Email = "",
    [string]$Password = "SmokePass1234!",
    [string]$Nickname = "phone-smoke",

    [switch]$SkipSignup
)

$ErrorActionPreference = "Stop"

$previousWhatIfPreference = $WhatIfPreference
$WhatIfPreference = $false
try {
    . (Join-Path $PSScriptRoot "dev-env.ps1") -Quiet
} finally {
    $WhatIfPreference = $previousWhatIfPreference
}

function Mask-Phone {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value) -or $Value.Length -lt 4) {
        return "****"
    }

    return "****$($Value.Substring($Value.Length - 4))"
}

function Invoke-HoneyApi {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [string]$AccessToken = ""
    )

    $headers = @{}
    if (-not [string]::IsNullOrWhiteSpace($AccessToken)) {
        $headers["Authorization"] = "Bearer $AccessToken"
    }

    $uri = "$($BaseUrl.TrimEnd('/'))$Path"
    $jsonBody = $null
    if ($null -ne $Body) {
        $jsonBody = $Body | ConvertTo-Json -Depth 5
    }

    try {
        if ($null -ne $jsonBody) {
            return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -Body $jsonBody -ContentType "application/json; charset=utf-8"
        }
        return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers
    } catch {
        $statusCode = $null
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        $message = $_.Exception.Message
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
            $message = $_.ErrorDetails.Message
        }
        throw "[phone-smoke] API request failed. method=$Method path=$Path status=$statusCode message=$message"
    }
}

if ($Email -eq "") {
    $stamp = Get-Date -Format "yyyyMMddHHmmss"
    $Email = "phone-smoke-$stamp@example.com"
}

$maskedPhone = Mask-Phone $Phone
$targetDescription = "create or use smoke account '$Email' and transmit phone $maskedPhone to $BaseUrl for SMS verification"
if (-not $PSCmdlet.ShouldProcess($targetDescription, "Run phone verification live smoke")) {
    Write-Host "[phone-smoke] Skipped."
    exit 0
}

Write-Host "[phone-smoke] Target: $BaseUrl"
Write-Host "[phone-smoke] Phone:  $maskedPhone"

if (-not $SkipSignup) {
    Write-Host "[phone-smoke] Creating smoke account..."
    Invoke-HoneyApi -Method "POST" -Path "/api/auth/signup" -Body @{
        email = $Email
        password = $Password
        nickname = $Nickname
    } | Out-Null
}

Write-Host "[phone-smoke] Logging in..."
$login = Invoke-HoneyApi -Method "POST" -Path "/api/auth/login" -Body @{
    email = $Email
    password = $Password
}

$accessToken = $login.data.accessToken
if ([string]::IsNullOrWhiteSpace($accessToken)) {
    throw "[phone-smoke] Login response did not include an access token."
}

Write-Host "[phone-smoke] Requesting SMS code..."
$sendResponse = Invoke-HoneyApi -Method "POST" -Path "/api/auth/phone/send-code" -AccessToken $accessToken -Body @{
    phone = $Phone
}
if (-not $sendResponse.data.sent) {
    throw "[phone-smoke] Send-code response did not confirm delivery request."
}

$code = Read-Host -Prompt "[phone-smoke] Enter the verification code received on $maskedPhone"
if ([string]::IsNullOrWhiteSpace($code)) {
    throw "[phone-smoke] Verification code is required."
}

Write-Host "[phone-smoke] Verifying code..."
$verifyResponse = Invoke-HoneyApi -Method "POST" -Path "/api/auth/phone/verify-code" -AccessToken $accessToken -Body @{
    phone = $Phone
    code = $code
}

if (-not $verifyResponse.data.phoneVerified) {
    throw "[phone-smoke] Verify-code response did not mark the account as phone verified."
}

Write-Host "[phone-smoke] Phone verification live smoke passed."
