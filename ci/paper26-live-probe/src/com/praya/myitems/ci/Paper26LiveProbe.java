package com.praya.myitems.ci;

import com.praya.agarthalib.modern.Paper26LegacyItemTags;
import core.praya.agarthalib.bridge.unity.Bridge;
import net.kyori.adventure.nbt.*;
import org.bukkit.Material;
import com.praya.agarthalib.utility.TextUtil;
import com.praya.agarthalib.utility.EquipmentUtil;
import com.praya.agarthalib.modern.Paper26ItemModels;
import com.praya.myitems.modern.Paper26ExternalModels;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import com.praya.agarthalib.AgarthaLib;
import api.praya.agarthalib.builder.support.SupportEssentials;
import api.praya.agarthalib.manager.plugin.SupportManagerAPI;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.entity.Zombie;
import org.bukkit.Bukkit;
import com.praya.myitems.MyItems;
import com.praya.myitems.manager.game.AbilityWeaponManager;
import com.praya.myitems.manager.register.RegisterAbilityWeaponManager;
import api.praya.myitems.builder.ability.AbilityWeapon;
import api.praya.myitems.builder.ability.AbilityItemWeapon;
import api.praya.myitems.builder.ability.AbilityWeaponAttributeBaseDamage;
import api.praya.myitems.builder.ability.AbilityWeaponAttributeCastDamage;
import api.praya.myitems.builder.ability.AbilityWeaponAttributeEffect;
import api.praya.myitems.builder.lorestats.LoreStatsEnum;
import api.praya.myitems.builder.lorestats.LoreStatsOption;
import com.praya.myitems.manager.game.LoreStatsManager;
import com.praya.myitems.modern.Paper26StatsMath;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Real Paper server fixture. Kept outside src/ and production jars.
 * Exercises byte serialization -> custom_data -> BridgeTagsNBT without
 * modifying any player's inventory or persistent world data.
 */
public final class Paper26LiveProbe extends JavaPlugin {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    /** Validate all 25 shipped class singletons against the running plugin,
     * configured damage scales, and exact existing lore encode/decode path.
     * Do not fire destructive combat in this startup smoke fixture.
     */
    private void verifyWeaponAbilities() throws Exception {
        final MyItems myitems = (MyItems) Bukkit.getPluginManager().getPlugin("MyItems");
        check(myitems != null && myitems.isEnabled(), "MyItems enabled for ability validation");
        final RegisterAbilityWeaponManager registered =
                myitems.getRegisterManager().getRegisterAbilityWeaponManager();
        final AbilityWeaponManager abilityManager =
                myitems.getGameManager().getAbilityWeaponManager();

        final String[] names = {
                "AirShock", "BadLuck", "Blind", "BubbleDeflector",
                "Cannibalism", "Confuse", "Curse", "DarkFlame", "DarkImpact",
                "Flame", "FlameWheel", "Freeze", "Harm", "Hungry",
                "Levitation", "Lightning", "Poison", "Roots", "Slow",
                "Tired", "Vampirism", "VenomBlast", "VenomSpread",
                "Weak", "Wither"
        };
        final Set<String> ids = new HashSet<>();
        check(registered.getAllAbilityWeapon().size() == names.length,
                "exactly 25 skills registered, not " + registered.getAllAbilityWeapon().size());
        for (String name : names) {
            final Class<?> type = Class.forName(
                    "com.praya.myitems.builder.ability.weapon.AbilityWeapon" + name);
            final AbilityWeapon ability = (AbilityWeapon) type.getMethod("getInstance")
                    .invoke(null);
            check(ability != null && ability.getPlugin() == myitems,
                    name + " singleton bound to active MyItems");
            final String id = ability.getID();
            check(id != null && !id.isBlank() && ids.add(id),
                    name + " has a unique nonempty ID");
            check(registered.getAbilityWeapon(id) == ability,
                    name + " is registered by its original ID");
            check(ability.getKeyLore() != null && !ability.getKeyLore().isBlank(),
                    name + " has configured lore alias");
            final int maxGrade = ability.getMaxGrade();
            check(maxGrade > 0, name + " has valid max grade");
            if (ability instanceof AbilityWeaponAttributeBaseDamage base) {
                check(Double.isFinite(base.getBaseBonusDamage(1))
                                && Double.isFinite(base.getBasePercentDamage(1)),
                        name + " base damage scales are finite");
            }
            if (ability instanceof AbilityWeaponAttributeCastDamage cast) {
                check(Double.isFinite(cast.getCastBonusDamage(1))
                                && Double.isFinite(cast.getCastPercentDamage(1)),
                        name + " cast damage scales are finite");
            }
            if (ability instanceof AbilityWeaponAttributeEffect effect) {
                check(effect.getEffectDuration(1) >= 0,
                        name + " effect duration must not be negative");
            }

            final String lore = abilityManager.getTextAbility(id, 1, 65.0);
            check(lore != null && !lore.isEmpty(),
                    name + " can render current lore");
            final AbilityItemWeapon decoded = abilityManager.getAbilityItemWeapon(lore);
            check(decoded != null && decoded.getAbility().equalsIgnoreCase(id)
                            && decoded.getGrade() == 1
                            && Math.abs(decoded.getChance() - 65.0) < 0.001,
                    name + " can decode its own lore without losing grade/chance");
        }
        getLogger().info("PASS: 25/25 weapon ability classes registered, configured and lore-roundtripped");
    }

    /** Execute real cast() implementations on disposable mobs on Paper's
     * main thread. This proves that mob-vs-mob skill effects cannot fail
     * solely because the attacker/target is not a CommandSender.
     */
    private void verifyMobSkillCasts() {
        final World world = Bukkit.getWorlds().get(0);
        final Location origin = world.getSpawnLocation().clone().add(0.5, 2.0, 0.5);
        final Zombie attacker = world.spawn(origin, Zombie.class);
        final Zombie victim = world.spawn(origin.clone().add(2.5, 0.0, 0.0), Zombie.class);
        attacker.setAI(false);
        victim.setAI(false);
        attacker.setSilent(true);
        victim.setSilent(true);
        attacker.setInvulnerable(true);
        victim.setInvulnerable(true);

        final RegisterAbilityWeaponManager registered =
                ((MyItems) Bukkit.getPluginManager().getPlugin("MyItems"))
                        .getRegisterManager().getRegisterAbilityWeaponManager();
        for (String id : new String[] {"Freeze", "Roots", "Curse"}) {
            final AbilityWeapon ability = registered.getAbilityWeapon(id);
            check(ability != null, "mob cast ability exists: " + id);
            try {
                ability.cast(attacker, victim, 1, 3.0);
            } catch (RuntimeException | LinkageError ex) {
                throw new AssertionError("Mob cast failed for " + id, ex);
            }
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (attacker.isValid()) attacker.remove();
            if (victim.isValid()) victim.remove();
        }, 100L);
        getLogger().info("PASS: 3/3 real mob-target skill casts (Freeze, Roots, Curse)");
    }

    /** Real Paper plugin classloader smoke, with an API-shaped test provider.
     * This does NOT claim the real EssentialsX 2.22.0 runs on Paper 26.3.
     */
    private void verifyEssentialsWarpProvider() {
        final AgarthaLib agartha = (AgarthaLib) Bukkit.getPluginManager().getPlugin("AgarthaLib");
        check(agartha != null && agartha.isEnabled(), "AgarthaLib ready for Essentials warp API");
        final SupportManagerAPI supportManager = new SupportManagerAPI(agartha);
        final SupportEssentials adapter = supportManager.getSupportEssentials();
        check(adapter != null, "EssentialsX named provider detected");
        check(adapter.getSource() == Bukkit.getPluginManager().getPlugin("Essentials"),
                "Live provider plugin instance resolved");
        check(adapter == supportManager.getSupportEssentials(), "Adapter cache retained while provider active");

        final World world = Bukkit.getWorlds().get(0);
        check(List.of("spawn", "market", "missing").equals(
                List.copyOf(adapter.getWarpNames())), "Modern IWarps collection preserved");
        check(List.of("spawn", "market").equals(
                List.copyOf(adapter.getWarpNames(world))), "Current world filtering preserved");
        check(adapter.getWarp("spawn") != null
                        && adapter.getWarp("spawn").getBlockX() == 13,
                "Correct position through the provider-owned IWarps interface");
        check(adapter.getWarp("missing") == null, "Missing warp still fails closed");
        check(adapter.getWarps().size() == 2, "Invalid warp not included in locations");
        check(adapter.getWarps(world).size() == 2, "World-filtered locations preserved");
        getLogger().info("PASS: EssentialsX 2.22 IWarps contract on real Paper plugin classloaders");
    }

    /** Real Paper runtime: optional provider APIs through isolated plugin
     * classloaders, model-only mutation and lossless serialized item state.
     */
    private void verifyItemModels() {
        final ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        item.setAmount(2);
        final NamespacedKey idKey = new NamespacedKey(this, "existing_item_id");
        final ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bMyItems test sword");
        meta.setLore(List.of("§7Damage: 42", "§5Ability: Vampirism I"));
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, "original:myitem");
        item.setItemMeta(meta);
        final ItemStack baseline = item.clone();

        check(Paper26ExternalModels.apply(item, "itemsadder", "missing:item")
                        == Paper26ExternalModels.Status.ID_NOT_FOUND,
                "ItemsAdder unknown registry entry rejected");
        check(item.equals(baseline), "unknown ItemsAdder model cannot mutate item");
        check(Paper26ExternalModels.apply(item, "itemsadder", "test:ruby_sword")
                        == Paper26ExternalModels.Status.APPLIED,
                "ItemsAdder CustomStack.getInstance and getItemStack resolved");
        check(NamespacedKey.fromString("itemsadder:ruby_sword")
                        .equals(item.getItemMeta().getItemModel()),
                "ItemsAdder item_model copied");

        check(Paper26ExternalModels.apply(item, "oraxen", "missing")
                        == Paper26ExternalModels.Status.ID_NOT_FOUND,
                "Oraxen unknown model rejected");
        check(Paper26ExternalModels.apply(item, "oraxen", "storm_blade")
                        == Paper26ExternalModels.Status.APPLIED,
                "Oraxen OraxenItems.getItemById and build resolved");
        check(NamespacedKey.fromString("oraxen:storm_blade")
                        .equals(item.getItemMeta().getItemModel()), "Oraxen item_model copied");
        check(Paper26ExternalModels.apply(item, "itemsadder", "test:legacy_sword")
                        == Paper26ExternalModels.Status.APPLIED,
                "Legacy CMD provider item copied via component API");
        check(item.getItemMeta().getItemModel() == null,
                "CMD selector clears stale modern item_model");
        check(item.getItemMeta().getCustomModelDataComponent().getFloats()
                        .equals(List.of(2026.0F)), "legacy float model data preserved");
        check(Paper26ItemModels.applyKey(item, "myitems:dragon_sword")
                        == Paper26ItemModels.Result.APPLIED,
                "direct Paper26 namespaced item model works");
        check(Paper26ItemModels.applyNumber(item, 6543)
                        == Paper26ItemModels.Result.APPLIED,
                "direct modern CMD component works");

        final ItemStack saved = ItemStack.deserializeBytes(item.serializeAsBytes());
        check(saved.getType() == baseline.getType(), "material unchanged");
        check(saved.getAmount() == baseline.getAmount(), "amount unchanged");
        check(saved.getItemMeta().getDisplayName()
                        .equals(baseline.getItemMeta().getDisplayName()), "name unchanged");
        check(saved.getItemMeta().getLore()
                        .equals(baseline.getItemMeta().getLore()), "lore/skills unchanged");
        check("original:myitem".equals(saved.getItemMeta().getPersistentDataContainer()
                        .get(idKey, PersistentDataType.STRING)), "existing PDC ID unchanged");
        check(saved.getItemMeta().getCustomModelDataComponent().getFloats()
                        .equals(List.of(6543.0F)), "model survived ItemStack serialization");

        check(Paper26ItemModels.clear(item) == Paper26ItemModels.Result.APPLIED,
                "clear visual selectors");
        check(item.getItemMeta().getItemModel() == null
                        && !item.getItemMeta().hasCustomModelDataComponent(),
                "clear must remove only model selectors");
        check("original:myitem".equals(item.getItemMeta().getPersistentDataContainer()
                        .get(idKey, PersistentDataType.STRING)), "clear retains PDC");
        check(item.getItemMeta().getLore().equals(baseline.getItemMeta().getLore()),
                "clear retains ability/stat lore");
        getLogger().info("PASS: ItemsAdder/Oraxen model-only bridges and ItemStack roundtrip");
    }

    /** On a real Paper server use the SAME legacy EquipmentUtil entrypoints
     * as /itemname, /loreadd, /loreset, /loreinsert. Verify that RGB input
     * roundtrips through current Adventure-backed Paper ItemMeta, while the
     * lore-change event path and existing skills/PDC remain untouched.
     */
    private void verifyHexItemColors() {
        final String red = "§x§f§f§3§3§6§6";
        final String blue = "§x§1§2§a§b§c§d";
        final String green = "§x§0§0§f§f§0§0";
        check((red + "Sword §lSoul").equals(
                TextUtil.colorful("&#FF3366Sword &lSoul")),
                "HEX and legacy formatting mix in original TextUtil.colorful");
        check(("§aOld " + blue + "New").equals(
                TextUtil.colorful("&aOld <#12ABCD>New")),
                "old colors and angle HEX can coexist");
        check("§cText".equals(TextUtil.colorful("&cText")),
                "original &c legacy parsing unchanged");
        check("#xyz000".equals(TextUtil.colorful("#xyz000")),
                "invalid hex remains visible unchanged");

        final ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        final NamespacedKey idKey = new NamespacedKey(this, "hex_original_item_id");
        final ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, "myitems:hex_sword");
        item.setItemMeta(meta);

        EquipmentUtil.setDisplayName(item, "&#FF3366Sword &lSoul");
        EquipmentUtil.addLore(item, "&7Damage: 42");
        EquipmentUtil.addLore(item, "<#12ABCD>Ability: Vampirism I");
        EquipmentUtil.setLore(item, 1, "&#00FF00&lDamage: 42");
        EquipmentUtil.insertLore(item, 2, "&eLegacy lore");
        final ItemMeta colored = item.getItemMeta();
        check((red + "Sword §lSoul").equals(EquipmentUtil.getDisplayName(item)),
                "HEX name written by existing EquipmentUtil");
        final List<String> lore = EquipmentUtil.getLores(item);
        check(lore != null && lore.size() == 3, "lore add/set/insert line indexes retained");
        check((green + "§lDamage: 42").equals(lore.get(0)),
                "HEX lore-set uses same existing event-aware entrypoint");
        check("§eLegacy lore".equals(lore.get(1)), "inserted legacy lore preserved");
        check((blue + "Ability: Vampirism I").equals(lore.get(2)),
                "HEX lore-add preserves ability text");
        check(colored.displayName() != null && colored.lore() != null
                && colored.lore().size() == 3, "Paper Adventure ItemMeta content populated");
        check("myitems:hex_sword".equals(
                colored.getPersistentDataContainer().get(idKey, PersistentDataType.STRING)),
                "existing item ID not mutated by coloring");

        // Simulates ItemConfig.loadConfig -> EquipmentUtil.colorful(gameItem).
        // It must not downgrade stored RGB through deprecated String getters.
        EquipmentUtil.colorful(item);
        check((red + "Sword §lSoul").equals(EquipmentUtil.getDisplayName(item)),
                "configuration reload retains original 24-bit display color");
        check(lore.equals(EquipmentUtil.getLores(item)),
                "configuration reload retains 24-bit lore and legacy formatting");

        final ItemStack reloaded = ItemStack.deserializeBytes(item.serializeAsBytes());
        final ItemMeta restored = reloaded.getItemMeta();
        check(EquipmentUtil.getDisplayName(item).equals(EquipmentUtil.getDisplayName(reloaded)),
                "HEX name survives actual Paper ItemStack NBT roundtrip");
        check(lore.equals(EquipmentUtil.getLores(reloaded)),
                "HEX, legacy, skill and stat lores survive NBT roundtrip");
        check("myitems:hex_sword".equals(
                restored.getPersistentDataContainer().get(idKey, PersistentDataType.STRING)),
                "original item ID survives NBT roundtrip");
        getLogger().info("PASS: MyItems HEX and legacy item-name/lore commands on real Paper 26.3");
    }


    /** Test the original stats manager on actual serialized ItemStacks. */
    private void verifyRealStatsLore() {
        final MyItems myitems = (MyItems) Bukkit.getPluginManager().getPlugin("MyItems");
        final LoreStatsManager stats = myitems.getGameManager().getStatsManager();
        final ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        EquipmentUtil.addLore(sword, stats.getTextLoreStats(LoreStatsEnum.DAMAGE, 10.0, 20.0));
        EquipmentUtil.addLore(sword, stats.getTextLoreStats(LoreStatsEnum.CRITICAL_CHANCE, 17.5));
        EquipmentUtil.addLore(sword, stats.getTextLoreStats(LoreStatsEnum.CRITICAL_DAMAGE, 1.5));
        EquipmentUtil.addLore(sword, stats.getTextLoreStats(LoreStatsEnum.DURABILITY, 8, 12));
        EquipmentUtil.addLore(sword, stats.getTextLoreStats(LoreStatsEnum.ATTACK_SPEED, 1.25));
        EquipmentUtil.addLore(sword, stats.getTextLoreStats(LoreStatsEnum.ARMOR_TOUGHNESS, 2));
        EquipmentUtil.addLore(sword, stats.getTextLoreStats(LoreStatsEnum.LIFESTEAL, 10));
        EquipmentUtil.addLore(sword, stats.getTextLoreStats(LoreStatsEnum.COOLDOWN_REDUCTION, 15));
        final ItemStack copy = ItemStack.deserializeBytes(sword.serializeAsBytes());
        getLogger().info("Stats diagnostic expected key=" +
                stats.getKeyStats(LoreStatsEnum.DAMAGE, true).replace('§', '~') +
                " before=" + EquipmentUtil.getLores(sword).toString().replace('§', '~') +
                " after=" + EquipmentUtil.getLores(copy).toString().replace('§', '~'));
        check(stats.hasLoreStats(copy, LoreStatsEnum.DAMAGE), "real serialized item Damage marker");
        check(stats.getLoreValue(copy, LoreStatsEnum.DAMAGE, LoreStatsOption.MIN) == 10.0, "Damage min");
        check(stats.getLoreValue(copy, LoreStatsEnum.DAMAGE, LoreStatsOption.MAX) == 20.0, "Damage max");
        check(stats.getLoreValue(copy, LoreStatsEnum.CRITICAL_CHANCE, null) == 17.5, "Critical chance");
        check(Math.abs(stats.getLoreValue(copy, LoreStatsEnum.CRITICAL_DAMAGE, null) - 0.5) < 0.001,
                "Critical multiplier");
        check(stats.getLoreValue(copy, LoreStatsEnum.DURABILITY, LoreStatsOption.CURRENT) == 8, "Durability current");
        check(stats.getLoreValue(copy, LoreStatsEnum.DURABILITY, LoreStatsOption.MAX) == 12, "Durability max");
        check(stats.getLoreStatsWeapon(copy).getDamage() >= 10.0, "standalone weapon stats");
        check(stats.getLoreValue(copy, LoreStatsEnum.ATTACK_SPEED, null) == 1.25, "modern Attack Speed lore");
        check(stats.getLoreValue(copy, LoreStatsEnum.ARMOR_TOUGHNESS, null) == 2, "modern Armor Toughness lore");
        check(stats.getLoreValue(copy, LoreStatsEnum.LIFESTEAL, null) == 10, "modern Lifesteal percentage lore");
        check(stats.getLoreValue(copy, LoreStatsEnum.COOLDOWN_REDUCTION, null) == 15, "modern CDR percentage lore");
        check(Paper26StatsMath.outgoingBonus(true, 10, 20) == 10, "PvP route");
        check(Paper26StatsMath.outgoingBonus(false, 10, 20) == 20, "PvE route");
        check(Paper26StatsMath.incomingDefenseBonus(true, 10, 20) == 10, "PvP defense route");
        check(Paper26StatsMath.incomingDefenseBonus(false, 10, 20) == 20, "PvE defense route");
        getLogger().info("PASS: Paper Stats item lore NBT and PvP/PvE branch contracts");
    }

    /** Disposable mob-vs-mob live damage hook; client PvP requires separate players. */
    private void verifyRealMobStatsCombat() {
        final MyItems myitems = (MyItems) Bukkit.getPluginManager().getPlugin("MyItems");
        final LoreStatsManager stats = myitems.getGameManager().getStatsManager();
        final World world = Bukkit.getWorlds().get(0);
        final Location pos = world.getSpawnLocation().clone().add(5.5, 3, 5.5);
        final Zombie a = world.spawn(pos, Zombie.class);
        final Zombie b = world.spawn(pos.clone().add(3, 0, 0), Zombie.class);
        try {
            a.setAI(false);
            b.setAI(false);
            a.setSilent(true);
            b.setSilent(true);
            check(a.getEquipment() != null && b.getEquipment() != null, "mob equipment");
            final ItemStack weapon = new ItemStack(Material.IRON_SWORD);
            EquipmentUtil.addLore(weapon, stats.getTextLoreStats(LoreStatsEnum.DAMAGE, 5));
            a.getEquipment().setItemInMainHand(weapon);
            final ItemStack armor = new ItemStack(Material.IRON_CHESTPLATE);
            EquipmentUtil.addLore(armor, stats.getTextLoreStats(LoreStatsEnum.DEFENSE, 2));
            b.getEquipment().setChestplate(armor);
            check(stats.getLoreStatsWeapon(a).getDamage() >= 5, "equipped mob damage");
            check(stats.getLoreStatsArmor(b).getDefense() >= 2, "equipped mob defense");
            final double before = b.getHealth();
            b.damage(2, a);
            final double after = b.getHealth();
            check(Double.isFinite(after) && after >= 0 && after <= before, "finite live mob combat health");
            getLogger().info("PASS: Paper live mob-vs-mob stats damage event");
        } finally {
            if (a.isValid()) a.remove();
            if (b.isValid()) b.remove();
        }
    }

    @Override
    public void onEnable() {
        try {
            ItemStack original = new ItemStack(Material.DIAMOND_SWORD);
            final int originalAmount = original.getAmount();
            final byte[] bytes = original.serializeAsBytes();
            final CompoundBinaryTag root = BinaryTagIO.reader(4 * 1024 * 1024L)
                    .read(new ByteArrayInputStream(bytes), BinaryTagIO.Compression.GZIP);

            final CompoundBinaryTag oldTags = CompoundBinaryTag.builder()
                    .putString("LegacyID", "Kiếm:&a|old")
                    .putInt("Level", 14)
                    .put("Stats12", ListBinaryTag.builder(BinaryTagTypes.STRING)
                            .add(StringBinaryTag.stringBinaryTag("Speed:2"))
                            .add(StringBinaryTag.stringBinaryTag("Damage:5"))
                            .build())
                    .put("Custom", CompoundBinaryTag.builder()
                            .put("Stats15", ListBinaryTag.builder(BinaryTagTypes.COMPOUND)
                                    .add(CompoundBinaryTag.builder()
                                            .putString("stat", "Armor:4").build())
                                    .build())
                            .build())
                    .build();
            final CompoundBinaryTag originalComponents = root.getCompound("components");
            final CompoundBinaryTag.Builder components = CompoundBinaryTag.builder();
            for (String key : originalComponents.keySet())
                components.put(key, originalComponents.get(key));
            components.put("minecraft:custom_data", oldTags);

            final CompoundBinaryTag.Builder rebuilt = CompoundBinaryTag.builder();
            for (String key : root.keySet()) {
                if (!"components".equals(key)) rebuilt.put(key, root.get(key));
            }
            rebuilt.put("components", components.build());
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            BinaryTagIO.writer().write(rebuilt.build(), output, BinaryTagIO.Compression.GZIP);

            final ItemStack restored = ItemStack.deserializeBytes(output.toByteArray());
            check(restored.getType() == Material.DIAMOND_SWORD, "material preserved");
            check(restored.getAmount() == originalAmount, "stack amount preserved");
            check("Kiếm:&a|old".equals(
                    Bridge.getBridgeTagsNBT().getString("LegacyID", restored)),
                    "legacy string read through actual BridgeTagsNBT");
            check(Bridge.getBridgeTagsNBT().getInt("Level", restored) == 14,
                    "historical integer read through BridgeTagsNBT");
            check(List.of("Speed:2","Damage:5").equals(
                    Bridge.getBridgeTagsNBT().getListString("Stats12", restored)),
                    "Minecraft 1.12 List<String> survives");
            check(List.of("Armor:4").equals(
                    Bridge.getBridgeTagsNBT().getListString("Stats15", "Custom", restored)),
                    "Minecraft 1.15 List<Compound{stat}> survives");
            check(Paper26LegacyItemTags.customData(restored) != null, "old data observable");

            Bridge.getBridgeTagsNBT().setString("LegacyID", restored, "modern");
            check("modern".equals(Bridge.getBridgeTagsNBT().getString("LegacyID", restored)),
                    "new PDC has precedence over old raw NBT");
            Bridge.getBridgeTagsNBT().remove("LegacyID", restored);
            check(Bridge.getBridgeTagsNBT().getString("LegacyID", restored) == null,
                    "PDC tombstone prevents resurrecting intentionally removed NBT");
            check(!"modern".equals(Bridge.getBridgeTagsNBT().getString("LegacyID", original)),
                    "original untouched item does not inherit migrated metadata");

            getLogger().info("PASS: live legacy NBT read-only fallback and PDC precedence");
            verifyWeaponAbilities();
            verifyMobSkillCasts();
            verifyEssentialsWarpProvider();
            verifyItemModels();
            verifyHexItemColors();
            verifyRealStatsLore();
            verifyRealMobStatsCombat();
        } catch (Throwable error) {
            getLogger().severe("FAIL: live NBT/ability compatibility test: " + error);
            throw new IllegalStateException("Paper26 live NBT probe failed", error);
        }
    }
}
