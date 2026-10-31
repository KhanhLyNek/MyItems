package com.praya.myitems.modern;

import com.praya.agarthalib.modern.Paper26ItemModels;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** /myitems model <itemsadder|oraxen|key|data|clear> [model] */
public final class Paper26ModelCommand {
    private Paper26ModelCommand() {}

    public static boolean execute(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("myitems.command.model")
                && !sender.hasPermission("myitems.admin")) {
            sender.sendMessage("§cYou lack permission: myitems.command.model");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cA player must hold the item to change its model.");
            return true;
        }
        if (args.length < 2 || args.length > 3) {
            usage(sender);
            return true;
        }
        final ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            sender.sendMessage("§cHold a MyItems item in your main hand.");
            return true;
        }
        final ItemStack changed = held.clone(); // Never mutate inventory on failure.
        final String mode = args[1].toLowerCase(java.util.Locale.ROOT);
        if (mode.equals("clear") && args.length == 2) {
            return finish(player, changed, Paper26ItemModels.clear(changed));
        }
        if (args.length != 3) {
            usage(sender);
            return true;
        }
        final String value = args[2];
        if (mode.equals("itemsadder") || mode.equals("oraxen")) {
            final Paper26ExternalModels.Status outcome =
                    Paper26ExternalModels.apply(changed, mode, value);
            if (outcome == Paper26ExternalModels.Status.APPLIED) {
                player.getInventory().setItemInMainHand(changed);
                sender.sendMessage("§aVisual model copied from " + mode + ": " + value
                        + " §7(original MyItems metadata and skills retained).");
            } else {
                final String reason = switch (outcome) {
                    case NOT_INSTALLED -> "The provider is not enabled.";
                    case ID_NOT_FOUND -> "No model with this ID is registered (or data is not loaded).";
                    case MATERIAL_MISMATCH -> "Legacy CMD requires the same base material as the provider item. Change material first.";
                    case NO_VISUAL_MODEL -> "That provider item has no supported item_model or CustomModelData component.";
                    case INVALID_ID -> "Invalid model ID.";
                    case INVALID_ITEM -> "Cannot apply model to this item.";
                    default -> "Provider API is incompatible or unavailable; item unchanged.";
                };
                sender.sendMessage("§cModel not changed: " + reason);
            }
            return true;
        }
        if (mode.equals("key")) {
            return finish(player, changed, Paper26ItemModels.applyKey(changed, value));
        }
        if (mode.equals("data")) {
            try {
                return finish(player, changed, Paper26ItemModels.applyNumber(
                        changed, Integer.parseInt(value)));
            } catch (final NumberFormatException ex) {
                sender.sendMessage("§cExpected a nonnegative whole-number CustomModelData.");
                return true;
            }
        }
        usage(sender);
        return true;
    }

    private static boolean finish(final Player player, final ItemStack changed,
                                  final Paper26ItemModels.Result result) {
        if (result == Paper26ItemModels.Result.APPLIED) {
            player.getInventory().setItemInMainHand(changed);
            player.sendMessage("§aItem model updated; MyItems ID, lore, skills and NBT retained.");
        } else {
            player.sendMessage("§cModel not changed: " + result.name());
        }
        return true;
    }

    private static void usage(final CommandSender sender) {
        sender.sendMessage("§e/myitems model itemsadder <namespace:item>");
        sender.sendMessage("§e/myitems model oraxen <item_id>");
        sender.sendMessage("§e/myitems model key <namespace:path>");
        sender.sendMessage("§e/myitems model data <nonnegative_number>");
        sender.sendMessage("§e/myitems model clear");
    }
}
