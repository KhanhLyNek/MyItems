package com.praya.myitems.modern;

import org.bukkit.command.CommandSender;

/**
 * @deprecated The /lymeowzitem model command now runs from the LyMeowzItem
 * namespace. This bridge retains the original public static method descriptor.
 */
@Deprecated
public final class Paper26ModelCommand {
    private Paper26ModelCommand() { }

    public static boolean execute(final CommandSender sender, final String[] args) {
        return com.lymeowz.item.command.ModelCommand.execute(sender, args);
    }
}
