package dev.lone.itemsadder.api;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import java.util.List;

/** CI-only ItemsAdder public CustomStack ABI shape; never packaged to users. */
public final class CustomStack {
    private final ItemStack stack;
    private CustomStack(final ItemStack stack) { this.stack = stack; }
    public static CustomStack getInstance(final String id) {
        if ("test:ruby_sword".equals(id)) {
            final ItemStack stack = new ItemStack(Material.DIAMOND_SWORD);
            final ItemMeta meta = stack.getItemMeta();
            meta.setItemModel(NamespacedKey.fromString("itemsadder:ruby_sword"));
            stack.setItemMeta(meta);
            return new CustomStack(stack);
        }
        if ("test:legacy_sword".equals(id)) {
            final ItemStack stack = new ItemStack(Material.DIAMOND_SWORD);
            final ItemMeta meta = stack.getItemMeta();
            final CustomModelDataComponent data = meta.getCustomModelDataComponent();
            data.setFloats(List.of(2026.0F));
            meta.setCustomModelDataComponent(data);
            stack.setItemMeta(meta);
            return new CustomStack(stack);
        }
        return null;
    }
    public ItemStack getItemStack() { return stack.clone(); }
}
