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
            # Deliberately reviewed Paper 26.3 behavioral bug fix. Keep the
            # original SHA in legacy-logic-baseline.json for audit. Permit
            # ONLY this exact replacement blob, which is independently
            # checked by check_paper26_ability_regressions.py and the live
            # Paper 26.3 skill fixture.
            reviewed_fixes = {
                "src/com/praya/myitems/builder/ability/weapon/AbilityWeaponVenomSpread.java":
                    "bd4932b02dc53374879527ace2bdfc4aae5e2541",
            }
            if reviewed_fixes.get(relative) == actual:
                continue
            problems.append(f"changed: {relative} (original {expected[:12]}, current {actual[:12]})")

    if problems:
        print("FAIL: a baseline gameplay implementation or original YAML was modified.")
        for problem in problems:
            print(" -", problem)
        print("Do not automatically refresh this file list. Review the original behavior")
        print("and approve each intentional change with tests before changing the baseline.")
        return 1

    print(f"PASS: {len(DATA['files'])} protected files match original hashes or one exact reviewed fix.")
    print("The original hashes are retained; approved fixes require a matching git blob.")
    print("This is a source-integrity check, NOT proof of matching runtime behavior.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
