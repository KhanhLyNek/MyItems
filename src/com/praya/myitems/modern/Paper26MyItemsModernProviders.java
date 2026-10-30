package com.praya.myitems.modern;

import com.praya.agarthalib.modern.Paper26OptionalProviders;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import java.util.List;

/**
 * Optional provider seam. The historic MyItems skill, combat and item
 * calculators are intentionally NOT redirected here automatically.
 */
public final class Paper26MyItemsModernProviders {
    private Paper26MyItemsModernProviders() {}

    public static Integer auraSkillLevel(final OfflinePlayer player, final String skill) {
        return Paper26OptionalProviders.auraSkillLevel(player, skill);
    }
    public static Integer mmoCoreLevel(final OfflinePlayer player) {
        return Paper26OptionalProviders.mmoCoreLevel(player);
    }
    public static String mmoCoreClassId(final OfflinePlayer player) {
        return Paper26OptionalProviders.mmoCoreClassId(player);
    }
    public static Boolean landsClaimed(final Plugin owner, final Location at) {
        return Paper26OptionalProviders.landsClaimed(owner, at);
    }
    public static boolean transientHologram(final String id, final Location at, final List<String> lines) {
        return Paper26OptionalProviders.createTransientHologram(id, at, lines);
    }
}
