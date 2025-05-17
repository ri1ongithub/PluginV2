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
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import org.bukkit.scoreboard.Team;
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
    private TabList tabList = null;

    // Team prefix for line identifiers
    private static final String TEAM_PREFIX = "omc_sb_line_";

    public ScoreboardManager() {
        OMCPlugin.registerEvents(this);
        CommandsManager.getHandler().register(this);
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllScoreboards, 0L, 20L * 5); // 20x5 = 5s
        if (LuckPermsAPI.hasLuckPerms()) globalTeamManager = new GlobalTeamManager(playerScoreboards);
        tabList = new TabList();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (disabledPlayers.contains(player.getUniqueId())) return;

        Scoreboard sb = createNewScoreboard(player);
        player.setScoreboard(sb);

        // Update TabList header/footer
        tabList.updateTabList(player);
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

            MessagesManager.sendMessage(player,
                    Component.translatable("omc.scoreboard.enabled").color(NamedTextColor.GREEN),
                    Prefix.OPENMC, MessageType.INFO, true);
        } else {
            disabledPlayers.add(uuid);
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            MessagesManager.sendMessage(player,
                    Component.translatable("omc.scoreboard.disabled").color(NamedTextColor.RED),
                    Prefix.OPENMC, MessageType.INFO, true);
        }
    }

    private Scoreboard createNewScoreboard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective;
        if (canShowLogo) {
            objective = scoreboard.registerNewObjective("sb_aywen", "dummy",
                    Component.text(PlaceholderAPI.setPlaceholders(player, "%img_openmc%")));
        } else {
            objective = scoreboard.registerNewObjective("sb_aywen", "dummy",
                    Component.translatable("omc.scoreboard.title")
                            .decorate(TextDecoration.BOLD)
                            .color(NamedTextColor.LIGHT_PURPLE));
        }
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Register all possible teams for line customization
        for (int i = 0; i < 15; i++) {
            Team team = scoreboard.registerNewTeam(TEAM_PREFIX + i);
            team.addEntry(getEntryForScore(i));
        }

        updateScoreboard(player, scoreboard, objective);
        return scoreboard;
    }

    private void updateAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (disabledPlayers.contains(player.getUniqueId())) continue;

            updateScoreboard(player);
            tabList.updateTabList(player);
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
        // Reset scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        int line = 11; // Starting line

        if (Restart.isRestarting) {
            // Restart message - translatable version
            setLine(scoreboard, objective, line--, Component.text("§7")); // Spacer
            setLine(scoreboard, objective, line--, Component.text("   ")); // Spacer

            Component restartComponent = Component.translatable("omc.scoreboard.restart_countdown",
                    NamedTextColor.RED,
                    Component.text(DateUtils.convertSecondToTime(Restart.remainingTime)));
            setLine(scoreboard, objective, line--, restartComponent);

            setLine(scoreboard, objective, line--, Component.text("   ")); // Spacer
            setLine(scoreboard, objective, line--, Component.translatable("omc.scoreboard.server_address")
                    .color(TextColor.fromHexString("#FF55FF"))); // Server address
            return;
        }

        // Normal scoreboard - translatable version
        setLine(scoreboard, objective, line--, Component.text("§7")); // Spacer

        // Username line
        Component nameLine = Component.translatable("omc.scoreboard.player_name",
                Component.text("§8• §f"),
                Component.text("§7" + player.getName()));
        setLine(scoreboard, objective, line--, nameLine);

        // City line
        City city = CityManager.getPlayerCity(player.getUniqueId());
        Component cityName = city != null ? Component.text(city.getName()) :
                Component.translatable("omc.scoreboard.no_city");
        Component cityLine = Component.translatable("omc.scoreboard.city",
                Component.text("§8• §f"),
                Component.text("§7").append(cityName));
        setLine(scoreboard, objective, line--, cityLine);

        // Balance line
        String balance = EconomyManager.getInstance().getMiniBalance(player.getUniqueId());
        Component balanceLine = Component.text("§8•  §r" + EconomyManager.getEconomyIcon() + " §d" + balance);
        setLine(scoreboard, objective, line--,  balanceLine);

        setLine(scoreboard, objective, line--, Component.text("  ")); // Spacer

        // Location line (only in main world)
        if (player.getWorld().getName().equalsIgnoreCase("world")) {
            City chunkCity = CityManager.getCityFromChunk(player.getChunk().getX(), player.getChunk().getZ());
            Component locationName = (chunkCity != null) ? Component.text(chunkCity.getName()) :
                    Component.translatable("omc.scoreboard.wilderness");

            Component locationLine = Component.translatable("omc.scoreboard.location",
                    Component.text("§8• §f"),
                    Component.text("§7").append(locationName));
            setLine(scoreboard, objective, line--, locationLine);
        }

        // Contest info
        ContestData data = ContestManager.getInstance().data;
        int phase = data.getPhase();
        if (phase != 1) {
            setLine(scoreboard, objective, line--, Component.text(" ")); // Spacer
            setLine(scoreboard, objective, line--, Component.text("§8• §6§lCONTEST!")); // Contest title

            // Camp vs Camp line
            Component contestLine = Component.text("§8• §f"+
                    ChatColor.valueOf(data.getColor1()) + data.getCamp1() + " §8VS " +
                            ChatColor.valueOf(data.getColor2()) + data.getCamp2());
            setLine(scoreboard, objective, line--, contestLine);

            // Contest end timer
            Component endTimeLine = Component.translatable("omc.scoreboard.contest_end_time",
                    Component.text("§8• §f"),
                    Component.text(DateUtils.getTimeUntilNextMonday()));
            setLine(scoreboard, objective, line--, endTimeLine);
        }

        setLine(scoreboard, objective, line--, Component.text("   ")); // Spacer

        // Server address
        Component serverAddressLine = Component.translatable("omc.scoreboard.server_address")
                .color(TextColor.fromHexString("#FF55FF"));
        setLine(scoreboard, objective, line--, serverAddressLine);

        if (LuckPermsAPI.hasLuckPerms() && globalTeamManager != null) globalTeamManager.updatePlayerTeam(player);
    }

    /**
     * Set a line in the scoreboard with the given component
     *
     * @param scoreboard The scoreboard to update
     * @param objective The objective to add the score to
     * @param score The score value (line number)
     * @param component The component to display
     */
    private void setLine(Scoreboard scoreboard, Objective objective, int score, Component component) {
        String entry = getEntryForScore(score);
        objective.getScore(entry).setScore(score);

        Team team = scoreboard.getTeam(TEAM_PREFIX + score);
        if (team != null) {
            team.prefix(Component.text(""));
            team.suffix(component);
        }
    }

    /**
     * Get a unique entry for the given score
     *
     * @param score The score value
     * @return A unique entry string
     */
    private String getEntryForScore(int score) {
        return "§" + (score < 10 ? score : "a§" + (score - 10));
    }
}