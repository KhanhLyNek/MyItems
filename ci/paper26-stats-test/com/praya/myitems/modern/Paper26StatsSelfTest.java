package com.praya.myitems.modern;

import com.praya.myitems.modern.Paper26LoreStatsNumbers.Kind;

public final class Paper26StatsSelfTest {
    private static int checks;
    private static void eq(double actual, double expected) {
        checks++;
        if (!Double.isFinite(actual) || Math.abs(actual - expected) > 0.000001)
            throw new AssertionError("expected " + expected + ", got " + actual);
    }
    private static void yes(boolean ok) {
        checks++;
        if (!ok) throw new AssertionError("Stats regression assertion failed");
    }
    private static double read(String v, Kind k, String opt) {
        return Paper26LoreStatsNumbers.read("Damage §0§2§r" + v + "§0§2§r", "§0§2§r", k, opt,
                "§a", "§c", " §7➨ ", "§7/", "§7x");
    }
    public static void main(String[] args) {
        eq(read("§a10.0 §7➨ §a25.0", Kind.DAMAGE, "MIN"), 10);
        // Verify the old API descriptor and the new implementation agree.
        for (Kind kind : Kind.values()) {
            String value = kind == Kind.DAMAGE ? "§a4 §7➨ §a8"
                    : kind == Kind.DURABILITY ? "§a4§7/§a8"
                    : kind == Kind.PERCENT ? "§a12.5%"
                    : (kind == Kind.MULTIPLIER || kind == Kind.CRITICAL_DAMAGE)
                        ? "§a1.5§7x" : "§a12.5";
            String option = kind == Kind.DAMAGE || kind == Kind.DURABILITY ? "MAX" : null;
            double existing = read(value, kind, option);
            double migrated = com.lymeowz.item.stats.LoreStatsNumbers.read(
                    "Damage §0§2§r" + value + "§0§2§r", "§0§2§r",
                    com.lymeowz.item.stats.LoreStatsNumbers.Kind.valueOf(kind.name()),
                    option, "§a", "§c", " §7➨ ", "§7/", "§7x");
            eq(migrated, existing);
        }
        eq(com.lymeowz.item.stats.LoreStatsNumbers.read(null, "x", null, null,
                null, null, null, null, null), 0);
        eq(Paper26LoreStatsNumbers.read("Damage §0§2§r§a5§0§2§r",
                "§0§2§r", null, null, "§a", "§c", null, null, null), 0);
        eq(read("§a10.0 §7➨ §a25.0", Kind.DAMAGE, "MAX"), 25);
        eq(read("§a25.0 §7➨ §a10.0", Kind.DAMAGE, "MIN"), 10);
        double random = read("§a10.0 §7➨ §a25.0", Kind.DAMAGE, null);
        yes(random >= 10 && random <= 25);
        eq(read("§c-15.0", Kind.DAMAGE, null), -15);
        eq(read("§a10§7/§a20", Kind.DURABILITY, "CURRENT"), 10);
        eq(read("§a10§7/§a20", Kind.DURABILITY, "MAX"), 20);
        eq(read("§c-5%", Kind.PERCENT, null), -5);
        eq(read("§a1.25§7x", Kind.CRITICAL_DAMAGE, null), 0.25);
        eq(read("§a2.0§7x", Kind.MULTIPLIER, null), 2);
        eq(read("§x§f§f§1§1§0§0+12.5", Kind.NUMBER, null), 12.5);
        eq(read("NaN", Kind.NUMBER, null), 0);
        eq(read("Infinity", Kind.NUMBER, null), 0);
        eq(read("bad", Kind.NUMBER, null), 0);
        eq(Paper26LoreStatsNumbers.read("X §0§2§r§a3 (a+b) §c5§0§2§r", "§0§2§r", Kind.DAMAGE,
                "MAX", "§a", "§c", " (a+b) ", "/", "x"), 5);
        eq(Paper26StatsMath.repairedDurability(5, 10, 3), 8);
        eq(com.lymeowz.item.stats.CombatMath.repairedDurability(5, 10, 3), 8);
        eq(Paper26StatsMath.repairedDurability(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE),
            com.lymeowz.item.stats.CombatMath.repairedDurability(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
        eq(Paper26StatsMath.repairedDurability(5, 10, -8), 0);
        eq(Paper26StatsMath.repairedDurability(5, 10, Integer.MAX_VALUE), 10);
        eq(Paper26StatsMath.repairedDurability(5, 10, Integer.MIN_VALUE), 0);
        eq(Paper26StatsMath.repairedDurability(5, -1, 100), 0);
        eq(Paper26StatsMath.experienceToNextLevel(1), 175);
        eq(Paper26StatsMath.experienceToNextLevel(10),
            com.lymeowz.item.stats.CombatMath.experienceToNextLevel(10));
        eq(Paper26StatsMath.experienceToNextLevel(2), 300);
        eq(Paper26StatsMath.experienceToNextLevel(0), 175);
        eq(Paper26StatsMath.accuracyMultiplier(100), 1);
        eq(Paper26StatsMath.accuracyMultiplier(0), 0);
        eq(Paper26StatsMath.accuracyMultiplier(1), 0);
        eq(Paper26StatsMath.chance(Double.NaN), 0);
        eq(Paper26StatsMath.chance(250), 100);
        yes(!Paper26StatsMath.rollChance(0, 0));
        yes(!com.lymeowz.item.stats.CombatMath.rollChance(0, 0));
        yes(com.lymeowz.item.stats.CombatMath.rollChance(100, 0.99999));
        yes(Paper26StatsMath.rollChance(100, 0.999999));
        yes(Paper26StatsMath.rollChance(50, 0.499));
        yes(!Paper26StatsMath.rollChance(50, 0.5));
        yes(!Paper26StatsMath.rollChance(Double.NaN, 0.1));
        yes(!Paper26StatsMath.rollChance(99, Double.NaN));
        eq(Paper26StatsMath.damage(Double.POSITIVE_INFINITY), 0);
        eq(Paper26StatsMath.damage(-8), 0);
        eq(Paper26StatsMath.criticalChance(false, 0, 2.5, 5), 12.5);
        eq(Paper26StatsMath.criticalChance(true, 0, 2.5, 5), 7.5);
        eq(Paper26StatsMath.criticalChance(true, 250, 5, 5), 100);
        eq(Paper26StatsMath.criticalChance(true, Double.NaN, 0, 0), 0);
        eq(Paper26StatsMath.criticalDamageMultiplier(false, 0, 10, 5), 1.35);
        eq(Paper26StatsMath.criticalDamageMultiplier(true, 0.4, 10, 5), 1.55);
        eq(Paper26StatsMath.balancedDamage(10, 0, 10), 10);
        eq(Paper26StatsMath.balancedDamage(20, 15, 12),
            com.lymeowz.item.stats.CombatMath.balancedDamage(20, 15, 12));
        eq(Paper26StatsMath.withBasePercentBonus(15, 10, 2, 50), 22);
        eq(Paper26StatsMath.withBasePercentBonus(30, 20, 2, 50), 42);
        eq(Paper26StatsMath.withBasePercentBonus(150, 100, 20, 10), 180);
        eq(Paper26StatsMath.outgoingBonus(true, 20, 40), 20);
        eq(Paper26StatsMath.criticalChance(true, 20, 5, 3),
            com.lymeowz.item.stats.CombatMath.criticalChance(true, 20, 5, 3));
        eq(Paper26StatsMath.criticalDamageMultiplier(true, 0.3, 10, 20),
            com.lymeowz.item.stats.CombatMath.criticalDamageMultiplier(true, 0.3, 10, 20));
        eq(Paper26StatsMath.outgoingBonus(false, 20, 40), 40);
        eq(Paper26StatsMath.incomingDefenseBonus(true, 25, 70), 25);
        eq(Paper26StatsMath.incomingDefenseBonus(false, 25, 70), 70);
        eq(Paper26StatsMath.percentFactor(-150), 0);
        eq(Paper26StatsMath.percentFactor(50), 1.5);
        eq(Paper26StatsMath.percentFactor(Double.NaN), 1);
        eq(Paper26StatsMath.percentFactor(Double.POSITIVE_INFINITY), 1);
        eq(Paper26ModernStatsFormulas.attackSpeed(4, 1.25), 5.25);
        eq(com.lymeowz.item.stats.ModernStatsFormulas.attackSpeed(4, 1.25), 5.25);
        eq(Paper26ModernStatsFormulas.lifeSteal(20, 10, 5),
           com.lymeowz.item.stats.ModernStatsFormulas.lifeSteal(20, 10, 5));
        eq(Paper26ModernStatsFormulas.cooldownTicks(100, 20),
           com.lymeowz.item.stats.ModernStatsFormulas.cooldownTicks(100, 20));
        eq(Paper26ModernStatsFormulas.attackSpeed(4, -100), 0.1);
        eq(Paper26ModernStatsFormulas.armorToughness(2, 3), 5);
        eq(Paper26ModernStatsFormulas.armorToughness(2, -9), 0);
        eq(Paper26ModernStatsFormulas.lifeSteal(20, 10, 5), 2);
        eq(Paper26ModernStatsFormulas.lifeSteal(20, 200, 5), 5);
        eq(Paper26ModernStatsFormulas.lifeSteal(-2, 30, 5), 0);
        eq(Paper26ModernStatsFormulas.cooldownTicks(100, 20), 80);
        eq(Paper26ModernStatsFormulas.cooldownTicks(1, 99), 1);
        eq(Paper26ModernStatsFormulas.cooldownTicks(100, 100), 0);
        eq(Paper26StatsMath.balancedDamage(10, 20, 0), 10);
        System.out.println("PASS: " + checks + " Stats regression assertions");
    }
}
