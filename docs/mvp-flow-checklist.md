# Honeytong MVP Flow Checklist

## User Flows

- [x] Signup creates an account.
- [x] Login stores access and refresh tokens.
- [x] Phone verification enables protected actions.
- [x] Region verification sets the user's active neighborhood.
- [x] Place list, search, nearby, and detail load.
- [x] Authenticated user can create, edit, and delete own place.
- [x] Authenticated user can recommend and cancel recommendation.
- [x] Authenticated user can verify a visit within policy limits.
- [x] Authenticated user can create, edit, and delete own place comment.
- [x] Authenticated user can create, edit, and delete own community post.
- [x] Authenticated user can report supported targets.
- [x] My page shows profile, activity, registered places, and reports.

## Admin Flows

- [x] Admin dashboard loads.
- [x] Admin can review users.
- [x] Admin can sanction users and adjust trust.
- [x] Admin can moderate places.
- [x] Admin can process reports.
- [x] Admin can invalidate recommendations and visits.
- [x] Super admin can update policies.
- [x] Audit logs capture admin actions.

## Technical Gates

- [x] Backend test suite passes.
- [x] Frontend production build passes.
- [x] Local backend starts with documented `.env`.
- [x] Local frontend connects to backend through `VITE_API_BASE_URL`.
- [x] Production profile requirements are documented.
