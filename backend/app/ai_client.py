from __future__ import annotations

import json
import os
from urllib.request import Request, urlopen


def configured(provider: str) -> bool:
    return {
        "chatgpt": bool(os.getenv("OPENAI_API_KEY")),
        "gemini": bool(os.getenv("GEMINI_API_KEY")),
        "superhuman-go": bool(os.getenv("SUPERHUMAN_GO_API_KEY") and os.getenv("SUPERHUMAN_GO_API_URL")),
    }[provider]


def _request(url: str, headers: dict[str, str], body: dict) -> dict:
    request = Request(url, data=json.dumps(body).encode(), headers={"Content-Type": "application/json", **headers}, method="POST")
    with urlopen(request, timeout=60) as response:
        return json.loads(response.read())


def complete(provider: str, prompt: str) -> str:
    if provider == "chatgpt":
        result = _request(
            os.getenv("OPENAI_API_URL", "https://api.openai.com/v1/chat/completions"),
            {"Authorization": f"Bearer {os.environ['OPENAI_API_KEY']}"},
            {"model": os.getenv("OPENAI_MODEL", "gpt-4o-mini"), "messages": [{"role": "user", "content": prompt}]},
        )
        return result["choices"][0]["message"]["content"]
    if provider == "gemini":
        url = os.getenv("GEMINI_API_URL", "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent")
        result = _request(f"{url}?key={os.environ['GEMINI_API_KEY']}", {}, {"contents": [{"parts": [{"text": prompt}]}]})
        return result["candidates"][0]["content"]["parts"][0]["text"]
    result = _request(
        os.environ["SUPERHUMAN_GO_API_URL"],
        {"Authorization": f"Bearer {os.environ['SUPERHUMAN_GO_API_KEY']}"},
        {"prompt": prompt},
    )
    return result.get("response") or result.get("text") or json.dumps(result)