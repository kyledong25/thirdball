# Third Ball

> A full-stack operations hub for a university table tennis club: manage 250+ members, maintain an internal USATT-style rating ladder, run tournaments, and coordinate practice blocks.

| Administrator operations desk | Member self-service dashboard |
| --- | --- |
| ![Third Ball administrator dashboard](docs/screenshots/admin-dashboard.svg) | ![Third Ball member dashboard](docs/screenshots/member-dashboard.svg) |

## Highlights

- **USATT rating ladder:** New players complete a five-match provisional phase before receiving a rating; established-player results follow the fixed USATT point-exchange chart, retain before/after ratings, and can be safely invalidated by an administrator.
- **Role-based dashboards:** Administrators manage the ladder, dues, events, generated brackets, and result review; members maintain their own profile, view rating progress, and sign up for club events.
- **Tournament desk:** Register the field, generate a rating-seeded single-elimination bracket with automatic byes, record results, and advance winners automatically.
- **Practice operations and global calendar:** Publish single- or multi-day blocks, enforce capacity, and show one combined schedule of practices and tournaments.
- **Built for a real club:** PostgreSQL transactions and pessimistic locks protect rating updates and capacity checks when many members act at once.

## Stack

- **Backend:** Java 11, Spring Boot, Spring Data JPA, Flyway, PostgreSQL, REST APIs
- **Frontend:** React functional components and hooks, Axios, Vite, semantic HTML, custom CSS
- **Local infrastructure:** Docker Compose / PostgreSQL 15

## Run locally

### 1. Prerequisites

- Java 11 (already compatible with this project) and Maven 3.8+.
- PostgreSQL 15, either installed locally or started with Docker Compose.

### 2. Start PostgreSQL

```bash
docker compose up -d postgres
```

The default database connection is `jdbc:postgresql://localhost:5432/thirdball`, user `thirdball`, password `thirdball_dev`. Override it with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` when required.

### 3. Start the API

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 11) mvn spring-boot:run
```

Homebrew's Maven installation may otherwise select its bundled Java 26 runtime. This project targets Java 11, so use the command above (or export the same `JAVA_HOME` in your shell) until the backend is upgraded to Spring Boot 3 / Java 17+.

Flyway creates the schema automatically. Hibernate is configured to validate (never alter) it at startup.

### 4. Start the frontend

The React frontend is in [`frontend`](./frontend). It uses functional components, hooks, Axios, and custom CSS; Vite proxies `/api` requests to the Spring Boot service, so no development CORS configuration is needed.

In a second terminal, with the backend still running on port 8080:

```bash
cd frontend
npm install
npm run dev
```

Open [http://127.0.0.1:5173](http://127.0.0.1:5173). The frontend covers:

- player creation, USATT ladder rankings, and ladder-result submission;
- tournament creation, player registration, rating-seeded bracket generation, result entry, and round-based bracket visualization;
- multi-day practice-block publishing, capacities, member registration, a global calendar, dues tracking, and member rating-history charts.

Build a production bundle with `npm run build` from `frontend`.

### 5. Populate the demo dataset (optional)

The repeat-safe seed script adds 26 fictional players with varied ratings, two completed tournaments, an open tournament, completed bracket matches, and upcoming practice blocks:

```bash
docker compose exec -T postgres psql -U thirdball -d thirdball < scripts/seed-demo.sql
```

Running it again preserves existing rows. The project’s local database has already been seeded once.

## Local networking and CORS

Vite proxies browser requests from port 5173 to Spring Boot on port 8080, so ordinary local development does not hit a cross-origin boundary. The backend also has a global CORS policy for direct API calls; it permits both `http://localhost:5173` and `http://localhost:3000` by default.

Override the allowed browser origins when needed:

```bash
CORS_ALLOWED_ORIGINS=https://your-frontend.example.com \
JAVA_HOME=$(/usr/libexec/java_home -v 11) mvn spring-boot:run
```

For a deployed React build, set `VITE_API_BASE_URL` to the public backend API base (for example, `https://api.example.com/api`). See [`frontend/.env.example`](frontend/.env.example).

## Move PostgreSQL to Supabase

Third Ball already externalizes its database settings, so it can run on Supabase PostgreSQL without a code change. Use a new, empty Supabase project for the initial import. Do not expose a database password to the frontend or commit it to this repository.

1. In the Supabase dashboard, open **Connect** and copy either the Direct connection string or the Session pooler connection string. A Direct connection is best for migrations when the host supports IPv6; Session pooler is the appropriate alternative for IPv4-only hosts.
2. Start the backend once with the corresponding JDBC settings so Flyway applies the schema. The database URI is different from JDBC URI: replace `postgresql://` with `jdbc:postgresql://` and add `?sslmode=require`.

   ```bash
   DB_URL='jdbc:postgresql://aws-0-your-region.pooler.supabase.com:5432/postgres?sslmode=require' \
   DB_USERNAME='postgres.your-project-ref' \
   DB_PASSWORD='your-supabase-database-password' \
   SERVER_PORT=0 \
   JAVA_HOME=$(/usr/libexec/java_home -v 11) mvn spring-boot:run
   ```

   Stop the process after its startup log reports that Flyway is up to date.
3. Copy the local application data. The helper exports only table data, preserves the target's Flyway history, will not overwrite a non-empty target, restores the bracket in one transaction, and verifies the four primary table counts before declaring success.

   ```bash
   export SUPABASE_DB_URL='postgresql://postgres.your-project-ref:your-supabase-database-password@aws-0-your-region.pooler.supabase.com:5432/postgres'
   bash scripts/copy-local-data-to-supabase.sh
   ```

   The helper uses the PostgreSQL client already in the local Docker container, so it does not require `psql`, `pg_dump`, or `pg_restore` to be installed on your Mac. It creates a temporary dump under the system temporary directory and deletes it on exit.
4. Set the same `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` on the deployed Spring Boot service, plus the production `CORS_ALLOWED_ORIGINS` value.

Supabase recommends a direct connection for migrations where IPv6 is available, and Session pooler as the IPv4-compatible fallback. Its PostgreSQL migration guide also notes that managed roles and Row Level Security policies are not copied by a dump/restore; Third Ball uses its Spring Boot API rather than exposing tables directly, so no frontend database credential should be used. See the [Supabase migration guide](https://supabase.com/docs/guides/platform/migrating-to-supabase/postgres) and [connection guide](https://supabase.com/docs/guides/database/connecting-to-postgres).

## API foundation

| Capability | Routes |
| --- | --- |
| Players | `POST`, `GET` `/api/players`; `POST /api/players/{id}/remove-from-ladder` |
| Tournaments | `POST`, `GET` `/api/tournaments`; `POST /api/tournaments/{id}/registrations` |
| Matches / ladder results | `POST`, `GET` `/api/matches`; `GET /api/matches/{id}`; `POST /api/matches/{id}/result`; `POST /api/matches/{id}/invalidate` |
| Bracket data | `GET /api/tournaments/{id}/matches` |
| Practice blocks | `POST`, `GET` `/api/practice-sessions`; `POST /api/practice-sessions/{id}/registrations` |

Create two players:

```bash
curl -X POST http://localhost:8080/api/players \
  -H 'Content-Type: application/json' \
  -d '{"displayName":"Maya Chen","email":"maya@example.edu"}'
```

Schedule and conclude a ladder match (replace player IDs):

```bash
curl -X POST http://localhost:8080/api/matches \
  -H 'Content-Type: application/json' \
  -d '{"playerOneId":1,"playerTwoId":2,"roundNumber":1}'

curl -X POST http://localhost:8080/api/matches/1/result \
  -H 'Content-Type: application/json' \
  -d '{"playerOneScore":3,"playerTwoScore":1}'
```

## USATT rating behavior

For two established players, Third Ball compares their pre-match ratings and applies the fixed USATT point-exchange chart. A higher-rated player winning receives the expected-result value; a lower-rated player winning receives the upset value. The winner gains the listed amount and the loser loses the same amount, preserving the rating pool's total.

| Rating spread | Higher-rated player wins | Lower-rated player wins |
| --- | ---: | ---: |
| 0–12 | 8 | 8 |
| 13–37 | 7 | 10 |
| 38–62 | 6 | 13 |
| 63–87 | 5 | 16 |
| 88–112 | 4 | 20 |
| 113–137 | 3 | 25 |
| 138–162 | 2 | 30 |
| 163–187 | 2 | 35 |
| 188–212 | 1 | 40 |
| 213–237 | 1 | 45 |
| 238+ | 0 | 50 |

New players start **unrated**. Their first five completed results must be against an established player and do not alter that opponent's rating. After the fifth result, Third Ball sets the starting rating as follows:

- Mixed record: midpoint of the highest-rated player defeated and the lowest-rated player lost to (rounded to the nearest whole rating point).
- Undefeated: 250 points above the highest-rated player defeated.
- Winless: 50 points below the lowest-rated player lost to.

The newly established player uses the standard chart from their next match forward. The service locks both player rows in a consistent order and the match row before applying the calculation, so concurrent score submissions cannot overwrite a rating change or score the same match twice.

## Role-based access control

Flyway migration `V6` adds a `club_users` account table with a unique email, BCrypt password hash, `ADMIN` or `MEMBER` role, and an optional one-to-one link to `players`. Existing player records are retained unchanged. Member registration reuses a matching player email when one exists, or creates a new unrated player record.

| Route | Role | Purpose |
| --- | --- | --- |
| `POST /api/auth/register` | Public | Create a MEMBER account and linked player record. |
| `GET /api/auth/me` | Authenticated | Return the current account and role. |
| `GET /api/member/practice-sessions` | MEMBER | List upcoming practice sessions. |
| `GET /api/member/tournaments` | MEMBER | List upcoming tournaments. |
| `GET /api/member/profile` / `PUT /api/member/profile` | MEMBER | Read or update graduation year, skill level, and phone number. |
| `GET /api/member/rating-history` | MEMBER | Read the member's chart-ready rating series from completed result snapshots. |
| `GET /api/member/ladder` | MEMBER | Read the active global ladder, limited to rank, player name, and rating information. |
| `POST /api/member/practice-sessions/{id}/signup` | MEMBER | Register the authenticated member for practice. |
| `POST /api/member/tournaments/{id}/signup` | MEMBER | Register the authenticated member for a tournament. |
| `PUT /api/players/{id}/rating` | ADMIN | Correct a player's rating and mark them established. |
| `PUT /api/players/{id}/dues` | ADMIN | Mark a member's dues paid or unpaid. |
| `POST /api/players/{id}/remove-from-ladder` | ADMIN | Soft-remove a player from active ladder and event operations while retaining history. |
| `POST /api/matches/{id}/invalidate` | ADMIN | Cancel the newest completed result for its players and restore their pre-match ratings. |
| `POST /api/tournaments/{id}/generate-bracket` | ADMIN | Lock registration and build a rating-seeded single-elimination tree. |
| Existing `/api/players`, `/api/matches`, `/api/tournaments`, and `/api/practice-sessions` routes | ADMIN | Club operations dashboard. |

The web client uses HTTP Basic authentication over the existing HTTPS deployment and keeps the authorization value only in browser session storage. Configure the first administrator through Render's environment variables before deploying. After the first `ADMIN` account is created, the bootstrap password is ignored.

## Deployment checklist

Third Ball is configured for a Supabase + Render + Vercel deployment. No database credential belongs in the frontend or the repository.

1. The Supabase database has already been migrated. On Render, create a **Web Service** with the **Docker** runtime (the checked-in [`Dockerfile`](Dockerfile) builds the Java service) and set `SPRING_PROFILES_ACTIVE=prod` plus these environment variables:

   ```text
   DB_URL=jdbc:postgresql://<supabase-host>:5432/postgres?sslmode=require
   DB_USERNAME=<supabase-connection-user>
   DB_PASSWORD=<supabase-database-password>
   BOOTSTRAP_ADMIN_EMAIL=<administrator-email>
   BOOTSTRAP_ADMIN_PASSWORD=<administrator-password>
   CORS_ALLOWED_ORIGINS=https://<your-vercel-project>.vercel.app
   ```

   Get the hostname, connection user, and password from Supabase **Connect**. Use the Session pooler if Render cannot reach the direct connection; convert Supabase's `postgresql://` URI to `jdbc:postgresql://` for `DB_URL`, keeping `sslmode=require`. Render supplies `PORT`; [`application-prod.properties`](src/main/resources/application-prod.properties) reads it automatically.

2. On Vercel, set the project root to `frontend`, build with `npm run build`, publish `dist`, and add this build-time environment variable:

   ```text
   VITE_API_BASE_URL=https://<your-render-service>.onrender.com/api
   ```

   See [`frontend/.env.example`](frontend/.env.example). Vite exposes only `VITE_`-prefixed values to the browser, so never place Neon or Render secrets there.

3. After both deploys, update Render's `CORS_ALLOWED_ORIGINS` with the exact Vercel origin (no path or trailing slash), redeploy the backend, and confirm the backend startup log says Flyway is up to date.

## Current capabilities

The generated tournament bracket seeds established members by rating, places unrated members after them, creates the smallest power-of-two tree, and automatically advances opening-round byes and later winners. The member rating-history chart deliberately uses only completed-match rating snapshots; direct administrator corrections remain visible in the current rating but are not fabricated into historical match points.
