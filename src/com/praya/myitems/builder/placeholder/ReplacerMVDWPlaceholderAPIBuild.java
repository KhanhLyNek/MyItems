package com.praya.myitems.builder.placeholder;

import com.praya.agarthalib.modern.Paper26MVdWRegistration;
import com.praya.myitems.MyItems;
import com.praya.myitems.manager.plugin.PlaceholderManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * The historical MyItems MVdW wildcard is kept; actual optional API classes
 * are resolved by AgarthaLib rather than linked in the plugin class loader.
 */
public class ReplacerMVDWPlaceholderAPIBuild {
    private final MyItems plugin;
    private final String placeholder;

    public ReplacerMVDWPlaceholderAPIBuild(final MyItems plugin, final String placeholder) {
        this.plugin = plugin;
        this.placeholder = placeholder;
    }

    public final String getPlaceholder() {
        return this.placeholder;
    }

    public final void register() {
        final PlaceholderManager manager = plugin.getPluginManager().getPlaceholderManager();
        final String wildcard = String.valueOf(getPlaceholder()) + "_*";
        final Plugin provider = Bukkit.getPluginManager().getPlugin("MVdWPlaceholderAPI");
        Paper26MVdWRegistration.register(plugin, provider, wildcard,
                (player, originalPlaceholder) -> manager.getReplacement(player,
                        originalPlaceholder.split(String.valueOf(getPlaceholder()) + "_")[1]));
    }
}
