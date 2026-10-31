package com.praya.myitems.modern;

/** Decodes MyItems' existing colored lore values without treating configured symbols as regexes. */
public final class Paper26LoreStatsNumbers {
    public enum Kind { DAMAGE, DURABILITY, PERCENT, MULTIPLIER, CRITICAL_DAMAGE, NUMBER }

    private Paper26LoreStatsNumbers() { }

    public static double read(String lore, String marker, Kind kind, String option,
                              String positiveColor, String negativeColor, String range,
                              String divider, String multiplier) {
        if (lore == null || marker == null || marker.isEmpty() || kind == null) return 0.0;
        final int first = lore.indexOf(marker);
        if (first < 0) return 0.0;
        final int from = first + marker.length();
        final int last = lore.indexOf(marker, from);
        String value = lore.substring(from, last >= 0 ? last : lore.length());

        if (kind == Kind.DAMAGE || kind == Kind.DURABILITY) {
            final String separator = kind == Kind.DAMAGE ? range : divider;
            if (separator != null && !separator.isEmpty()) {
                final int split = value.indexOf(separator);
                if (split >= 0) {
                    final double a = parseNumber(value.substring(0, split), positiveColor, negativeColor);
                    final double b = parseNumber(value.substring(split + separator.length()), positiveColor, negativeColor);
                    if (!Double.isFinite(a) || !Double.isFinite(b)) return 0.0;
                    if (kind == Kind.DURABILITY) {
                        return "MAX".equals(option) ? b : a;
                    }
                    final double min = Math.min(a, b);
                    final double max = Math.max(a, b);
                    if ("MAX".equals(option)) return max;
                    if ("MIN".equals(option) || "CURRENT".equals(option)) return min;
                    return min + Math.random() * (max - min);
                }
            }
            // Legacy single-value damage lore is also supported.
        }
        if (kind == Kind.PERCENT) value = value.replace("%", "");
        if (kind == Kind.MULTIPLIER || kind == Kind.CRITICAL_DAMAGE) {
            if (multiplier != null && !multiplier.isEmpty()) value = value.replace(multiplier, "");
        }
        final double result = parseNumber(value, positiveColor, negativeColor);
        if (!Double.isFinite(result)) return 0.0;
        return kind == Kind.CRITICAL_DAMAGE ? result - 1.0 : result;
    }

    private static double parseNumber(String raw, String positiveColor, String negativeColor) {
        if (raw == null) return Double.NaN;
        String value = raw;
        if (positiveColor != null && !positiveColor.isEmpty()) value = value.replace(positiveColor, "");
        if (negativeColor != null && !negativeColor.isEmpty()) value = value.replace(negativeColor, "");
        // Legacy (&a / section-sign) as well as modern section-sign hex sequences.
        value = value.replaceAll("(?i)[§&][0-9a-fk-orx]", "").trim();
        try {
            final double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) ? parsed : Double.NaN;
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }
}
