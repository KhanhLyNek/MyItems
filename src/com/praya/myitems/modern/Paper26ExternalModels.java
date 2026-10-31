package com.praya.myitems.modern;

import com.praya.agarthalib.modern.Paper26ItemModels;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Optional, provider-owned public API adapters. Neither ItemsAdder nor Oraxen
 * classes are ever linked into the MyItems production JAR.
 *
 * Deliberately copies only the model selector. ItemsAdder/Oraxen PDC identity,
 * durability, usages and mechanics are not copied or forged.
 */
public final class Paper26ExternalModels {
    public enum Status { APPLIED, NOT_INSTALLED, ID_NOT_FOUND, INCOMPATIBLE_API,
        NO_VISUAL_MODEL, MATERIAL_MISMATCH, INVALID_ITEM, INVALID_ID }
    private Paper26ExternalModels() {}

    public static Status apply(final ItemStack item, final String provider, final String id) {
        if (item == null || item.getType().isAir()) return Status.INVALID_ITEM;
        if (id == null || id.isBlank() || id.length() > 256) return Status.INVALID_ID;
        if (!"itemsadder".equalsIgnoreCase(provider) && !"oraxen".equalsIgnoreCase(provider))
            return Status.INVALID_ID;
        if (!Bukkit.isPrimaryThread()) return Status.INCOMPATIBLE_API;

        final String name = "itemsadder".equalsIgnoreCase(provider) ? "ItemsAdder" : "Oraxen";
        final Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        if (plugin == null || !plugin.isEnabled()) return Status.NOT_INSTALLED;
        try {
            final ClassLoader owner = plugin.getClass().getClassLoader();
            final Object template;
            if ("ItemsAdder".equals(name)) {
                final Class<?> api = Class.forName(
                        "dev.lone.itemsadder.api.CustomStack", false, owner);
                final Object source = api.getMethod("getInstance", String.class).invoke(null, id);
                if (source == null) return Status.ID_NOT_FOUND;
                template = source.getClass().getMethod("getItemStack").invoke(source);
            } else {
                final Class<?> api = Class.forName(
                        "io.th0rgal.oraxen.api.OraxenItems", false, owner);
                final Object source = api.getMethod("getItemById", String.class).invoke(null, id);
                if (source == null) return Status.ID_NOT_FOUND;
                template = source.getClass().getMethod("build").invoke(source);
            }
            if (!(template instanceof ItemStack sample)) return Status.INCOMPATIBLE_API;
            return switch (Paper26ItemModels.copyFrom(item, sample)) {
                case APPLIED -> Status.APPLIED;
                case MATERIAL_MISMATCH -> Status.MATERIAL_MISMATCH;
                case NO_MODEL -> Status.NO_VISUAL_MODEL;
                case INVALID_ITEM -> Status.INVALID_ITEM;
                case INVALID_ID -> Status.INVALID_ID;
            };
        } catch (final ClassNotFoundException | NoSuchMethodException
                       | IllegalAccessException | InvocationTargetException
                       | LinkageError | RuntimeException ex) {
            plugin.getLogger().warning("MyItems model bridge: " + name
                    + " API could not supply visual model for '" + id + "' ("
                    + ex.getClass().getSimpleName() + ")");
            return Status.INCOMPATIBLE_API;
        }
    }
}
