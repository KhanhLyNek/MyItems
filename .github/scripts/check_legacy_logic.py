#!/usr/bin/env python3
"""Fail CI when legacy gameplay implementations or YAML have been changed.

The hashes are Git blob IDs captured before modernizing the implementations.
Updates to the baseline require a documented, deliberate review. This is
deliberately stricter than parsing individual IDs or damage formulas.
"""
from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = json.loads((ROOT / ".github/legacy-logic-baseline.json").read_text("utf-8"))


def main() -> int:
    problems = []
    for relative, expected in DATA["files"].items():
        path = ROOT / relative
        if not path.is_file():
            problems.append(f"missing: {relative}")
            continue
        result = subprocess.run(
            ["git", "hash-object", "--", str(path)],
            capture_output=True, text=True, check=True, cwd=ROOT,
        )
        actual = result.stdout.strip()
        if actual != expected:
            problems.append(f"changed: {relative} (original {expected[:12]}, current {actual[:12]})")

    if problems:
        print("FAIL: a baseline gameplay implementation or original YAML was modified.")
        for problem in problems:
            print(" -", problem)
        print("Do not automatically refresh this file list. Review the original behavior")
        print("and approve each intentional change with tests before changing the baseline.")
        return 1

    print(f"PASS: {len(DATA['files'])} protected original files unchanged.")
    print("This is a source-integrity check, NOT proof of matching runtime behavior.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
