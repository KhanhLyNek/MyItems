package com.praya.myitems.modern;

import io.lumine.mythic.api.config.MythicConfig;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compile-verified, read-only MythicMobs 5.x event seam.
 *
 * <p>Do not register this as a listener or re-enable retired spawn/death
 * listeners until MythicMobs' loot tables and MyItems' exact drop behavior
 * have matching live tests. In particular, DropTable is NOT List<String>.
 * No item, world, drop or entity state is modified here.
 */
public final class Paper26MythicMobsReader {
    private Paper26MythicMobsReader() {
    }

    public static List<String> equipmentLines(final MythicMobSpawnEvent event) {
        if (event == null || event.isCancelled()) {
            return Collections.emptyList();
        }
        return configLines(event.getMobType(), "Equipment");
    }

    public static List<String> dropLines(final MythicMobDeathEvent event) {
        return event == null ? Collections.emptyList()
                : configLines(event.getMobType(), "Drops");
    }

    private static List<String> configLines(final MythicMob mob, final String path) {
        if (mob == null) {
            return Collections.emptyList();
        }
        final MythicConfig config = mob.getConfig();
        if (config == null) {
            return Collections.emptyList();
        }
        final List<String> lines = config.getStringList(path);
        return lines == null || lines.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(lines));
    }
}
