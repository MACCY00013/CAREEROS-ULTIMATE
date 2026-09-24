from fastapi.testclient import TestClient

from backend.app.main import app


def test_health_endpoint():
    with TestClient(app) as client:
        response = client.get("/api/v1/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_skill_sync_is_persistent_for_process():
    with TestClient(app) as client:
        response = client.post("/api/v1/skills/sync", json={"name": "Test skill", "level": "proficient"})
        assert response.status_code == 200
        skills = client.get("/api/v1/skills").json()["items"]
    assert {skill["name"] for skill in skills}.__contains__("Test skill")