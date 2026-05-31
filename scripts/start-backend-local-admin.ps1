param(
    [string]$Email = "local-admin@honeytong.test",
    [string]$Password,
    [string]$Nickname = "local-admin",
    [string]$Phone = "",
    [ValidateSet("ADMIN", "SUPER_ADMIN")]
    [string]$Role = "SUPER_ADMIN",
    [switch]$ResetPassword,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs = @("bootRun")
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Password)) {
    throw "Password is required. Pass -Password <local-admin-password>."
}

$env:ADMIN_BOOTSTRAP_ENABLED = "true"
$env:ADMIN_BOOTSTRAP_EMAIL = $Email
$env:ADMIN_BOOTSTRAP_PASSWORD = $Password
$env:ADMIN_BOOTSTRAP_NICKNAME = $Nickname
$env:ADMIN_BOOTSTRAP_ROLE = $Role
$env:ADMIN_BOOTSTRAP_PHONE = $Phone
$env:ADMIN_BOOTSTRAP_PHONE_VERIFIED = "true"
$env:ADMIN_BOOTSTRAP_RESET_PASSWORD = if ($ResetPassword) { "true" } else { "false" }

Write-Host "[admin-bootstrap] email: $Email" -ForegroundColor Cyan
Write-Host "[admin-bootstrap] role:  $Role" -ForegroundColor Cyan
Write-Host "[admin-bootstrap] reset password: $($env:ADMIN_BOOTSTRAP_RESET_PASSWORD)" -ForegroundColor Cyan

& (Join-Path $PSScriptRoot "run-backend-gradle.ps1") @GradleArgs
exit $LASTEXITCODE
