# MVP Target Go/No-Go Input Request

Date: 2026-05-06

## Purpose

This document lists the exact inputs required before Codex can execute the approved deployed target-environment go/no-go checklist.

Do not paste raw secrets, database passwords, provider credentials, verification codes, access tokens, refresh tokens, or full phone numbers into repository documents.

Provide sensitive values only through the approved private release channel or the target runtime environment.

## Required Non-Secret Values

Fill these values in the approved release thread or private run context:

- target backend base URL:
- target frontend URL:
- target release commit or deployed artifact identifier:
- target environment name:
- target database/schema name or identifier:
- DB backup completed: yes/no
- DB restore owner:
- previous backend artifact or image location:
- previous frontend artifact location:
- new backend artifact or image location:
- new frontend artifact location:
- release operator name or role:
- rollback decision owner:
- log access owner or log review location:
- approved release window:
- approved observation window:

## Required Secret/Private Confirmations

Record only confirmation status in docs. Do not record the values.

- `DB_URL` configured: yes/no
- `DB_USERNAME` configured: yes/no
- `DB_PASSWORD` configured: yes/no
- `JWT_SECRET` configured: yes/no
- `KAKAO_REST_API_KEY` configured: yes/no
- `KAKAO_JAVASCRIPT_KEY` configured: yes/no
- `SOLAPI_API_KEY` configured: yes/no
- `SOLAPI_API_SECRET` configured: yes/no
- `SOLAPI_FROM` configured: yes/no
- approved recipient phone for SOLAPI smoke provided privately: yes/no
- approved GPS coordinate for Kakao region verification provided privately: yes/no
- admin operator account available privately: yes/no

## Required Runtime Flags

Confirm these target runtime flags before smoke execution:

- `SPRING_PROFILES_ACTIVE=prod`
- `FLYWAY_ENABLED=true`
- `JPA_DDL_AUTO=validate`
- `REGION_SEED_ENABLED=false`
- `POLICY_SEED_ENABLED=false`
- `ADMIN_BOOTSTRAP_ENABLED=false`
- `RANKING_SCHEDULER_ENABLED=false`
- SQL bind logging disabled

## Execution Gate

Codex can execute the deployed target go/no-go only after all required non-secret values and private confirmations are available.

Until then, the target go/no-go status is:

- [ ] GO
- [ ] NO-GO
- [x] PENDING TARGET INPUT

## Result Recording

After execution, record sanitized pass/fail results in:

- `docs/mvp-target-go-no-go-result.md`

Do not record:

- full phone numbers
- verification codes
- provider credentials
- database passwords
- JWT secrets
- raw access or refresh tokens
- raw provider responses
- raw logs containing sensitive fields
