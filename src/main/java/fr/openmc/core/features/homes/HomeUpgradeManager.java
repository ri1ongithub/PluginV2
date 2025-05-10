package fr.openmc.core.features.homes;

import fr.openmc.core.features.economy.EconomyManager;
import fr.openmc.core.utils.ItemUtils;
import fr.openmc.core.utils.customitems.CustomItemRegistry;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class HomeUpgradeManager {

    private final HomesManager homesManager;
    @Getter public static HomeUpgradeManager instance;

    public HomeUpgradeManager(HomesManager homesManager) {
        this.homesManager = homesManager;
        this.instance = this;
    }

    public HomeLimits getCurrentUpgrade(Player player) {
        int currentLimit = homesManager.getHomeLimit(player.getUniqueId());
        for (HomeLimits upgrade : HomeLimits.values()) {
            if (upgrade.getLimit() == currentLimit) {
                return upgrade;
            }
        }
        return HomeLimits.LIMIT_0;
    }

    public HomeLimits getNextUpgrade(HomeLimits current) {
        return HomeLimits.values()[current.ordinal() + 1];
    }

    public void upgradeHome(Player player) {
        int currentHomes = homesManager.getHomes(player.getUniqueId()).size();
        int currentUpgrade = homesManager.getHomeLimit(player.getUniqueId());
        HomeLimits nextUpgrade = getNextUpgrade(getCurrentUpgrade(player));
        Material matAywenite = CustomItemRegistry.getByName("omc_items:aywenite").getBest().getType();

        if (nextUpgrade != null) {
            double balance = EconomyManager.getInstance().getBalance(player.getUniqueId());
            int price = nextUpgrade.getPrice();
            int aywenite = nextUpgrade.getAyweniteCost();

            if (currentHomes < currentUpgrade) {
                MessagesManager.sendMessage(
                        player,
                        Component.translatable("omc.homes.upgrade.not_reached_limit"),
                        Prefix.HOME,
                        MessageType.ERROR,
                        true
                );
                return;
            }

            if (balance >= price) {
                if (!ItemUtils.hasEnoughItems(player, matAywenite, aywenite)) {
                    MessagesManager.sendMessage(
                            player,
                            Component.translatable("omc.homes.upgrade.not_enough_aywenite", Component.text(aywenite)),
                            Prefix.HOME,
                            MessageType.ERROR,
                            false
                    );
                    return;
                }

                ItemUtils.removeItemsFromInventory(player, matAywenite, aywenite);
                EconomyManager.getInstance().withdrawBalance(player.getUniqueId(), price);
                homesManager.updateHomeLimit(player.getUniqueId());

                int updatedHomesLimit = homesManager.getHomeLimit(player.getUniqueId());

                MessagesManager.sendMessage(
                        player,
                        Component.translatable("omc.homes.upgrade.success", Component.text(updatedHomesLimit), Component.text(price), Component.text(aywenite)),
                        Prefix.HOME,
                        MessageType.SUCCESS,
                        true
                );
            } else {
                MessagesManager.sendMessage(
                        player,
                        Component.translatable("omc.homes.upgrade.not_enough_money"),
                        Prefix.HOME,
                        MessageType.ERROR,
                        true
                );
            }
        } else {
            MessagesManager.sendMessage(
                    player,
                    Component.translatable("omc.homes.upgrade.max_limit"),
                    Prefix.HOME,
                    MessageType.ERROR,
                    true
            );
        }
    }

}
