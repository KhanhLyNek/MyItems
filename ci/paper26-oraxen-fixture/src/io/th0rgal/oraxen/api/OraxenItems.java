package io.th0rgal.oraxen.api;

import io.th0rgal.oraxen.items.ItemBuilder;

/** CI-only Oraxen public OraxenItems ABI shape; never packaged to users. */
public final class OraxenItems {
    private OraxenItems() {}
    public static ItemBuilder getItemById(final String id) {
        return "storm_blade".equals(id) ? new ItemBuilder() : null;
    }
}
