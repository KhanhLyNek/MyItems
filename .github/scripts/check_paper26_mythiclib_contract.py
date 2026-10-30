#!/usr/bin/env python3
"""Cross-repository source contract for optional MythicLib, no Factions APIs."""
from pathlib import Path
root = Path(__file__).resolve().parents[2]
lib = root / "agarthalib"
assert lib.is_dir(), "AgarthaLib dependency checkout is missing"
my = (root / "src/com/praya/myitems/modern/Paper26MyItemsMythicLibAdapter.java").read_text(encoding="utf-8")
adapter = (lib / "src/com/praya/agarthalib/modern/Paper26MythicLibStats.java").read_text(encoding="utf-8")
manager = (lib / "src/api/praya/agarthalib/manager/plugin/SupportManagerAPI.java").read_text(encoding="utf-8")
assert "Paper26MythicLibStats.read(mythicLib, player, statKey)" in my
assert "public static Double read(final Plugin provider, final OfflinePlayer player, final String stat)" in adapter
assert 'PluginUtil.getPlugin("MythicLib")' in manager
for obsolete in ("SupportFactionsUUID.java", "SupportFactionsMassive.java", "SupportFactionsLegacy.java"):
    assert not (lib / "src/api/praya/agarthalib/builder/support" / obsolete).exists(), obsolete
assert "- MythicLib" in (root / "resources/plugin.yml").read_text(encoding="utf-8")
assert "- MythicLib" in (lib / "resources/plugin.yml").read_text(encoding="utf-8")
print("PASS: MyItems and AgarthaLib MythicLib ABI alignment; Factions providers absent")
