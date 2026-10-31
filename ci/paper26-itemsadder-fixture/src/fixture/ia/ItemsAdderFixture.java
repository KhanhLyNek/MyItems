package fixture.ia;

import org.bukkit.plugin.java.JavaPlugin;

/** API-shaped ItemsAdder test plugin, NEVER the real provider. */
public final class ItemsAdderFixture extends JavaPlugin {
    @Override public void onEnable() {
        getLogger().info("CI ItemsAdder public CustomStack fixture loaded");
    }
}
