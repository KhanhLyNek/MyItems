package com.praya.myitems;

import com.praya.myitems.manager.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/** CI-only plugin signature: not in MyItems production source set. */
public class MyItems extends JavaPlugin {
    public PluginManager getPluginManager() {
        return null;
    }
}
