param(
    [switch]$Quiet
)

$ErrorActionPreference = "Stop"

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$BackendDir = Join-Path $RootDir "backend"

function Normalize-ProcessEnvironmentKeys {
    $processEnv = [Environment]::GetEnvironmentVariables("Process")
    $groups = @{}
    foreach ($key in $processEnv.Keys) {
        $lowerKey = $key.ToString().ToLowerInvariant()
        if (-not $groups.ContainsKey($lowerKey)) {
            $groups[$lowerKey] = @()
        }
        $groups[$lowerKey] += $key.ToString()
    }

    foreach ($group in $groups.GetEnumerator()) {
        if ($group.Value.Count -le 1) {
            continue
        }

        $canonical = if ($group.Key -eq "path") { "Path" } else { $group.Value[0] }
        $value = [Environment]::GetEnvironmentVariable($canonical, "Process")
        if ([string]::IsNullOrEmpty($value)) {
            foreach ($key in $group.Value) {
                $candidate = [Environment]::GetEnvironmentVariable($key, "Process")
                if (-not [string]::IsNullOrEmpty($candidate)) {
                    $value = $candidate
                    break
                }
            }
        }

        foreach ($key in $group.Value) {
            if ($key -cne $canonical) {
                [Environment]::SetEnvironmentVariable($key, $null, "Process")
            }
        }
        [Environment]::SetEnvironmentVariable($canonical, $value, "Process")
    }
}

function Add-PathEntry {
    param([string]$PathEntry)

    if ([string]::IsNullOrWhiteSpace($PathEntry) -or -not (Test-Path $PathEntry)) {
        return
    }

    $entries = $env:Path -split ';' | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    $alreadyPresent = $entries | Where-Object { $_.TrimEnd('\') -ieq $PathEntry.TrimEnd('\') }
    if (-not $alreadyPresent) {
        $env:Path = "$PathEntry;$env:Path"
    }
}

function Find-FirstExistingPath {
    param([string[]]$Candidates)

    foreach ($candidate in $Candidates) {
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and (Test-Path $candidate)) {
            return (Resolve-Path $candidate).Path
        }
    }

    return $null
}

Normalize-ProcessEnvironmentKeys

$nodeHome = Find-FirstExistingPath @(
    $env:HONEY_NODE_HOME,
    "C:\Program Files\nodejs",
    (Join-Path $env:LOCALAPPDATA "Programs\nodejs")
)

if ($nodeHome) {
    Add-PathEntry $nodeHome
    $nodeExe = Join-Path $nodeHome "node.exe"
    $npmCmd = Join-Path $nodeHome "npm.cmd"
    if (Test-Path $nodeExe) {
        Set-Alias -Name node -Value $nodeExe -Scope Global -Force
        $env:HONEY_NODE_EXE = $nodeExe
    }
    if (Test-Path $npmCmd) {
        Set-Alias -Name npm -Value $npmCmd -Scope Global -Force
        $env:HONEY_NPM_CMD = $npmCmd
    }
}

$gitHome = Find-FirstExistingPath @(
    $env:HONEY_GIT_HOME,
    "C:\Program Files\Git\cmd",
    "C:\Program Files\Git\bin"
)

if ($gitHome) {
    Add-PathEntry $gitHome
    $gitExe = Join-Path $gitHome "git.exe"
    if (Test-Path $gitExe) {
        Set-Alias -Name git -Value $gitExe -Scope Global -Force
        $env:HONEY_GIT_EXE = $gitExe
    }
}

$javaHome = Find-FirstExistingPath @(
    $env:HONEY_JAVA_HOME,
    $env:JAVA_HOME,
    (Join-Path $env:USERPROFILE ".jdks\ms-21.0.10"),
    (Join-Path $env:USERPROFILE ".jdks\ms-21*"),
    (Join-Path $env:USERPROFILE ".jdks\openjdk-21*"),
    "C:\Program Files\Eclipse Adoptium\jdk-21*",
    "C:\Program Files\Java\jdk-21*"
)

if ($javaHome) {
    $env:JAVA_HOME = $javaHome
    Add-PathEntry (Join-Path $javaHome "bin")
    $javaExe = Join-Path $javaHome "bin\java.exe"
    if (Test-Path $javaExe) {
        Set-Alias -Name java -Value $javaExe -Scope Global -Force
        $env:HONEY_JAVA_EXE = $javaExe
    }
}

if (Test-Path $BackendDir) {
    $env:GRADLE_USER_HOME = Join-Path $BackendDir ".gradle-user-home"
}

if (-not $Quiet) {
    Write-Host "[dev-env] root: $RootDir"
    Write-Host "[dev-env] node: $($env:HONEY_NODE_EXE)"
    Write-Host "[dev-env] npm:  $($env:HONEY_NPM_CMD)"
    Write-Host "[dev-env] git:  $($env:HONEY_GIT_EXE)"
    Write-Host "[dev-env] JAVA_HOME: $($env:JAVA_HOME)"
    Write-Host "[dev-env] GRADLE_USER_HOME: $($env:GRADLE_USER_HOME)"
}
