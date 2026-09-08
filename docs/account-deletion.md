# Account deletion — server deployment still required

The app no longer deletes a subset of database rows and claims success. It calls
`POST /functions/v1/delete-account` with the signed-in user's bearer token and
`{"confirm":true}`. An unavailable endpoint, failed request, wrong user ID or
unconfirmed response does not clear the local session or claim deletion succeeded.

This endpoint is NOT deployed by this commit. Do not submit the build for review
until its server implementation is deployed and tested with disposable accounts.

## Required server contract

- Accept POST only; validate `confirm === true`.
- Verify the access token with Supabase Auth (`getUser(token)`), not just JWT
  decoding. Derive the target ID from that verified user; never accept a target
  user ID from the request body. Reject missing, invalid and expired credentials.
- Keep admin/service-role and Apple credentials on the server only.
- Audit the actual schema, RLS and foreign keys before implementing cleanup.
  `supabase-schema-readonly.sql` provides a non-mutating schema inventory.
- Remove the user's application data, including TEXT-keyed legacy tables that
  do not cascade from auth.users. Never match by display name or a client-supplied
  Firebase ID. Review migrations linking legacy identities before deletion.
- Revoke Sign in with Apple provider tokens when applicable. Existing accounts
  may need Apple reauthentication to obtain a revocable provider token.
- Delete owned Supabase Storage objects via the Storage API and user media in R2
  via a server-only, ownership-checked inventory. Do not delete shared book/audio
  assets. Preserve retry state until these steps succeed.
- Delete the Auth user with `auth.admin.deleteUser(user.id)` (hard deletion).
  Check the returned error. Make retries safe; an intermediate cleanup failure
  must not be reported as complete. Review RLS so outstanding JWTs cannot continue
  accessing the app after deletion.
- Only after all required deletion steps succeed, return HTTP 200 with
  `{"deleted":true,"user_id":"<verified-user-id>"}`. Pending work is NOT success.
- Test invalid tokens, forged target IDs, wrong-user access, provider revocation,
  storage failures, database failures, retries and successful end-to-end cleanup.
  Use deliberately created test accounts, never real user accounts.

## Missing information

The supplied followers SQL is an old recreation script, not a current schema.
It contains DROP TABLE and unrestricted INSERT/DELETE policies. Do not execute it.
The complete live schema and R2 media ownership mapping are still needed. No
production data or policy changes have been made here.

References:
- https://supabase.com/docs/reference/javascript/auth-admin-deleteuser
- https://supabase.com/docs/guides/auth/managing-user-data
- https://developer.apple.com/support/offering-account-deletion-in-your-app/
- https://developer.apple.com/documentation/technotes/tn3194-handling-account-deletions-and-revoking-tokens-for-sign-in-with-apple
