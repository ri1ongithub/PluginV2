package fr.openmc.core.features.city.commands;

import fr.openmc.core.features.city.CPermission;
import fr.openmc.core.features.city.City;
import fr.openmc.core.features.city.CityManager;
import fr.openmc.core.features.city.menu.ChestMenu;
import fr.openmc.core.features.economy.EconomyManager;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.annotation.CommandPermission;

public class CityChestCommand {
    @Command({"city chest", "ville coffre"})
    @Description("Ouvre le coffre de la ville")
    @CommandPermission("omc.commands.city.chest")
    void chest(Player player, @Optional @Named("page") @Range(min=0) Integer page) {
        City city = CityManager.getPlayerCity(player.getUniqueId());
        if (city == null) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.player_no_city"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (!city.hasPermission(player.getUniqueId(), CPermission.CHEST)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.chest.no_permission_view"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (city.getChestWatcher() != null) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.chest.already_open"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if ((page == null)) page = 1;
        if (page < 1) page = 1;
        if (page > city.getChestPages()) page = city.getChestPages();

        new ChestMenu(city, page).open(player);
    }

    @Command({"city upgradechest", "ville upgradecoffre"})
    @Description("Améliore la coffre de la ville")
    @CommandPermission("omc.commands.city.chest_upgrade")
    void upgrade(Player player) {
        City city = CityManager.getPlayerCity(player.getUniqueId());
        if (city == null) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.player_no_city"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (!city.hasPermission(player.getUniqueId(), CPermission.CHEST_UPGRADE)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.chest.no_permission_upgrade"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (city.getChestPages() >= 5) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.chest.max_level"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        int price = city.getChestPages()*5000; // fonction linéaire f(x)=ax ; a=5000
        if (city.getBalance() < price) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.chest.not_enough_money", Component.text(price), Component.text(EconomyManager.getEconomyIcon())), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        city.updateBalance((double) -price);

        city.upgradeChest();
        MessagesManager.sendMessage(player, Component.translatable("omc.city.chest.upgrade.success"), Prefix.CITY, MessageType.SUCCESS, false);
    }
}