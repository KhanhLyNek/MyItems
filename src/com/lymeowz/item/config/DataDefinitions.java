package com.lymeowz.item.config;

import com.praya.myitems.MyItems;
import com.praya.agarthalib.utility.FileUtil;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import core.praya.agarthalib.builder.main.DataBuild;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Canonical LyMeowzItem path dictionary parser, retaining the historical
 * case-sensitive keys and default Resources/data.yml exactly as shipped.
 * No YAML rewrite or on-disk migration is performed by this adapter.
 */
public final class DataDefinitions {
    private DataDefinitions() { }

    public static HashMap<String, DataBuild> load(final MyItems plugin) {
        final HashMap<String, DataBuild> map = new HashMap<>();
        final FileConfiguration config = FileUtil.getFileConfigurationResource((JavaPlugin)plugin, "Resources/data.yml");
        for (final String path : config.getKeys(true)) {
            final String key = path.replace(".", "_");
            if (config.isString(path)) {
                final String text = config.getString(path);
                final List<String> list = new ArrayList<String>();
                list.add(text);
                final DataBuild dataBuild = new DataBuild(key, (List)list);
                map.put(key, dataBuild);
            }
            else {
                if (!config.isList(path)) {
                    continue;
                }
                final List<String> list2 = (List<String>)config.getStringList(path);
                final DataBuild dataBuild2 = new DataBuild(key, (List)list2);
                map.put(key, dataBuild2);
            }
        }
        return map;
    }
}
