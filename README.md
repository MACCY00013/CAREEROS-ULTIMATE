# CareerOS

CareerOS is a local-first career operating system by **Saket Yadav / Maccy Creations**. This repository contains the first runnable web foundation: a FastAPI API and a responsive PWA shell for Skills Vault, Career Paths, Job Feed, and AI Hub.

## Run locally

```bash
cp .env.example .env
python -m pip install -r backend/requirements.txt
uvicorn backend.app.main:app --reload
```

Open `http://localhost:8000`. Docker users can run `docker compose up --build` after creating `.env`.

## Deploy with Vercel

Import this repository at https://vercel.com/new. Vercel uses `vercel.json` to serve the PWA from `frontend/` and expose FastAPI through `api/index.py`. Configure these Vercel environment variables: `DATABASE_URL` (Supabase PostgreSQL connection string), `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `WEB_ORIGIN` (your Vercel URL), `OPENAI_API_KEY`, `GEMINI_API_KEY`, `SUPERHUMAN_GO_API_KEY`, `SUPERHUMAN_GO_API_URL`, and `REDIS_URL` from an external Redis provider such as Upstash. Docker Compose and the Render blueprint are alternatives; Vercel does not run PostgreSQL or Redis containers.

The Android client is under `android/`. Build it against a public HTTPS API with `cd android && ./gradlew assembleDebug -PapiBaseUrl=https://your-api.example.com`. Set the GitHub Actions repository variable `API_BASE_URL` to the same public URL before downloading a CI APK; otherwise the build intentionally falls back to the placeholder and cannot sync. For a phone on the same Wi-Fi as a development machine, use that machine's LAN address, such as `-PapiBaseUrl=http://192.168.1.20:8000`, and use the debug variant only. Cellular data and production Wi-Fi require a publicly reachable HTTPS endpoint. The app stores skills in Room and schedules WorkManager sync when network connectivity is available. GitHub Actions builds and uploads a debug APK on every push and pull request.

The current unsigned debug APK was built successfully at `artifacts/careeros-debug.apk` in the local workspace. It is not committed because release APKs should be signed through a protected build pipeline.

For a signed release, configure the repository secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`, then push a tag such as `v0.1.0`. The release workflow restores the keystore only on the GitHub runner and uploads the signed APK as an artifact.

To configure them interactively without exposing passwords in shell history, run `bash scripts/configure_android_signing.sh`. It requires a GitHub token with repository Actions-secret write permission.

## Configuration

The Supabase project URL is represented in `.env.example`; the database password and all AI provider keys must be supplied locally or through the deployment secret manager. Never commit `.env`, database passwords, service-role keys, or provider keys. The publishable Supabase key is not a substitute for server-side authorization.

The current API exposes `/api/v1/health`, `/api/v1/dashboard`, `/api/v1/skills`, `/api/v1/skills/sync`, `/api/v1/careers`, `/api/v1/user/career-profile`, `/api/v1/applications`, `/api/v1/applications/sync`, `/api/v1/ai/roadmap`, `/api/v1/ai/ats-check`, `/api/v1/jobs/feed`, `/api/v1/ai/hub/providers`, `/api/v1/ai/hub/process`, and `/api/v1/ai/hub/{id}`. Skills, applications, career profiles, and AI responses persist through SQLAlchemy. AI prompts are queued in Redis and processed by the `ai-worker` service using server-side provider keys. Local development uses SQLite; Docker configures PostgreSQL. The job feed is seeded demo data until provider-specific adapters, terms-of-use checks, deduplication, and rate limits are implemented. Do not scrape sites that prohibit it; prefer official APIs and employer feeds.

## Production checklist

- Replace the in-memory stores with PostgreSQL migrations and repository services.
- Add Redis-backed queue workers, durable offline outbox records, auth with rotated HTTP-only sessions, MFA, and endpoint rate limits.
- Configure Supabase RLS and server-side JWT verification; keep the service-role key server-only.
- Add reviewed privacy/terms text, consent management, account deletion, audit logs, backups, Sentry, and a domain managed through your DNS provider.
- Provide real 192x192 and 512x512 PNG/WebP icons plus iOS splash assets before store or PWA release.
- Add provider adapters only after credentials, API permissions, attribution, and usage terms are confirmed.
- Deploy with `render.yaml` from the Render dashboard. It provisions the API, PostgreSQL, Redis, and AI worker; enter provider keys as protected environment variables, then copy the generated API URL into the Android `API_BASE_URL` repository variable. Place `Caddyfile` behind a host with a DNS record for `DOMAIN` if you use a custom reverse proxy; a custom domain cannot be registered without access to the domain registrar and deployment account.

The repository does not contain an APK or a custom domain yet. Those require a signed Android build pipeline and access to a domain/DNS and deployment account; they cannot be safely created from source alone.