param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$NpmArgs
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "dev-env.ps1") -Quiet

if ([string]::IsNullOrWhiteSpace($env:HONEY_NPM_CMD) -or -not (Test-Path $env:HONEY_NPM_CMD)) {
    Write-Error "[npm] npm.cmd not found. Run scripts/check-dev-env.ps1 for details."
    exit 1
}

$frontendDir = Resolve-Path (Join-Path $PSScriptRoot "..\frontend")

Push-Location $frontendDir
try {
    & $env:HONEY_NPM_CMD @NpmArgs
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
