package fr.openmc.core.commands.admin.freeze;

import fr.openmc.core.OMCPlugin;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;

public class FreezeManager {

	public static final List<Player> FROZEN_PLAYERS = new ArrayList<>();
	private static Player player;

	public FreezeManager() {
		Bukkit.getServer().getPluginManager().registerEvents(new FreezeListener(), OMCPlugin.getInstance());
	}

	/**
	 * Freeze or unfreeze a player
	 *
	 * @param player The player who freeze/unfreeze
	 * @param target The player to freeze/unfreeze
	 */
	public static void switchFreeze(Player player, Player target) {
		FreezeManager.player = player;
		if (target == null) {
			MessagesManager.sendMessage(player,
					Component.translatable("omc.freeze.player_not_found"),
					Prefix.OPENMC, MessageType.ERROR, false);
		} else {
			if (FROZEN_PLAYERS.contains(target)) {
				target.setInvulnerable(false);
				FROZEN_PLAYERS.remove(target);

				MessagesManager.sendMessage(player,
						Component.translatable("omc.freeze.unfroze", Component.text(target.getName())),
						Prefix.OPENMC, MessageType.SUCCESS, false);

				MessagesManager.sendMessage(target,
						Component.translatable("omc.freeze.unfrozen"),
						Prefix.OPENMC, MessageType.INFO, true);
			} else {
				target.setInvulnerable(true);
				Location location = target.getLocation();
				location.setY(location.getWorld().getHighestBlockYAt(location) + 1);
				target.teleport(location);
				FROZEN_PLAYERS.add(target);

				target.sendTitle("§4Freeze", "§5Si vous vous déconnectez, vous serez banni");

				MessagesManager.sendMessage(player,
						Component.translatable("omc.freeze.froze", Component.text(target.getName())),
						Prefix.OPENMC, MessageType.SUCCESS, false);

				MessagesManager.sendMessage(target,
						Component.translatable("omc.freeze.frozen"),
						Prefix.OPENMC, MessageType.WARNING, true);
			}
		}
	}

	/**
	 * Contact the freezer to explain the reason of the disconnection
	 *
	 * @param reason The reason of the disconnection
	 */
	public static void contactFreezer(PlayerQuitEvent.QuitReason reason) {
		if (player == null) return;
		String key = switch (reason) {
			case KICKED -> "omc.freeze.quit.kicked";
			case TIMED_OUT -> "omc.freeze.quit.timeout";
			case ERRONEOUS_STATE -> "omc.freeze.quit.error";
			default -> "omc.freeze.quit.default";
		};

		MessagesManager.sendMessage(player,
				Component.translatable(key),
				Prefix.OPENMC, MessageType.INFO, true);
	}
}
