$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "dev-env.ps1") -Quiet

$required = @(
    @{ Name = "node"; Command = $env:HONEY_NODE_EXE; Args = @("--version") },
    @{ Name = "npm"; Command = $env:HONEY_NPM_CMD; Args = @("--version") },
    @{ Name = "git"; Command = $env:HONEY_GIT_EXE; Args = @("--version") },
    @{ Name = "java"; Command = $env:HONEY_JAVA_EXE; Args = @("--version") }
)

$missing = @()

foreach ($tool in $required) {
    if ([string]::IsNullOrWhiteSpace($tool.Command) -or -not (Test-Path $tool.Command)) {
        $missing += $tool.Name
        continue
    }

    Write-Host "[$($tool.Name)] $($tool.Command)"
    & $tool.Command @($tool.Args) | Select-Object -First 1
}

if ($missing.Count -gt 0) {
    Write-Error ("Missing local tool(s): " + ($missing -join ", "))
    exit 1
}

Write-Host "[dev-env] OK"
