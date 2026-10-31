package com.praya.myitems.modern;

/**
 * @deprecated Runtime Stats math now belongs to LyMeowzItem's namespace.
 * These original public entrypoints remain for binary compatibility.
 */
@Deprecated
public final class Paper26StatsMath {
    private Paper26StatsMath() { }

    public static boolean rollChance(double percent, double uniform) {
        return com.lymeowz.item.stats.CombatMath.rollChance(percent, uniform);
    }

    public static double chance(double value) {
        return com.lymeowz.item.stats.CombatMath.chance(value);
    }

    public static double criticalChance(boolean hasLore, double loreChance, double socketChance, double itemSetChance) {
        return com.lymeowz.item.stats.CombatMath.criticalChance(hasLore, loreChance, socketChance, itemSetChance);
    }

    public static double criticalDamageMultiplier(boolean hasLore, double loreBonus,
                                                  double socketPercent, double itemSetPercent) {
        return com.lymeowz.item.stats.CombatMath.criticalDamageMultiplier(hasLore, loreBonus, socketPercent, itemSetPercent);
    }

    public static double damage(double value) {
        return com.lymeowz.item.stats.CombatMath.damage(value);
    }

    public static double accuracyMultiplier(double accuracy) {
        return com.lymeowz.item.stats.CombatMath.accuracyMultiplier(accuracy);
    }

    public static double balancedDamage(double damage, double defense, double modus) {
        return com.lymeowz.item.stats.CombatMath.balancedDamage(damage, defense, modus);
    }

    public static double withBasePercentBonus(double existingTotal, double baseStat,
                                              double flatBonus, double percentBonus) {
        return com.lymeowz.item.stats.CombatMath.withBasePercentBonus(existingTotal, baseStat, flatBonus, percentBonus);
    }

    public static double outgoingBonus(boolean victimPlayer, double pvp, double pve) {
        return com.lymeowz.item.stats.CombatMath.outgoingBonus(victimPlayer, pvp, pve);
    }

    public static double incomingDefenseBonus(boolean attackerPlayer, double pvp, double pve) {
        return com.lymeowz.item.stats.CombatMath.incomingDefenseBonus(attackerPlayer, pvp, pve);
    }

    public static double percentFactor(double bonus) {
        return com.lymeowz.item.stats.CombatMath.percentFactor(bonus);
    }

    public static int repairedDurability(int current, int maximum, int delta) {
        return com.lymeowz.item.stats.CombatMath.repairedDurability(current, maximum, delta);
    }

    public static double experienceToNextLevel(int level) {
        return com.lymeowz.item.stats.CombatMath.experienceToNextLevel(level);
    }
}
