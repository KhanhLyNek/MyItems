package com.lymeowz.item.permission;

import org.bukkit.command.CommandSender;
import com.lymeowz.lib.permission.PermissionBridge;

/**
 * All LyMeowzItem commands accept either canonical or legacy nodes during
 * migration. New nodes default to false except the explicit OP admin parent.
 * No automatic blanket access for non-operators.
 */
public final class ItemPermissions {
    private ItemPermissions() { }

    public static String canonical(String node) {
        return PermissionBridge.canonical(node);
    }

    public static boolean allowed(CommandSender sender, String node) {
        if (node == null || node.isEmpty()) return true;
        // Unlike historic AgarthaLib's SenderUtil, MyItems checked EVERY sender.
        // Keep that behavior, including permission plugins' console policy.
        final String modern = canonical(node);
        return sender.hasPermission(modern)
                || (!modern.equalsIgnoreCase(node) && sender.hasPermission(node));
    }
}
