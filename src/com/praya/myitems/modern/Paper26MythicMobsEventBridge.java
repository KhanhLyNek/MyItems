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

import java.util.List;
import org.bukkit.event.HandlerList;
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

        final Listener listener = new Listener() {};
        try {
            final ClassLoader loader = provider.getClass().getClassLoader();
            final Class<? extends Event> spawn = (Class<? extends Event>) Class
                    .forName("io.lumine.mythic.bukkit.events.MythicMobSpawnEvent", false, loader)
                    .asSubclass(Event.class);
            final Class<? extends Event> death = (Class<? extends Event>) Class
                    .forName("io.lumine.mythic.bukkit.events.MythicMobDeathEvent", false, loader)
                    .asSubclass(Event.class);

            final Paper26MythicEventAccess spawnAccess =
                    new Paper26MythicEventAccess(spawn);
            final Paper26MythicEventAccess deathAccess =
                    new Paper26MythicEventAccess(death);
            if (!Entity.class.isAssignableFrom(spawnAccess.entityType())
                    || !Entity.class.isAssignableFrom(deathAccess.entityType())) {
                plugin.getLogger().warning("MythicMobs event getEntity() is not a Bukkit Entity");
                return false;
            }
            final Paper26MythicEquipmentHandler equipment =
                    new Paper26MythicEquipmentHandler(plugin);
            final Paper26MythicDropHandler drops =
                    new Paper26MythicDropHandler(plugin);
            final AtomicBoolean warnedSpawn = new AtomicBoolean();
            final AtomicBoolean warnedDeath = new AtomicBoolean();

            final EventExecutor onSpawn = (ignored, event) -> {
                try {
                    final Entity entity = (Entity) spawnAccess.entity(event);
                    if (entity != null) {
                        final List<String> lines = spawnAccess.configLines(event, "Equipment");
                        equipment.equipFromConfig(entity, lines);
                    }
                } catch (ReflectiveOperationException | LinkageError | ClassCastException ex) {
                    warnOnce(plugin, "spawn equipment", ex, warnedSpawn);
                }
            };
            final EventExecutor onDeath = (ignored, event) -> {
                try {
                    final Entity entity = (Entity) deathAccess.entity(event);
                    if (entity != null) {
                        final Location location = entity.getLocation();
                        final List<String> lines = deathAccess.configLines(event, "Drops");
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
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            // A failure registering the second event must not leave the first
            // event active with only half of the custom-item integration.
            HandlerList.unregisterAll(listener);
            plugin.getLogger().warning("Cannot activate modern MythicMobs events: " + ex);
            return false;
        }
    }

    private static void warnOnce(final MyItems plugin, final String action,
                                 final Throwable ex, final AtomicBoolean warned) {
        if (warned.compareAndSet(false, true)) {
            plugin.getLogger().warning("MythicMobs 5 " + action
                    + " reflection failed. Custom MyItems integration remains partial: " + ex);
        }
    }
}
