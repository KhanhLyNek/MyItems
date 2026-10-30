#!/usr/bin/env python3
"""Ability contract guards: 25 registered skills, safe lore parsing and Venom Spread duration.

The original 43-file baseline is deliberately not rewritten; the one
reviewed VenomSpread fix is pinned to an exact git blob in check_legacy_logic.
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[2]
skills = root / "src/com/praya/myitems/builder/ability/weapon"
register = (root / "src/com/praya/myitems/manager/register/RegisterAbilityWeaponManager.java").read_text("utf-8")
manager = (root / "src/com/praya/myitems/manager/game/AbilityWeaponManager.java").read_text("utf-8")
venom = (skills / "AbilityWeaponVenomSpread.java").read_text("utf-8")
files = sorted(skills.glob("AbilityWeapon*.java"))
assert len(files) == 25, f"Expected 25 skill classes, got {len(files)}"
for path in files:
    name = path.stem
    body = path.read_text("utf-8")
    assert f"class {name} " in body, name
    assert "void cast(final Entity caster, final Entity target" in body, name
    assert f"{name}.getInstance()" in register, name

assert register.count("this.register(abilityWeapon") == 25, "Some skill not registered"
assert 'if (grade > 0 && abilityWeapon != null)' in manager, "Unknown lore skill NPE"
# createPotion(PotionEffectType, duration, grade): real AgarthaLib signature.
correct = "PotionUtil.createPotion(potionType, duration, grade)"
wrong = "PotionUtil.createPotion(potionType, grade, duration)"
assert correct in venom, "Secondary Venom Spread target has wrong potion argument order"
assert wrong not in venom, "Wrong duration/grade potion order reintroduced"
assert "this.getEffectDuration(grade)" in venom, "Effect duration no longer from grade config"
print("PASS: 25 skill class signatures/registrations, safe unknown lore and Venom Spread duration")
