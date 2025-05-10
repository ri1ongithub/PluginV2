package fr.openmc.core.features.tpa.commands;

import fr.openmc.core.OMCPlugin;
import fr.openmc.core.features.tpa.TPAQueue;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.annotation.CommandPermission;

public class TPACommand {

	private final OMCPlugin plugin;

	public TPACommand(OMCPlugin plugin) {
		this.plugin = plugin;
	}

	@Command({"tpa", "tpask"})
	@CommandPermission("omc.commands.tpa")
	@AutoComplete("@players")
	public void tpAsk(Player player, @Named("player") Player target) {
		if (TPAQueue.QUEUE.requesterHasPendingRequest(player)) {
			MessagesManager.sendMessage(player,
					Component.translatable("omc.tpa.request_already_sent")
							.append(Component.translatable("omc.tpa.cancel_instruction")
									.clickEvent(ClickEvent.runCommand("/tpcancel"))
									.hoverEvent(HoverEvent.showText(Component.translatable("omc.tpa.cancel_hover")))),
					Prefix.OPENMC, MessageType.ERROR, true);
			return;
		}

		if (target == null) {
			MessagesManager.sendMessage(player, Component.translatable("omc.tpa.player_not_found"), Prefix.OPENMC, MessageType.ERROR, false);
			return;
		}

		if (player == target) {
			MessagesManager.sendMessage(player, Component.translatable("omc.tpa.cannot_request_self"), Prefix.OPENMC, MessageType.ERROR, false);
			return;
		}

		if (TPAQueue.QUEUE.hasPendingRequest(player)) {
			MessagesManager.sendMessage(player, Component.translatable("omc.tpa.request_pending_accept"), Prefix.OPENMC, MessageType.ERROR, true);
			return;
		}

		sendTPARequest(player, target);
	}

	private void sendTPARequest(Player player, Player target) {
		TPAQueue.QUEUE.addRequest(player, target);

		MessagesManager.sendMessage(target,
				Component.translatable("omc.tpa.incoming_request", Component.text(player.getName()))
						.append(Component.translatable("omc.tpa.accept_instruction")
								.clickEvent(ClickEvent.runCommand("/tpaccept"))
								.hoverEvent(HoverEvent.showText(Component.translatable("omc.tpa.accept_hover"))))
						.append(Component.translatable("omc.tpa.deny_instruction")
								.clickEvent(ClickEvent.runCommand("/tpdeny"))
								.hoverEvent(HoverEvent.showText(Component.translatable("omc.tpa.deny_hover")))),
				Prefix.OPENMC, MessageType.INFO, true);

		MessagesManager.sendMessage(player,
				Component.translatable("omc.tpa.request_sent", Component.text(target.getName()))
						.append(Component.translatable("omc.tpa.cancel_instruction")
								.clickEvent(ClickEvent.runCommand("/tpcancel"))
								.hoverEvent(HoverEvent.showText(Component.translatable("omc.tpa.cancel_hover")))),
				Prefix.OPENMC, MessageType.SUCCESS, true);

		new BukkitRunnable() {
			@Override
			public void run() {
				TPAQueue.QUEUE.expireRequest(player, target);
			}
		}.runTaskLater(plugin, 600);
	}
}
