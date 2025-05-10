package fr.openmc.core.features.tpa.commands;

import fr.openmc.core.OMCPlugin;
import fr.openmc.core.features.tpa.TPAQueue;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Named;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.bukkit.annotation.CommandPermission;

public class TPAcceptCommand {

	@Command("tpaccept")
	@CommandPermission("omc.commands.tpa")
	public void tpAccept(Player target, @Optional @Named("player") Player player) {
		if (!TPAQueue.QUEUE.hasPendingRequest(target)) {
			MessagesManager.sendMessage(target, Component.translatable("omc.tpa.no_request"), Prefix.OPENMC, MessageType.ERROR, false);
			return;
		}

		if (TPAQueue.QUEUE.hasMultipleRequests(target)) {
			if (player == null) {
				MessagesManager.sendMessage(target, Component.translatable("omc.tpa.multiple_requests"), Prefix.OPENMC, MessageType.ERROR, false);
				return;
			}

			if (!TPAQueue.QUEUE.getRequesters(target).contains(player)) {
				MessagesManager.sendMessage(target,
						Component.translatable("omc.tpa.no_request_from", Component.text(player.getName())),
						Prefix.OPENMC, MessageType.ERROR, false);
				return;
			}
		} else {
			player = TPAQueue.QUEUE.getRequesters(target).getFirst();
		}

		if (target.getFallDistance() > 0) {
			MessagesManager.sendMessage(target, Component.translatable("omc.tpa.teleport_failed_falling"), Prefix.OPENMC, MessageType.ERROR, true);
			MessagesManager.sendMessage(player, Component.translatable("omc.tpa.teleport_failed_falling_requester"), Prefix.OPENMC, MessageType.ERROR, true);
			return;
		}

		if (!player.isOnline()) {
			MessagesManager.sendMessage(target, Component.translatable("omc.tpa.player_not_online"), Prefix.OPENMC, MessageType.ERROR, true);
			return;
		}

		if (TPAQueue.QUEUE.getTargetByRequester(player) != null) {
			if (TPAQueue.QUEUE.getTargetByRequester(player).equals(target)) teleport(player, target);
		}
	}

	private void teleport(Player requester, Player target) {
		new BukkitRunnable() {
			@Override
			public void run() {
				Location loc = target.getLocation();
				requester.sendTitle(
						PlaceholderAPI.setPlaceholders(requester, "§0%img_tp_effect%"),
						"§a§lTéléportation...",
						20, 10, 10
				);

				new BukkitRunnable() {
					@Override
					public void run() {
						requester.teleport(loc);
						MessagesManager.sendMessage(target, Component.translatable("omc.tpa.success"), Prefix.OPENMC, MessageType.SUCCESS, true);
						MessagesManager.sendMessage(requester, Component.translatable("omc.tpa.success"), Prefix.OPENMC, MessageType.SUCCESS, true);

						TPAQueue.QUEUE.removeRequest(requester, target);
					}
				}.runTaskLater(OMCPlugin.getInstance(), 10);
			}
		}.runTaskLater(OMCPlugin.getInstance(), 10);
	}
}