from __future__ import annotations

import os

from sqlalchemy import Integer, String, create_engine, select
from sqlalchemy.orm import DeclarativeBase, Mapped, Session, mapped_column, sessionmaker


DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./careeros.db")
connect_args = {"check_same_thread": False} if DATABASE_URL.startswith("sqlite") else {}
engine = create_engine(DATABASE_URL, connect_args=connect_args, pool_pre_ping=True)
SessionLocal = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)


class Base(DeclarativeBase):
    pass


class Skill(Base):
    __tablename__ = "skills"
    name: Mapped[str] = mapped_column(String(120), primary_key=True)
    level: Mapped[str] = mapped_column(String(20), default="learning")


class CareerProfileRow(Base):
    __tablename__ = "career_profiles"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, default=1)
    target_role: Mapped[str] = mapped_column(String(120), default="")
    location: Mapped[str] = mapped_column(String(120), default="")
    budget: Mapped[str] = mapped_column(String(80), default="")


class Application(Base):
    __tablename__ = "applications"
    id: Mapped[str] = mapped_column(String(80), primary_key=True)
    company: Mapped[str] = mapped_column(String(120))
    role: Mapped[str] = mapped_column(String(160))
    status: Mapped[str] = mapped_column(String(20), default="wishlist")


def initialize(seed_skills: list[str]) -> None:
    Base.metadata.create_all(engine)
    with SessionLocal.begin() as database:
        existing = {skill.name for skill in database.scalars(select(Skill))}
        database.add_all(Skill(name=name) for name in seed_skills if name not in existing)
        if database.get(CareerProfileRow, 1) is None:
            database.add(CareerProfileRow(id=1))


def session() -> Session:
    return SessionLocal()