#!/usr/bin/env python3
"""Validate real source signatures on the matching feature branches, no stubs."""
from pathlib import Path
root = Path(__file__).resolve().parents[2]
lib = root / "agarthalib"
provider = (lib / "src/com/praya/agarthalib/modern/Paper26OptionalProviders.java").read_text("utf-8")
bridge = (root / "src/com/praya/myitems/modern/Paper26MyItemsModernProviders.java").read_text("utf-8")
for method in ("auraSkillLevel", "mmoCoreLevel", "mmoCoreClassId",
               "landsClaimed", "createTransientHologram"):
    assert method + "(" in provider, "AgarthaLib ABI changed: " + method
for method in ("auraSkillLevel", "mmoCoreLevel", "mmoCoreClassId", "landsClaimed",
               "transientHologram"):
    assert method + "(" in bridge, "MyItems ABI changed: " + method
for provider in ("AuraSkills", "MMOCore", "Lands", "DecentHolograms"):
    assert "- " + provider in (lib / "resources/plugin.yml").read_text("utf-8")
    assert "- " + provider in (root / "resources/plugin.yml").read_text("utf-8")
assert not any((lib / "src/api/praya/agarthalib/builder/support" / ("Support"+s+".java")).exists()
               for s in ("FactionsLegacy", "FactionsUUID", "FactionsMassive"))
print("PASS: four optional provider ABI contracts and plugin.yml in both repositories")
