# Phone Verification Live Smoke

This runbook verifies the production SMS sender without committing provider credentials or printing raw verification codes.

## Preconditions

- Backend is running against a test or local database.
- `PHONE_VERIFICATION_SENDER_PROVIDER=solapi` is set for the backend process.
- `SOLAPI_API_KEY`, `SOLAPI_API_SECRET`, and `SOLAPI_FROM` are set privately.
- Naver Cloud SENS is still supported with `PHONE_VERIFICATION_SENDER_PROVIDER=naver-sens` when a business account is available.
- The receiving phone number is available to the tester.
- Normal logs keep SQL bind logging disabled and do not log raw verification codes.

## Command

From the repository root:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\scripts\smoke-phone-verification.ps1 -Phone "01012345678"
```

Use `-BaseUrl` for a non-local backend and `-SkipSignup -Email ... -Password ...` when reusing an existing smoke account.

The script asks for confirmation before it creates or uses the smoke account and transmits the phone number to the backend/SMS provider. It masks the phone number in console output and prompts for the received code without logging it.

## Passing Result

- `POST /api/auth/phone/send-code` returns `sent=true`.
- The handset receives the SMS.
- `POST /api/auth/phone/verify-code` returns `phoneVerified=true`.
- Backend normal logs show delivery request/failure with only a masked phone number.

If provider credentials or sender-number registration are wrong, the API should fail as `EXTERNAL_SERVICE_ERROR` and no latest verification-code cache entry should be written.
