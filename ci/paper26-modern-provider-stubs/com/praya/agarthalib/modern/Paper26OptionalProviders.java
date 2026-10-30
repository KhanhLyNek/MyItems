package com.praya.agarthalib.modern;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import java.util.List;
/** CI-only ABI stub; real integration implementation is in AgarthaLib. */
public final class Paper26OptionalProviders {
    public static Integer auraSkillLevel(OfflinePlayer p, String s) { return null; }
    public static Integer mmoCoreLevel(OfflinePlayer p) { return null; }
    public static String mmoCoreClassId(OfflinePlayer p) { return null; }
    public static Boolean landsClaimed(Plugin owner, Location at) { return null; }
    public static boolean createTransientHologram(String id, Location at, List<String> l) { return false; }
}
