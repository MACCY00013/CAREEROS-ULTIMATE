from __future__ import annotations

import json
import os


def enqueue(queue_name: str, payload: dict) -> str:
    redis_url = os.getenv("REDIS_URL")
    if not redis_url:
        return "local-fallback"
    try:
        import redis

        client = redis.from_url(redis_url, decode_responses=True)
        client.rpush(queue_name, json.dumps(payload))
        return "redis"
    except Exception:
        return "local-fallback"