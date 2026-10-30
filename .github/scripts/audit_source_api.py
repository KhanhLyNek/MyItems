#!/usr/bin/env python3
"""Repository-wide static API risk inventory.

Runs through every Java source in the checkout, including packet handlers.
The report is intentionally NOT a build pass/fail or a runtime-logic test.
"""
from __future__ import annotations

from collections import defaultdict
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]

PATTERNS = {
    "Versioned NMS imports/references": re.compile(r"\bnet\.minecraft\.server\.v1_\d+_R\d+\b"),
    "Versioned CraftBukkit imports/references": re.compile(r"\borg\.bukkit\.craftbukkit\.v1_\d+_R\d+\b"),
    "Raw packet implementation classes": re.compile(r"\bPacket(?:Item|Particle|Entity|Server|Sounds|Inventory|Message)[A-Za-z_]*_1_\d+\b"),
    "Legacy registry lookups": re.compile(r"\b(?:PotionEffectType|Enchantment|Material|EntityType)\.getById\s*\(|\bPotionEffectType\.getByName\s*\("),
    "Runtime class reflection": re.compile(r"\bClass\.forName\s*\(|\bgetDeclared(?:Method|Field|Constructor)\s*\("),
    "Optional external plugin APIs": re.compile(r"\b(?:com\.massivecraft|com\.gamingmesh|com\.herocraftonline|com\.palmergames|com\.sucy|com\.sk89q|com\.gmail\.filoghost|be\.maximvdw|me\.clip\.placeholderapi|net\.ess3|com\.earth2me|org\.kingdoms|net\.sacredlabyrinth|com\.gmail\.nossr50|io\.lumine)\."),
}


def main() -> None:
    sources = sorted((ROOT / "src").rglob("*.java"))
    findings = defaultdict(list)
    for path in sources:
        relative = path.relative_to(ROOT).as_posix()
        content = path.read_text(encoding="utf-8", errors="replace")
        for name, regex in PATTERNS.items():
            matches = len(regex.findall(content))
            if matches:
                findings[name].append((relative, matches))

    print(f"## Static source inventory: {ROOT.name}")
    print(f"Inspected **{len(sources)} Java source files** (not only compiler's first 1000 errors).")
    print()
    print("| Area | Affected files | Matches |")
    print("|---|---:|---:|")
    for name, items in findings.items():
        print(f"| {name} | {len(items)} | {sum(count for _, count in items)} |")
    print()
    for name, items in findings.items():
        if not items:
            continue
        print(f"### {name}")
        for filename, count in sorted(items, key=lambda x: -x[1])[:8]:
            print(f"- `{filename}` — {count} hits")
    print()
    print("**Limits:** This is a source-wide heuristic scan. It does not prove correctness,")
    print("preservation of game mechanics, or compatibility with a Paper test server.")


if __name__ == "__main__":
    main()
