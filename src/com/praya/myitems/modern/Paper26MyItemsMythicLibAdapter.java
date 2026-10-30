package com.praya.myitems.modern;

import com.praya.agarthalib.modern.Paper26MythicLibStats;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

/**
 * Optional, read-only AgarthaLib/MyItems MythicLib integration boundary.
 * Does not replace the MyItems skill damage formulas or create stat modifiers.
 */
public final class Paper26MyItemsMythicLibAdapter {
    private Paper26MyItemsMythicLibAdapter() {}

    public static Double getStat(final OfflinePlayer player, final String statKey) {
        if (player == null || !player.isOnline() || statKey == null || statKey.isBlank()) {
            return null;
        }
        final Plugin mythicLib = Bukkit.getPluginManager().getPlugin("MythicLib");
        return mythicLib != null && mythicLib.isEnabled()
                ? Paper26MythicLibStats.read(mythicLib, player, statKey) : null;
    }
}
