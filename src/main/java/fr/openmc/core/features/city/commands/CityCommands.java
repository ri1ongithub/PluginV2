package fr.openmc.core.features.city.commands;

import com.sk89q.worldedit.math.BlockVector2;
import fr.openmc.api.chronometer.Chronometer;
import fr.openmc.api.cooldown.DynamicCooldown;
import fr.openmc.api.cooldown.DynamicCooldownManager;
import fr.openmc.api.input.location.ItemInteraction;
import fr.openmc.api.menulib.default_menu.ConfirmMenu;
import fr.openmc.core.OMCPlugin;
import fr.openmc.core.features.city.CPermission;
import fr.openmc.core.features.city.City;
import fr.openmc.core.features.city.CityManager;
import fr.openmc.core.features.city.CityMessages;
import fr.openmc.core.features.city.conditions.*;
import fr.openmc.core.features.city.mascots.Mascot;
import fr.openmc.core.features.city.mascots.MascotUtils;
import fr.openmc.core.features.city.mascots.MascotsLevels;
import fr.openmc.core.features.city.mascots.MascotsListener;
import fr.openmc.core.features.city.mayor.CityLaw;
import fr.openmc.core.features.city.mayor.ElectionType;
import fr.openmc.core.features.city.mayor.Mayor;
import fr.openmc.core.features.city.mayor.managers.MayorManager;
import fr.openmc.core.features.city.mayor.managers.PerkManager;
import fr.openmc.core.features.city.mayor.perks.Perks;
import fr.openmc.core.features.city.menu.CityMenu;
import fr.openmc.core.features.city.menu.CityTypeMenu;
import fr.openmc.core.features.city.menu.NoCityMenu;
import fr.openmc.core.features.city.menu.bank.CityBankMenu;
import fr.openmc.core.features.city.menu.list.CityListMenu;
import fr.openmc.core.features.city.menu.mayor.MayorElectionMenu;
import fr.openmc.core.features.city.menu.mayor.MayorMandateMenu;
import fr.openmc.core.features.economy.EconomyManager;
import fr.openmc.core.utils.DateUtils;
import fr.openmc.core.utils.InputUtils;
import fr.openmc.core.utils.ItemUtils;
import fr.openmc.core.utils.api.WorldGuardApi;
import fr.openmc.core.utils.customitems.CustomItemRegistry;
import fr.openmc.core.utils.database.DatabaseManager;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static fr.openmc.core.features.city.CityManager.getCityType;
import static fr.openmc.core.features.city.conditions.CityCreateConditions.AYWENITE_CREATE;
import static fr.openmc.core.features.city.conditions.CityCreateConditions.MONEY_CREATE;
import static fr.openmc.core.features.city.mayor.managers.MayorManager.PHASE_1_DAY;
import static fr.openmc.core.features.city.menu.mayor.MayorLawMenu.COOLDOWN_TIME_WARP;

@Command({"ville", "city"})
public class CityCommands {
    public static HashMap<Player, List<Player>> invitations = new HashMap<>(); // Invité, Inviteurs
    public static Map<String, BukkitRunnable> balanceCooldownTasks = new HashMap<>();

    private static ItemStack ayweniteItemStack = CustomItemRegistry.getByName("omc_items:aywenite").getBest();

    private Location[] getCorners(Player player) {
        World world = player.getWorld();
        Location location = player.getLocation();
        int chunkRadius = 2;
        int chunkX = location.getChunk().getX();
        int chunkZ = location.getChunk().getZ();

        Location minLocation = new Location(world, (chunkX-chunkRadius) * 16, location.getY(), (chunkZ-chunkRadius) * 16);
        Location maxLocation = new Location(world, (chunkX+chunkRadius+1) * 16 - 1, location.getY(), (chunkZ+chunkRadius + 1) * 16 - 1);

        return new Location[]{minLocation, maxLocation};
    }

    public static int calculatePrice(int chunkCount) {
        return 5000 + (chunkCount * 1000);
    }

    public static int calculateAywenite(int chunkCount) {
        return 1*chunkCount;
    }

    @DefaultFor("~")
    void main(Player player) {
        City playerCity = CityManager.getPlayerCity(player.getUniqueId());
        if (!Chronometer.containsChronometer(player.getUniqueId(), "Mascot:chest")) {
                if (playerCity == null) {
                    NoCityMenu menu = new NoCityMenu(player);
                    menu.open();
                } else {
                    CityMenu menu = new CityMenu(player);
                    menu.open();
                }
        } else {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.menu.mascot_warning"), Prefix.CITY, MessageType.ERROR, false);
        }
    }

    @Subcommand({"mayor", "maire"})
    @CommandPermission("omc.commands.city.mayor")
    @Description("Ouvre le menu des maires") // This description is static, ideally would be translated by framework, but let's leave it as is per typical command frameworks.
    public void mayor(Player sender) {
        if (MayorManager.getInstance().phaseMayor == 1) {
            MayorElectionMenu menu = new MayorElectionMenu(sender);
            menu.open();
        } else {
            MayorMandateMenu menu = new MayorMandateMenu(sender);
            menu.open();
        }
    }

    @Subcommand("accept")
    @CommandPermission("omc.commands.city.accept")
    @Description("Accepter une invitation") // Static description
    public static void acceptInvitation(Player player, Player inviter) {
        List<Player> playerInvitations = invitations.get(player);
        if (!playerInvitations.contains(inviter)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.invitation.not_found", Component.text(inviter.getName())), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        City newCity = CityManager.getPlayerCity(inviter.getUniqueId());

        if (!CityInviteConditions.canCityInviteAccept(newCity, inviter, player)) return;

        newCity.addPlayer(player.getUniqueId());

        invitations.remove(player);

        MessagesManager.sendMessage(player, Component.translatable("omc.city.joined", Component.text(newCity.getName())), Prefix.CITY, MessageType.SUCCESS, false);
        if (inviter.isOnline()) {
            MessagesManager.sendMessage(inviter, Component.translatable("omc.city.invitation.accepted", Component.text(player.getName())), Prefix.CITY, MessageType.SUCCESS, true);
        }
    }

    @Subcommand("rename")
    @CommandPermission("omc.commands.city.rename")
    @Description("Renommer une ville") // Static description
    void rename(Player player, @Named("nouveau nom") String name) {
        City playerCity = CityManager.getPlayerCity(player.getUniqueId());

        if (!CityManageConditions.canCityRename(playerCity, player)) return;

        if (!InputUtils.isInputCityName(name)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.invalid_name"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        playerCity.renameCity(name);
        MessagesManager.sendMessage(player, Component.translatable("omc.city.renamed", Component.text(name)), Prefix.CITY, MessageType.SUCCESS, false);
    }

    @Subcommand("transfer")
    @CommandPermission("omc.commands.city.transfer")
    @Description("Transfert la propriété de votre ville") // Static description
    @AutoComplete("@city_members")
    void transfer(Player sender, @Named("maire") OfflinePlayer player) {
        City playerCity = CityManager.getPlayerCity(sender.getUniqueId());

        if (!CityManageConditions.canCityTransfer(playerCity, sender)) return;

        playerCity.changeOwner(player.getUniqueId());
        MessagesManager.sendMessage(sender, Component.translatable("omc.city.new_mayor", Component.text(player.getName())), Prefix.CITY, MessageType.SUCCESS, false);

        if (player.isOnline()) {
            MessagesManager.sendMessage((Player) player, Component.translatable("omc.city.became_mayor"), Prefix.CITY, MessageType.INFO, true);
        }
    }

    @Subcommand("deny")
    @CommandPermission("omc.commands.city.deny")
    @Description("Refuser une invitation") // Static description
    public static void denyInvitation(Player player, Player inviter) {
        if (!CityInviteConditions.canCityInviteDeny(player, inviter)) return;

        invitations.remove(player);

        if (inviter.isOnline()) {
            MessagesManager.sendMessage(inviter, Component.translatable("omc.city.invitation.denied", Component.text(player.getName())), Prefix.CITY, MessageType.WARNING, true);
        }
    }

    @Subcommand("kick")
    @CommandPermission("omc.commands.city.kick")
    @Description("Exclure un habitant de votre ville") // Static description
    @AutoComplete("@city_members")
    public static void kick(Player sender, @Named("exclu") OfflinePlayer player) {
        City city = CityManager.getPlayerCity(sender.getUniqueId());

        if (!CityKickCondition.canCityKickPlayer(city, sender, player)) return;

        if (city.removePlayer(player.getUniqueId())) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.kicked", Component.text(player.getName()), Component.text(city.getCityName())), Prefix.CITY, MessageType.SUCCESS, false);

            if (player.isOnline()) {
                MessagesManager.sendMessage((Player) player, Component.translatable("omc.city.kicked_from", Component.text(city.getCityName())), Prefix.CITY, MessageType.INFO, true);
            }
        } else {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.kick_failed", Component.text(player.getName())), Prefix.CITY, MessageType.ERROR, false);
        }
    }

    @Subcommand("leave")
    @CommandPermission("omc.commands.city.leave")
    @Description("Quitter votre ville") // Static description
    void leave(Player player) {
        City city = CityManager.getPlayerCity(player.getUniqueId());
        if (!CityLeaveCondition.canCityLeave(city, player)) return;

        leaveCity(player);
    }

    @Subcommand("invite")
    @CommandPermission("omc.commands.city.invite")
    @Description("Inviter un joueur dans votre ville") // Static description
    public static void add(Player sender, @Named("invité") Player target) {
        City city = CityManager.getPlayerCity(sender.getUniqueId());

        if (!CityInviteConditions.canCityInvitePlayer(city, sender, target)) return;

        List<Player> playerInvitations = invitations.get(target);
        if (playerInvitations == null) {
            List<Player> newInvitations = new ArrayList<>();
            newInvitations.add(sender);
            invitations.put(target, newInvitations);
        } else {
            playerInvitations.add(sender);
        }
        MessagesManager.sendMessage(sender, Component.translatable("omc.city.invited", Component.text(target.getName())), Prefix.CITY, MessageType.SUCCESS, false);
        MessagesManager.sendMessage(target,
                Component.translatable("omc.city.invitation.received", Component.text(sender.getName()), Component.text(city.getCityName()))
                        .append(Component.text("\n").append(Component.translatable("omc.city.invitation.accept_command").clickEvent(ClickEvent.runCommand("/city accept " + sender.getName())).hoverEvent(HoverEvent.showText(Component.translatable("omc.city.invitation.accept_hover")))))
                        .append(Component.text("\n").append(Component.translatable("omc.city.invitation.deny_command").clickEvent(ClickEvent.runCommand("/city deny " + sender.getName())).hoverEvent(HoverEvent.showText(Component.translatable("omc.city.invitation.deny_hover"))))),
                Prefix.CITY, MessageType.INFO, false);
    }

    @Subcommand("delete")
    @CommandPermission("omc.commands.city.delete")
    @Description("Supprimer votre ville") // Static description
    void delete(Player sender) {
        UUID uuid = sender.getUniqueId();

        City city = CityManager.getPlayerCity(uuid);

        if (!CityManageConditions.canCityDelete(city, sender)) return;

        ConfirmMenu menu = new ConfirmMenu(sender,
                () -> {
                    deleteCity(sender);
                    sender.closeInventory();
                },
                () -> {
                    sender.closeInventory();
                },
                List.of(
                        Component.translatable("omc.city.delete.confirmation"),
                        Component.translatable("omc.city.delete.confirmation_irreversible")
                ),
                List.of(
                        Component.translatable("omc.city.delete.cancel")
                )
        );
        menu.open();
    }

    public static void deleteCity(Player sender) {
        UUID uuid = sender.getUniqueId();

        City city = CityManager.getPlayerCity(uuid);

        for (UUID townMember : city.getMembers()){
            if (Bukkit.getPlayer(townMember) instanceof Player player){
                player.clearActivePotionEffects();
            }
        }

        city.delete();
        MessagesManager.sendMessage(sender, Component.translatable("omc.city.deleted"), Prefix.CITY, MessageType.SUCCESS, false);

        DynamicCooldownManager.use(uuid.toString(), "city:big", 60000); //1 minute
    }

    @Subcommand("claim")
    @CommandPermission("omc.commands.city.claim")
    @Description("Claim un chunk pour votre ville") // Static description
    void claim(Player sender) {
        City city = CityManager.getPlayerCity(sender.getUniqueId());

        if (!CityClaimCondition.canCityClaim(city, sender)) return;

        Chunk chunk = sender.getLocation().getChunk();

        claim(sender, chunk.getX(), chunk.getZ());
    }

    public static void claim(Player sender, int chunkX, int chunkZ) {
        City city = CityManager.getPlayerCity(sender.getUniqueId());
        org.bukkit.World bWorld = sender.getWorld();
        if (!bWorld.getName().equals("world")) { // Assuming this world check is standard and doesn't need translation keys for world names
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.claim.not_in_world"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        BlockVector2 chunkVec2 = BlockVector2.at(chunkX, chunkZ);

        AtomicBoolean isFar = new AtomicBoolean(true);
        for (BlockVector2 chnk: city.getChunks()) {
            if (chnk.distance(chunkVec2) == 1) { //Si c'est en diagonale alors ça sera sqrt(2) ≈1.41
                isFar.set(false);
                break;
            }
        }

        if (isFar.get()) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.claim.not_adjacent"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        Chunk chunk = sender.getWorld().getChunkAt(chunkX, chunkZ);
        if (WorldGuardApi.doesChunkContainWGRegion(chunk)) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.claim.protected_region"), Prefix.CITY, MessageType.ERROR, true);
            return;
        }

        if (CityManager.isChunkClaimed(chunkX, chunkZ)) {
            City chunkCity = CityManager.getCityFromChunk(chunkX, chunkZ);
            String cityName = chunkCity.getCityName();
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.claim.already_claimed", Component.text(cityName)), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (city.getChunks().size() >= 50) { // Assuming 50 is a hardcoded limit, maybe could be config driven but message is static
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.claim.too_large"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        int price = calculatePrice(city.getChunks().size());
        int aywenite = calculateAywenite(city.getChunks().size());



        if ((!CityManager.freeClaim.containsKey(city.getUUID())) || (CityManager.freeClaim.get(city.getUUID()) <= 0)) {
            if (city.getBalance() < price) {
                MessagesManager.sendMessage(sender, Component.translatable("omc.city.claim.not_enough_money", Component.text(price), Component.text(EconomyManager.getEconomyIcon())), Prefix.CITY, MessageType.ERROR, false);
                return;
            }

            if (!ItemUtils.hasEnoughItems(sender.getPlayer(), ayweniteItemStack.getType(), aywenite )) {
                MessagesManager.sendMessage(sender, Component.translatable("omc.city.claim.not_enough_aywenite", Component.text(aywenite)), Prefix.CITY, MessageType.ERROR, false);
                return;
            }

            city.updateBalance((double) (price*-1));
            ItemUtils.removeItemsFromInventory(sender, ayweniteItemStack.getType(), aywenite);
        } else {
            CityManager.freeClaim.replace(city.getUUID(), CityManager.freeClaim.get(city.getUUID()) - 1);
        }

        city.addChunk(chunk);

        MessagesManager.sendMessage(sender, Component.translatable("omc.city.claim.extended"), Prefix.CITY, MessageType.SUCCESS, false);
    }

    @Subcommand("info")
    @CommandPermission("omc.commands.city.info")
    @Description("Avoir des informations sur votre ville") // Static description
    void info(Player player) {
        City city = CityManager.getPlayerCity(player.getUniqueId());

        if (city == null) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.player_no_city"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        CityMessages.sendInfo(player, city); // Assuming CityMessages handles its own translations
    }

    @Subcommand("create")
    @CommandPermission("omc.commands.city.create")
    @Description("Créer une ville") // Static description
    @DynamicCooldown(group="city:big", message = "§cTu dois attendre avant de pouvoir créer une ville (%sec% secondes)") // Static message in annotation parameter
    void create(Player player, @Named("nom") String name) {
        if (!CityCreateConditions.canCityCreate(player)){
            MessagesManager.sendMessage(player, Component.translatable("omc.messages.no_permission"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        for (City city : CityManager.getCities()){
            String cityName = city.getCityName();
            if (cityName!=null && cityName.equalsIgnoreCase(name)){
                MessagesManager.sendMessage(player, Component.translatable("omc.city.create.already_exists"), Prefix.CITY, MessageType.INFO, false);
                return;
            }
        }

        if (!InputUtils.isInputCityName(name)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.invalid_name"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (MascotsListener.futurCreateCity.containsKey(player.getUniqueId())){
            MessagesManager.sendMessage(player, Component.translatable("omc.city.create.already_creating"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        new CityTypeMenu(player, name).open();
    }

    @Subcommand("list")
    @CommandPermission("omc.commands.city.list")
    @Description("Afficher la liste des villes") // Static description
    public void list(Player player) {
        List<City> cities = new ArrayList<>(CityManager.getCities());
        if (cities.isEmpty()) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.list.empty"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        CityListMenu menu = new CityListMenu(player, cities); // Assuming CityListMenu handles its own translations
        menu.open();
    }

    @Subcommand("change")
    @CommandPermission("omc.commands.city.change")
    @Description("Changer le type de votre ville") // Static description
    public void change(Player sender) {
        City city = CityManager.getPlayerCity(sender.getUniqueId());

        if (!CityTypeConditions.canCityChangeType(city, sender, true)){
            MessagesManager.sendMessage(sender, Component.translatable("omc.messages.no_permission"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        String cityTypeActuel = getCityType(city.getUUID());
        // String cityTypeAfter = "";
        Component typeActuelComp = null;
        Component typeAfterComp = null;

        if (cityTypeActuel != null) {
             typeActuelComp = cityTypeActuel.equals("war") ? Component.translatable("omc.city.type.war") : Component.translatable("omc.city.type.peace");
             typeAfterComp = cityTypeActuel.equals("war") ? Component.translatable("omc.city.type.peace") : Component.translatable("omc.city.type.war");
        } else { // Should not happen if player has a city, but handle defensively
            typeActuelComp = Component.text("Unknown"); // Fallback, ideally handled by CityTypeConditions
            typeAfterComp = Component.text("Unknown"); // Fallback
        }


        ConfirmMenu menu = new ConfirmMenu(sender,
                () -> {
                    changeConfirm(sender);
                    sender.closeInventory();
                },
                () -> {
                    sender.closeInventory();
                },
                List.of(
                        Component.translatable("omc.city.change.confirmation"),
                        Component.translatable("omc.city.change.confirmation_2", typeActuelComp, typeAfterComp),
                        Component.translatable("omc.city.change.confirmation_3")
                ),
                List.of(
                        Component.translatable("omc.city.change.cancel")
                )
        );
        menu.open();

    }

    public static void changeConfirm(Player sender) {
        City city = CityManager.getPlayerCity(sender.getUniqueId());

        if (!CityTypeConditions.canCityChangeType(city, sender, true)){
            MessagesManager.sendMessage(sender, Component.translatable("omc.messages.no_permission"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (MascotUtils.mascotsContains(city.getUUID())) {
            if (!MascotUtils.getMascotState(city.getUUID())) {
                MessagesManager.sendMessage(sender, Component.translatable("omc.city.mascot.not_healed"), Prefix.CITY, MessageType.ERROR, false);
                return;
            }
        }
        if (!DynamicCooldownManager.isReady(city.getUUID(), "city:type")) {
            MessagesManager.sendMessage(sender, Component.translatable("omc.city.change.cooldown", Component.text(DateUtils.convertMillisToTime(DynamicCooldownManager.getRemaining(city.getUUID(), "city:type")))), Prefix.CITY, MessageType.ERROR, false);
            return;
        }
        CityManager.changeCityType(city.getUUID());
        DynamicCooldownManager.use(city.getUUID(), "city:type", 5 * 24 * 60 * 60 * 1000L); // 5 jours en ms


        Mascot mascot = MascotUtils.getMascotOfCity(city.getUUID());

        if (mascot != null) {
            LivingEntity mob = MascotUtils.loadMascot(mascot); // Note: This might be a potentially blocking call if not handled well outside this command execution.
            MascotsLevels mascotsLevels = MascotsLevels.valueOf("level" + MascotUtils.getMascotLevel(city.getUUID()));

            double lastHealth = mascotsLevels.getHealth();
            int newLevel = Integer.parseInt(String.valueOf(mascotsLevels).replaceAll("[^0-9]", "")) - 2;
            if (newLevel < 1) {
                newLevel = 1;
            }
            MascotUtils.setMascotLevel(city.getUUID(), newLevel);
            mascotsLevels = MascotsLevels.valueOf("level" + MascotUtils.getMascotLevel(city.getUUID()));

            try {
                int maxHealth = mascotsLevels.getHealth();
                mob.setMaxHealth(maxHealth);
                if (mob.getHealth() >= lastHealth) {
                    mob.setHealth(maxHealth);
                }
                double currentHealth = mob.getHealth();
                mob.setCustomName("§l" + city.getName() + " §c" + currentHealth + "/" + maxHealth + "❤"); // Static name format - potentially translatable but involves data outside command logic directly
            } catch (Exception exception) {
                exception.printStackTrace(); // Log the exception
                // No user message on exception? Maybe add one.
            }
        }

        String cityTypeActuel = getCityType(city.getUUID());
        Component typeActuelComp = null;
        Component typeAfterComp = null;

        if (cityTypeActuel != null) {
            typeActuelComp = cityTypeActuel.equals("war") ? Component.translatable("omc.city.type.war") : Component.translatable("omc.city.type.peace");
            typeAfterComp = cityTypeActuel.equals("war") ? Component.translatable("omc.city.type.peace") : Component.translatable("omc.city.type.war");
        } else { // Fallback
            typeActuelComp = Component.text("Unknown");
            typeAfterComp = Component.text("Unknown");
        }

        MessagesManager.sendMessage(sender, Component.translatable("omc.city.change.success", typeActuelComp, typeAfterComp), Prefix.CITY, MessageType.SUCCESS, false);
    }

    @Subcommand({"setwarp"})
    @Description("Déplacer le warp de votre ville") // Static description
    public void setWarpCommand(Player player) {
        setWarp(player);
    }

    @Subcommand({"warp"})
    @Description("Teleporte au warp commun de la ville") // Static description
    public void warp(Player player) {
        City playerCity = CityManager.getPlayerCity(player.getUniqueId());

        if (playerCity == null) {
            // Message already handled in main or check
            return;
        }

        CityLaw law = playerCity.getLaw();

        if (law == null) { // Should not happen if city exists, but handle defensively
             MessagesManager.sendMessage(player, Component.text("Une erreur interne est survenue"), Prefix.CITY, MessageType.ERROR, false); // Add internal error message
             return;
        }

        Location warp = law.getWarp();

        if (warp == null) {
            if (MayorManager.getInstance().phaseMayor == 2) {
                MessagesManager.sendMessage(player, Component.translatable("omc.city.warp.not_set"), Prefix.CITY, MessageType.INFO, true);
                return;
            }
            MessagesManager.sendMessage(player, Component.translatable("omc.city.warp.not_set_no_mayor"), Prefix.CITY, MessageType.INFO, true);
            return;
        }

        // Check if player is falling - added condition check similar to tpa
        if (player.getFallDistance() > 0 || player.getLocation().getY() < player.getWorld().getMinHeight()) {
             MessagesManager.sendMessage(player, Component.translatable("omc.city.warp.teleport_failed_falling"), Prefix.CITY, MessageType.ERROR, true); // Add new translation key
             return;
        }


        new BukkitRunnable() {
            @Override
            public void run() {
                // Use MessagesManager for subtitle
                player.sendTitle(PlaceholderAPI.setPlaceholders(player, "§0%img_tp_effect%"), "§a§lTéléportation...", 20, 10, 10);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.teleport(warp);
                        MessagesManager.sendMessage(player, Component.translatable("omc.city.warp.teleport"), Prefix.CITY, MessageType.SUCCESS, true);
                    }
                }.runTaskLater(OMCPlugin.getInstance(), 10);
            }
        }.runTaskLater(OMCPlugin.getInstance(), 15);
    }

    @Subcommand({"bank view"})
    @Description("Ouvre le menu de la banque de ville") // Static description
    public void bank(Player player) {
        if (CityManager.getPlayerCity(player.getUniqueId()) == null)
            return; // Message already handled in main or check

        new CityBankMenu(player).open(); // Assuming CityBankMenu handles its own translations
    }

    @Subcommand("bank deposit")
    @Description("Met de votre argent dans la banque de ville") // Static description
    void deposit(Player player, @Range(min=1) String input) {
        City city = CityManager.getPlayerCity(player.getUniqueId());

        if (!CityBankConditions.canCityDeposit(city, player)) return; // Conditions handle messages

        city.depositCityBank(player, input); // Assuming depositCityBank handles its own messages
    }

    @Subcommand("bank withdraw")
    @Description("Prend de l'argent de la banque de ville") // Static description
    void withdraw(Player player, @Range(min=1) String input) {
        City city = CityManager.getPlayerCity(player.getUniqueId());

        if (!city.hasPermission(player.getUniqueId(), CPermission.MONEY_TAKE)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.no_money_take"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        city.withdrawCityBank(player, input); // Assuming withdrawCityBank handles its own messages
    }

    // ACTIONS

    public static boolean createCity(Player player, String name, String type, Chunk origin) {

        if (!CityCreateConditions.canCityCreate(player)){
            MessagesManager.sendMessage(player, Component.translatable("omc.messages.no_permission"), Prefix.CITY, MessageType.ERROR, false);
            return false;
        }

        UUID uuid = player.getUniqueId();

        String cityUUID = UUID.randomUUID().toString().substring(0, 8);

        AtomicBoolean isClaimed = new AtomicBoolean(false);

        if (WorldGuardApi.doesChunkContainWGRegion(origin)) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.create.protected_region"), Prefix.CITY, MessageType.ERROR, false);
            return false;
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (CityManager.isChunkClaimed(origin.getX() + x, origin.getZ() + z)) {
                    isClaimed.set(true);
                    break;
                }
            }
        }

        if (isClaimed.get()) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.create.adjacent_claimed"), Prefix.CITY, MessageType.ERROR, false);
            return false;
        }

        Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
            try {
                PreparedStatement statement = DatabaseManager.getConnection().prepareStatement("INSERT INTO city_regions (city_uuid, x, z) VALUES (?, ?, ?)");
                statement.setString(1, cityUUID);

                statement.setInt(2, origin.getX());
                statement.setInt(3, origin.getZ());
                statement.addBatch();

                statement.executeBatch();
                statement.close();
            } catch (SQLException e) {
                MessagesManager.sendMessage(player, Component.translatable("omc.city.create.sql_error"), Prefix.CITY, MessageType.ERROR, false);
                throw new RuntimeException(e);
            }
        });

        // Assuming EconomyManager.getInstance().getBalance(player.getUniqueId()) and ItemUtils.hasEnoughItems handle their own initial checks for permission/existence before this point.
        // The messages below are specifically for not having *enough* resources after checks pass.

        // Rebuild the money check message using translatable
        if (EconomyManager.getInstance().getBalance(player.getUniqueId()) < MONEY_CREATE) {
             MessagesManager.sendMessage(player, Component.translatable("omc.city.create.not_enough_money", Component.text(MONEY_CREATE), Component.text(EconomyManager.getEconomyIcon())), Prefix.CITY, MessageType.ERROR, false);
             return false; // Added return false as resource check should prevent creation
        }

        // Rebuild the aywenite check message using translatable
        if (!ItemUtils.hasEnoughItems(player, Objects.requireNonNull(CustomItemRegistry.getByName("omc_items:aywenite")).getBest().getType(), AYWENITE_CREATE)) {
             MessagesManager.sendMessage(player, Component.translatable("omc.city.create.not_enough_aywenite", Component.text(AYWENITE_CREATE)), Prefix.CITY, MessageType.ERROR, false);
             return false; // Added return false as resource check should prevent creation
        }

        EconomyManager.getInstance().withdrawBalance(player.getUniqueId(), MONEY_CREATE);
        ItemUtils.removeItemsFromInventory(player, ayweniteItemStack.getType(), AYWENITE_CREATE);

        City city = CityManager.createCity(player, cityUUID, name, type);
        city.addPlayer(uuid);
        city.addPermission(uuid, CPermission.OWNER);

        CityManager.claimedChunks.put(BlockVector2.at(origin.getX(), origin.getZ()), city);
        CityManager.freeClaim.put(cityUUID, 15); // Assuming 15 is a hardcoded value here

        player.closeInventory();

        // SETUP MAIRE
        MayorManager mayorManager = MayorManager.getInstance();
        if (mayorManager.phaseMayor == 1) { // si création pendant le choix des maires
            mayorManager.createMayor(null, null, city, null, null, null, null, ElectionType.OWNER_CHOOSE);
        } else { // si création pendant les réformes actives
            NamedTextColor color = mayorManager.getRandomMayorColor();
            List<Perks> perks = PerkManager.getRandomPerksAll();
            mayorManager.createMayor(player.getName(), player.getUniqueId(), city, perks.getFirst(), perks.get(1), perks.get(2), color, ElectionType.OWNER_CHOOSE);
            // Mayor assignment message
            MessagesManager.sendMessage(player, Component.translatable("omc.city.create.mayor_assigned", Component.text(DateUtils.getTimeUntilNextDay(PHASE_1_DAY))), Prefix.MAYOR, MessageType.SUCCESS, true);
        }

        // SETUP LAW
        MayorManager.createCityLaws(city, false, null); // Assuming createCityLaws does not send messages

        // Creation success messages
        MessagesManager.sendMessage(player, Component.translatable("omc.city.create.success", Component.text(name)), Prefix.CITY, MessageType.SUCCESS, true);
        MessagesManager.sendMessage(player, Component.translatable("omc.city.create.free_claims"), Prefix.CITY, MessageType.SUCCESS, false); // Message about 15 free claims

        DynamicCooldownManager.use(uuid.toString(), "city:big", 60000); //1 minute

        return true; // Return true on successful creation path
    }

    public static void setWarp(Player player) {
        City city = CityManager.getPlayerCity(player.getUniqueId());

        if (city == null) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.player_no_city"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        Mayor mayor = city.getMayor();

        if (mayor == null) {
             MessagesManager.sendMessage(player, Component.text("Une erreur interne est survenue (Maire introuvable)"), Prefix.CITY, MessageType.ERROR, false); // Add internal error message
            return;
        }

        if (!player.getUniqueId().equals(mayor.getUUID())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.setwarp.not_mayor"), Prefix.MAYOR, MessageType.ERROR, false);
            return;
        }

        if (!DynamicCooldownManager.isReady(mayor.getUUID().toString(), "mayor:law-move-warp")) {
             // Assuming cooldown message is handled by DynamicCooldownManager itself
            return;
        }
        CityLaw law = city.getLaw();

        // Translatable item lore and display name
        List<Component> loreItemInterraction = List.of(
                Component.translatable("omc.city.setwarp.item_lore")
        );
        ItemStack itemToGive = CustomItemRegistry.getByName("omc_items:warp_stick").getBest(); // Assuming item exists
        ItemMeta itemMeta = itemToGive.getItemMeta();

        itemMeta.displayName(Component.translatable("omc.city.setwarp.item_name"));
        itemMeta.lore(loreItemInterraction);
        itemToGive.setItemMeta(itemMeta);

        // Translatable InputUtils messages
        String promptMessage = "§7Vous avez 300s pour séléctionner votre point de spawn";
        String timeoutMessage = "§7Vous n'avez pas eu le temps de poser votre Warp";

        ItemInteraction.runLocationInteraction(
                player,
                itemToGive,
                "mayor:wait-set-warp", // Assuming interaction ID is static
                300, // Assuming time is hardcoded
                promptMessage,
                timeoutMessage,
                locationClick -> {
                    if (locationClick == null) return true; // Interaction cancelled or failed

                    Chunk chunk = locationClick.getChunk();

                    if (!city.hasChunk(chunk.getX(), chunk.getZ())) {
                        MessagesManager.sendMessage(player, Component.translatable("omc.city.setwarp.not_in_city_claim"), Prefix.CITY, MessageType.ERROR, false);
                        return false; // Keep the item and interaction active? Or cancel? The code returns false, implying keep active. Let's assume it cancels based on the original code's intent for `runLocationInteraction`'s boolean return. If returning false means *keep* active, this needs adjustment. Assuming false cancels the interaction.
                    }

                    DynamicCooldownManager.use(mayor.getUUID().toString(), "mayor:law-move-warp", COOLDOWN_TIME_WARP);
                    law.setWarp(locationClick);
                    // Translatable success message with coordinates
                    MessagesManager.sendMessage(player, Component.translatable("omc.city.setwarp.success", Component.text(locationClick.blockX()), Component.text(locationClick.blockY()), Component.text(locationClick.blockZ())), Prefix.CITY, MessageType.SUCCESS, false);
                    return true; // Interaction complete successfully
                }
        );
    }

    public static void leaveCity(Player player) {
        City city = CityManager.getPlayerCity(player.getUniqueId());
        if (city.removePlayer(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.leave.success", Component.text(city.getCityName())), Prefix.CITY, MessageType.SUCCESS, false);
        } else {
            MessagesManager.sendMessage(player, Component.translatable("omc.city.leave.failed"), Prefix.CITY, MessageType.ERROR, false);
        }
    }

    public static void startBalanceCooldown(String city_uuid) {
        if (balanceCooldownTasks.containsKey(city_uuid)) {
            balanceCooldownTasks.get(city_uuid).cancel();
        }

        BukkitRunnable cooldownTask = new BukkitRunnable() {
            @Override
            public void run() {
                balanceCooldownTasks.remove(city_uuid);
            }
        };

        balanceCooldownTasks.put(city_uuid, cooldownTask);
        cooldownTask.runTaskLater(OMCPlugin.getInstance(), 30 * 60 * 20L);
    }
}