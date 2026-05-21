param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "dev-env.ps1") -Quiet

$backendDir = Resolve-Path (Join-Path $PSScriptRoot "..\backend")
$gradlew = Join-Path $backendDir "gradlew.bat"

if (-not (Test-Path $gradlew)) {
    Write-Error "[gradle] backend Gradle wrapper not found: $gradlew"
    exit 1
}

Push-Location $backendDir
try {
    & $gradlew @GradleArgs
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
