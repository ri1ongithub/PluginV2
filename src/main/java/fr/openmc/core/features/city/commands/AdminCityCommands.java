package fr.openmc.core.features.city.commands;

import fr.openmc.api.cooldown.DynamicCooldownManager;
import fr.openmc.core.features.city.CPermission;
import fr.openmc.core.features.city.City;
import fr.openmc.core.features.city.CityManager;
import fr.openmc.core.features.city.CityMessages;
import fr.openmc.core.features.city.listeners.ProtectionListener;
import fr.openmc.core.features.city.mascots.MascotUtils;
import fr.openmc.core.features.city.mascots.MascotsManager;
import fr.openmc.core.features.economy.EconomyManager;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.AutoComplete;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Named;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.sql.SQLException;
import java.util.UUID;

@Command("admcity")
@CommandPermission("omc.admins.commands.admincity")
public class AdminCityCommands {
    @Subcommand("deleteCity")
    @CommandPermission("omc.admins.commands.admincity.deleteCity")
    void deleteCity(Player player, @Named("uuid") String cityUUID) {
        City city = CityManager.getCity(cityUUID);

        if (city == null) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.not_found"), Prefix.STAFF, MessageType.ERROR, false);
            return;
        }

        city.delete();
        MessagesManager.sendMessage(player, Component.translatable("omc.admcity.delete.success"), Prefix.STAFF, MessageType.SUCCESS, false);
    }

    @Subcommand("info")
    @CommandPermission("omc.admins.commands.admincity.info")
    @AutoComplete("<uuid>")
    void info(Player player, @Named("uuid") String cityUUID) {
        City city = CityManager.getCity(cityUUID);

        if (city == null) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.not_found"), Prefix.STAFF, MessageType.ERROR, false);
            return;
        }

        CityMessages.sendInfo(player, city); // Assuming CityMessages handles its own translations
    }

    @Subcommand("rename")
    @CommandPermission("omc.admins.commands.admincity.rename")
    void rename(Player player, @Named("uuid") String cityUUID, @Named("nouveau nom") String newName) {
        // Aucune vérification de nom, mais il faut espérer que le nom est valide
        City city = CityManager.getCity(cityUUID);
        if (city == null) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.not_found"), Prefix.STAFF, MessageType.ERROR, false);
            return;
        }
        city.renameCity(newName);

        MessagesManager.sendMessage(player, Component.translatable("omc.admcity.rename.success"), Prefix.STAFF, MessageType.SUCCESS, false);
    }

    @Subcommand("setOwner")
    @CommandPermission("omc.admins.commands.admincity.setOwner")
    void setOwner(Player player, @Named("uuid") String cityUUID, @Named("nouveau maire") Player newOwner) {
        City city = CityManager.getCity(cityUUID);

        if (city == null) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.not_found"), Prefix.STAFF, MessageType.ERROR, false);
            return;
        }

        city.changeOwner(newOwner.getUniqueId());
        MessagesManager.sendMessage(player, Component.translatable("omc.admcity.setowner.success"), Prefix.STAFF, MessageType.SUCCESS, false);
    }

    @Subcommand("setBalance")
    @CommandPermission("omc.admins.commands.admincity.setBalance")
    void setBalance(Player player, @Named("uuid") String cityUUID, @Named("balance") double newBalance) {
        City city = CityManager.getCity(cityUUID);
        if (city == null) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.not_found"), Prefix.STAFF, MessageType.ERROR, false);
            return;
        }

        city.setBalance(newBalance);
        MessagesManager.sendMessage(player, Component.translatable("omc.admcity.setbalance.success"), Prefix.STAFF, MessageType.SUCCESS, false);
    }

    @Subcommand("getBalance")
    @CommandPermission("omc.admins.commands.admincity.getBalance")
    void getBalance(Player player, String cityUUID) {
        City city = CityManager.getCity(cityUUID);
        if (city == null) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.not_found"), Prefix.STAFF, MessageType.ERROR, false);
            return;
        }

        MessagesManager.sendMessage(player, Component.translatable("omc.admcity.getbalance.success", Component.text(city.getBalance()), Component.text(EconomyManager.getEconomyIcon())), Prefix.STAFF, MessageType.INFO, false);
    }

    @Subcommand("add")
    @CommandPermission("omc.admins.commands.admincity.add")
    void add(Player player, @Named("uuid") String cityUUID, Player newMember) {
        City city = CityManager.getCity(cityUUID);

        if (city == null) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.not_found"), Prefix.STAFF, MessageType.ERROR, false);
            return;
        }

        if (CityManager.getPlayerCity(newMember.getUniqueId()) != null) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.player_in_city"), Prefix.STAFF, MessageType.ERROR, false);
            return;
        }

        city.addPlayer(newMember.getUniqueId());
        MessagesManager.sendMessage(player, Component.translatable("omc.admcity.add.success"), Prefix.STAFF, MessageType.SUCCESS, false);
    }

    @Subcommand("remove")
    @CommandPermission("omc.admins.commands.admincity.remove")
    void remove(Player player, @Named("uuid") String cityUUID, Player member) {
        City city = CityManager.getPlayerCity(member.getUniqueId());
        if (city == null) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.player_no_city"), Prefix.STAFF, MessageType.ERROR, false);
            return;
        }

        if (city.hasPermission(member.getUniqueId(), CPermission.OWNER)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.admcity.remove.is_owner"), Prefix.STAFF, MessageType.ERROR, false);
            return;
        }

        city.removePlayer(member.getUniqueId());
        MessagesManager.sendMessage(player, Component.translatable("omc.admcity.remove.success"), Prefix.STAFF, MessageType.SUCCESS, false);
    }

    @Subcommand("getPlayer")
    @CommandPermission("omc.admins.commands.admincity.getPlayer")
    void getPlayer(Player player, Player member) {
        City city = CityManager.getPlayerCity(member.getUniqueId());
        if (city == null) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.player_no_city"), Prefix.STAFF, MessageType.ERROR, false);
            return;
        }

        MessagesManager.sendMessage(player, Component.translatable("omc.admcity.getplayer.success", Component.text(city.getName()), Component.text(city.getUUID())), Prefix.STAFF, MessageType.INFO, false);
    }

    @Subcommand("claim bypass")
    @CommandPermission("omc.admins.commands.admincity.claim.bypass")
    public void bypass(Player player) {
        UUID uuid = player.getUniqueId();
        Boolean canBypass = ProtectionListener.playerCanBypass.get(uuid);

        if (canBypass == null || !canBypass) {
            ProtectionListener.playerCanBypass.put(uuid, true);
            MessagesManager.sendMessage(player, Component.translatable("omc.admcity.claim.bypass.toggled_on"), Prefix.STAFF, MessageType.SUCCESS, false);
        } else {
            ProtectionListener.playerCanBypass.replace(uuid, false);
            MessagesManager.sendMessage(player, Component.translatable("omc.admcity.claim.bypass.toggled_off"), Prefix.STAFF, MessageType.SUCCESS, false);
        }
    }

    @Subcommand("freeclaim add")
    @CommandPermission("omc.admins.commands.admincity.freeclaim.add")
    public void freeClaimAdd(@Named("player") Player player, @Named("claim") int claim) {
        City city = CityManager.getPlayerCity(player.getUniqueId());
        if (city==null){
            MessagesManager.sendMessage(player, Component.translatable("omc.city.not_found"), Prefix.STAFF, MessageType.ERROR, false);
            return;
        }
        if (CityManager.freeClaim.get(city.getUUID())==null){
            CityManager.freeClaim.put(city.getUUID(), claim);
            // Add success message for initial setting? Or just let add cover it?
            MessagesManager.sendMessage(player, Component.translatable("omc.admcity.freeclaim.add.success", Component.text(claim), Component.text(city.getName())), Prefix.STAFF, MessageType.SUCCESS, false);
            return;
        }
        int newClaimCount = CityManager.freeClaim.get(city.getUUID()) + claim;
        CityManager.freeClaim.replace(city.getUUID(), newClaimCount);
        MessagesManager.sendMessage(player, Component.translatable("omc.admcity.freeclaim.add.success_total", Component.text(claim), Component.text(city.getName()), Component.text(newClaimCount)), Prefix.STAFF, MessageType.SUCCESS, false);
    }

    @Subcommand("freeclaim remove")
    @CommandPermission("omc.admins.commands.admincity.freeclaim.remove")
    public void freeClaimRemove(@Named("player") Player player, @Named("claim") int claim) {
        City city = CityManager.getPlayerCity(player.getUniqueId());
        if (city==null){
            MessagesManager.sendMessage(player, Component.translatable("omc.city.not_found"), Prefix.STAFF, MessageType.ERROR, false);
            return;
        }
        if (CityManager.freeClaim.get(city.getUUID()) == null || CityManager.freeClaim.get(city.getUUID()) <= 0) {
             MessagesManager.sendMessage(player, Component.translatable("omc.city.no_free_claims"), Prefix.STAFF, MessageType.ERROR, false);
             return;
        }

        int currentClaims = CityManager.freeClaim.get(city.getUUID());
        int newClaimCount = currentClaims - claim;

        if (newClaimCount <= 0){
            CityManager.freeClaim.remove(city.getUUID());
            MessagesManager.sendMessage(player, Component.translatable("omc.admcity.freeclaim.remove.success_all", Component.text(city.getName())), Prefix.STAFF, MessageType.SUCCESS, false);
            return;
        }
        CityManager.freeClaim.replace(city.getUUID(), newClaimCount);
        MessagesManager.sendMessage(player, Component.translatable("omc.admcity.freeclaim.remove.success_total", Component.text(claim), Component.text(city.getName()), Component.text(newClaimCount)), Prefix.STAFF, MessageType.SUCCESS, false);

    }

    @Subcommand("freeclaim delete")
    @CommandPermission("omc.admins.commands.admincity.freeclaim.remove") // Permission seems to match remove
    public void freeClaimDelete(@Named("player") Player player) {
        City city = CityManager.getPlayerCity(player.getUniqueId());
        if (city==null){
            MessagesManager.sendMessage(player, Component.translatable("omc.city.not_found"), Prefix.STAFF, MessageType.ERROR, false);
            return;
        }
        if (CityManager.freeClaim.containsKey(city.getUUID())) {
             CityManager.freeClaim.remove(city.getUUID());
             MessagesManager.sendMessage(player, Component.translatable("omc.admcity.freeclaim.delete.success", Component.text(city.getName())), Prefix.STAFF, MessageType.SUCCESS, false);
        } else {
             MessagesManager.sendMessage(player, Component.translatable("omc.city.no_free_claims"), Prefix.STAFF, MessageType.ERROR, false);
        }
    }

    @Subcommand("mascots remove")
    @CommandPermission("omc.admins.commands.admcity.mascots.remove")
    public void forceRemoveMascots (Player sender, @Named("player") Player target) throws SQLException {
        City city = CityManager.getPlayerCity(target.getUniqueId());

        if (city != null){
            String city_uuid = city.getUUID();
            // The uuidList check here is redundant if we get the city from the player target.
            // If the player has a city, the city_uuid must be in the list of cities.
            // If the intention was to remove by city UUID directly, the command signature should be different.
            // Assuming the command is for the target player's city:

            if (MascotUtils.mascotsContains(city_uuid)) { // Check if the city actually has a mascot
                 MascotsManager.removeMascotsFromCity(city_uuid);
                 MessagesManager.sendMessage(sender, Component.translatable("omc.admcity.mascots.remove.success", Component.text(city.getName())), Prefix.STAFF, MessageType.SUCCESS, false);
            } else {
                 MessagesManager.sendMessage(sender, Component.translatable("omc.admcity.mascots.remove.no_mascot", Component.text(city.getName())), Prefix.STAFF, MessageType.ERROR, false);
            }

        } else {
             MessagesManager.sendMessage(sender, Component.translatable("omc.city.player_no_city"), Prefix.STAFF, MessageType.ERROR, false);
        }
    }

    @Subcommand("mascots immunityoff")
    @CommandPermission("omc.admins.commands.admcity.mascots.immunityoff")
    public void removeMascotImmunity(Player sender, @Named("player") Player target){
        City city = CityManager.getPlayerCity(target.getUniqueId());

        if (city==null){
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.player_no_city"), Prefix.STAFF, MessageType.ERROR, false);
            return;
        }

        String city_uuid = city.getUUID();

        if (!MascotUtils.mascotsContains(city_uuid)) { // Check if the city actually has a mascot
             MessagesManager.sendMessage(sender, Component.translatable("omc.admcity.mascots.immunityoff.no_mascot", Component.text(city.getName())), Prefix.STAFF, MessageType.ERROR, false);
             return;
        }

        if (!MascotUtils.getMascotImmunity(city_uuid)){ // Check if it's already NOT immune (meaning state is false)
             MessagesManager.sendMessage(sender, Component.translatable("omc.admcity.mascots.immunityoff.already_off", Component.text(city.getName())), Prefix.STAFF, MessageType.ERROR, false);
            return;
        }


        MascotUtils.changeMascotImmunity(city_uuid, false);
        DynamicCooldownManager.clear(city_uuid, "mascot:immunity"); // Clears the player cooldown, maybe need city cooldown?
        UUID mascotUUID = MascotUtils.getMascotUUIDOfCity(city_uuid); // Assuming this retrieves the actual entity UUID
        if (mascotUUID!=null){
            Entity mob = Bukkit.getEntity(mascotUUID);
            if (mob!=null) mob.setGlowing(false); // Remove glowing effect if entity is loaded
        }

        MessagesManager.sendMessage(sender, Component.translatable("omc.admcity.mascots.immunityoff.success", Component.text(city.getName())), Prefix.STAFF, MessageType.SUCCESS, false);

    }
}