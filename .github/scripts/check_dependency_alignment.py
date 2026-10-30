#!/usr/bin/env python3
"""Keep the connected Paper 26.3 repositories on compatible public APIs.

This audits build coordinates; it neither installs external plugins nor
pretends the still-failing full legacy/NMS build is successful.
"""
from __future__ import annotations

from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[2]
myitems = (root / "build.gradle.kts").read_text(encoding="utf-8")
agartha_file = root / "agarthalib" / "build.gradle.kts"
if not agartha_file.is_file():
    sys.exit("FAIL: checkout AgarthaLib into agarthalib/ before auditing APIs")
agartha = agartha_file.read_text(encoding="utf-8")

required = {
    "io.papermc.paper:paper-api": "26.3.build.230-stable",
    "me.clip:placeholderapi": "2.12.3",
    "com.github.MilkBowl:VaultAPI": "1.7",
}

def coordinates(text: str) -> dict[str, str]:
    output: dict[str, str] = {}
    for group, artifact, version in re.findall(
        r'compileOnly\("([^:\"]+):([^:\"]+):([^\"]+)"\)', text
    ):
        output[f"{group}:{artifact}"] = version
    return output

errors: list[str] = []
for label, data in [("AgarthaLib", agartha), ("MyItems", myitems)]:
    coords = coordinates(data)
    for artifact, expected in required.items():
        actual = coords.get(artifact)
        if actual != expected:
            errors.append(f"{label}: {artifact} should be {expected}, got {actual}")
    if "compileOnly(\"org.bukkit:bukkit:" in data:
        errors.append(f"{label}: no legacy Bukkit dependency may conflict with Paper")
    if not re.search(
        r'compileOnly\("com\.github\.MilkBowl:VaultAPI:1\.7"\)\s*\{'
        r'\s*isTransitive\s*=\s*false', data
    ):
        errors.append(f"{label}: VaultAPI must not pull Bukkit 1.13 transitively")

lib_version = re.search(r'^version\s*=\s*"([^"]+)"', agartha, re.M)
linked_version = coordinates(myitems).get("com.praya:AgarthaLib")
if lib_version is None or linked_version != lib_version.group(1):
    errors.append(
        f"MyItems AgarthaLib dependency {linked_version} does not match library version "
        f"{lib_version.group(1) if lib_version else '<missing>'}"
    )

if errors:
    print("FAIL: Paper 26.3 integration dependency drift:")
    for error in errors:
        print(" -", error)
    sys.exit(1)

for name, value in required.items():
    print(f"PASS: both repos compile against {name}:{value}")
print(f"PASS: MyItems consumes AgarthaLib {linked_version}")
print("PASS: VaultAPI's obsolete Bukkit 1.13 transitives are blocked")
