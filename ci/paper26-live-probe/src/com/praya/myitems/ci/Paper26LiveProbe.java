package com.praya.myitems.ci;

import com.praya.agarthalib.modern.Paper26LegacyItemTags;
import core.praya.agarthalib.bridge.unity.Bridge;
import net.kyori.adventure.nbt.*;
import org.bukkit.Material;
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
        } catch (Throwable error) {
            getLogger().severe("FAIL: live NBT/ability compatibility test: " + error);
            throw new IllegalStateException("Paper26 live NBT probe failed", error);
        }
    }
}
