# HEX colors for MyItems on Paper 26.3

When using **/itemname**, **/lore add**, **/lore set** or **/lore insert**,
MyItems resolves placeholders as before. AgarthaLib's existing `TextUtil.colorful`
and `EquipmentUtil` pipeline now understands RGB HEX alongside legacy formats.

Examples (hold your item in your main hand):

```text
/itemname &#FFAA00Golden Sword &lLegendary
/lore add &#55FF99Poison Damage: &f+10
/lore set 1 <#AABBCC>Defense: &a+7
/lore insert 2 &bOld Color <#FF3366>New Color
```

Supported: `&#RRGGBB`, `<#RRGGBB>`, `{#RRGGBB}`,
standalone `#RRGGBB`, and `&x&R&R&G&G&B&B` (six hexadecimal
characters). Legacy `&0..&9`, `&a..&f`, `&k..&o`, `&r` and existing
`§` colors remain supported. Incomplete or invalid HEX codes remain literal.

The model-independent edit pathway **does not** rewrite existing item
data, PDC/NBT, skills, IDs, enchantments, commands, config, or lore events.
Existing stored YAML and item names/lore are decoded using the same shared
color conversion helper, so no one-time migration is required.

Paper's text Component API is the long-term successor to deprecated
String setters. The original MyItems equipment helpers are intentionally
retained for compatibility and their cancellable lore-change event.
Tests run the exact helper routes on a live Paper 26.3 server and assert
the resulting Adventure ItemMeta plus a real ItemStack serialization
roundtrip.
