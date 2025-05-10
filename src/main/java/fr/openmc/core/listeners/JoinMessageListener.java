package fr.openmc.core.listeners;

import fr.openmc.core.OMCPlugin;
import fr.openmc.core.commands.utils.SpawnManager;
import fr.openmc.core.features.quests.objects.Quest;
import fr.openmc.core.features.scoreboards.TabList;
import fr.openmc.core.features.friend.FriendManager;
import fr.openmc.core.features.quests.QuestsManager;
import fr.openmc.core.utils.LuckPermsAPI;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class JoinMessageListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        MessagesManager.sendMessage(player, Component.translatable("omc.player.join.welcome"), Prefix.OPENMC, MessageType.INFO, false);

        TabList.getInstance().updateTabList(player);

        FriendManager.getInstance().getFriendsAsync(player.getUniqueId()).thenAccept(friendsUUIDS -> {
            for (UUID friendUUID : friendsUUIDS) {
                final Player friend = player.getServer().getPlayer(friendUUID);
                if (friend != null && friend.isOnline()) {
                    MessagesManager.sendMessage(friend, Component.translatable("omc.player.join.friend_joined", LuckPermsAPI.getFormattedPAPIPrefix(player) + player.getName()), Prefix.FRIEND, MessageType.NONE, true);
                }
            }
        }).exceptionally(throwable -> {
            OMCPlugin.getInstance().getLogger().severe("An error occurred while loading friends of " + player.getName() + " : " + throwable.getMessage());
            return null;
        });

        // Quest pending reward notification
        Bukkit.getScheduler().runTaskAsynchronously(OMCPlugin.getInstance(), () -> {
            for (Quest quest : QuestsManager.getInstance().getAllQuests()) {
                if (quest.hasPendingRewards(player.getUniqueId())) {
                    int pendingRewardsNumber = quest.getPendingRewardTiers(player.getUniqueId()).size();
                    Bukkit.getScheduler().runTask(OMCPlugin.getInstance(), () -> {
                        MessagesManager.sendMessage(player,
                                Component.translatable("omc.player.join.quest.pending_rewards", Component.text(pendingRewardsNumber))
                                                .append(Component.translatable("omc.player.join.quest.pending_rewards.click_here"))
                                                .clickEvent(ClickEvent.runCommand("/quest")),
                                Prefix.QUEST,
                                MessageType.INFO,
                                true);
                    });
                    break;
                }
            }
        });

        event.joinMessage(Component.text("§8[§a§l+§8] §r" + "§r" + LuckPermsAPI.getFormattedPAPIPrefix(player) + player.getName()));

        // Adjust player's spawn location
        if (!player.hasPlayedBefore()) player.teleport(SpawnManager.getInstance().getSpawnLocation());


        new BukkitRunnable() {
            @Override
            public void run() {
                TabList.getInstance().updateTabList(player);
            }
        }.runTaskTimer(OMCPlugin.getInstance(), 0L, 100L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        QuestsManager.getInstance().saveQuests(player.getUniqueId());

        FriendManager.getInstance().getFriendsAsync(player.getUniqueId()).thenAccept(friendsUUIDS -> {
            for (UUID friendUUID : friendsUUIDS) {
                final Player friend = player.getServer().getPlayer(friendUUID);
                if (friend != null && friend.isOnline()) {
                    MessagesManager.sendMessage(friend, Component.translatable("omc.player.leave.friend_left", Component.text(LuckPermsAPI.getFormattedPAPIPrefix(player) + player.getName())), Prefix.FRIEND, MessageType.NONE, true);
                }
            }
        }).exceptionally(throwable -> {
            OMCPlugin.getInstance().getLogger().severe("An error occurred while loading friends of " + player.getName() + " : " + throwable.getMessage());
            return null;
        });

        event.quitMessage(Component.text("§8[§c§l-§8] §r" + "§r" + LuckPermsAPI.getFormattedPAPIPrefix(player) + player.getName()));
    }

}
