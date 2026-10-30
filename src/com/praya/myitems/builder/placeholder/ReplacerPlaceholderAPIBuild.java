package com.praya.myitems.builder.placeholder;

import com.praya.myitems.MyItems;
import com.praya.myitems.manager.plugin.PlaceholderManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * MyItems internal PAPI expansion with its historic prefix, original
 * attribute/ability resolver, and optional external PAPI runtime.
 */
public class ReplacerPlaceholderAPIBuild extends PlaceholderExpansion {
    private final String placeholder;
    private final MyItems plugin;

    public ReplacerPlaceholderAPIBuild(final MyItems plugin, final String placeholder) {
        this.plugin = plugin;
        this.placeholder = placeholder;
    }

    public final String getPlaceholder() {
        return this.placeholder;
    }

    /** Original public registration method, now using supported PAPI API. */
    public final boolean hook() {
        return this.register();
    }

    @Override
    public @NotNull String getIdentifier() {
        return this.placeholder;
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(final OfflinePlayer player, @NotNull final String identifier) {
        // The former PlaceholderHook exposed only a live Player. Keep that
        // exact behavior; do not substitute unrelated offline player stats.
        final Player online = player != null && player.isOnline() ? player.getPlayer() : null;
        return onPlaceholderRequest(online, identifier);
    }

    @Override
    public String onPlaceholderRequest(final Player player, @NotNull final String identifier) {
        final PlaceholderManager manager = plugin.getPluginManager().getPlaceholderManager();
        return manager.getReplacement(player, identifier);
    }
}
