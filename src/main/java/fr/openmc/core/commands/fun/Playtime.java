package fr.openmc.core.commands.fun;

import fr.openmc.core.OMCPlugin;
import fr.openmc.core.utils.DateUtils;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import net.kyori.adventure.text.Component;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.bukkit.annotation.CommandPermission;

public class Playtime {
    @Command("playtime")
    @CommandPermission("omc.commands.playtime")
    @Description("Donne votre temps de jeu")
    private void playtime(Player player) {
        long timePlayed = player.getStatistic(Statistic.PLAY_ONE_MINUTE);

        MessagesManager.sendMessage(player, Component.translatable("omc.commands.playtime.success", "Playtime : " + DateUtils.convertTime(timePlayed), Component.text(DateUtils.convertTime(timePlayed))), Prefix.OPENMC, MessageType.INFO, true);
    }
}
