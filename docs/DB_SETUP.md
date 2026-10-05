# Local MySQL setup

The checked-in SQL schema is the source of truth for the database structure.
Each developer can run it on a local MySQL 8.0.19+ server.

For a Vietnamese table-by-table explanation of keys, relationships, status
values, and backend rules, see `docs/DATABASE_GUIDE_VI.md`.

## Create or upgrade the database

For a new database, `src/main/resources/db/mysql-schema.sql` is the complete
fresh-install script: it creates all 17 tables, seeds the roles, then runs
verification queries. Open it in MySQL Workbench and execute the whole file.
The final results should show 17 base tables and `ADMIN` → `SYSTEM`,
`TEACHER` / `STUDENT` → `COURSE`.

This fresh-install script does not alter existing tables. For an existing v1
database, do not use it as a migration.

If the database was created earlier from schema v1, back it up first, then run
`src/main/resources/db/mysql-upgrade-v1-to-v2.sql` exactly once as an
administrator. Do not run the upgrade on a fresh v2 database. The upgrade
preserves existing data and adds columns and constraints. It assumes existing
system-role assignments use `ADMIN` and course memberships use `TEACHER` or
`STUDENT`; invalid role assignments can make the migration fail. The database
cannot validate that each `ACTIVITIES` row has exactly one matching subtype
row, so the service layer must maintain that invariant.

After either path, confirm the database and role seed data:

   ```sql
   USE lms_db;
   SHOW TABLES;
   SELECT code, name, scope FROM roles ORDER BY code;
   ```

Expected role scopes are `ADMIN` → `SYSTEM`, and `TEACHER` / `STUDENT` →
`COURSE`. The v2 foreign keys enforce that system-role assignments use system
roles and course memberships use course roles.

`mysql-schema.sql` creates missing objects but does not upgrade existing
tables. For future schema changes, create a new versioned upgrade script,
back up the database, and apply that script rather than expecting
`CREATE TABLE IF NOT EXISTS` to alter existing tables.

The schema enforces allowed status values, role scope, positive attempt
numbers, valid date ranges, and timestamp requirements for drafts,
submissions, published announcements, completed activities, and grade release
times. The backend must enforce rules involving several rows: membership
before submitting, course staff permissions, grade not exceeding assignment
maximum, and exactly one activity detail subtype matching its type.

## Create a restricted local application account

In Workbench, connected as the local MySQL administrator, create a password
locally and run these statements after replacing the placeholder. Do not share
the password or commit it:

```sql
CREATE USER IF NOT EXISTS 'lms_app'@'localhost'
IDENTIFIED BY 'REPLACE_WITH_A_STRONG_LOCAL_PASSWORD';

GRANT SELECT, INSERT, UPDATE, DELETE
ON lms_db.*
TO 'lms_app'@'localhost';
```

The application account is for Spring Boot data access only. Use an
administrator account to run schema DDL and database setup scripts.
`lms_app` is a local account on each developer's own MySQL server; it is not
the LMS administrator account used to sign in to the web application.

Course and user data should normally be archived or marked inactive rather
than hard-deleted, especially after submissions or grades exist.

## Configure Spring Boot

1. Copy `src/main/resources/application.properties.example` to
   `src/main/resources/application.properties`. This local file is ignored by
   Git. The example imports an optional `.env` file from the project root.
2. Copy `.env.example` to `.env` in the project root and replace
   `DB_PASSWORD` with the local `lms_app` password. `.env` is ignored by Git
   and must be created separately on each developer's machine. Alternatively,
   provide the values as environment variables in the IDE run configuration.
   `DB_URL` and `DB_USERNAME` can be left at their local defaults, or set in
   `.env` / the process environment:
   - `DB_URL=jdbc:mysql://localhost:3306/lms_db`
   - `DB_USERNAME=lms_app`
3. (Optional) Set `LMS_JWT_SECRET` to a locally generated Base64-encoded key
   of at least 32 bytes. Generate one with `openssl rand -base64 32`, or leave
   it empty for a temporary key that changes on each application restart.
   `LMS_JWT_EXPIRATION_SECONDS` defaults to `3600`.
4. Start Spring Boot with the project root as the working directory, so Spring
   can find `.env`. Hibernate uses `validate`: it checks that Java entities
   match the SQL schema and does not create or modify database tables.

Do not use the MySQL `root` account in the application configuration and do
not commit `application.properties` or database passwords.
