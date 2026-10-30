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

# AgarthaLib's original JsonUtil.generateJsonItem() is used by MyItems
# messages. Check the complete modern hover-data handoff, not merely that
# the server-side source compiles while packetGetJsonItem returns null.
lib_root = root / "agarthalib"
message_packet = (lib_root / "src/com/praya/agarthalib/packet/message/PacketMessage_Paper26_3.java").read_text("utf-8")
json_builder = (lib_root / "src/core/praya/agarthalib/builder/message/JsonBuild.java").read_text("utf-8")
hover_payload = (lib_root / "src/com/praya/agarthalib/modern/Paper26HoverItemPayload.java").read_text("utf-8")
if not re.search(
    r'packetGetJsonItem\s*\([^)]*\)\s*\{[^}]*return\s+Paper26HoverItemPayload\.encode\(',
    message_packet, re.S,
):
    errors.append("AgarthaLib: legacy item hover entry point is not connected to modern payload")
if 'return TextUtil.colorful(jsonObject.toString())' in json_builder:
    errors.append("AgarthaLib: global color conversion mutates item hover data components")
if 'colorizeLegacy(' not in json_builder:
    errors.append("AgarthaLib: historic message colors are not translated separately")
if 'mapHover.put("contents", contents)' not in json_builder:
    errors.append("AgarthaLib: legacy JSON builder does not emit show_item.contents")
if 'Paper26HoverItemPayload.contents(value)' not in json_builder:
    errors.append("AgarthaLib: show_item marker is not validated before attaching to message")
if "getStringList" in hover_payload:
    errors.append("AgarthaLib: item hover must not depend on legacy item NBT lists")
if not re.search(
    r'implementation\("com\.googlecode\.json-simple:json-simple:1\.1\.1"\)',
    agartha,
):
    errors.append("AgarthaLib: JSON-simple missing from runtime dependency graph")
if 'it.name.startsWith("json-simple-")' not in agartha:
    errors.append("AgarthaLib: standalone plugin jar does not package the JSON parser")
if '"testPaper26HoverPipeline"' not in agartha:
    errors.append("AgarthaLib: missing actual legacy JSON DSL regression task")

if errors:
    print("FAIL: Paper 26.3 integration dependency drift:")
    for error in errors:
        print(" -", error)
    sys.exit(1)

for name, value in required.items():
    print(f"PASS: both repos compile against {name}:{value}")
print(f"PASS: MyItems consumes AgarthaLib {linked_version}")
print("PASS: VaultAPI's obsolete Bukkit 1.13 transitives are blocked")
print("PASS: MyItems legacy item hover reaches validated Paper show_item.contents with packaged JSON runtime")
