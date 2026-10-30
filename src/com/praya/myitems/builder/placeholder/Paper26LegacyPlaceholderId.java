package com.praya.myitems.builder.placeholder;

/**
 * Parses the existing MVdW wildcard prefix without treating it as a regex.
 * A missing/malformed prefix must never invoke MyItems' item/skill resolver.
 */
public final class Paper26LegacyPlaceholderId {
    private Paper26LegacyPlaceholderId() {
    }

    /**
     * Example: ("myitems", "myitems_text_ability:venom_blast") returns
     * "text_ability:venom_blast" unchanged.
     */
    public static String extract(final String prefix, final String placeholder) {
        if (prefix == null || prefix.isEmpty() || placeholder == null
                || placeholder.length() <= prefix.length() + 1) {
            return null;
        }
        if (!placeholder.regionMatches(true, 0, prefix, 0, prefix.length())
                || placeholder.charAt(prefix.length()) != '_') {
            return null;
        }
        // Keep all later underscores, colons and even repeated prefix text.
        return placeholder.substring(prefix.length() + 1);
    }
}
