#!/usr/bin/env python3
"""Fail CI if the Paper 26 migration changes existing MyItems public contracts.

This intentionally checks the source-controlled YAML defaults rather than
rewriting users' YAML files. New commands/skills/settings remain allowed.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASELINE = json.loads((ROOT / ".github/compatibility-baseline.json").read_text(encoding="utf-8"))


def plugin_contract(text: str) -> tuple[dict[str, list[str]], list[str]]:
    commands: dict[str, list[str]] = {}
    permission_children: list[str] = []
    section = None
    current = None
    in_aliases = False
    for line in text.splitlines():
        heading = re.fullmatch(r"([^\s:#]+):\s*", line)
        if heading:
            section = heading.group(1)
            current = None
            in_aliases = False
            continue
        if section == "commands":
            command = re.fullmatch(r"  ([\w-]+):\s*", line)
            if command:
                current = command.group(1)
                commands[current] = []
                in_aliases = False
                continue
            if re.match(r"    aliases:\s*", line):
                in_aliases = True
                continue
            if in_aliases and current and re.match(r"    -\s*", line):
                commands[current].append(re.sub(r"^\s*-\s*", "", line).strip())
                continue
            if re.match(r"    [a-zA-Z]", line):
                in_aliases = False
        if section == "permissions":
            permission = re.fullmatch(r"      ([\w.-]+):\s*true\s*", line)
            if permission:
                permission_children.append(permission.group(1))
    return commands, permission_children


def ability_contract(text: str) -> dict[str, dict[str, str]]:
    abilities: dict[str, dict[str, str]] = {}
    current = None
    for line in text.splitlines():
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        item = re.fullmatch(r"([A-Za-z_]\w*):\s*(?:#.*)?", line)
        if item:
            current = item.group(1)
            abilities[current] = {}
            continue
        setting = re.fullmatch(r"  ([A-Za-z_]\w*):\s*(.*?)\s*", line)
        if current and setting:
            abilities[current][setting.group(1)] = setting.group(2).split(" #", 1)[0].strip()
    return abilities


def check() -> int:
    plugin_yaml = (ROOT / "resources/plugin.yml").read_text(encoding="utf-8")
    ability_yaml = (ROOT / "resources/Configuration/ability_weapon.yml").read_text(encoding="utf-8")
    subcommands_yaml = (ROOT / "resources/Resources/command.yml").read_text(encoding="utf-8")
    commands, permissions = plugin_contract(plugin_yaml)
    abilities = ability_contract(ability_yaml)
    subcommands = set(re.findall(r"^  ([A-Za-z]\w*):\s*(?:#.*)?$", subcommands_yaml, re.M))
    problems = []

    for name, saved_aliases in BASELINE["pluginCommands"].items():
        if name not in commands:
            problems.append(f"Deleted public command /{name}")
        else:
            removed = set(saved_aliases) - set(commands[name])
            if removed:
                problems.append(f"/{name}: removed aliases {sorted(removed)}")

    for permission in BASELINE["permissionChildren"]:
        if permission not in permissions:
            problems.append(f"Removed permission: {permission}")

    for skill, settings in BASELINE["abilityDefaults"].items():
        if skill not in abilities:
            problems.append(f"Deleted legacy ability ID: {skill}")
            continue
        for key, expected in settings.items():
            actual = abilities[skill].get(key)
            if actual != expected:
                problems.append(f"{skill}.{key}: was {expected!r}, now {actual!r}")

    for key in BASELINE["subcommandIds"]:
        if key not in subcommands:
            problems.append(f"Removed legacy subcommand ID: {key}")

    venom = (ROOT / "src/com/praya/myitems/builder/ability/weapon/AbilityWeaponVenomBlast.java").read_text(encoding="utf-8")
    if '"Venom_Blast"' not in venom:
        problems.append("Venom_Blast source identifier disappeared")

    if problems:
        print(f"FAILED: {len(problems)} MyItems compatibility change(s) detected:")
        for item in problems:
            print(" -", item)
        print("Review each intentional change against the migration baseline; do not silently update the baseline.")
        return 1

    print(f"PASS: {len(BASELINE['pluginCommands'])} existing commands and their aliases preserved")
    print(f"PASS: {len(BASELINE['abilityDefaults'])} abilities and their configured values preserved")
    print(f"PASS: {len(BASELINE['subcommandIds'])} legacy subcommand IDs preserved")
    print(f"PASS: {len(BASELINE['permissionChildren'])} permissions preserved")
    print("Additional optional commands/abilities/configuration remain permitted.")
    return 0


if __name__ == "__main__":
    sys.exit(check())
