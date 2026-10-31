// 
// Decompiled by Procyon v0.5.36
// 

package com.praya.myitems.config.plugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import com.praya.myitems.manager.plugin.DataManager;
import com.praya.myitems.manager.plugin.PluginManager;
import java.util.List;
import java.util.ArrayList;
import org.bukkit.plugin.java.JavaPlugin;
import com.praya.agarthalib.utility.FileUtil;
import java.util.Iterator;
import java.util.Collection;
import com.praya.myitems.MyItems;
import core.praya.agarthalib.builder.command.CommandBuild;
import java.util.HashMap;
import com.praya.myitems.builder.handler.HandlerCommand;

public class CommandConfig extends HandlerCommand
{
    private final HashMap<String, CommandBuild> mapCommand;
    
    public CommandConfig(final MyItems plugin) {
        super(plugin);
        this.mapCommand = new HashMap<String, CommandBuild>();
        this.setup();
    }
    
    public final Collection<String> getCommandIDs() {
        return this.mapCommand.keySet();
    }
    
    public final Collection<CommandBuild> getCommandBuilds() {
        return this.mapCommand.values();
    }
    
    public final CommandBuild getCommand(final String id) {
        for (final String key : this.getCommandIDs()) {
            if (key.equalsIgnoreCase(id)) {
                return this.mapCommand.get(key);
            }
        }
        return null;
    }
    
    public final void setup() {
        this.reset();
        this.loadConfig();
    }
    
    private final void reset() {
        this.mapCommand.clear();
    }
    
    private final void loadConfig() {
        this.mapCommand.putAll(com.lymeowz.item.command.CommandDefinitions.load(this.plugin));
    }
}
