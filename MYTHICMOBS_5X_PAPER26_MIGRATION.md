# MythicMobs 5.x ↔ MyItems Paper 26.3 compatibility

Status: **read-only API seam in isolated compilation; not enabled for gameplay**.

## Verified public contracts

- Official API instructions provide the Lumine Maven repository and `io.lumine:Mythic-Dist:5.12.1` as a **compileOnly** example.
- New Bukkit event classes use `io.lumine.mythic.bukkit.events.MythicMobSpawnEvent` and `MythicMobDeathEvent`, not the retired `io.lumine.xikage.mythicmobs.*` packages.
- `MythicMob#getConfig()` exposes `MythicConfig#getStringList("Equipment")` and `getStringList("Drops")` for raw historical line auditing. These are **not** the actual generated loot or equipment.
- A named `DropTable` is an object that generates a `LootBag`, **not a `List<String>`**. The original MyItems one-token drop-table cast is invalid; do not silently replace it with an empty list or auto-generate drops, because that could duplicate or lose rewards.
- MythicMobs 5.13.0 was publicly listed for Minecraft versions up to 26.2, not confirmed on Paper 26.3. Passing Java compilation against 5.12.1 does **not** verify 5.13.0 server compatibility.

## Migration gates before enabling the listener

1. Capture representative MythicMobs mob YAML for direct `MyItems` equipment and drop lines, nested named drop tables, ranged amounts, probabilities, equipment slot aliases and generated/ItemSet items.
2. Resolve named tables through MythicMobs' current drop APIs; validate generated loot behavior and ensure MyItems never double-drops its items.
3. Run server tests with the precise MythicMobs artifact **supporting** the target Paper release, including spawn cancellation, death location, inventory slots and chunk unload/respawn.
4. Remove the old listener exclusions and register only the verified modern listener, preserving legacy YAML/ability logic and versioned optional-plugin detection.

The original listeners are retained in Git for behavior comparison; none are re-enabled in this change.

References:
- https://wiki.mythiccraft.io/mythicmobs/API
- https://www.mythiccraft.io/javadocs/mythic/
- https://hangar.papermc.io/Lumine/MythicMobs/versions
- https://modrinth.com/plugin/mythicmobs/versions
