package com.praya.myitems.modern;

/**
 * @deprecated Lore number parsing now lives in com.lymeowz.item.stats.
 * Legacy callers keep their original enum descriptor and static method.
 */
@Deprecated
public final class Paper26LoreStatsNumbers {
    public enum Kind { DAMAGE, DURABILITY, PERCENT, MULTIPLIER, CRITICAL_DAMAGE, NUMBER }
    private Paper26LoreStatsNumbers() { }

    public static double read(String lore, String marker, Kind kind, String option,
                              String positiveColor, String negativeColor, String range,
                              String divider, String multiplier) {
        return com.lymeowz.item.stats.LoreStatsNumbers.read(lore, marker,
                kind == null ? null : com.lymeowz.item.stats.LoreStatsNumbers.Kind.valueOf(kind.name()),
                option, positiveColor, negativeColor, range, divider, multiplier);
    }
}
