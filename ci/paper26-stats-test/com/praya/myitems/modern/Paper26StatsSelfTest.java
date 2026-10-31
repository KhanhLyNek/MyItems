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
        eq(Paper26StatsMath.experienceToNextLevel(1), 175);
        eq(Paper26StatsMath.experienceToNextLevel(2), 300);
        eq(Paper26StatsMath.experienceToNextLevel(0), 175);
        eq(Paper26StatsMath.accuracyMultiplier(100), 1);
        eq(Paper26StatsMath.accuracyMultiplier(0), 0);
        eq(Paper26StatsMath.accuracyMultiplier(1), 0);
        eq(Paper26StatsMath.chance(Double.NaN), 0);
        eq(Paper26StatsMath.chance(250), 100);
        eq(Paper26StatsMath.damage(Double.POSITIVE_INFINITY), 0);
        eq(Paper26StatsMath.damage(-8), 0);
        eq(Paper26StatsMath.balancedDamage(10, 0, 10), 10);
        eq(Paper26StatsMath.balancedDamage(10, 20, 0), 10);
        System.out.println("PASS: " + checks + " Stats regression assertions");
    }
}
