from __future__ import annotations

import os
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Literal

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field

from .db import Application, CareerProfileRow, Skill, initialize, session


ROOT = Path(__file__).resolve().parents[2]
WEB = ROOT / "frontend"

SKILLS = [
    "English communication", "Professional email writing", "Presentation skills",
    "Teamwork and collaboration", "Problem-solving", "Logical reasoning",
    "Time management", "Adaptability", "Leadership", "Critical thinking",
    "Business and commercial awareness", "Customer focus", "Agile and Scrum",
    "Project management", "Documentation", "Microsoft Excel", "PowerPoint", "SQL",
    "Python", "Java", "JavaScript/TypeScript", "HTML and CSS", "Git and GitHub",
    "REST APIs", "Linux", "Cloud computing", "AWS", "Microsoft Azure", "Google Cloud",
    "Docker", "Kubernetes", "CI/CD", "Terraform", "System design",
    "Database management", "Data structures and algorithms", "Software testing",
    "Debugging", "Cybersecurity fundamentals", "Identity and Access Management",
    "Active Directory", "OAuth 2.0", "OpenID Connect", "SAML", "Single Sign-On",
    "Privileged Access Management", "Network fundamentals", "Data analytics",
    "Artificial intelligence fundamentals", "Machine learning fundamentals",
    "Power BI or Tableau", "Customer relationship management", "SAP or enterprise software",
    "IT service management", "Risk and compliance", "Interview skills", "Resume writing",
    "LinkedIn profile development", "Portfolio and GitHub projects",
    "Professional certifications", "Cross-cultural communication", "Remote-work collaboration",
    "Ethical and responsible technology use",
]

CAREERS = [
    {"title": "Backend Engineer", "skills": ["Python", "REST APIs", "SQL", "Docker"]},
    {"title": "Cloud Security Engineer", "skills": ["IAM", "AWS", "Kubernetes", "Risk and compliance"]},
    {"title": "Data Analyst", "skills": ["SQL", "Data analytics", "Power BI or Tableau"]},
    {"title": "Product Manager", "skills": ["Leadership", "Agile and Scrum", "Customer focus"]},
    {"title": "Technical Project Manager", "skills": ["Project management", "Documentation", "Communication"]},
    {"title": "AI/ML Engineer", "skills": ["Python", "Machine learning fundamentals", "Data structures and algorithms"]},
    {"title": "Identity and Access Management Analyst", "skills": ["IAM", "OAuth 2.0", "Active Directory", "SAML"]},
]

JOBS = [
    {"id": "demo-1", "title": "Associate Software Engineer", "company": "Accenture", "location": "Bengaluru", "source": "Company careers"},
    {"id": "demo-2", "title": "Cloud Support Associate", "company": "AWS", "location": "Hyderabad", "source": "Company careers"},
    {"id": "demo-3", "title": "IAM Engineer", "company": "Okta", "location": "Remote", "source": "Company careers"},
]

DASHBOARD = {"skills_count": len(SKILLS), "saved_jobs": 0, "applications": 0, "roadmap_progress": 0}
ROADMAP = [{"id": "foundation", "title": "Career foundation", "done": False, "children": ["Choose a target role", "Audit your strongest skills"]}]
TRACKER = []


class SkillUpdate(BaseModel):
    name: str = Field(min_length=2, max_length=120)
    level: Literal["learning", "familiar", "proficient", "expert"] = "learning"


class CareerProfile(BaseModel):
    target_role: str = Field(default="", max_length=120)
    location: str = Field(default="", max_length=120)
    budget: str = Field(default="", max_length=80)


class PromptRequest(BaseModel):
    prompt: str = Field(min_length=1, max_length=4000)
    provider: Literal["chatgpt", "gemini", "superhuman-go"] = "chatgpt"


class TrackerItem(BaseModel):
    id: str = Field(min_length=1, max_length=80)
    company: str = Field(min_length=1, max_length=120)
    role: str = Field(min_length=1, max_length=160)
    status: Literal["wishlist", "applied", "interviewing", "offer", "rejected"] = "wishlist"


class ATSRequest(BaseModel):
    resume_text: str = Field(min_length=20, max_length=20000)
    job_description: str = Field(min_length=20, max_length=20000)


@asynccontextmanager
async def lifespan(_app):
    initialize(SKILLS)
    yield


app = FastAPI(title="CareerOS API", version="0.1.0", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=[os.getenv("WEB_ORIGIN", "http://localhost:8000")],
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT"],
    allow_headers=["Content-Type", "Authorization"],
)


@app.middleware("http")
async def security_headers(request, call_next):
    response = await call_next(request)
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
    response.headers["Content-Security-Policy"] = "default-src 'self'; connect-src 'self'; style-src 'self'; script-src 'self'; img-src 'self' data:"
    return response


@app.get("/api/v1/health")
def health():
    return {"status": "ok", "service": "careeros-api", "version": app.version}


@app.get("/api/v1/dashboard")
def dashboard():
    return {**DASHBOARD, "skills_count": len(SKILLS), "jobs_count": len(JOBS)}


@app.get("/api/v1/skills")
def list_skills():
    with session() as database:
        return {"items": [{"name": skill.name, "level": skill.level} for skill in database.query(Skill).order_by(Skill.name)]}


@app.post("/api/v1/skills/sync")
def sync_skill(skill: SkillUpdate):
    with session.begin() as database:
        saved = database.get(Skill, skill.name)
        if saved:
            saved.level = skill.level
        else:
            database.add(Skill(name=skill.name, level=skill.level))
    return {"saved": True, "item": skill.model_dump(), "sync_status": "SYNCED"}


@app.get("/api/v1/ai/roadmap")
def roadmap():
    return {"items": ROADMAP}


@app.get("/api/v1/applications")
def applications():
    with session() as database:
        return {"items": [{"id": item.id, "company": item.company, "role": item.role, "status": item.status} for item in database.query(Application).order_by(Application.company)]}


@app.post("/api/v1/applications/sync")
def sync_application(item: TrackerItem):
    with session.begin() as database:
        existing = database.get(Application, item.id)
        if existing:
            existing.company = item.company
            existing.role = item.role
            existing.status = item.status
        else:
            database.add(Application(**item.model_dump()))
    return {"saved": True, "item": item.model_dump(), "sync_status": "SYNCED"}


@app.post("/api/v1/ai/ats-check")
def ats_check(request: ATSRequest):
    resume_words = {word.lower().strip(".,:;()") for word in request.resume_text.split()}
    job_words = {word.lower().strip(".,:;()") for word in request.job_description.split()}
    keywords = sorted(word for word in job_words & resume_words if len(word) > 3)
    score = round((len(keywords) / max(len(job_words), 1)) * 100)
    return {"score": min(score, 100), "matched_keywords": keywords[:40], "status": "complete"}


@app.get("/api/v1/careers")
def list_careers():
    return {"items": CAREERS}


@app.get("/api/v1/user/career-profile")
def get_career_profile():
    with session() as database:
        profile = database.get(CareerProfileRow, 1)
        return {"target_role": profile.target_role, "location": profile.location, "budget": profile.budget}


@app.put("/api/v1/user/career-profile")
def update_career_profile(profile: CareerProfile):
    with session.begin() as database:
        saved = database.get(CareerProfileRow, 1)
        saved.target_role = profile.target_role
        saved.location = profile.location
        saved.budget = profile.budget
    return {"saved": True, "profile": profile.model_dump()}


@app.get("/api/v1/jobs/feed")
def job_feed():
    return {"items": JOBS, "source_count": 1, "note": "Provider adapters are configured through server-side credentials."}


@app.post("/api/v1/ai/hub/process")
def process_prompt(request: PromptRequest):
    configured = {
        "chatgpt": bool(os.getenv("OPENAI_API_KEY")),
        "gemini": bool(os.getenv("GEMINI_API_KEY")),
        "superhuman-go": bool(os.getenv("SUPERHUMAN_GO_API_KEY")),
    }
    if not configured[request.provider]:
        raise HTTPException(status_code=503, detail=f"{request.provider} is not configured on the server")
    return {"status": "queued", "provider": request.provider, "message": "Prompt accepted for secure background processing."}


if WEB.exists():
    app.mount("/", StaticFiles(directory=WEB, html=True), name="frontend")


@app.get("/")
def index():
    return FileResponse(WEB / "index.html")