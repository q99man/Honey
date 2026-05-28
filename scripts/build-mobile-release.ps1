# Honeytong Mobile App Release Build Script
# Automates the mobile release build process.

$ErrorActionPreference = "Stop"

# 1. Normalize the local tool session with dev-env.ps1.
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRootDir = Split-Path -Parent $ScriptDir
$DevEnvScript = Join-Path $ProjectRootDir "scripts\dev-env.ps1"

if (Test-Path $DevEnvScript) {
    Write-Host "Initializing tool session environment via dev-env.ps1..." -ForegroundColor Cyan
    . $DevEnvScript
} else {
    Write-Host "Warning: dev-env.ps1 not found, using existing environment." -ForegroundColor Yellow
}

# 2. Verify the mobile directory.
$MobileDir = Join-Path $ProjectRootDir "mobile"
if (-not (Test-Path $MobileDir)) {
    throw "Error: Mobile directory not found at $MobileDir"
}

Push-Location $MobileDir
try {
    Write-Host "Getting Flutter dependencies..." -ForegroundColor Cyan
    flutter pub get

    Write-Host "Running Flutter Analyze..." -ForegroundColor Cyan
    $oldPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    flutter analyze
    $ErrorActionPreference = $oldPreference

    Write-Host "Building Release APK..." -ForegroundColor Cyan
    flutter build apk --release

    Write-Host "Building Release App Bundle (AAB) for Google Play..." -ForegroundColor Cyan
    flutter build appbundle --release

    Write-Host "`nBuild completed successfully!" -ForegroundColor Green
    Write-Host "Artifacts location:" -ForegroundColor Cyan
    Write-Host " - APK: mobile/build/app/outputs/flutter-apk/app-release.apk" -ForegroundColor White
    Write-Host " - AAB: mobile/build/app/outputs/bundle/release/app-release.aab" -ForegroundColor White
}
finally {
    Pop-Location
}
