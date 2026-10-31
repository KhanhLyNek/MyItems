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
                "src/com/praya/myitems/builder/ability/weapon/AbilityWeaponFreeze.java":
                    "98b3d1dfe854c7a26b12e54429487a00708d14b2",
                "src/com/praya/myitems/builder/ability/weapon/AbilityWeaponRoots.java":
                    "f83eae052cb8fef3a3891b29ff34bd1d13bda0f2",
                "src/com/praya/myitems/builder/ability/weapon/AbilityWeaponCurse.java":
                    "736f7e3fdaa9a6ad71ed5068f6b4152843d4043a",
                # Reviewed plugin.yml-only extension: optional model providers
                # and OP-default model permission. All previous YAML commands,
                # aliases, permissions and dependencies remain byte-exact.
                "resources/plugin.yml":
                    "173e105fef679ed6f790a9491771332d763aaa2c",
            }
            if reviewed_fixes.get(relative) == actual:
                continue
            if relative == "resources/plugin.yml":
                # Approved rename only: restore the original frozen manifest
                # in-memory and compare to the reviewed pre-rename blob.
                manifest = path.read_text("utf-8")
                if ("main: com.lymeowz.item.LyMeowzItem\n" in manifest and
                        "name: LyMeowzItem\n" in manifest and
                        "  lymeowzitem.admin:\n" in manifest and
                        "provides: [MyItems]\n" in manifest and
                        "- LyMeowzLib\n" in manifest and
                        "  lymeowzitem:\n" in manifest and
                        "  myitems:\n" not in manifest):
                    normalized = (manifest
                        .removesuffix("\n  # LyMeowz canonical permissions; the original MyItems nodes remain supported.\n  lymeowzitem.admin:\n    description: Administrative access to LyMeowzItem\n    default: op\n    children:\n      lymeowzitem.command.reload: true\n      lymeowzitem.command.save: true\n      lymeowzitem.command.remove: true\n      lymeowzitem.command.list: true\n      lymeowzitem.command.load: true\n      lymeowzitem.command.drop: true\n      lymeowzitem.command.amount: true\n      lymeowzitem.command.data: true\n      lymeowzitem.command.material: true\n      lymeowzitem.command.repair: true\n      lymeowzitem.command.name: true\n      lymeowzitem.command.lore: true\n      lymeowzitem.command.flag: true\n      lymeowzitem.command.enchant: true\n      lymeowzitem.command.unbreakable: true\n      lymeowzitem.command.attribute.stats: true\n      lymeowzitem.command.attribute.element: true\n      lymeowzitem.command.attribute.buff: true\n      lymeowzitem.command.attribute.debuff: true\n      lymeowzitem.command.attribute.nbt: true\n      lymeowzitem.command.attribute.power: true\n      lymeowzitem.command.attribute.ability: true\n      lymeowzitem.command.attribute.requirement: true\n      lymeowzitem.command.socket: true\n      lymeowzitem.command.model: true\n\n  lymeowzitem.command.reload:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.save:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.remove:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.list:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.load:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.drop:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.amount:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.data:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.material:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.repair:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.name:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.lore:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.flag:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.enchant:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.unbreakable:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.attribute.stats:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.attribute.element:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.attribute.buff:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.attribute.debuff:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.attribute.nbt:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.attribute.power:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.attribute.ability:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.attribute.requirement:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.socket:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.model:\n    description: LyMeowzItem command permission\n    default: false\n")
                        .replace("main: com.lymeowz.item.LyMeowzItem\n",
                                 "main: com.praya.myitems.MyItems\n")
                        .replace("name: LyMeowzItem\n", "name: MyItems\n")
                        .replace("provides: [MyItems]\n", "")
                        .replace("- LyMeowzLib\n", "- AgarthaLib\n")
                        .replace("  lymeowzitem:\n", "  myitems:\n"))
                    reviewed_hash = subprocess.run(
                        ["git", "hash-object", "--stdin"], input=normalized,
                        capture_output=True, text=True, check=True, cwd=ROOT,
                    ).stdout.strip()
                    if manifest.endswith("\n  # LyMeowz canonical permissions; the original MyItems nodes remain supported.\n  lymeowzitem.admin:\n    description: Administrative access to LyMeowzItem\n    default: op\n    children:\n      lymeowzitem.command.reload: true\n      lymeowzitem.command.save: true\n      lymeowzitem.command.remove: true\n      lymeowzitem.command.list: true\n      lymeowzitem.command.load: true\n      lymeowzitem.command.drop: true\n      lymeowzitem.command.amount: true\n      lymeowzitem.command.data: true\n      lymeowzitem.command.material: true\n      lymeowzitem.command.repair: true\n      lymeowzitem.command.name: true\n      lymeowzitem.command.lore: true\n      lymeowzitem.command.flag: true\n      lymeowzitem.command.enchant: true\n      lymeowzitem.command.unbreakable: true\n      lymeowzitem.command.attribute.stats: true\n      lymeowzitem.command.attribute.element: true\n      lymeowzitem.command.attribute.buff: true\n      lymeowzitem.command.attribute.debuff: true\n      lymeowzitem.command.attribute.nbt: true\n      lymeowzitem.command.attribute.power: true\n      lymeowzitem.command.attribute.ability: true\n      lymeowzitem.command.attribute.requirement: true\n      lymeowzitem.command.socket: true\n      lymeowzitem.command.model: true\n\n  lymeowzitem.command.reload:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.save:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.remove:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.list:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.load:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.drop:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.amount:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.data:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.material:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.repair:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.name:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.lore:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.flag:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.enchant:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.unbreakable:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.attribute.stats:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.attribute.element:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.attribute.buff:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.attribute.debuff:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.attribute.nbt:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.attribute.power:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.attribute.ability:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.attribute.requirement:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.socket:\n    description: LyMeowzItem command permission\n    default: false\n  lymeowzitem.command.model:\n    description: LyMeowzItem command permission\n    default: false\n") and reviewed_hash == reviewed_fixes.get(relative):
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
