package fr.openmc.core.features.tpa.commands;

import fr.openmc.core.features.tpa.TPAQueue;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.bukkit.annotation.CommandPermission;

public class TPCancelCommand {

	/**
	 * Command to cancel a teleport request.
	 * @param player The player who wants to cancel the request.
	 */
	@Command("tpcancel")
	@CommandPermission("omc.commands.tpa")
	public void tpCancel(Player player) {
		if (!TPAQueue.QUEUE.requesterHasPendingRequest(player)) {
			MessagesManager.sendMessage(player, Component.translatable("omc.tpa.no_pending_request"), Prefix.OPENMC, MessageType.ERROR, false);
			return;
		}

		Player target = TPAQueue.QUEUE.getTargetByRequester(player);

		TPAQueue.QUEUE.removeRequest(player, target);
		MessagesManager.sendMessage(player,
				Component.translatable("omc.tpa.cancel_success", Component.text(target.getName())),
				Prefix.OPENMC, MessageType.SUCCESS, true);

		MessagesManager.sendMessage(target,
				Component.translatable("omc.tpa.cancel_notify_target", Component.text(player.getName())),
				Prefix.OPENMC, MessageType.INFO, true);
	}
}
