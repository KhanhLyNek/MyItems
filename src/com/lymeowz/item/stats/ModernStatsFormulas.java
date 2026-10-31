package com.lymeowz.item.stats;

/**
 * Tested numerical building blocks for future opt-in modern stat adapters.
 * IMPORTANT: No combat or attribute adapter invokes these formulas yet.
 */
public final class ModernStatsFormulas {
    private ModernStatsFormulas() { }

    public static double attackSpeed(double vanillaBase, double flatBonus) {
        if (!Double.isFinite(vanillaBase)) vanillaBase = 4.0;
        if (!Double.isFinite(flatBonus)) flatBonus = 0;
        return Math.max(0.1, vanillaBase + flatBonus);
    }

    public static double armorToughness(double vanillaBase, double flatBonus) {
        if (!Double.isFinite(vanillaBase)) vanillaBase = 0;
        if (!Double.isFinite(flatBonus)) flatBonus = 0;
        return Math.max(0.0, vanillaBase + flatBonus);
    }

    public static double lifeSteal(double finalDamage, double percentage, double missingHealth) {
        if (!Double.isFinite(finalDamage) || !Double.isFinite(missingHealth)) return 0;
        final double p = Double.isFinite(percentage) ? Math.max(0, Math.min(100, percentage)) : 0;
        return Math.min(Math.max(0, missingHealth), Math.max(0, finalDamage) * p / 100.0);
    }

    public static int cooldownTicks(int originalTicks, double reductionPercent) {
        final int ticks = Math.max(0, originalTicks);
        final double p = Double.isFinite(reductionPercent) ? Math.max(0, Math.min(100, reductionPercent)) : 0;
        return (int)Math.ceil(ticks * (1.0 - p / 100.0));
    }
}
