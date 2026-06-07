"""Adds a brief JavaDoc class comment to Java source files that do not already have one."""
from __future__ import annotations

import utils.transport
import utils.session
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src" / "main" / "java" / "org" / "smj" / "utilitybilling"


def describe(name: str, parent: str) -> str:
    base = name.removesuffix(".java")
    if name.endswith("Controller.java"):
        area = base.removesuffix("Controller")
        return f"REST controller exposing {area} API endpoints."
    if parent == "service" or base.endswith("Service"):
        area = base.removesuffix("Service")
        return f"Service layer implementing {area} business rules."
    if parent == "repository" or base.endswith("Repository"):
        area = base.removesuffix("Repository")
        return f"Spring Data repository for {area} persistence."
    if parent == "entity":
        return f"JPA entity mapped to the {base.lower()} database table."
    if parent == "request":
        return f"Validated request DTO for {base} API input."
    if parent == "response":
        return f"Response DTO returned by {base.removesuffix('Response')} APIs."
    if parent == "enums":
        return f"Domain enum defining {base} constants."
    if parent == "exception":
        return f"Application exception used for {base.removesuffix('Exception')} errors."
    if parent == "security":
        return f"Security component handling {base} for JWT authentication."
    if parent == "util":
        return f"Utility helpers for {base}."
    if parent == "config":
        return f"Spring configuration for {base}."
    if base == "UtilitybillingApplication":
        return "Spring Boot entry point for the Utility Billing application."
    return f"Utility Billing component: {base}."


def add_comment(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    if text.lstrip().startswith("/**"):
        return False

    match = re.search(r"(\n(?:@[^\n]+\n)*)(public |class |interface |enum |record )", text)
    if not match:
        return False

    insert_at = match.start(1)
    parent = path.parent.name
    comment = f"/**\n * {describe(path.name, parent)}\n */\n"
    path.write_text(text[:insert_at] + comment + text[insert_at:], encoding="utf-8")
    return True


def main() -> None:
    updated = 0
    for java_file in sorted(ROOT.rglob("*.java")):
        if add_comment(java_file):
            updated += 1
            print(f"Updated: {java_file.relative_to(ROOT)}")
    print(f"Done. Updated {updated} file(s).")


if __name__ == "__main__":
    main()
