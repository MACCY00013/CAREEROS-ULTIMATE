# CareerOS

CareerOS is a local-first career operating system by **Saket Yadav / Maccy Creations**. This repository contains the first runnable web foundation: a FastAPI API and a responsive PWA shell for Skills Vault, Career Paths, Job Feed, and AI Hub.

## Run locally

```bash
cp .env.example .env
python -m pip install -r backend/requirements.txt
uvicorn backend.app.main:app --reload
```

Open `http://localhost:8000`. Docker users can run `docker compose up --build` after creating `.env`.

## Configuration

The Supabase project URL is represented in `.env.example`; the database password and all AI provider keys must be supplied locally or through the deployment secret manager. Never commit `.env`, database passwords, service-role keys, or provider keys. The publishable Supabase key is not a substitute for server-side authorization.

The current API exposes `/api/v1/health`, `/api/v1/dashboard`, `/api/v1/skills`, `/api/v1/skills/sync`, `/api/v1/careers`, `/api/v1/user/career-profile`, `/api/v1/applications`, `/api/v1/applications/sync`, `/api/v1/ai/roadmap`, `/api/v1/ai/ats-check`, `/api/v1/jobs/feed`, and `/api/v1/ai/hub/process`. Skills, applications, and the career profile persist through SQLAlchemy. Local development uses SQLite; Docker configures PostgreSQL, while Redis is ready for the worker layer. The job feed is seeded demo data until provider-specific adapters, terms-of-use checks, deduplication, and rate limits are implemented. Do not scrape sites that prohibit it; prefer official APIs and employer feeds.

## Production checklist

- Replace the in-memory stores with PostgreSQL migrations and repository services.
- Add Redis-backed queue workers, durable offline outbox records, auth with rotated HTTP-only sessions, MFA, and endpoint rate limits.
- Configure Supabase RLS and server-side JWT verification; keep the service-role key server-only.
- Add reviewed privacy/terms text, consent management, account deletion, audit logs, backups, Sentry, and a domain managed through your DNS provider.
- Provide real 192x192 and 512x512 PNG/WebP icons plus iOS splash assets before store or PWA release.
- Add provider adapters only after credentials, API permissions, attribution, and usage terms are confirmed.

The repository does not contain an APK or a custom domain yet. Those require a signed Android build pipeline and access to a domain/DNS and deployment account; they cannot be safely created from source alone.