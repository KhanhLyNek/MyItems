package com.praya.myitems.utility.main;

import com.praya.agarthalib.utility.PlayerUtil;
import com.praya.myitems.config.plugin.MainConfig;
import org.bukkit.entity.Player;

/**
 * Refresh MyItems-owned maximum health through the surviving AgarthaLib
 * custom health event. Legacy CombatStamina/LifeEssence event APIs are
 * retired, and must not be imitated by unrelated MMOCore/AuraSkills stats.
 */
public final class TriggerSupportUtil {
    private TriggerSupportUtil() {}
    public static void updateSupport(final Player player) {
        if (player == null) return;
        if (MainConfig.getInstance().isStatsEnableMaxHealth()) {
            PlayerUtil.setMaxHealth(player);
        }
    }
}
