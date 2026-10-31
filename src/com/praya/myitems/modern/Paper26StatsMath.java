package com.praya.myitems.modern;

/** Isolated finite-number guards for the legacy MyItems combat and leveling formulas. */
public final class Paper26StatsMath {
    private Paper26StatsMath() { }

    public static double chance(double value) {
        if (Double.isNaN(value)) return 0.0;
        return Math.max(0.0, Math.min(100.0, value));
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    /** Default 5% crit unless weapon lore overrides it; socket and set always apply. */
    public static double criticalChance(boolean hasLore, double loreChance, double socketChance, double itemSetChance) {
        final double base = hasLore ? finiteOrZero(loreChance) : 5.0;
        return chance(base + finiteOrZero(socketChance) + finiteOrZero(itemSetChance));
    }

    /** Lore is an additive multiplier; socket/set critical damage use percentages. */
    public static double criticalDamageMultiplier(boolean hasLore, double loreBonus,
                                                  double socketPercent, double itemSetPercent) {
        final double base = hasLore ? 1.0 + finiteOrZero(loreBonus) : 1.2;
        return damage(base + (finiteOrZero(socketPercent) + finiteOrZero(itemSetPercent)) / 100.0);
    }

    public static double damage(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    public static double accuracyMultiplier(double accuracy) {
        if (!Double.isFinite(accuracy) || accuracy <= 0.0) return 0.0;
        return Math.max(0.0, 1.0 + Math.log10(accuracy / 100.0));
    }

    public static double balancedDamage(double damage, double defense, double modus) {
        final double validDamage = damage(damage);
        if (!Double.isFinite(modus) || modus <= 0.0) return validDamage;
        final double validDefense = damage(defense);
        final double divisor = 1.0 + Math.log10(1.0 + 10.0 * validDefense / modus);
        if (!Double.isFinite(divisor) || divisor <= 0.0) return validDamage;
        return damage(validDamage / divisor * 10.0 / modus);
    }

    /** Apply set percent bonus only to base lore, not already-added socket/element values. */
    public static double withBasePercentBonus(double existingTotal, double baseStat,
                                              double flatBonus, double percentBonus) {
        final double value = existingTotal + flatBonus + baseStat * percentBonus / 100.0;
        return Double.isFinite(value) ? value : 0.0;
    }

    /** PvP damage bonuses key off the victim, defense bonuses off the attacker. */
    public static double outgoingBonus(boolean victimPlayer, double pvp, double pve) {
        return Double.isFinite(victimPlayer ? pvp : pve) ? (victimPlayer ? pvp : pve) : 0.0;
    }

    public static double incomingDefenseBonus(boolean attackerPlayer, double pvp, double pve) {
        return Double.isFinite(attackerPlayer ? pvp : pve) ? (attackerPlayer ? pvp : pve) : 0.0;
    }

    public static double percentFactor(double bonus) {
        return Double.isFinite(bonus) ? Math.max(0.0, 1.0 + bonus / 100.0) : 1.0;
    }

    public static double experienceToNextLevel(int level) {
        final double safeLevel = Math.max(1, level);
        return safeLevel * safeLevel * 25.0 + safeLevel * 50.0 + 100.0;
    }
}
