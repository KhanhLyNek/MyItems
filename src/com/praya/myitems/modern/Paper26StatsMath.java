package com.praya.myitems.modern;

/** Isolated finite-number guards for the legacy MyItems combat and leveling formulas. */
public final class Paper26StatsMath {
    private Paper26StatsMath() { }

    public static double chance(double value) {
        if (Double.isNaN(value)) return 0.0;
        return Math.max(0.0, Math.min(100.0, value));
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

    public static double experienceToNextLevel(int level) {
        final double safeLevel = Math.max(1, level);
        return safeLevel * safeLevel * 25.0 + safeLevel * 50.0 + 100.0;
    }
}
