# Development Environment

This project uses a small Windows bootstrap script so Git, Node/npm, Java, and Gradle resolve the same way across Codex, PowerShell, and local dev terminals.

## Why This Exists

Windows environment variables are copied when a process starts. If `PATH` or `JAVA_HOME` is changed later, already-open terminals, running backend processes, Vite dev servers, and Codex shell sessions do not automatically receive the new values.

Use the project bootstrap instead of relying on whatever the current terminal inherited.

## Standard Session Setup

From the repository root:

```powershell
. .\scripts\dev-env.ps1
```

Check the resolved tools:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-dev-env.ps1
```

The script resolves and pins:
- Node from `C:\Program Files\nodejs` by default
- npm through `npm.cmd` to avoid PowerShell `npm.ps1` execution-policy issues
- Git from `C:\Program Files\Git\cmd` or `C:\Program Files\Git\bin`
- Java 21 from `%USERPROFILE%\.jdks\ms-21.0.10` first, then common JDK 21 locations
- Gradle user home to `backend\.gradle-user-home`

Optional override variables:

```powershell
$env:HONEY_NODE_HOME = "C:\Program Files\nodejs"
$env:HONEY_GIT_HOME = "C:\Program Files\Git\cmd"
$env:HONEY_JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.10"
```

## Standard Commands

Backend:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-backend-gradle.ps1 test
powershell -ExecutionPolicy Bypass -File .\scripts\run-backend-gradle.ps1 bootRun
```

Frontend:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-frontend-npm.ps1 run lint
powershell -ExecutionPolicy Bypass -File .\scripts\run-frontend-npm.ps1 run build
```

Local staging smoke:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-local-staging-smoke.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\run-local-staging-smoke.ps1 -Stop
```

The smoke wrapper uses the latest `honey_stage_rehearsal_*` schema by default, checks that its backend and frontend ports are free, starts the backend and Vite with isolated ports, writes PID and URL state to `.tmp\local-staging-smoke\state.json`, and stops only the recorded processes when `-Stop` is used.

For a long-running frontend dev server, first run the session setup, then:

```powershell
cd frontend
npm run dev -- --host 127.0.0.1
```

## Restart Rule

After changing Windows system environment variables:
- restart Codex if Codex commands still miss tools
- restart the backend process
- restart the Vite dev server
- reload the in-app browser page

Do not rely on Git hooks for local verification. Run explicit project commands instead.
