package fr.openmc.core.features.tpa.commands;

import fr.openmc.core.features.tpa.TPAQueue;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Named;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.bukkit.annotation.CommandPermission;

public class TPDenyCommand {

	/**
	 * Command to deny a teleportation request
	 * @param target The player denying the request.
	 * @param player The player who sent the teleportation request (optional).
	 */
	@Command("tpdeny")
	@CommandPermission("omc.commands.tpa")
	public void tpDeny(Player target, @Optional @Named("player") Player player) {
		if (!TPAQueue.QUEUE.hasPendingRequest(target)) {
			MessagesManager.sendMessage(target, Component.translatable("omc.tpa.no_pending_request"), Prefix.OPENMC, MessageType.ERROR, false);
			return;
		}

		if (TPAQueue.QUEUE.hasMultipleRequests(target)) {
			if (player == null) {
				MessagesManager.sendMessage(target, Component.translatable("omc.tpa.deny_missing_name"), Prefix.OPENMC, MessageType.ERROR, false);
				return;
			}

			if (!TPAQueue.QUEUE.getRequesters(target).contains(player)) {
				MessagesManager.sendMessage(target, Component.translatable("omc.tpa.deny_unknown_requester", Component.text(player.getName())), Prefix.OPENMC, MessageType.ERROR, false);
				return;
			}
		} else {
			player = TPAQueue.QUEUE.getRequesters(target).getFirst();
		}

		MessagesManager.sendMessage(target, Component.translatable("omc.tpa.deny_success", Component.text(player.getName())), Prefix.OPENMC, MessageType.SUCCESS, false);
		MessagesManager.sendMessage(player, Component.translatable("omc.tpa.deny_notify_requester", Component.text(target.getName())), Prefix.OPENMC, MessageType.ERROR, false);

		TPAQueue.QUEUE.removeRequest(player, target);
	}
}