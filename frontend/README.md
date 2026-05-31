# Honeytong Frontend

React + TypeScript + Vite frontend for Honeytong user and admin web flows.

## Local Run

```powershell
cd C:\SpringWork\Honey
.\scripts\run-frontend-npm.ps1 install
.\scripts\run-frontend-npm.ps1 run dev
```

The frontend reads `VITE_API_BASE_URL` from `frontend/.env`. The local default is `http://127.0.0.1:8080`.

## Local Admin Account

Admin accounts are created through the backend's disabled-by-default local bootstrap flow, not through a public API.

Start the backend with a local `SUPER_ADMIN` account:

```powershell
cd C:\SpringWork\Honey
.\scripts\start-backend-local-admin.ps1 -Email local-admin@honeytong.test -Password "<local password>" -ResetPassword
```

Then open:

```text
http://localhost:5173/admin/login
```

After login, use `/admin/policies` to adjust development policies such as the region registration scope for real-device testing.

Keep the normal mobile test user as a `USER` account so user-flow validation still reflects production permissions.
