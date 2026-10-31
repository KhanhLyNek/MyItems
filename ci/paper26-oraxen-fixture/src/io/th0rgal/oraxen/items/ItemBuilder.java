package io.th0rgal.oraxen.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** API-shaped, minimal builder in CI only. */
public final class ItemBuilder {
    public ItemStack build() {
        final ItemStack stack = new ItemStack(Material.DIAMOND_SWORD);
        final ItemMeta meta = stack.getItemMeta();
        meta.setItemModel(NamespacedKey.fromString("oraxen:storm_blade"));
        stack.setItemMeta(meta);
        return stack;
    }
}
