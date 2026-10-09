# Authentication API

The server supports both the existing browser form/session flow and a
stateless JSON API. API authentication uses signed HS256 bearer tokens.

## Configure the signing key

Set `LMS_JWT_SECRET` to a Base64-encoded random key of at least 32 bytes before
starting the application. For example, generate a key locally with:

```text
openssl rand -base64 32
```

Keep the key outside source control. If it is omitted, the application creates
a random key for the current process; tokens issued with that key stop working
after a restart. `LMS_JWT_EXPIRATION_SECONDS` optionally sets token lifetime
(default: 3600 seconds).

The database still needs the `DB_PASSWORD` environment variable as described
in `docs/DB_SETUP.md`.

## Endpoints

### Register

`POST /api/auth/register` accepts JSON and returns `201 Created`.

```json
{
  "email": "student@example.com",
  "fullName": "Nguyen Van An",
  "password": "StrongPass123",
  "confirmPassword": "StrongPass123"
}
```

Passwords must contain at least eight characters, including a letter and a
digit. The response contains the new user's `id`, `email`, and `fullName`;
passwords and password hashes are never returned.

### Log in

`POST /api/auth/login` accepts `email` and `password`; success returns a
short-lived access token:

```json
{
  "tokenType": "Bearer",
  "accessToken": "<signed-token>",
  "expiresIn": 3600
}
```

Invalid credentials return `401 Unauthorized`. Duplicate registration returns
`409 Conflict`; invalid request data returns `400 Bad Request`.

### Current authenticated user

`GET /api/auth/me` requires the header
`Authorization: Bearer <accessToken>`. It returns the authenticated user's
identifier, email, and role authorities. Missing, invalid, or expired tokens
are rejected with `401 Unauthorized`.

Every `/api/**` request is stateless and does not use the browser login
session. The browser routes continue to use Spring Security form login and
session authentication.
