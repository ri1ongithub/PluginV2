package fr.openmc.core.features.homes.menu;

import dev.lone.itemsadder.api.CustomStack;
import fr.openmc.api.menulib.Menu;
import fr.openmc.api.menulib.utils.InventorySize;
import fr.openmc.api.menulib.utils.ItemBuilder;
import fr.openmc.core.features.homes.Home;
import fr.openmc.core.features.homes.HomesManager;
import fr.openmc.core.features.homes.utils.HomeUtil;
import fr.openmc.core.utils.customitems.CustomItemRegistry;
import fr.openmc.core.utils.messages.MessageType;
import fr.openmc.core.utils.messages.MessagesManager;
import fr.openmc.core.utils.messages.Prefix;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeDeleteConfirmMenu extends Menu {

    private final Home home;
    private final HomesManager homesManager;

    public HomeDeleteConfirmMenu(Player owner, Home home) {
        super(owner);
        this.home = home;
        this.homesManager = HomesManager.getInstance();
    }

    @Override
    public @NotNull String getName() {
        return PlaceholderAPI.setPlaceholders(this.getOwner(), "§r§f%img_offset_-8%%img_omc_homes_menus_home_delete%");
    }

    @Override
    public @NotNull InventorySize getInventorySize() {
        return InventorySize.SMALLEST;
    }

    @Override
    public @NotNull Map<Integer, ItemStack> getContent() {
        Map<Integer, ItemStack> content = new HashMap<>();
        Player player = getOwner();

        try {
            content.put(2, new ItemBuilder(
                            this,
                            CustomItemRegistry.getByName("omc_homes:omc_homes_icon_bin_red").getBest(),
                            itemMeta -> {
                                itemMeta.displayName(Component.translatable("omc.homes.menu.delete.confirm"));
                                itemMeta.lore(List.of(Component.translatable("omc.homes.menu.delete.confirm.lore")));
                        }
                ).setOnClick(event -> {
                    homesManager.removeHome(home);
                    MessagesManager.sendMessage(
                        player,
                        Component.translatable("omc.homes.menu.delete.success", Component.text(home.getName())),
                        Prefix.HOME, MessageType.SUCCESS, true
                    );
                    player.closeInventory();
                    })
            );

            content.put(4, new ItemBuilder(
                    this,
                    HomeUtil.getHomeIconItem(home),
                    itemMeta -> itemMeta.setDisplayName("§a" + home.getName())
            ));

            content.put(6, new ItemBuilder(
                    this,
                    CustomItemRegistry.getByName("omc_homes:omc_homes_icon_bin").getBest(),
                itemMeta -> itemMeta.displayName(Component.translatable("omc.homes.menu.delete.cancel"))
        ).setBackButton());

            return content;
        } catch (Exception e) {
            MessagesManager.sendMessage(player, Component.translatable("omc.homes.menu.error"), Prefix.OPENMC, MessageType.ERROR, false);
            player.closeInventory();
            e.printStackTrace();
        }
        return content;
    }

    @Override
    public void onInventoryClick(InventoryClickEvent inventoryClickEvent) {}
}
