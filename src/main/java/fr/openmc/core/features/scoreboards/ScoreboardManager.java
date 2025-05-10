package fr.openmc.core.features.scoreboards;

import fr.openmc.core.OMCPlugin;
import fr.openmc.core.CommandsManager;
import fr.openmc.core.commands.utils.Restart;
import fr.openmc.core.features.city.City;
import fr.openmc.core.features.city.CityManager;
import fr.openmc.core.features.contest.ContestData;
import fr.openmc.core.features.contest.managers.ContestManager;
import fr.openmc.core.features.economy.EconomyManager;
import fr.openmc.core.utils.DateUtils;
import fr.openmc.core.utils.LuckPermsAPI;
import fr.openmc.core.utils.PapiAPI;
import fr.openmc.core.utils.customitems.CustomItemRegistry;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ScoreboardManager implements Listener {
    public Set<UUID> disabledPlayers = new HashSet<>();
    public HashMap<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private final boolean canShowLogo = PapiAPI.hasPAPI() && CustomItemRegistry.hasItemsAdder();
    OMCPlugin plugin = OMCPlugin.getInstance();
    private GlobalTeamManager globalTeamManager = null;

    public ScoreboardManager() {
        OMCPlugin.registerEvents(this);
        CommandsManager.getHandler().register(this);
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllScoreboards, 0L, 20L * 5); //20x5 = 5s
        if (LuckPermsAPI.hasLuckPerms()) globalTeamManager = new GlobalTeamManager(playerScoreboards);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (disabledPlayers.contains(player.getUniqueId())) return;

        Scoreboard sb = createNewScoreboard(player);
        player.setScoreboard(sb);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerScoreboards.remove(player.getUniqueId());
    }

    @Command("sb")
    @CommandPermission("omc.commands.scoreboard")
    @Description("Active / désactive le scoreboard")
    public void onScoreboardCommand(Player player) {
        UUID uuid = player.getUniqueId();
        if (disabledPlayers.contains(uuid)) {
            disabledPlayers.remove(uuid);

            playerScoreboards.remove(uuid);
            player.setScoreboard(createNewScoreboard(player));
            updateScoreboard(player);

            MessagesManager.sendMessage(player, Component.text("Scoreboard activé").color(NamedTextColor.GREEN), Prefix.OPENMC, MessageType.INFO, true);
        } else {
            disabledPlayers.add(uuid);
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            MessagesManager.sendMessage(player, Component.text("Scoreboard désactivé").color(NamedTextColor.RED), Prefix.OPENMC, MessageType.INFO, true);
        }
    }

    private Scoreboard createNewScoreboard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective;
        if (canShowLogo) {
            objective = scoreboard.registerNewObjective("sb_aywen", "dummy", PlaceholderAPI.setPlaceholders(player, "%img_openmc%"));
        } else {
            objective = scoreboard.registerNewObjective("sb_aywen", "dummy", Component.text("OPENMC").decorate(TextDecoration.BOLD).color(NamedTextColor.LIGHT_PURPLE));
        }
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        updateScoreboard(player, scoreboard, objective);
        return scoreboard;
    }

    private void updateAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (disabledPlayers.contains(player.getUniqueId())) continue;

            updateScoreboard(player);
        }
    }

    private void updateScoreboard(Player player) {
        playerScoreboards.computeIfAbsent(player.getUniqueId(), (uuid) -> {
            Scoreboard sb = createNewScoreboard(player);
            player.setScoreboard(sb);
            return sb;
        });

        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) return;

        Objective objective = scoreboard.getObjective("sb_aywen");
        if (objective == null) return;

        updateScoreboard(player, scoreboard, objective);
    }

    private void updateScoreboard(Player player, Scoreboard scoreboard, Objective objective) {
        /*
         * 07 |
         * 06 | Username
         * 05 | City name
         * 04 | Argent
         * 03 |
         * 02 | Nom territoire
         * 01 |
         * 00 | ip
         */

        // TODO : Traduire le sb en récupérent la langue du joueur avec player.locale();

        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }


        if (Restart.isRestarting) {
            objective.getScore("§7").setScore(3);
            objective.getScore("   ").setScore(2);
            objective.getScore("§cRedémarrage dans " + DateUtils.convertSecondToTime(Restart.remainingTime)).setScore(2);
            objective.getScore("   ").setScore(1);
            objective.getScore("§d      ᴘʟᴀʏ.ᴏᴘᴇɴᴍᴄ.ꜰʀ").setScore(0);
            return;
        }

        objective.getScore("§7").setScore(11);
        
        objective.getScore("§8• §fNom: §7"+player.getName()).setScore(10);

        City city = CityManager.getPlayerCity(player.getUniqueId());
        String cityName = city != null ? city.getName() : "Aucune";
        objective.getScore("§8• §fVille§7: "+cityName).setScore(9);

        String balance = EconomyManager.getInstance().getMiniBalance(player.getUniqueId());
        objective.getScore("§8• §r"+EconomyManager.getEconomyIcon()+" §d"+balance).setScore(8);

        objective.getScore("  ").setScore(7);

        if (player.getWorld().getName().equalsIgnoreCase("world")) {
            City chunkCity = CityManager.getCityFromChunk(player.getChunk().getX(), player.getChunk().getZ());
            String chunkCityName = (chunkCity != null) ? chunkCity.getName() : "Nature";
            objective.getScore("§8• §fLocation§7: " + chunkCityName).setScore(6);
        }

        ContestData data = ContestManager.getInstance().data;
        int phase = data.getPhase();
        if(phase != 1) {
            objective.getScore(" ").setScore(5);
            objective.getScore("§8• §6§lCONTEST!").setScore(4);
            objective.getScore(ChatColor.valueOf(data.getColor1()) + data.getCamp1() + " §8VS " + ChatColor.valueOf(data.getColor2()) + data.getCamp2()).setScore(3);
            objective.getScore("§cFin dans " + DateUtils.getTimeUntilNextMonday()).setScore(2);
        }

        objective.getScore("   ").setScore(1);
        objective.getScore("§d      ᴘʟᴀʏ.ᴏᴘᴇɴᴍᴄ.ꜰʀ").setScore(0);

        if (LuckPermsAPI.hasLuckPerms() && globalTeamManager != null) globalTeamManager.updatePlayerTeam(player);
    }
}
