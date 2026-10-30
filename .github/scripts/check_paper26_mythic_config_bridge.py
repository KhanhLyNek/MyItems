#!/usr/bin/env python3
"""Verify the modern MythicMobs 5 bridge retained original MyItems grammar.

This verifies source parity for the old item/drop/equipment calculations; it
DOES NOT prove current MythicMobs event runtime compatibility.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[2]
legacy = root / "src/com/praya/myitems/listener/support"
modern = root / "src/com/praya/myitems/modern"

old_drop = (legacy / "ListenerMythicMobDeath.java").read_text("utf-8")
new_drop = (modern / "Paper26MythicDropHandler.java").read_text("utf-8")
prefix = "    private final void dropItems("
old_fn = old_drop[old_drop.index(prefix):old_drop.rfind("\n}")]
new_fn = new_drop[new_drop.index(prefix):new_drop.rfind("\n}")]
assert old_fn.strip() == new_fn.strip(), "Legacy MyItems drop chance/amount changed"

old_spawn = (legacy / "ListenerMythicMobSpawn.java").read_text("utf-8")
new_spawn = (modern / "Paper26MythicEquipmentHandler.java").read_text("utf-8")
original = old_spawn[old_spawn.index(
    "    public void eventMythicMobSpawn(final MythicMobSpawnEvent event) {"):
    old_spawn.rfind("\n}")]
original = original.replace(
    "public void eventMythicMobSpawn(final MythicMobSpawnEvent event)",
    "public void equipFromConfig(final Entity entity, final List<String> equipment)")
for line in (
    "        final MythicMob mythicMob = event.getMobType();\n",
    "        final Entity entity = event.getEntity();\n",
    "        final List<String> equipment = (List<String>)mythicMob.getEquipment();\n",
):
    assert line in original
    original = original.replace(line, "")
new_fn = new_spawn[new_spawn.index(
    "    public void equipFromConfig(final Entity entity, final List<String> equipment)"):
    new_spawn.rfind("\n}")]
assert original.strip() == new_fn.strip(), "Original equipment chance/generator logic changed"

bridge = (modern / "Paper26MythicMobsEventBridge.java").read_text("utf-8")
assert "io.lumine.mythic.bukkit.events.MythicMobSpawnEvent" in bridge
assert "io.lumine.mythic.bukkit.events.MythicMobDeathEvent" in bridge
assert 'deathAccess.configLines(event, "Drops")' in bridge
assert 'spawnAccess.configLines(event, "Equipment")' in bridge
assert 'Bukkit.isPrimaryThread()' in bridge
assert bridge.count('catch (ReflectiveOperationException | LinkageError | RuntimeException ex)') >= 3
assert "registerEvent" in bridge
assert "new Paper26MythicEventAccess(spawn)" in bridge
assert "new Paper26MythicEventAccess(death)" in bridge
assert "HandlerList.unregisterAll(listener)" in bridge
assert 'getMob.getReturnType().getMethod("getConfig")' in (
    modern / "Paper26MythicEventAccess.java").read_text("utf-8")
assert 'getConfig.getReturnType().getMethod("getStringList", String.class)' in (
    modern / "Paper26MythicEventAccess.java").read_text("utf-8")
assert "getDropTable()" not in bridge, "Named tables require a separate audited API"

boot = (root / "src/com/praya/myitems/MyItems.java").read_text("utf-8")
assert "Paper26MythicMobsEventBridge.register(this)" in boot
assert "new ListenerMythicMobDeath" not in boot
assert "new ListenerMythicMobSpawn" not in boot
print("PASS: exact old custom drop/equipment grammar retained; new MythicMobs 5 optional events registered")

# Dependency-disabled MyItems must not continue registering gameplay callbacks.
boot_body = boot.split("public void onEnable()", 1)[1].split(
    "private final void setPluginManager()", 1)[0]
assert boot_body.count("if (!isEnabled()) return;") >= 2
assert boot_body.index("getHookConfig().setup()") < boot_body.rindex(
    "if (!isEnabled()) return;") < boot_body.index("this.setup()")
