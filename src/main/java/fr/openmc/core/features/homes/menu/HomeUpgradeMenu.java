package fr.openmc.core.features.homes.menu;

import dev.lone.itemsadder.api.CustomStack;
import fr.openmc.api.menulib.Menu;
import fr.openmc.api.menulib.utils.InventorySize;
import fr.openmc.api.menulib.utils.ItemBuilder;
import fr.openmc.core.features.economy.EconomyManager;
import fr.openmc.core.features.homes.HomeLimits;
import fr.openmc.core.features.homes.HomeUpgradeManager;
import fr.openmc.core.features.homes.HomesManager;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import fr.openmc.core.utils.customitems.CustomItemRegistry;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeUpgradeMenu extends Menu {

    private final HomeUpgradeManager homeUpgradeManager;
    private final HomesManager homesManager;

    public HomeUpgradeMenu(Player owner) {
        super(owner);
        this.homeUpgradeManager = HomeUpgradeManager.getInstance();
        this.homesManager = HomesManager.getInstance();
    }

    @Override
    public @NotNull String getName() {
        return PlaceholderAPI.setPlaceholders(this.getOwner(), "§r§f%img_offset_-8%%img_omc_homes_menus_home_upgrade%");
    }

    @Override
    public @NotNull Map<Integer, ItemStack> getContent() {
        Map<Integer, ItemStack> items = new HashMap<>();

        try {
            int currentHome = homesManager.getHomeLimit(getOwner().getUniqueId());

            int homeMaxLimit = HomeLimits.values().length - 1;

            HomeLimits lastUpgrade = HomeLimits.valueOf("LIMIT_" + homeMaxLimit);
            HomeLimits nextUpgrade = homeUpgradeManager.getNextUpgrade(homeUpgradeManager.getCurrentUpgrade(getOwner().getPlayer())) != null
                    ? homeUpgradeManager.getNextUpgrade(homeUpgradeManager.getCurrentUpgrade(getOwner().getPlayer()))
                    : lastUpgrade;

            int finalCurrentHome = currentHome;
            items.put(4, new ItemBuilder(this, CustomItemRegistry.getByName("omc_homes:omc_homes_icon_upgrade").getBest(), itemMeta -> {
                itemMeta.displayName(Component.translatable("omc.homes.menu.upgrade.title"));
                List<Component> lore = new ArrayList<>();

            lore.add(Component.translatable("omc.homes.menu.upgrade.current", Component.text(finalCurrentHome)));

            if (nextUpgrade.getLimit() >= lastUpgrade.getLimit()) {
                lore.add(Component.translatable("omc.homes.menu.upgrade.max_reached"));
                } else {
                    lore.add(Component.translatable(
                        "omc.homes.menu.upgrade.price",
                        Component.text(String.valueOf(nextUpgrade.getPrice())),
                        Component.text(EconomyManager.getEconomyIcon())
                ));
                    lore.add(Component.translatable("omc.homes.menu.upgrade.aywenite", Component.text(nextUpgrade.getAyweniteCost())));
                    lore.add(Component.translatable("omc.homes.menu.upgrade.next_limit", Component.text(nextUpgrade.getLimit())));
                    lore.add(Component.translatable("omc.homes.menu.upgrade.click_to_upgrade"));
                }

                itemMeta.lore(lore);
            }).setOnClick(event -> {
                homeUpgradeManager.upgradeHome(getOwner());
                getOwner().closeInventory();
            }));

            return items;
        } catch (Exception e) {
            MessagesManager.sendMessage(getOwner(), Component.text("§cUne Erreur est survenue, veuillez contacter le Staff"), Prefix.OPENMC, MessageType.ERROR, false);
            getOwner().closeInventory();
            e.printStackTrace();
        }
        return items;
    }

    @Override
    public @NotNull InventorySize getInventorySize() {
        return InventorySize.SMALLEST;
    }

    @Override
    public void onInventoryClick(InventoryClickEvent inventoryClickEvent) {}
}
