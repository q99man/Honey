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

- [ ] Admin dashboard loads.
- [ ] Admin can review users.
- [ ] Admin can sanction users and adjust trust.
- [ ] Admin can moderate places.
- [ ] Admin can process reports.
- [ ] Admin can invalidate recommendations and visits.
- [ ] Super admin can update policies.
- [ ] Audit logs capture admin actions.

## Technical Gates

- [x] Backend test suite passes.
- [x] Frontend production build passes.
- [x] Local backend starts with documented `.env`.
- [ ] Local frontend connects to backend through `VITE_API_BASE_URL`.
- [ ] Production profile requirements are documented.
