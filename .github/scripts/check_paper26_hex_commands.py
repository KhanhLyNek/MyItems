#!/usr/bin/env python3
"""Guard RGB markup support without changing legacy name/lore permissions or events."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
src = ROOT / "src/com/praya/myitems/command"
commands = {
    "CommandItemName.java": "EquipmentUtil.setDisplayName(item, name)",
    "CommandLoreAdd.java": "EquipmentUtil.addLore(item, lore)",
    "CommandLoreSet.java": "EquipmentUtil.setLore(item, line, lore)",
    "CommandLoreInsert.java": "EquipmentUtil.insertLore(item, line, lore)",
}
for file, expected in commands.items():
    content = (src / file).read_text(encoding="utf-8")
    assert expected in content, f"Original MyItems edit command broken: {file}"
    assert "TextUtil.hookPlaceholderAPI" in content, f"Placeholder support changed: {file}"
assert "ItemLoresChangeEvent" in (
    ROOT / "agarthalib/src/com/praya/agarthalib/utility/EquipmentUtil.java"
).read_text("utf-8"), "Existing cancellable lore-change event must be retained"
text = (
    ROOT / "agarthalib/src/com/praya/agarthalib/utility/TextUtil.java"
).read_text("utf-8")
assert "Paper26HexColors.translate(text)" in text, "AgarthaLib HEX translator not connected"
assert "ChatColor.translateAlternateColorCodes('&'" in text, "Legacy color decoder removed"
hex_parser = (
    ROOT / "agarthalib/src/com/praya/agarthalib/modern/Paper26HexColors.java"
).read_text("utf-8")
for marker in ("AMP_X", "replaceRgb", "toLegacyHex"):
    assert marker in hex_parser, "Missing HEX grammar support: " + marker
print("PASS: four original MyItems name/lore routes and legacy cancellable lore event preserved")
