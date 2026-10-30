package net.ess3.api;

import java.util.Collection;
import org.bukkit.Location;

/** Test-only EssentialsX 2.22.0 public warp interface shape. Not shipped. */
public interface IWarps {
    Collection<String> getList();
    Location getWarp(String name) throws Exception;
}
