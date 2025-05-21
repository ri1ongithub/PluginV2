package fr.openmc.core.features.city.commands;

import fr.openmc.core.features.city.CPermission;
import fr.openmc.core.features.city.City;
import fr.openmc.core.features.city.CityManager;
import fr.openmc.core.features.city.menu.CitizensPermsMenu;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.UUID;

@Command({"ville perms", "city perms"})
public class CityPermsCommands {
    private static boolean verification(Player sender, UUID playerUUID) {
        City city = CityManager.getPlayerCity(playerUUID);

        if (city == null) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.player_no_city"), Prefix.CITY, MessageType.ERROR, false);
            return false;
        }

        if (!city.getMembers().contains(playerUUID)) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.perms.verification.not_in_city"), Prefix.CITY, MessageType.ERROR, false);
            return false;
        }

        if (city.hasPermission(playerUUID, CPermission.OWNER)) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.perms.verification.owner_full_perms"), Prefix.CITY, MessageType.ERROR, false);
            return false;
        }

        return true;
    }

    private static boolean verificationForModif(Player sender, CPermission permission) {
        City city = CityManager.getPlayerCity(sender.getUniqueId());

        if (city == null) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.player_no_city"), Prefix.CITY, MessageType.ERROR, false);
            return false;
        }

        if (!(city.hasPermission(sender.getUniqueId(), CPermission.PERMS))) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.perms.verification_modif.no_perms_manage"), Prefix.CITY, MessageType.ERROR, false);
            return false;
        }

        if (!city.hasPermission(sender.getUniqueId(), permission) && permission == CPermission.PERMS) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.perms.verification_modif.only_mayor_can_modify"), Prefix.CITY, MessageType.ERROR, false);
            return false;
        }

        return true;
    }

    @DefaultFor("~")
    @CommandPermission("omc.commands.city.perm")
    @AutoComplete("@city_members")
    void getGUI(Player sender, @Optional OfflinePlayer member) {
        if (member == null) {
            CitizensPermsMenu.openBook(sender);
            return;
        }

        CitizensPermsMenu.openBookFor(sender, member.getUniqueId());
    }

    @Subcommand("switch")
    @CommandPermission("omc.commands.city.perm.switch")
    @Description("Inverse la permission d'un joueur")
    @AutoComplete("@city_members")
    public static void swap(Player sender, OfflinePlayer player, CPermission permission) {
        if (!verification(sender, player.getUniqueId())) return;
        if (!verificationForModif(sender, permission)) return;
        City city = CityManager.getPlayerCity(sender.getUniqueId());

        if (city == null) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.player_no_city"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (!city.getMembers().contains(player.getUniqueId())) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.perms.verification.not_in_city"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (city.hasPermission(player.getUniqueId(), permission)) {
            city.removePermission(player.getUniqueId(), permission);
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.perms.swap.lost_perm", Component.text(player.getName()), Component.text(permission.toString())), Prefix.CITY, MessageType.SUCCESS, false);
        } else {
            city.addPermission(player.getUniqueId(), permission);
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.perms.swap.gained_perm", Component.text(player.getName()), Component.text(permission.toString())), Prefix.CITY, MessageType.SUCCESS, false);
        }
    }

    @Subcommand("add")
    @CommandPermission("omc.commands.city.perm.add")
    @Description("Ajouter des permissions à un membre")
    @AutoComplete("@city_members")
    void add(Player sender, OfflinePlayer player, CPermission permission) {
        if (!verification(sender, player.getUniqueId())) return;
        if (!verificationForModif(sender, permission)) return;
        City city = CityManager.getPlayerCity(sender.getUniqueId());

        if (city == null) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.player_no_city"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (!city.getMembers().contains(player.getUniqueId())) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.perms.verification.not_in_city"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (city.hasPermission(player.getUniqueId(), permission)) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.perms.add.already_has", Component.text(player.getName())), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        city.addPermission(player.getUniqueId(), permission);
        MessagesManager.sendMessage(sender, Component.translatable("omc.city.perms.add.success", Component.text(player.getName())), Prefix.CITY, MessageType.SUCCESS, false);
    }

    @Subcommand("remove")
    @CommandPermission("omc.commands.city.perm.remove")
    @Description("Retirer des permissions à un membre")
    @AutoComplete("@city_members")
    void remove(Player sender, OfflinePlayer player, CPermission permission) {
        if (!verification(sender, player.getUniqueId())) return;
        if (!verificationForModif(sender, permission)) return;
        City city = CityManager.getPlayerCity(sender.getUniqueId());

        if (city == null) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.player_no_city"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (!city.getMembers().contains(player.getUniqueId())) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.perms.verification.not_in_city"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (!city.hasPermission(player.getUniqueId(), permission)) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.perms.remove.does_not_have", Component.text(player.getName())), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        city.removePermission(player.getUniqueId(), permission);
        MessagesManager.sendMessage(sender, Component.translatable("omc.city.perms.add.success", Component.text(player.getName())), Prefix.CITY, MessageType.SUCCESS, false);
    }


    @Subcommand("get")
    @CommandPermission("omc.commands.city.perm.get")
    @Description("Obtenir les permissions d'un membre")
    @AutoComplete("@city_members")
    void get(Player sender, OfflinePlayer player) {
        if (!verification(sender, player.getUniqueId())) return;
        City city = CityManager.getPlayerCity(sender.getUniqueId());

        if (city == null) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.player_no_city"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (!city.getMembers().contains(player.getUniqueId())) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.perms.verification.not_in_city"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (!(city.hasPermission(sender.getUniqueId(), CPermission.PERMS))) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.perms.get.no_permission_view_other", Component.text(player.getName())), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (!city.getMembers().contains(player.getUniqueId())) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.perms.verification.not_in_city"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        Component content = Component.text(player.getName()).decorate(TextDecoration.UNDERLINED).append(Component.newline());

        for (CPermission permission : CPermission.values()) {
            if (city.hasPermission(player.getUniqueId(), permission)) {
                content = content.append(Component.text(permission.toString()).append(Component.newline()));
            }
        }

        sender.openBook(Book.book(
                Component.translatable("omc.city.perms.get.book_title", Component.text(player.getName())).decorate(TextDecoration.BOLD),
                Component.text(""),
                content
        ));
    }
}