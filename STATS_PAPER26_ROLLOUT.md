# Modern Stats staging — Paper 26.3

Four NEW lore stat identifiers are recognized by `LoreStatsEnum` and can optionally be set in existing `Configuration.Stats`. The shipped legacy `resources/Configuration/config.yml` is intentionally kept byte-identical, as required by the repository's compatibility guard:
- `Attack Speed` (flat attribute points, e.g. 1.25)
- `Armor Toughness` (flat attribute points, e.g. 2)
- `Lifesteal` (% of damage, e.g. 10%)
- `Cooldown Reduction` (% of cooldown, e.g. 15%)

They can be formatted and parsed using the **existing** `LoreStatsManager` / `EquipmentUtil` APIs. Old YAML and all legacy stat names remain unchanged. If old server configs lack these keys, the getters use the names above.

**NOT enabled for gameplay:** This commit only adds lore/schema coverage and pure, regression-tested formulas. Do not represent these stats as granting live combat effects yet. Before activating Attack Speed and Armor Toughness, implement additive, uniquely-keyed Paper attribute modifiers and rigorous equip/swap/logout cleanup; avoid overwriting mods from other plugins. Lifesteal must use actual successful damage and must not heal on cancelled attacks, and CDR must hook the existing named cooldown producers safely. Activate separately with default-off per-feature toggles, validated live PvP/PvE tests and anti-duplication checks.

## Compatibility verified by the preexisting CI and additional probes
- Paper 26.3 real server lifecycle and NBT `minecraft:custom_data` read-only fallback
- Existing HEX/legacy name/lore, 25 ability registrations, and API-shaped EssentialsX / ItemsAdder / Oraxen fixtures
- New Stats lore encode/decode on actual serialized Paper `ItemStack`
- New formulas tested in an isolated Java test task

Not proven: live two-human PvP, proprietary plugins' actual release jars, or damage against real MythicMobs entities. Those require controlled server acceptance tests.

## Optional YAML labels for existing server config files

These are optional keys to add beneath your existing `Configuration.Stats` section.
No mechanic becomes active by adding the keys; these only change names.

```yaml
Identifier_Attack_Speed: "Attack Speed"
Identifier_Armor_Toughness: "Armor Toughness"
Identifier_Lifesteal: "Lifesteal"
Identifier_Cooldown_Reduction: "Cooldown Reduction"
```
