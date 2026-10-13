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

## Course participation

Administrators publish a course and assign active instructors from its course
form. Assigned instructors can open the course from **My courses** or
**Dashboard**, create sections, add/edit lessons in those sections, hide
lessons from students, and publish course announcements. Hiding a lesson
retains its record.

Students use **Browse courses** to find published courses and enroll. Enrolled
students then see the course in **My courses** and **Dashboard**, where they
can open visible sections, lessons, and published announcements. A student
cannot access a course's private content before enrollment.

Archived courses cannot be opened through course pages or lesson and
announcement endpoints. Their learning data remains in the database; an
administrator can restore access by changing the course status from the course
edit page.

## Assignments and learning files

An active instructor opens a course section and uses **Bài tập** to create or
edit an assignment, set its due date, maximum grade, late-submission policy,
and optionally attach instructions. Instructors use **Tài liệu học tập** to
publish downloadable files for that section. Enrolled students can submit text,
files, or both, and can submit another attempt when needed. The grading page
shows links to the files attached to each submission.

Instructors open **Chấm điểm** to review submitted attempts, enter a score up
to the assignment's maximum grade, and add feedback. Saving a grade keeps it
private; use **Công bố điểm** when it is ready for the student. Editing a
published grade makes it private again until it is published again. Students
open **Điểm** to see published scores and feedback, plus their progress across
visible course activities. They can mark lessons and learning files complete;
an assignment is completed automatically after a submission is made.

Course files are stored outside MySQL in the directory configured by
`lms.course-files.storage-directory` (default: `./data/course-files`); the
database stores file metadata and links. The application accepts PDF,
PowerPoint, Word, ODP, ODT, and TXT files, up to 20 MB per file and 5 files per
upload, with a 50 MB combined file limit. The multipart request limit should be
at least 55 MB; the example configuration sets it to 20 MB per file and 55 MB
per request. Back up the storage directory together with the database so
existing file links remain valid. Files written for an operation are removed
if its database transaction rolls back.
