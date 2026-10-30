package com.praya.myitems.modern;

import com.praya.myitems.MyItems;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MythicMobs 5 Bukkit events, loaded only if the actual provider is enabled.
 * No MythicMobs 4 typed dependency in MyItems.
 *
 * Reads the user's existing MythicMobs mob Drops/Equipment YAML lists through
 * MythicMob.getConfig().getStringList(key), matching the old MyItems grammar.
 * This is not equivalent to the MythicMobs 5 DropTable/LootBag pipeline:
 * ordinary and named table drops are owned by MythicMobs and not duplicated.
 */
public final class Paper26MythicMobsEventBridge {
    private Paper26MythicMobsEventBridge() {}

    @SuppressWarnings("unchecked")
    public static boolean register(final MyItems plugin) {
        final Plugin provider = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (plugin == null || !plugin.isEnabled() || provider == null || !provider.isEnabled())
            return false;

        try {
            final ClassLoader loader = provider.getClass().getClassLoader();
            final Class<? extends Event> spawn = (Class<? extends Event>) Class
                    .forName("io.lumine.mythic.bukkit.events.MythicMobSpawnEvent", false, loader)
                    .asSubclass(Event.class);
            final Class<? extends Event> death = (Class<? extends Event>) Class
                    .forName("io.lumine.mythic.bukkit.events.MythicMobDeathEvent", false, loader)
                    .asSubclass(Event.class);

            final Paper26MythicEquipmentHandler equipment =
                    new Paper26MythicEquipmentHandler(plugin);
            final Paper26MythicDropHandler drops =
                    new Paper26MythicDropHandler(plugin);
            final Listener listener = new Listener() {};
            final AtomicBoolean warnedSpawn = new AtomicBoolean();
            final AtomicBoolean warnedDeath = new AtomicBoolean();

            final EventExecutor onSpawn = (ignored, event) -> {
                try {
                    final Entity entity = (Entity) event.getClass().getMethod("getEntity").invoke(event);
                    if (entity != null) {
                        final List<String> lines = configLines(event, "Equipment");
                        equipment.equipFromConfig(entity, lines);
                    }
                } catch (ReflectiveOperationException | LinkageError | ClassCastException ex) {
                    warnOnce(plugin, "spawn equipment", ex, warnedSpawn);
                }
            };
            final EventExecutor onDeath = (ignored, event) -> {
                try {
                    final Entity entity = (Entity) event.getClass().getMethod("getEntity").invoke(event);
                    if (entity != null) {
                        final Location location = entity.getLocation();
                        final List<String> lines = configLines(event, "Drops");
                        drops.dropFromConfig(lines, location);
                    }
                } catch (ReflectiveOperationException | LinkageError | ClassCastException ex) {
                    warnOnce(plugin, "mob custom drops", ex, warnedDeath);
                }
            };
            Bukkit.getPluginManager().registerEvent(spawn, listener, EventPriority.NORMAL,
                    onSpawn, plugin, true);
            Bukkit.getPluginManager().registerEvent(death, listener, EventPriority.NORMAL,
                    onDeath, plugin, true);
            plugin.getLogger().info("MythicMobs 5 config-backed MyItems custom equipment/drops enabled");
            return true;
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException ex) {
            plugin.getLogger().warning("Cannot activate modern MythicMobs events: " + ex);
            return false;
        }
    }

    /** Read only the raw user-configured strings; no reflection-based mutation. */
    private static List<String> configLines(final Event event, final String key)
            throws ReflectiveOperationException {
        final Object mob = event.getClass().getMethod("getMobType").invoke(event);
        if (mob == null) return List.of();
        final Object cfg = mob.getClass().getMethod("getConfig").invoke(mob);
        if (cfg == null) return List.of();
        final Object result = cfg.getClass().getMethod("getStringList", String.class).invoke(cfg, key);
        if (!(result instanceof List<?> list)) return List.of();
        final List<String> lines = new ArrayList<>();
        for (final Object line : list) {
            if (line instanceof String text) lines.add(text);
        }
        return lines;
    }

    private static void warnOnce(final MyItems plugin, final String action,
                                 final Throwable ex, final AtomicBoolean warned) {
        if (warned.compareAndSet(false, true)) {
            plugin.getLogger().warning("MythicMobs 5 " + action
                    + " reflection failed. Custom MyItems integration remains partial: " + ex);
        }
    }
}
