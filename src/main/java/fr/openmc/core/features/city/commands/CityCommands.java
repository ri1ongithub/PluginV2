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
import net.kyori.adventure.text.format.TextDecoration;
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
            MessagesManager.sendMessage(player, Component.text("Vous ne pouvez pas ouvrir le menu des villes si vous devez poser votre mascotte"), Prefix.CITY, MessageType.ERROR, false);
        }
    }

    @Subcommand({"mayor", "maire"})
    @CommandPermission("omc.commands.city.mayor")
    @Description("Ouvre le menu des maires")
    public void mayor(Player sender) {
        if (MayorManager.getInstance().phaseMayor==1) {
            MayorElectionMenu menu = new MayorElectionMenu(sender);
            menu.open();
        } else {
            MayorMandateMenu menu = new MayorMandateMenu(sender);
            menu.open();
        }
    }

    @Subcommand("accept")
    @CommandPermission("omc.commands.city.accept")
    @Description("Accepter une invitation")
    public static void acceptInvitation(Player player, Player inviter) {
        List<Player> playerInvitations = invitations.get(player);
        if (!playerInvitations.contains(inviter)) {
            MessagesManager.sendMessage(player, Component.text(inviter.getName() + " ne vous a pas invité"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        City newCity = CityManager.getPlayerCity(inviter.getUniqueId());

        if (!CityInviteConditions.canCityInviteAccept(newCity, inviter, player)) return;

        newCity.addPlayer(player.getUniqueId());

        invitations.remove(player);

        MessagesManager.sendMessage(player, Component.text("Tu as rejoint "+ newCity.getName()), Prefix.CITY, MessageType.SUCCESS, false);
        if (inviter.isOnline()) {
            MessagesManager.sendMessage(inviter, Component.text(player.getName()+" a accepté ton invitation !"), Prefix.CITY, MessageType.SUCCESS, true);
        }
    }

    @Subcommand("rename")
    @CommandPermission("omc.commands.city.rename")
    @Description("Renommer une ville")
    void rename(Player player, @Named("nouveau nom") String name) {
        City playerCity = CityManager.getPlayerCity(player.getUniqueId());

        if (!CityManageConditions.canCityRename(playerCity, player)) return;

        if (!InputUtils.isInputCityName(name)) {
            MessagesManager.sendMessage(player, Component.text("Le nom de ville est invalide, il doit seulement comporter des caractères alphanumeriques et maximum 24 caractères."), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        playerCity.renameCity(name);
        MessagesManager.sendMessage(player, Component.text("La ville a été renommée en " + name), Prefix.CITY, MessageType.SUCCESS, false);
    }

    @Subcommand("transfer")
    @CommandPermission("omc.commands.city.transfer")
    @Description("Transfert la propriété de votre ville")
    @AutoComplete("@city_members")
    void transfer(Player sender, @Named("maire") OfflinePlayer player) {
        City playerCity = CityManager.getPlayerCity(sender.getUniqueId());

        if (!CityManageConditions.canCityTransfer(playerCity, sender)) return;

        playerCity.changeOwner(player.getUniqueId());
        MessagesManager.sendMessage(sender, Component.text("Le nouveau maire est "+player.getName()), Prefix.CITY, MessageType.SUCCESS, false);

        if (player.isOnline()) {
            MessagesManager.sendMessage((Player) player, Component.text("Vous êtes devenu le maire de la ville"), Prefix.CITY, MessageType.INFO, true);
        }
    }

    @Subcommand("deny")
    @CommandPermission("omc.commands.city.deny")
    @Description("Refuser une invitation")
    public static void denyInvitation(Player player, Player inviter) {
        if (!CityInviteConditions.canCityInviteDeny(player, inviter)) return;

        invitations.remove(player);

        if (inviter.isOnline()) {
            MessagesManager.sendMessage(inviter, Component.text(player.getName()+" a refusé ton invitation"), Prefix.CITY, MessageType.WARNING, true);
        }
    }

    @Subcommand("kick")
    @CommandPermission("omc.commands.city.kick")
    @Description("Exclure un habitant de votre ville")
    @AutoComplete("@city_members")
    public static void kick(Player sender, @Named("exclu") OfflinePlayer player) {
        City city = CityManager.getPlayerCity(sender.getUniqueId());

        if (!CityKickCondition.canCityKickPlayer(city, sender, player)) return;

        if (city.removePlayer(player.getUniqueId())) {
            MessagesManager.sendMessage(sender, Component.text("Tu as exclu "+player.getName()+" de la ville "+ city.getCityName()), Prefix.CITY, MessageType.SUCCESS, false);

            if (player.isOnline()) {
                MessagesManager.sendMessage((Player) player, Component.text("Tu as été exclu de la ville "+ city.getCityName()), Prefix.CITY, MessageType.INFO, true);
            }
        } else {
            MessagesManager.sendMessage(sender, Component.text("Impossible d'exclure "+player.getName()+" de la ville"), Prefix.CITY, MessageType.ERROR, false);
        }
    }

    @Subcommand("leave")
    @CommandPermission("omc.commands.city.leave")
    @Description("Quitter votre ville")
    void leave(Player player) {
        City city = CityManager.getPlayerCity(player.getUniqueId());
        if (!CityLeaveCondition.canCityLeave(city, player)) return;

        leaveCity(player);
    }

    @Subcommand("invite")
    @CommandPermission("omc.commands.city.invite")
    @Description("Inviter un joueur dans votre ville")
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
        MessagesManager.sendMessage(sender, Component.text("Tu as invité "+target.getName()+" dans ta ville"), Prefix.CITY, MessageType.SUCCESS, false);
        MessagesManager.sendMessage(target,
                Component.text("Tu as été invité(e) par " + sender.getName() + " dans la ville " + city.getCityName() + "\n")
                        .append(Component.text("§8Faite §a/city accept §8pour accepter\n").clickEvent(ClickEvent.runCommand("/city accept " + sender.getName())).hoverEvent(HoverEvent.showText(Component.text("Accepter l'invitation"))))
                        .append(Component.text("§8Faite §c/city deny §8pour refuser\n").clickEvent(ClickEvent.runCommand("/city deny " + sender.getName())).hoverEvent(HoverEvent.showText(Component.text("Refuser l'invitation")))),
                Prefix.CITY, MessageType.INFO, false);
    }

    @Subcommand("delete")
    @CommandPermission("omc.commands.city.delete")
    @Description("Supprimer votre ville")
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
                        Component.text("§cEs-tu sûr de vouloir supprimer ta ville ?"),
                        Component.text("§cCette action est §4§lIRREVERSIBLE")
                ),
                List.of(
                        Component.text("§7Ne pas supprimer la ville")
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
        MessagesManager.sendMessage(sender, Component.text("Votre ville a été supprimée"), Prefix.CITY, MessageType.SUCCESS, false);

        DynamicCooldownManager.use(uuid.toString(), "city:big", 60000); //1 minute
    }

    @Subcommand("claim")
    @CommandPermission("omc.commands.city.claim")
    @Description("Claim un chunk pour votre ville")
    void claim(Player sender) {
        City city = CityManager.getPlayerCity(sender.getUniqueId());

        if (!CityClaimCondition.canCityClaim(city, sender)) return;

        Chunk chunk = sender.getLocation().getChunk();

        claim(sender, chunk.getX(), chunk.getZ());
    }

    public static void claim(Player sender, int chunkX, int chunkZ) {
        City city = CityManager.getPlayerCity(sender.getUniqueId());
        org.bukkit.World bWorld = sender.getWorld();
        if (!bWorld.getName().equals("world")) {
            MessagesManager.sendMessage(sender, Component.text("Tu ne peux pas étendre ta ville ici"), Prefix.CITY, MessageType.ERROR, false);
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
            MessagesManager.sendMessage(sender, Component.text("Ce chunk n'est pas adjacent à ta ville"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        Chunk chunk = sender.getWorld().getChunkAt(chunkX, chunkZ);
        if (WorldGuardApi.doesChunkContainWGRegion(chunk)) {
            MessagesManager.sendMessage(sender, Component.text("Ce chunk est dans une région protégée"), Prefix.CITY, MessageType.ERROR, true);
            return;
        }

        if (CityManager.isChunkClaimed(chunkX, chunkZ)) {
            City chunkCity = CityManager.getCityFromChunk(chunkX, chunkZ);
            String cityName = chunkCity.getCityName();
            MessagesManager.sendMessage(sender, Component.text("Ce chunk est déjà claim par " + cityName + "."), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (city.getChunks().size() >= 50) {
            MessagesManager.sendMessage(sender, Component.text("Ta ville est trop grande"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        int price = calculatePrice(city.getChunks().size());
        int aywenite = calculateAywenite(city.getChunks().size());



        if ((!CityManager.freeClaim.containsKey(city.getUUID())) || (CityManager.freeClaim.get(city.getUUID()) <= 0)) {
            if (city.getBalance() < price) {
                MessagesManager.sendMessage(sender, Component.text("Ta ville n'a pas assez d'argent ("+price+EconomyManager.getEconomyIcon()+" nécessaires)"), Prefix.CITY, MessageType.ERROR, false);
                return;
            }

            if (!ItemUtils.hasEnoughItems(sender.getPlayer(), ayweniteItemStack.getType(), aywenite )) {
                MessagesManager.sendMessage(sender, Component.text("Vous n'avez pas assez d'§dAywenite §f("+aywenite+ " nécessaires)"), Prefix.CITY, MessageType.ERROR, false);
                return;
            }

            city.updateBalance((double) (price*-1));
            ItemUtils.removeItemsFromInventory(sender, ayweniteItemStack.getType(), aywenite);
        } else {
            CityManager.freeClaim.replace(city.getUUID(), CityManager.freeClaim.get(city.getUUID()) - 1);
        }

        city.addChunk(chunk);

        MessagesManager.sendMessage(sender, Component.text("Ta ville a été étendue"), Prefix.CITY, MessageType.SUCCESS, false);
    }

    @Subcommand("info")
    @CommandPermission("omc.commands.city.info")
    @Description("Avoir des informations sur votre ville")
    void info(Player player) {
        City city = CityManager.getPlayerCity(player.getUniqueId());

        if (city == null) {
            MessagesManager.sendMessage(player, MessagesManager.Message.PLAYERNOCITY.getMessage(), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        CityMessages.sendInfo(player, city);
    }

    @Subcommand("create")
    @CommandPermission("omc.commands.city.create")
    @Description("Créer une ville")
    @DynamicCooldown(group="city:big", message = "§cTu dois attendre avant de pouvoir créer une ville (%sec% secondes)")
    void create(Player player, @Named("nom") String name) {
        if (!CityCreateConditions.canCityCreate(player)){
            MessagesManager.sendMessage(player, MessagesManager.Message.NOPERMISSION.getMessage(), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        for (City city : CityManager.getCities()){
            String cityName = city.getCityName();
            if (cityName!=null && cityName.equalsIgnoreCase(name)){
                MessagesManager.sendMessage(player, Component.text("§cUne ville possédant ce nom existe déjà"), Prefix.CITY, MessageType.INFO, false);
                return;
            }
        }

        if (!InputUtils.isInputCityName(name)) {
            MessagesManager.sendMessage(player, Component.text("Le nom de ville est invalide, il doit contenir seulement des caractères alphanumerique et doit faire moins de 24 charactères"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (MascotsListener.futurCreateCity.containsKey(player.getUniqueId())){
            MessagesManager.sendMessage(player, Component.text("Vous êtes déjà entrain de créer une ville"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        new CityTypeMenu(player, name).open();
    }

    @Subcommand("list")
    @CommandPermission("omc.commands.city.list")
    public void list(Player player) {
        List<City> cities = new ArrayList<>(CityManager.getCities());
        if (cities.isEmpty()) {
            MessagesManager.sendMessage(player, Component.text("Aucune ville n'existe"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        CityListMenu menu = new CityListMenu(player, cities);
        menu.open();
    }

    @Subcommand("change")
    @CommandPermission("omc.commands.city.change")
    public void change(Player sender) {
        City city = CityManager.getPlayerCity(sender.getUniqueId());

        if (!CityTypeConditions.canCityChangeType(city, sender, true)){
            MessagesManager.sendMessage(sender, MessagesManager.Message.NOPERMISSION.getMessage(), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        String cityTypeActuel = getCityType(city.getUUID());
        String cityTypeAfter = "";
        if (cityTypeActuel != null) {
            cityTypeActuel = cityTypeActuel.equals("war") ? "§cen guerre§7" : "§aen paix§7";
            cityTypeAfter = cityTypeActuel.equals("war") ? "§aen paix§7" : "§cen guerre§7";
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
                        Component.text("§cEs-tu sûr de vouloir changer le type de ta §dville §7?"),
                        Component.text("§7Vous allez passez d'une §dville " + cityTypeActuel + " à une §dville " + cityTypeAfter),
                        Component.text("§cSi tu fais cela ta mascotte §4§lPERDERA 2 NIVEAUX")
                ),
                List.of(
                        Component.text("§7Ne pas changer le type de ta §dville")
                )
        );
        menu.open();

    }

    public static void changeConfirm(Player sender) {
        City city = CityManager.getPlayerCity(sender.getUniqueId());

        if (!CityTypeConditions.canCityChangeType(city, sender, true)){
            MessagesManager.sendMessage(sender, MessagesManager.Message.NOPERMISSION.getMessage(), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        if (MascotUtils.mascotsContains(city.getUUID())) {
            if (!MascotUtils.getMascotState(city.getUUID())) {
                MessagesManager.sendMessage(sender, Component.text("Vous devez soigner votre mascotte avant"), Prefix.CITY, MessageType.ERROR, false);
                return;
            }
        }
        if (!DynamicCooldownManager.isReady(city.getUUID(), "city:type")) {
            MessagesManager.sendMessage(sender, Component.text("Vous devez attendre " + DateUtils.convertMillisToTime(DynamicCooldownManager.getRemaining(city.getUUID(), "city:type")) + " secondes pour changer de type de ville"), Prefix.CITY, MessageType.ERROR, false);
            return;
        }
        CityManager.changeCityType(city.getUUID());
        DynamicCooldownManager.use(city.getUUID(), "city:type", 5 * 24 * 60 * 60 * 1000L); // 5 jours en ms


        Mascot mascot = MascotUtils.getMascotOfCity(city.getUUID());

        if (mascot != null) {
            LivingEntity mob = MascotUtils.loadMascot(mascot);
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
                mob.setCustomName("§l" + city.getName() + " §c" + currentHealth + "/" + maxHealth + "❤");
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            String cityTypeActuel = getCityType(city.getUUID());
            String cityTypeAfter = "";
            if (cityTypeActuel != null) {
                cityTypeActuel = cityTypeActuel.equals("war") ? "§cen guerre§7" : "§aen paix§7";
                cityTypeAfter = cityTypeActuel.equals("war") ? "§aen paix§7" : "§cen guerre§7";
            }

            MessagesManager.sendMessage(sender, Component.text("Vous avez changé le type de votre ville de " + cityTypeActuel + " à " + cityTypeAfter), Prefix.CITY, MessageType.SUCCESS, false);

        }

        MessagesManager.sendMessage(sender, Component.text("Vous avez bien changé le §5type §fde votre §dville"), Prefix.CITY, MessageType.SUCCESS, false);
    }

    @Subcommand({"setwarp"})
    @Description("Déplacer le warp de votre ville")
    public void setWarpCommand(Player player) {
        setWarp(player);
    }

    @Subcommand({"warp"})
    @Description("Teleporte au warp commun de la ville")
    public void warp(Player player) {
        City playerCity = CityManager.getPlayerCity(player.getUniqueId());

        if (playerCity == null) return;

        CityLaw law = playerCity.getLaw();
        Location warp = law.getWarp();

        if (warp == null) {
            if (MayorManager.getInstance().phaseMayor == 2) {
                MessagesManager.sendMessage(player, Component.text("Le Warp de la Ville n'est pas encore défini ! Demandez au §6Maire §fActuel d'en mettre un ! §8§o*via /city setwarp ou avec le Menu des Lois*"), Prefix.CITY, MessageType.INFO, true);
                return;
            }
            MessagesManager.sendMessage(player, Component.text("Le Warp de la Ville n'est pas encore défini ! Vous devez attendre que un Maire soit élu pour mettre un Warp"), Prefix.CITY, MessageType.INFO, true);
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendTitle(PlaceholderAPI.setPlaceholders(player, "§0%img_tp_effect%"), "§a§lTéléportation...", 20, 10, 10);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.teleport(warp);
                        MessagesManager.sendMessage(player, Component.text("Vous avez été envoyé au Warp §fde votre §dVille"), Prefix.CITY, MessageType.SUCCESS, true);
                    }
                }.runTaskLater(OMCPlugin.getInstance(), 10);
            }
        }.runTaskLater(OMCPlugin.getInstance(), 15);
    }

    // making the subcommand only "bank" overrides "bank deposit" and "bank withdraw"
    @Subcommand({"bank view"})
    @Description("Ouvre le menu de la banque de ville")
    public void bank(Player player) {
        if (CityManager.getPlayerCity(player.getUniqueId()) == null)
            return;

        new CityBankMenu(player).open();
    }

    @Subcommand("bank deposit")
    @Description("Met de votre argent dans la banque de ville")
    void deposit(Player player, @Range(min=1) String input) {
        City city = CityManager.getPlayerCity(player.getUniqueId());

        if (!CityBankConditions.canCityDeposit(city, player)) return;

        city.depositCityBank(player, input);
    }

    @Subcommand("bank withdraw")
    @Description("Prend de l'argent de la banque de ville")
    void withdraw(Player player, @Range(min=1) String input) {
        City city = CityManager.getPlayerCity(player.getUniqueId());

        if (!city.hasPermission(player.getUniqueId(), CPermission.MONEY_TAKE)) {
            MessagesManager.sendMessage(player, MessagesManager.Message.PLAYERNOMONEYTAKE.getMessage(), Prefix.CITY, MessageType.ERROR, false);
            return;
        }

        city.withdrawCityBank(player, input);
    }

    // ACTIONS

    public static boolean createCity(Player player, String name, String type, Chunk origin) {

        if (!CityCreateConditions.canCityCreate(player)){
            MessagesManager.sendMessage(player, MessagesManager.Message.NOPERMISSION.getMessage(), Prefix.CITY, MessageType.ERROR, false);
            return false;
        }

        UUID uuid = player.getUniqueId();

        String cityUUID = UUID.randomUUID().toString().substring(0, 8);

        AtomicBoolean isClaimed = new AtomicBoolean(false);

        if (WorldGuardApi.doesChunkContainWGRegion(origin)) {
            MessagesManager.sendMessage(player, Component.text("Ce chunk est dans une région protégée"), Prefix.CITY, MessageType.ERROR, false);
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
            MessagesManager.sendMessage(player, Component.text("Une des parcelles autour de ce chunk est claim! "), Prefix.CITY, MessageType.ERROR, false);
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
                MessagesManager.sendMessage(player, Component.text("Une erreur est survenue, réessayez plus tard"), Prefix.CITY, MessageType.ERROR, false);
                throw new RuntimeException(e);
            }
        });

        if (EconomyManager.getInstance().getBalance(player.getUniqueId()) < MONEY_CREATE) {
            MessagesManager.sendMessage(player, Component.text("§cTu n'as pas assez d'Argent pour créer ta ville (" + MONEY_CREATE).append(Component.text(EconomyManager.getEconomyIcon() +" §cnécessaires)")).decoration(TextDecoration.ITALIC, false), Prefix.CITY, MessageType.ERROR, false);
        }

        if (!ItemUtils.hasEnoughItems(player, Objects.requireNonNull(CustomItemRegistry.getByName("omc_items:aywenite")).getBest().getType(), AYWENITE_CREATE)) {
            MessagesManager.sendMessage(player, Component.text("§cTu n'as pas assez d'§dAywenite §cpour créer ta ville (" + AYWENITE_CREATE +" nécessaires)"), Prefix.CITY, MessageType.ERROR, false);
        }

        EconomyManager.getInstance().withdrawBalance(player.getUniqueId(), MONEY_CREATE);
        ItemUtils.removeItemsFromInventory(player, ayweniteItemStack.getType(), AYWENITE_CREATE);

        City city = CityManager.createCity(player, cityUUID, name, type);
        city.addPlayer(uuid);
        city.addPermission(uuid, CPermission.OWNER);

        CityManager.claimedChunks.put(BlockVector2.at(origin.getX(), origin.getZ()), city);
        CityManager.freeClaim.put(cityUUID, 15);

        player.closeInventory();

        // SETUP MAIRE
        MayorManager mayorManager = MayorManager.getInstance();
        if (mayorManager.phaseMayor == 1) { // si création pendant le choix des maires
            mayorManager.createMayor(null, null, city, null, null, null, null, ElectionType.OWNER_CHOOSE);
        } else { // si création pendant les réformes actives
            NamedTextColor color = mayorManager.getRandomMayorColor();
            List<Perks> perks = PerkManager.getRandomPerksAll();
            mayorManager.createMayor(player.getName(), player.getUniqueId(), city, perks.getFirst(), perks.get(1), perks.get(2), color, ElectionType.OWNER_CHOOSE);
            MessagesManager.sendMessage(player, Component.text("Vous avez été désigné comme §6Maire de la Ville.\n§8§oVous pourrez choisir vos Réformes dans " + DateUtils.getTimeUntilNextDay(PHASE_1_DAY)), Prefix.MAYOR, MessageType.SUCCESS, true);
        }

        // SETUP LAW
        MayorManager.createCityLaws(city, false, null);

        MessagesManager.sendMessage(player, Component.text("Votre ville a été créée : " + name), Prefix.CITY, MessageType.SUCCESS, true);
        MessagesManager.sendMessage(player, Component.text("Vous disposez de 15 claims gratuits"), Prefix.CITY, MessageType.SUCCESS, false);

        DynamicCooldownManager.use(uuid.toString(), "city:big", 60000); //1 minute

        return true;
    }

    public static void setWarp(Player player) {
        City city = CityManager.getPlayerCity(player.getUniqueId());

        if (city == null) return;

        Mayor mayor = city.getMayor();

        if (mayor == null) return;

        if (!player.getUniqueId().equals(mayor.getUUID())) {
            MessagesManager.sendMessage(player, Component.text("Vous n'êtes pas le Maire de la ville"), Prefix.MAYOR, MessageType.ERROR, false);
            return;
        }

        if (!DynamicCooldownManager.isReady(mayor.getUUID().toString(), "mayor:law-move-warp")) {
            return;
        }
        CityLaw law = city.getLaw();

        List<Component> loreItemInterraction = List.of(
                Component.text("§7Cliquez sur l'endroit où vous voulez mettre le §9Warp")
        );
        ItemStack itemToGive = CustomItemRegistry.getByName("omc_items:warp_stick").getBest();
        ItemMeta itemMeta = itemToGive.getItemMeta();

        itemMeta.displayName(Component.text("§7Séléction du §9Warp"));
        itemMeta.lore(loreItemInterraction);
        itemToGive.setItemMeta(itemMeta);
        ItemInteraction.runLocationInteraction(
                player,
                itemToGive,
                "mayor:wait-set-warp",
                300,
                "§7Vous avez 300s pour séléctionner votre point de spawn",
                "§7Vous n'avez pas eu le temps de poser votre Warp",
                locationClick -> {
                    if (locationClick == null) return true;
                    Chunk chunk = locationClick.getChunk();

                    if (!city.hasChunk(chunk.getX(), chunk.getZ())) {
                        MessagesManager.sendMessage(player, Component.text("§cImpossible de mettre le Warp ici car ce n'est pas dans votre ville"), Prefix.CITY, MessageType.ERROR, false);
                        return false;
                    }

                    DynamicCooldownManager.use(mayor.getUUID().toString(), "mayor:law-move-warp", COOLDOWN_TIME_WARP);
                    law.setWarp(locationClick);
                    MessagesManager.sendMessage(player, Component.text("Vous venez de mettre le §9warp de votre ville §fen : \n §8- §fx=§6" + locationClick.x() + "\n §8- §fy=§6" + locationClick.y() + "\n §8- §fz=§6" + locationClick.z()), Prefix.CITY, MessageType.SUCCESS, false);
                    return true;
                }
        );
    }

    public static void leaveCity(Player player) {
        City city = CityManager.getPlayerCity(player.getUniqueId());
        if (city.removePlayer(player.getUniqueId())) {
            MessagesManager.sendMessage(player, Component.text("Tu as quitté "+ city.getCityName()), Prefix.CITY, MessageType.SUCCESS, false);
        } else {
            MessagesManager.sendMessage(player, Component.text("Impossible de quitter la ville"), Prefix.CITY, MessageType.ERROR, false);
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
