#!/usr/bin/env python3
"""Inspect MyItems' production JAR after linking new AgarthaLib master."""
from pathlib import Path
from zipfile import ZipFile
import sys

root = Path(__file__).resolve().parents[2]
candidates = [p for p in (root / "build/libs").glob("*.jar")
              if not p.name.endswith(("-sources.jar", "-javadoc.jar"))]
if len(candidates) != 1:
    sys.exit(f"Expected one MyItems binary JAR: {[p.name for p in candidates]}")
with ZipFile(candidates[0]) as z:
    names = set(z.namelist())
    manifest = z.read("plugin.yml").decode("utf-8")
    assert "name: LyMeowzItem\n" in manifest
    assert "- LyMeowzLib\n" in manifest
    assert "  lymeowzitem:\n" in manifest
    assert "  myitems:\n" not in manifest
    assert "    - myitem\n" in manifest
    required = (
        "plugin.yml",
        "com/praya/myitems/MyItems.class",
        "com/praya/myitems/builder/ability/weapon/AbilityWeaponConfuse.class",
        "com/praya/myitems/builder/ability/weapon/AbilityWeaponFreeze.class",
        "com/praya/myitems/listener/support/ListenerPlayerHealthMaxChange.class",
        "com/praya/myitems/modern/Paper26MyItemsModernProviders.class",
        "com/praya/myitems/modern/Paper26MyItemsMythicLibAdapter.class",
        "com/praya/myitems/modern/Paper26MythicMobsEventBridge.class",
        "com/praya/myitems/modern/Paper26MythicEventAccess.class",
        "org/json/simple/JSONObject.class",
    )
    for name in required:
        assert name in names, f"Missing required plugin class/runtime library: {name}"
    forbidden = (
        "ListenerPlayerLevelUp", "ListenerMythicMobDeath",
        "ListenerMythicMobSpawn", "ListenerPlayerStaminaRegenChange",
        "ListenerPlayerStaminaMaxChange", "ListenerPlayerHealthRegenChange",
    )
    for old in forbidden:
        assert f"com/praya/myitems/listener/support/{old}.class" not in names, old
    assert not any(n.startswith("net/minecraft/server/v1_") for n in names)
    print(f"PASS: {candidates[0].name}, {len(names)} entries; abilities kept, "
          "json-simple packaged, 6 obsolete optional event classes excluded")
