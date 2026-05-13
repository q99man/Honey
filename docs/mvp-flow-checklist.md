# Honeytong MVP Flow Checklist

## User Flows

- [x] Signup creates an account.
- [x] Login stores access and refresh tokens.
- [ ] Phone verification enables protected actions.
- [ ] Region verification sets the user's active neighborhood.
- [ ] Place list, search, nearby, and detail load.
- [ ] Authenticated user can create, edit, and delete own place.
- [ ] Authenticated user can recommend and cancel recommendation.
- [ ] Authenticated user can verify a visit within policy limits.
- [ ] Authenticated user can create, edit, and delete own place comment.
- [ ] Authenticated user can create, edit, and delete own community post.
- [ ] Authenticated user can report supported targets.
- [ ] My page shows profile, activity, registered places, and reports.

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
