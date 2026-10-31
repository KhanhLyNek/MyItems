package com.praya.myitems.modern;

/** @deprecated Use com.lymeowz.item.stats.ModernStatsFormulas. */
@Deprecated
public final class Paper26ModernStatsFormulas {
    private Paper26ModernStatsFormulas() { }

    public static double attackSpeed(double base, double bonus) {
        return com.lymeowz.item.stats.ModernStatsFormulas.attackSpeed(base, bonus);
    }
    public static double armorToughness(double base, double bonus) {
        return com.lymeowz.item.stats.ModernStatsFormulas.armorToughness(base, bonus);
    }
    public static double lifeSteal(double damage, double percent, double missingHealth) {
        return com.lymeowz.item.stats.ModernStatsFormulas.lifeSteal(damage, percent, missingHealth);
    }
    public static int cooldownTicks(int ticks, double percent) {
        return com.lymeowz.item.stats.ModernStatsFormulas.cooldownTicks(ticks, percent);
    }
}
