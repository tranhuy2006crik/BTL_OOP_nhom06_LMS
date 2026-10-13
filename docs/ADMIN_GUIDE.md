# Admin page

The application admin area is available at `/admin` to active users with the
system-scoped `ADMIN` role. The admin menu is visible only to administrators;
the web security configuration also restricts every `/admin` route, and the
controller checks the current persisted account role and status on each request.

## Initial administrator

The schema seeds the built-in `ADMIN` role, but does not create an administrator
account. Register or create the intended account first, then grant the role
once in MySQL (replace the email with that account's email):

```sql
INSERT INTO system_role_assignments (user_id, role_id, role_scope)
SELECT u.id, r.id, 'SYSTEM'
FROM users u
JOIN roles r ON r.code = 'ADMIN' AND r.scope = 'SYSTEM'
WHERE u.email = 'admin@example.com';
```

After assigning the role, sign out and back in so the session gets the
administrator authority.

## Available management

- **Users** (`/admin/users`): search and filter accounts, create and edit
  accounts, grant/revoke system administrator access, suspend/reactivate, and
  mark accounts deleted. Deleted accounts are retained for learning history.
- **Courses** (`/admin/courses`): create, list, search, edit, change status,
  manage dates and category, and archive. Archiving preserves course data.
- **Categories** (`/admin/categories`): create, edit, and delete hierarchical
  categories. Categories containing courses or child categories cannot be
  deleted. Move those records first.

Only `ADMIN` is a system-scoped role. Teacher and student roles belong to course
memberships and are not assigned through the system-role checkbox. The schema
already contains course and category tables; the admin pages use those tables
directly through JPA entities and repositories.

The last active administrator cannot be suspended, deleted, or demoted, and an
administrator cannot remove their own administrator access or disable their
own account.
