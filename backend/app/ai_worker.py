from __future__ import annotations

import json
import os
import time

from .ai_client import complete
from .db import AIResponse, Base, engine, session


def run() -> None:
    import redis

    Base.metadata.create_all(engine)
    client = redis.from_url(os.environ["REDIS_URL"], decode_responses=True)
    while True:
        _, raw = client.blpop("careeros:ai", timeout=5) or (None, None)
        if not raw:
            continue
        job = json.loads(raw)
        try:
            answer = complete(job["provider"], job["prompt"])
            status = "COMPLETED"
        except Exception as error:
            answer = str(error)
            status = "FAILED"
        with session.begin() as database:
            database.add(AIResponse(id=job["id"], provider=job["provider"], prompt=job["prompt"], response=answer, status=status))
        time.sleep(0.01)


if __name__ == "__main__":
    run()