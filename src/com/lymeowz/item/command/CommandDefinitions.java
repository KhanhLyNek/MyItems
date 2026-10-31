package com.lymeowz.item.command;

import com.praya.myitems.MyItems;
import com.praya.myitems.manager.plugin.PluginManager;
import com.praya.myitems.manager.plugin.DataManager;
import com.praya.agarthalib.utility.FileUtil;
import core.praya.agarthalib.builder.command.CommandBuild;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * LyMeowzItem's command configuration decoder. Retains all historic
 * per-command IDs and aliases; permissions are translated only when checked.
 */
public final class CommandDefinitions {
    private CommandDefinitions() { }

    public static HashMap<String, CommandBuild> load(final MyItems plugin) {
        final HashMap<String, CommandBuild> map = new HashMap<>();
        final PluginManager pluginManager = plugin.getPluginManager();
        final DataManager dataManager = pluginManager.getDataManager();
        final String path = dataManager.getPath("Path_File_Command");
        final FileConfiguration config = FileUtil.getFileConfigurationResource((JavaPlugin)plugin, path);
        for (final String key : config.getKeys(false)) {
            if (key.equalsIgnoreCase("Command")) {
                final ConfigurationSection idSection = config.getConfigurationSection(key);
                for (final String id : idSection.getKeys(false)) {
                    final ConfigurationSection mainDataSection = idSection.getConfigurationSection(id);
                    final List<String> aliases = new ArrayList<String>();
                    String permission = null;
                    for (final String mainData : mainDataSection.getKeys(false)) {
                        if (mainData.equalsIgnoreCase("Permission")) {
                            permission = mainDataSection.getString(mainData);
                        }
                        else {
                            if (!mainData.equalsIgnoreCase("Aliases")) {
                                continue;
                            }
                            if (mainDataSection.isString(mainData)) {
                                aliases.add(mainDataSection.getString(mainData));
                            }
                            else {
                                if (!mainDataSection.isList(mainData)) {
                                    continue;
                                }
                                aliases.addAll(mainDataSection.getStringList(mainData));
                            }
                        }
                    }
                    final CommandBuild commandBuild = new CommandBuild(id, permission, (List)aliases);
                    map.put(id, commandBuild);
                }
            }
        }
        return map;
    }
}
