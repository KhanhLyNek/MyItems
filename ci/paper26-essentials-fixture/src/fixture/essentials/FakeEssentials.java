package fixture.essentials;

import java.util.Collection;
import java.util.List;
import net.ess3.api.IWarps;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Disposable API-shaped plugin on CI only; never an EssentialsX replacement
 * for users or proof that EssentialsX itself supports Minecraft 26.3.
 */
public final class FakeEssentials extends JavaPlugin {
    private final IWarps warps = new FakeWarps();

    public IWarps getWarps() {
        return warps;
    }

    @Override
    public void onEnable() {
        getLogger().info("CI fixture: public net.ess3.api.IWarps enabled");
    }

    public static final class WarpNotFoundException extends Exception {}

    public static final class FakeWarps implements IWarps {
        private final List<String> names = List.of("spawn", "market", "missing");
        @Override
        public Collection<String> getList() {
            return names;
        }
        @Override
        public Location getWarp(final String name) throws WarpNotFoundException {
            final World world = Bukkit.getWorlds().get(0);
            return switch (name) {
                case "spawn" -> new Location(world, 13, 82, 21);
                case "market" -> new Location(world, 24, 64, 17);
                default -> throw new WarpNotFoundException();
            };
        }
    }
}
